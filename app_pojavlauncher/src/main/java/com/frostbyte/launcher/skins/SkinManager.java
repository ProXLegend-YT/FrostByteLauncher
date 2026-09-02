package com.frostbyte.launcher.skins;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.frostbyte.launcher.Tools;
import com.frostbyte.launcher.authenticator.AuthType;
import com.frostbyte.launcher.authenticator.accounts.Account;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles everything related to skins and capes:
 * - A small bundled preset gallery (assets/frostbyte_skins/)
 * - Looking up any player's real current skin by username (Mojang's public API, no auth needed)
 * - For Microsoft accounts: uploading a chosen skin to the real Mojang skin service, so it
 *   shows up on any server or launcher, and showing/hiding the account's owned cape(s).
 * - For offline/local accounts: applying a skin locally only, since there is no real Mojang
 *   profile to attach it to. This will render in FrostByte's own UI, but other players on a
 *   multiplayer server will not see it, as that requires a real Microsoft account's session data.
 */
public class SkinManager {

    private static final String MOJANG_UUID_LOOKUP = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String MOJANG_PROFILE_LOOKUP = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private static final String MC_SERVICES_SKIN_URL = "https://api.minecraftservices.com/minecraft/profile/skins";
    private static final String MC_SERVICES_SKIN_RESET_URL = "https://api.minecraftservices.com/minecraft/profile/skins/active";
    private static final String MC_SERVICES_CAPE_URL = "https://api.minecraftservices.com/minecraft/profile/capes/active";

    /**
     * Only real Microsoft/Mojang accounts have a profile on Mojang's skin service. Ely.by
     * (and any other third-party auth) has its own accessToken and isn't "local" by
     * Account.isLocal()'s definition, but it still isn't a Mojang account — sending its
     * token to Mojang's API gets rejected with an HTTP 401. Every Mojang-touching call in
     * this class should check this instead of isLocal().
     */
    private static boolean isMicrosoft(Account account) {
        return account.authType == AuthType.MICROSOFT;
    }

