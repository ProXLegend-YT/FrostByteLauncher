package com.frostbyte.launcher.profiles;

import static com.frostbyte.launcher.extra.ExtraCore.getValue;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ExpandableListView;

import androidx.appcompat.app.AlertDialog;

import com.frostbyte.launcher.JVersionList;
import com.frostbyte.launcher.R;
import com.frostbyte.launcher.extra.ExtraConstants;

public class VersionSelectorDialog {
    public static void open(Context context, boolean hideCustomVersions, VersionSelectorListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        ExpandableListView expandableListView = (ExpandableListView) LayoutInflater.from(context)
                .inflate(R.layout.dialog_expendable_list_view , null);
        JVersionList jVersionList = (JVersionList) getValue(ExtraConstants.RELEASE_TABLE);
        JVersionList.Version[] versionArray;
        if(jVersionList == null || jVersionList.versions == null) versionArray = new JVersionList.Version[0];
        else versionArray = jVersionList.versions;
        VersionListAdapter adapter = new VersionListAdapter(versionArray, hideCustomVersions, context);

        expandableListView.setAdapter(adapter);
        builder.setView(expandableListView);
        AlertDialog dialog = builder.show();

        expandableListView.setOnChildClickListener((parent, v1, groupPosition, childPosition, id) -> {
            String version = adapter.getChild(groupPosition, childPosition);
            listener.onVersionSelected(version, adapter.isSnapshotSelected(groupPosition));
            dialog.dismiss();
            return true;
        });
    }
}
