package com.frostbyte.launcher.skins;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.frostbyte.launcher.BaseActivity;
import com.frostbyte.launcher.R;
import com.frostbyte.launcher.authenticator.AuthType;
import com.frostbyte.launcher.authenticator.accounts.Account;
import com.frostbyte.launcher.authenticator.accounts.Accounts;
import com.frostbyte.launcher.authenticator.listener.LoginListener;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class SkinsActivity extends BaseActivity {

    @Override
    public boolean setFullscreen() {
        return false;
    }

    private Account mAccount;
    private SkinModelView mModelView;
    private ProgressBar mModelLoading;
    private ProgressBar mLoadingIndicator;
    private LinearLayout mCapesSection;
    private LinearLayout mCapesList;

    private final ActivityResultLauncher<String> mGalleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                setLoading(true);
                new Thread(() -> {
                    try {
                        Bitmap bitmap;
                        try (InputStream is = getContentResolver().openInputStream(uri)) {
                            bitmap = BitmapFactory.decodeStream(is);
                        }
                        if (bitmap == null) throw new IOException("Could not read the selected image");
                        if (!isValidSkinDimensions(bitmap.getWidth(), bitmap.getHeight())) {
                            bitmap.recycle();
                            runOnUiThread(() -> {
                                setLoading(false);
                                Toast.makeText(this, R.string.skins_invalid_dimensions, Toast.LENGTH_LONG).show();
                            });
                            return;
                        }
                        File temp = File.createTempFile("frostbyte_gallery_skin", ".png", getCacheDir());
                        try (FileOutputStream fos = new FileOutputStream(temp)) {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        }
                        boolean isSlim = bitmap.getWidth() == 64 && guessSlimFromArmWidth(bitmap);
                        bitmap.recycle();
                        SkinEntry entry = new SkinEntry(SkinEntry.Source.LOCAL_FILE, "From gallery", temp.getAbsolutePath(), isSlim);
                        runOnUiThread(() -> {
                            setLoading(false);
                            confirmAndApply(entry);
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(this, getString(R.string.skins_applied_error, e.getMessage()), Toast.LENGTH_LONG).show();
                        });
                    }
                }).start();
            });

    /** A real skin PNG is either the legacy 64x32 or modern 64x64 layout. */
    private boolean isValidSkinDimensions(int w, int h) {
        return w == 64 && (h == 64 || h == 32);
    }

    /**
     * Best-effort slim-arm detection: on a 64x64 skin, column 54 (inside the classic arm's
     * extra pixel column) is fully transparent on slim skins and opaque on classic ones.
     * Not perfect for every hand-edited skin, but a reasonable default; the person can still
     * see the model preview and re-pick if it looks wrong.
     */
    private boolean guessSlimFromArmWidth(Bitmap bitmap) {
        if (bitmap.getHeight() < 64) return false; // legacy 64x32 skins predate the slim model entirely
        try {
            return (bitmap.getPixel(55, 20) >>> 24) == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Microsoft access tokens are short-lived (about an hour). Since this screen can sit open
     * for a while before the person taps Apply, check freshness right before any Mojang-touching
     * call and silently refresh first if needed — otherwise Mojang rejects the request with a 401.
     * Only real Microsoft accounts need this; Ely.by and offline accounts never touch Mojang.
     */
    private void ensureFreshToken(Account account, Runnable onReady, Runnable onFailure) {
        if (account == null || account.authType != AuthType.MICROSOFT
                || System.currentTimeMillis() <= account.expiresAt) {
            onReady.run();
            return;
        }
        account.authType.createAuth().refreshAccount(new LoginListener() {
            @Override
            public void onLoginDone(Account refreshedAccount) {
                if (mAccount != null && mAccount.profileId.equals(refreshedAccount.profileId)) {
                    mAccount = refreshedAccount;
                }
                onReady.run();
            }

            @Override
            public void onLoginError(Throwable errorMessage) {
                onFailure.run();
            }

            @Override
            public void onLoginProgress(int step) {}

            @Override
            public void setMaxLoginProgress(int max) {}
        }, account);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skins);

        mAccount = Accounts.getCurrent();
        findViewById(R.id.skins_back_button).setOnClickListener(v -> finish());
        mLoadingIndicator = findViewById(R.id.skins_loading_indicator);
        mCapesSection = findViewById(R.id.skins_capes_section);
        mCapesList = findViewById(R.id.skins_capes_list);
        mModelView = findViewById(R.id.skins_model_view);
        mModelLoading = findViewById(R.id.skins_model_loading);

        TextView notice = findViewById(R.id.skins_account_notice);
        boolean isMicrosoft = mAccount != null && mAccount.authType == AuthType.MICROSOFT;
        boolean isElyBy = mAccount != null && mAccount.authType == AuthType.ELY_BY;
        int noticeRes = isMicrosoft ? R.string.skins_account_notice_microsoft
                : isElyBy ? R.string.skins_account_notice_elyby
                : R.string.skins_account_notice_local;
        notice.setText(noticeRes);

        findViewById(R.id.skins_upload_button).setOnClickListener(v -> mGalleryLauncher.launch("image/png"));

        EditText usernameInput = findViewById(R.id.skins_username_input);
        Button searchButton = findViewById(R.id.skins_username_search_button);
        View.OnClickListener doSearch = v -> performUsernameLookup(usernameInput.getText().toString().trim());
        searchButton.setOnClickListener(doSearch);
        usernameInput.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch.onClick(tv);
                return true;
            }
            return false;
        });

        if (mAccount != null) {
            loadCurrentSkinIntoModel(mAccount);
        }
        if (isMicrosoft) {
            loadOwnedCapes();
        }
    }

    /** Loads whichever account's real current skin into the 3D preview model. */
    private void loadCurrentSkinIntoModel(Account account) {
        mModelLoading.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                Bitmap skin = SkinManager.fetchCurrentSkinBitmap(account);
                boolean slim = skin.getHeight() >= 64 && guessSlimFromArmWidth(skin);
                runOnUiThread(() -> {
                    mModelLoading.setVisibility(View.GONE);
                    mModelView.setSkin(skin, slim);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    mModelLoading.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.skins_model_load_failed, Toast.LENGTH_SHORT).show();
                    loadFallbackSkinIntoModel();
                });
            }
        }).start();
    }

    /** Shows a bundled default skin in the 3D model when the account's real skin can't be fetched. */
    private void loadFallbackSkinIntoModel() {
        new Thread(() -> {
            try (java.io.InputStream is = getAssets().open("frostbyte_skins/frostbyte_fallback_steve.png")) {
                Bitmap fallback = BitmapFactory.decodeStream(is);
                runOnUiThread(() -> mModelView.setSkin(fallback, false));
            } catch (Exception ignored) {
                // If even the bundled fallback fails to load, leave the model empty rather than crash
            }
        }).start();
    }

    private void performUsernameLookup(String username) {
        if (username.isEmpty()) return;
        setLoading(true);
        new Thread(() -> {
            try {
                SkinEntry entry = SkinManager.lookupByUsername(username);
                runOnUiThread(() -> {
                    setLoading(false);
                    if (entry == null) {
                        Toast.makeText(this, R.string.skins_lookup_not_found, Toast.LENGTH_SHORT).show();
                    } else {
                        confirmAndApply(entry);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, getString(R.string.skins_lookup_error, e.getMessage()), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void confirmAndApply(SkinEntry entry) {
        List<Account> accounts;
        try {
            accounts = Accounts.load().accounts;
        } catch (Exception e) {
            accounts = mAccount != null ? Collections.singletonList(mAccount) : Collections.emptyList();
        }
        if (accounts.isEmpty()) {
            Toast.makeText(this, R.string.skins_no_accounts, Toast.LENGTH_LONG).show();
            return;
        }

        String[] accountLabels = new String[accounts.size()];
        for (int i = 0; i < accounts.size(); i++) {
            Account acc = accounts.get(i);
            accountLabels[i] = acc.username + " (" + acc.authType.name() + ")";
        }

        int[] selectedIndex = {accounts.indexOf(mAccount) >= 0 ? accounts.indexOf(mAccount) : 0};
        List<Account> finalAccounts = accounts;

        new AlertDialog.Builder(this)
                .setTitle(entry.displayName)
                .setSingleChoiceItems(accountLabels, selectedIndex[0], (dialog, which) -> selectedIndex[0] = which)
                .setPositiveButton(R.string.skins_apply, (dialog, which) -> {
                    Account chosen = finalAccounts.get(selectedIndex[0]);
                    mAccount = chosen;
                    applySkin(chosen, entry, entry.isSlimModel);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void applySkin(Account target, SkinEntry entry, boolean slim) {
        setLoading(true);
        ensureFreshToken(target, () -> new Thread(() -> {
            try {
                SkinManager.applySkin(this, target, entry, slim);
                target.updateSkinFace();
                try { target.save(); } catch (Exception ignored) {}
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, R.string.skins_applied_success, Toast.LENGTH_SHORT).show();
                    loadCurrentSkinIntoModel(target);
                });
            } catch (SkinManager.ElyByUploadUnsupportedException e) {
                // The skin WAS saved locally (so FrostByte's own preview updates below), but it
                // never reached Ely.by's servers or any multiplayer server — Ely.by's API has no
                // upload endpoint. Tell the person that plainly instead of claiming success.
                target.updateSkinFace();
                try { target.save(); } catch (Exception ignored) {}
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, R.string.skins_applied_local_only_elyby, Toast.LENGTH_LONG).show();
                    loadCurrentSkinIntoModel(target);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, getString(R.string.skins_applied_error, e.getMessage()), Toast.LENGTH_LONG).show();
                });
            }
        }).start(), () -> runOnUiThread(() -> {
            setLoading(false);
            Toast.makeText(this, R.string.skins_token_refresh_failed, Toast.LENGTH_LONG).show();
        }));
    }

    private void loadOwnedCapes() {
        new Thread(() -> {
            try {
                List<JSONObject> capes = SkinManager.fetchOwnedCapes(mAccount);
                runOnUiThread(() -> renderCapes(capes));
            } catch (Exception ignored) {
                // Silently skip the capes section if it fails to load; skins still work
            }
        }).start();
    }

    private void renderCapes(List<JSONObject> capes) {
        mCapesSection.setVisibility(View.VISIBLE);
        mCapesList.removeAllViews();

        if (capes.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.skins_cape_none);
            empty.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.frostbyte_text_secondary));
            empty.setPadding(32, 32, 32, 32);
            mCapesList.addView(empty);
            return;
        }

        for (JSONObject cape : capes) {
            View row = getLayoutInflater().inflate(R.layout.item_version_profile_layout, mCapesList, false);
            TextView textView = (TextView) row;
            String alias = cape.optString("alias", "Cape");
            boolean isActive = "ACTIVE".equals(cape.optString("state"));
            textView.setText(alias + (isActive ? " (active)" : ""));
            String capeId = cape.optString("id");
            textView.setOnClickListener(v -> toggleCape(capeId, isActive));
            mCapesList.addView(row);
        }
    }

    private void toggleCape(String capeId, boolean currentlyActive) {
        setLoading(true);
        ensureFreshToken(mAccount, () -> new Thread(() -> {
            try {
                if (currentlyActive) {
                    SkinManager.hideCape(mAccount);
                } else {
                    SkinManager.showCape(mAccount, capeId);
                }
                runOnUiThread(() -> {
                    setLoading(false);
                    loadOwnedCapes();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, getString(R.string.skins_applied_error, e.getMessage()), Toast.LENGTH_LONG).show();
                });
            }
        }).start(), () -> runOnUiThread(() -> {
            setLoading(false);
            Toast.makeText(this, R.string.skins_token_refresh_failed, Toast.LENGTH_LONG).show();
        }));
    }

    private void setLoading(boolean loading) {
        mLoadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
