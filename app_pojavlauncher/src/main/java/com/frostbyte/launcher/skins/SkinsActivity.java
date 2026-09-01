package com.frostbyte.launcher.skins;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
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
import com.frostbyte.launcher.progresskeeper.ProgressKeeper;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SkinsActivity extends BaseActivity {

    @Override
    public boolean setFullscreen() {
        return false;
    }

    private Account mAccount;
    private SkinGridAdapter mAdapter;
    private ProgressBar mLoadingIndicator;
    private LinearLayout mCapesSection;
    private LinearLayout mCapesList;
    private CheckBox mSlimCheckbox;

    private final ActivityResultLauncher<String> mGalleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                setLoading(true);
                new Thread(() -> {
                    try {
                        Bitmap bitmap;
                        try (java.io.InputStream is = getContentResolver().openInputStream(uri)) {
                            bitmap = android.graphics.BitmapFactory.decodeStream(is);
                        }
                        if (bitmap == null) throw new java.io.IOException("Could not read the selected image");
                        if (!isValidSkinDimensions(bitmap.getWidth(), bitmap.getHeight())) {
                            bitmap.recycle();
                            runOnUiThread(() -> {
                                setLoading(false);
                                Toast.makeText(this, R.string.skins_invalid_dimensions, Toast.LENGTH_LONG).show();
                            });
                            return;
                        }
                        java.io.File temp = java.io.File.createTempFile("frostbyte_gallery_skin", ".png", getCacheDir());
                        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(temp)) {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        }
                        bitmap.recycle();
                        SkinEntry entry = new SkinEntry(SkinEntry.Source.LOCAL_FILE, "From gallery", temp.getAbsolutePath(), mSlimCheckbox.isChecked());
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
     * Microsoft access tokens are short-lived (about an hour). Since this screen can sit open
     * for a while before the person taps Apply, check freshness right before any Mojang-touching
     * call and silently refresh first if needed — otherwise Mojang rejects the request with a 401.
     * Runs the given action once a usable token is guaranteed to be in place.
     */
    private void ensureFreshToken(Account account, Runnable onReady, Runnable onFailure) {
        if (account == null || account.isLocal() || !account.authType.requiresLogin()
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
        mSlimCheckbox = findViewById(R.id.skins_slim_checkbox);

        TextView notice = findViewById(R.id.skins_account_notice);
        boolean isLocal = mAccount == null || mAccount.isLocal();
        notice.setText(isLocal ? R.string.skins_account_notice_local : R.string.skins_account_notice_microsoft);

        GridView grid = findViewById(R.id.skins_grid);
        mAdapter = new SkinGridAdapter(this, this::confirmAndApply);
        grid.setAdapter(mAdapter);
        mAdapter.setEntries(SkinManager.getBundledPresets());

        findViewById(R.id.skins_upload_button).setOnClickListener(v -> mGalleryLauncher.launch("image/png"));
        findViewById(R.id.skins_reset_button).setOnClickListener(v -> confirmAndReset());

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

        if (!isLocal) {
            loadOwnedCapes();
        }
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
                        mAdapter.addEntry(entry);
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
            accounts = mAccount != null ? java.util.Collections.singletonList(mAccount) : java.util.Collections.emptyList();
        }
        if (accounts.isEmpty()) {
            Toast.makeText(this, R.string.skins_no_accounts, Toast.LENGTH_LONG).show();
            return;
        }

        View previewView = getLayoutInflater().inflate(R.layout.dialog_skin_preview, null);
        ImageView previewImage = previewView.findViewById(R.id.skin_preview_image);
        new Thread(() -> {
            try {
                Bitmap bitmap = SkinManager.loadPreviewBitmap(this, entry);
                if (bitmap != null) {
                    runOnUiThread(() -> previewImage.setImageBitmap(bitmap));
                }
            } catch (Exception ignored) {
                // Leave the placeholder image if the preview fails to load; picking an account still works
            }
        }).start();

        String[] accountLabels = new String[accounts.size()];
        for (int i = 0; i < accounts.size(); i++) {
            Account acc = accounts.get(i);
            accountLabels[i] = acc.username + " (" + acc.authType.name() + ")";
        }

        int[] selectedIndex = {accounts.indexOf(mAccount) >= 0 ? accounts.indexOf(mAccount) : 0};
        List<Account> finalAccounts = accounts;

        new AlertDialog.Builder(this)
                .setTitle(entry.displayName)
                .setView(previewView)
                .setSingleChoiceItems(accountLabels, selectedIndex[0], (dialog, which) -> selectedIndex[0] = which)
                .setPositiveButton(R.string.skins_apply, (dialog, which) -> {
                    Account chosen = finalAccounts.get(selectedIndex[0]);
                    mAccount = chosen;
                    applySkin(chosen, entry, entry.isSlimModel);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void confirmAndReset() {
        if (mAccount == null) return;
        Account target = mAccount;
        new AlertDialog.Builder(this)
                .setTitle(R.string.skins_reset)
                .setPositiveButton(R.string.skins_reset, (dialog, which) -> {
                    setLoading(true);
                    ensureFreshToken(target, () -> new Thread(() -> {
                        try {
                            SkinManager.resetSkin(target);
                            target.updateSkinFace();
                            try { target.save(); } catch (Exception ignored) {}
                            runOnUiThread(() -> {
                                setLoading(false);
                                Toast.makeText(this, R.string.skins_applied_success, Toast.LENGTH_SHORT).show();
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
                })
                .setNeutralButton(android.R.string.cancel, null)
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