    /**
     * Downloads the account's actual current full skin bitmap (not just the small face icon),
     * for showing in the 3D model preview. Must be called off the main thread.
     */
    public static Bitmap fetchCurrentSkinBitmap(Account account) throws IOException {
        if (!isMicrosoft(account) && account.authType.skinUrl == null) {
            // Offline accounts with no real skin source fall back to whatever was applied locally
            Bitmap local = getLocalSkin(account);
            if (local != null) return local;
            throw new IOException("No skin source available for this account");
        }
        if (!isMicrosoft(account)) {
            String url = String.format(account.authType.skinUrl, account.username);
            byte[] bytes = downloadBytes(url);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
        // Microsoft accounts: look up the real current skin via the session server, same
        // mechanism as looking up any other player's skin by username.
        SkinEntry entry = lookupByUsername(account.username);
        if (entry == null) {
            Bitmap local = getLocalSkin(account);
            if (local != null) return local;
            throw new IOException("Could not find a current skin for this account");
        }
        return downloadBytesAsBitmap(entry.reference);
    }

    private static Bitmap downloadBytesAsBitmap(String url) throws IOException {
        byte[] bytes = downloadBytes(url);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /** Look up a real Minecraft player's current skin by username, using Mojang's own
     * public, unauthenticated API. Works for any real Java Edition account.
     * Must be called off the main thread.
     */
    public static SkinEntry lookupByUsername(String username) throws IOException {
        String uuid = fetchUuidForUsername(username);
        if (uuid == null) return null;
        return fetchSkinForUuid(uuid, username);
    }

    private static String fetchUuidForUsername(String username) throws IOException {
        URL url = new URL(MOJANG_UUID_LOOKUP + username);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setUseCaches(false);
        conn.connect();
        int code = conn.getResponseCode();
        if (code == 204 || code == 404) return null;
        if (code < 200 || code >= 300) {
            throw new IOException("Lookup failed with HTTP " + code);
        }
        String body = Tools.read(conn.getInputStream());
        conn.disconnect();
        try {
            JSONObject json = new JSONObject(body);
            return json.getString("id");
        } catch (Exception e) {
            throw new IOException("Malformed response from Mojang", e);
        }
    }

    private static SkinEntry fetchSkinForUuid(String uuid, String displayName) throws IOException {
        URL url = new URL(MOJANG_PROFILE_LOOKUP + uuid);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setUseCaches(false);
        conn.connect();
        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IOException("Profile lookup failed with HTTP " + code);
        }
        String body = Tools.read(conn.getInputStream());
        conn.disconnect();
        try {
            JSONObject json = new JSONObject(body);
            JSONArray properties = json.getJSONArray("properties");
            for (int i = 0; i < properties.length(); i++) {
                JSONObject prop = properties.getJSONObject(i);
                if (!"textures".equals(prop.getString("name"))) continue;
                String decoded = new String(Base64.decode(prop.getString("value"), Base64.DEFAULT), StandardCharsets.UTF_8);
                JSONObject texturesRoot = new JSONObject(decoded).getJSONObject("textures");
                if (!texturesRoot.has("SKIN")) return null;
                JSONObject skinObj = texturesRoot.getJSONObject("SKIN");
                String skinUrl = skinObj.getString("url");
                boolean isSlim = skinObj.has("metadata")
                        && "slim".equals(skinObj.getJSONObject("metadata").optString("model"));
                return new SkinEntry(SkinEntry.Source.REMOTE_URL, displayName, skinUrl, isSlim);
            }
            return null;
        } catch (Exception e) {
            throw new IOException("Malformed profile response from Mojang", e);
        }
    }

    /**
     * Applies a chosen skin.
     * - Microsoft accounts: uploads the actual image bytes to Mojang's skin service, so the
     *   skin shows on any server or launcher from now on.
     * - Local/offline accounts: saves the skin locally only, for FrostByte's own rendering.
     * Must be called off the main thread.
     */
    public static void applySkin(android.content.Context ctx, Account account, SkinEntry entry, boolean isSlimModel) throws IOException {
        Bitmap bitmap = loadBitmap(ctx, entry);
        if (bitmap == null) throw new IOException("Could not decode the selected skin image");
        try {
            if (isMicrosoft(account)) {
                uploadSkinToMojang(account.accessToken, bitmap, isSlimModel);
            } else {
                saveLocalSkin(account, bitmap);
            }
        } finally {
            bitmap.recycle();
        }
    }

    /** Loads the raw skin bitmap for preview purposes (grid thumbnails, full-body preview). */
    public static Bitmap loadPreviewBitmap(android.content.Context ctx, SkinEntry entry) throws IOException {
        return loadBitmap(ctx, entry);
    }

    private static Bitmap loadBitmap(android.content.Context ctx, SkinEntry entry) throws IOException {
        switch (entry.source) {
            case BUNDLED_ASSET:
                try (InputStream is = ctx.getAssets().open(entry.reference)) {
                    return BitmapFactory.decodeStream(is);
                }
            case REMOTE_URL: {
                byte[] bytes = downloadBytes(entry.reference);
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            }
            case LOCAL_FILE:
                return BitmapFactory.decodeFile(entry.reference);
        }
        return null;
    }

    private static byte[] downloadBytes(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setUseCaches(false);
        conn.connect();
        try (InputStream is = conn.getInputStream(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }
            return bos.toByteArray();
        } finally {
            conn.disconnect();
        }
    }

    /** Uploads raw skin bytes to Mojang's real skin service. Requires a live Microsoft access token. */
    private static void uploadSkinToMojang(String accessToken, Bitmap skinBitmap, boolean isSlimModel) throws IOException {
        String boundary = "FrostByteBoundary" + System.currentTimeMillis();

        ByteArrayOutputStream pngBytesStream = new ByteArrayOutputStream();
        skinBitmap.compress(Bitmap.CompressFormat.PNG, 100, pngBytesStream);
        byte[] pngBytes = pngBytesStream.toByteArray();

        byte[] variantPart = ("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"variant\"\r\n\r\n"
                + (isSlimModel ? "slim" : "classic") + "\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] filePartHeader = ("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"skin.png\"\r\n"
                + "Content-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] filePartFooter = "\r\n".getBytes(StandardCharsets.UTF_8);
        byte[] closingBoundary = ("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

        int totalLength = variantPart.length + filePartHeader.length + pngBytes.length
                + filePartFooter.length + closingBoundary.length;

        URL url = new URL(MC_SERVICES_SKIN_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setFixedLengthStreamingMode(totalLength);
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream out = conn.getOutputStream()) {
            out.write(variantPart);
            out.write(filePartHeader);
            out.write(pngBytes);
            out.write(filePartFooter);
            out.write(closingBoundary);
        }

        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            String errorBody = safeReadError(conn);
            conn.disconnect();
            throw new IOException("Mojang rejected the skin upload (HTTP " + code + "): " + errorBody);
        }
        conn.disconnect();
    }

    /** Resets the account's skin back to Mojang's default (Steve/Alex). Microsoft accounts only. */
    public static void resetSkin(Account account) throws IOException {
        if (!isMicrosoft(account)) {
            getLocalSkinFile(account).delete();
            return;
        }
        URL url = new URL(MC_SERVICES_SKIN_RESET_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("Authorization", "Bearer " + account.accessToken);
        conn.setUseCaches(false);
        conn.connect();
        int code = conn.getResponseCode();
        conn.disconnect();
        if (code < 200 || code >= 300) {
            throw new IOException("Failed to reset skin (HTTP " + code + ")");
        }
    }

    /** Shows the account's already-owned cape, if any. Microsoft accounts only. */
    public static void showCape(Account account, String capeId) throws IOException {
        if (!isMicrosoft(account)) throw new IOException("Only Microsoft accounts can have capes");
        URL url = new URL(MC_SERVICES_CAPE_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Bearer " + account.accessToken);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setUseCaches(false);
        JSONObject payload = new JSONObject();
        try {
            payload.put("capeId", capeId);
        } catch (Exception ignored) {}
        try (OutputStream out = conn.getOutputStream()) {
            out.write(payload.toString().getBytes(StandardCharsets.UTF_8));
        }
        int code = conn.getResponseCode();
        conn.disconnect();
        if (code < 200 || code >= 300) {
            throw new IOException("Failed to show cape (HTTP " + code + ")");
        }
    }

    /** Hides the account's currently active cape. Microsoft accounts only. */
    public static void hideCape(Account account) throws IOException {
        if (!isMicrosoft(account)) return;
        URL url = new URL(MC_SERVICES_CAPE_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("Authorization", "Bearer " + account.accessToken);
        conn.setUseCaches(false);
        conn.connect();
        int code = conn.getResponseCode();
        conn.disconnect();
        if (code < 200 || code >= 300) {
            throw new IOException("Failed to hide cape (HTTP " + code + ")");
        }
    }

    /**
     * Fetches the list of capes this Microsoft account actually owns, straight from
     * Mojang's own profile endpoint. A launcher cannot grant new capes; it can only
     * show/hide capes the account already has.
     */
    public static List<JSONObject> fetchOwnedCapes(Account account) throws IOException {
        List<JSONObject> capes = new ArrayList<>();
        if (!isMicrosoft(account)) return capes;
        URL url = new URL("https://api.minecraftservices.com/minecraft/profile");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + account.accessToken);
        conn.setUseCaches(false);
        conn.connect();
        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            conn.disconnect();
            throw new IOException("Failed to fetch profile (HTTP " + code + ")");
        }
        String body = Tools.read(conn.getInputStream());
        conn.disconnect();
        try {
            JSONObject json = new JSONObject(body);
            JSONArray capeArray = json.optJSONArray("capes");
            if (capeArray != null) {
                for (int i = 0; i < capeArray.length(); i++) {
                    capes.add(capeArray.getJSONObject(i));
                }
            }
        } catch (Exception e) {
            throw new IOException("Malformed profile response", e);
        }
        return capes;
    }

    private static String safeReadError(HttpURLConnection conn) {
        try {
            return Tools.read(conn.getErrorStream());
        } catch (Exception e) {
            return "(no error body)";
        }
    }

    // --- Local-only skin storage for offline accounts ---

    private static File getLocalSkinFile(Account account) {
        return new File(Tools.DIR_CACHE, "local-skin-" + account.profileId + ".png");
    }

    private static void saveLocalSkin(Account account, Bitmap bitmap) throws IOException {
        File target = getLocalSkinFile(account);
        try (FileOutputStream fos = new FileOutputStream(target)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
    }

    /** Returns the locally-applied skin for an offline account, or null if none was set. */
    public static Bitmap getLocalSkin(Account account) {
        File file = getLocalSkinFile(account);
        if (!file.exists()) return null;
        return BitmapFactory.decodeFile(file.getAbsolutePath());
    }
}
