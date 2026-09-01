package com.frostbyte.launcher;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.gson.JsonSyntaxException;

import com.frostbyte.launcher.customcontrols.ControlData;
import com.frostbyte.launcher.customcontrols.ControlDrawerData;
import com.frostbyte.launcher.customcontrols.ControlJoystickData;
import com.frostbyte.launcher.customcontrols.ControlLayout;
import com.frostbyte.launcher.customcontrols.EditorExitable;
import com.frostbyte.launcher.prefs.LauncherPreferences;
import com.frostbyte.launcher.utils.CropperUtils;

import java.io.IOException;

import com.frostbyte.launcher.R;


public class CustomControlsActivity extends BaseActivity implements EditorExitable, CropperUtils.CropperReceiver {
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerNavigationView;
	private ControlLayout mControlLayout;
	private CropperUtils.CropperReceiver mCropperReceiver;
	private ActivityResultLauncher<?> mCropperLauncher;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mCropperLauncher = CropperUtils.registerCropper(this, this);

		setContentView(R.layout.activity_custom_controls);

		mControlLayout = findViewById(R.id.customctrl_controllayout);
		mDrawerLayout = findViewById(R.id.customctrl_drawerlayout);
		mDrawerNavigationView = findViewById(R.id.customctrl_navigation_view);
		View mPullDrawerButton = findViewById(R.id.drawer_button);

		mPullDrawerButton.setOnClickListener(v -> mDrawerLayout.openDrawer(mDrawerNavigationView));
		mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

		mDrawerNavigationView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,getResources().getStringArray(R.array.menu_customcontrol_customactivity)));
		mDrawerNavigationView.setOnItemClickListener((parent, view, position, id) -> {
			switch(position) {
				case 0: mControlLayout.addControlButton(new ControlData("New")); break;
				case 1: mControlLayout.addDrawer(new ControlDrawerData()); break;
				case 2: mControlLayout.addJoystickButton(new ControlJoystickData()); break;
				case 3: mControlLayout.openLoadDialog(); break;
				case 4: mControlLayout.openSaveDialog(this); break;
				case 5: mControlLayout.openSetDefaultDialog(); break;
				case 6: // Saving the currently shown control
					try {
						Uri contentUri = DocumentsContract.buildDocumentUri(getString(R.string.storageProviderAuthorities), mControlLayout.saveToDirectory(mControlLayout.mLayoutFileName));

						Intent shareIntent = new Intent();
						shareIntent.setAction(Intent.ACTION_SEND);
						shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
						shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
						shareIntent.setType("application/json");
						startActivity(shareIntent);

						Intent sendIntent = Intent.createChooser(shareIntent, mControlLayout.mLayoutFileName);
						startActivity(sendIntent);
					}catch (Exception e) {
						Tools.showError(this, e);
					}
					break;
				case 7: confirmRestoreFreshDefault(); break;
			}
			mDrawerLayout.closeDrawers();
		});
		mControlLayout.setModifiable(true);
	}

	/**
	 * Regenerates the default control layout from scratch using the current button
	 * styling (rounded, tinted), and saves it over the on-device default.json. This is
	 * needed because default.json is written to device storage once and then never
	 * automatically re-synced, so older installs can be stuck with outdated button styling
	 * even after an app update fixes it.
	 */
	private void confirmRestoreFreshDefault() {
		new androidx.appcompat.app.AlertDialog.Builder(this)
				.setTitle(R.string.customctrl_restore_fresh_default)
				.setMessage(R.string.customctrl_restore_fresh_default_confirm)
				.setPositiveButton(R.string.customctrl_restore_fresh_default, (dialog, which) -> {
					try {
						com.frostbyte.launcher.customcontrols.CustomControls fresh =
								new com.frostbyte.launcher.customcontrols.CustomControls(this);
						// mLayoutBitmaps is transient and not set by this constructor (it's normally
						// populated when a layout is deserialized from disk) - saveLayout() needs it
						// to exist, so give the freshly-built object an empty one here.
						fresh.mLayoutBitmaps = com.frostbyte.launcher.customcontrols.LayoutBitmaps.createEmpty();
						mControlLayout.loadLayout(fresh);
						mControlLayout.saveLayout(Tools.CTRLDEF_FILE);
					} catch (Exception e) {
						Tools.showError(this, e);
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	@Override
	public void onAttachedToWindow() {
		mControlLayout.post(()->{
			try {
				mControlLayout.loadLayout(LauncherPreferences.PREF_DEFAULTCTRL_PATH);
			}catch (IOException | JsonSyntaxException e) {
				Tools.showError(this, e);
			}
		});
	}

	public void startCropping(CropperUtils.CropperReceiver cropperReceiver) {
		mCropperReceiver = cropperReceiver;
		CropperUtils.startCropper(mCropperLauncher);
	}

	@Override
	public void onBackPressed() {
		mControlLayout.askToExit(this);
	}

	@Override
	public void exitEditor() {
		super.onBackPressed();
	}

	@Override
	public float getAspectRatio() {
		if(mCropperReceiver != null) return mCropperReceiver.getAspectRatio();
		return 1f;
	}

	@Override
	public int getTargetMaxSide() {
		if(mCropperReceiver != null) return mCropperReceiver.getTargetMaxSide();
		return 128;
	}

	@Override
	public void onCropped(Bitmap contentBitmap) {
		if(mCropperReceiver != null) mCropperReceiver.onCropped(contentBitmap);
	}

	@Override
	public void onFailed(Exception exception) {
		if(mCropperReceiver != null) mCropperReceiver.onFailed(exception);
	}
}
