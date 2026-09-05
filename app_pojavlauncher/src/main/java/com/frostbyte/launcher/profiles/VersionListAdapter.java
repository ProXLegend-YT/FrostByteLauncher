package com.frostbyte.launcher.profiles;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.TextView;

import com.frostbyte.launcher.JVersionList;
import com.frostbyte.launcher.R;
import com.frostbyte.launcher.Tools;
import com.frostbyte.launcher.utils.FilteredSubList;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class VersionListAdapter extends BaseExpandableListAdapter implements ExpandableListAdapter {
    
    private final LayoutInflater mLayoutInflater;

    private final String[] mGroups;
    private final String[] mInstalledVersions;
    private final List<?>[] mData;
    private final boolean mHideCustomVersions;
    private final int mSnapshotListPosition;

    public VersionListAdapter(JVersionList.Version[] versionList, boolean hideCustomVersions, Context ctx){
        mHideCustomVersions = hideCustomVersions;
        mLayoutInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        List<JVersionList.Version> releaseList = new FilteredSubList<>(versionList, item -> item.type.equals("release"));
        List<JVersionList.Version> snapshotList = new FilteredSubList<>(versionList, item -> item.type.equals("snapshot"));
        List<JVersionList.Version> betaList = new FilteredSubList<>(versionList, item -> item.type.equals("old_beta"));
        List<JVersionList.Version> alphaList = new FilteredSubList<>(versionList, item -> item.type.equals("old_alpha"));

        // Query installed versions
        mInstalledVersions = new File(Tools.DIR_GAME_NEW + "/versions").list();
        if(mInstalledVersions != null)
            Arrays.sort(mInstalledVersions);

        if(!areInstalledVersionsAvailable()){
            mGroups = new String[]{
                    ctx.getString(R.string.mcl_setting_veroption_release),
                    ctx.getString(R.string.mcl_setting_veroption_snapshot),
                    ctx.getString(R.string.mcl_setting_veroption_oldbeta),
                    ctx.getString(R.string.mcl_setting_veroption_oldalpha)
            };
            mData = new List[]{ releaseList, snapshotList, betaList, alphaList};
            mSnapshotListPosition = 1;
        }else{
            mGroups = new String[]{
                    ctx.getString(R.string.mcl_setting_veroption_installed),
                    ctx.getString(R.string.mcl_setting_veroption_release),
                    ctx.getString(R.string.mcl_setting_veroption_snapshot),
                    ctx.getString(R.string.mcl_setting_veroption_oldbeta),
                    ctx.getString(R.string.mcl_setting_veroption_oldalpha)
            };
            mData = new List[]{Arrays.asList(mInstalledVersions), releaseList, snapshotList, betaList, alphaList};
            mSnapshotListPosition = 2;
        }
    }

    @Override
    public int getGroupCount() {
        return mGroups.length;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mData[groupPosition].size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mData[groupPosition];
    }

    @Override
    public String getChild(int groupPosition, int childPosition) {
        if(isInstalledVersionSelected(groupPosition)){
            return mInstalledVersions[childPosition];
        }
        return ((JVersionList.Version)mData[groupPosition].get(childPosition)).id;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if(convertView == null)
            convertView = mLayoutInflater.inflate(R.layout.item_version_group, parent, false);

        TextView textView = (TextView) convertView;
        textView.setText(mGroups[groupPosition]);
        // Applied here instead of android:drawableTint in the layout XML, since that attribute
        // only works from API 23+ and this project supports down to API 21.
        for (android.graphics.drawable.Drawable d : textView.getCompoundDrawables()) {
            if (d != null) d.setColorFilter(textView.getCurrentTextColor(), android.graphics.PorterDuff.Mode.SRC_IN);
        }

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if(convertView == null)
            convertView = mLayoutInflater.inflate(R.layout.item_version_child, parent, false);
        ((TextView) convertView).setText(getChild(groupPosition, childPosition));
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public boolean isSnapshotSelected(int groupPosition) {
        return groupPosition == mSnapshotListPosition;
    }

    private boolean areInstalledVersionsAvailable(){
        if(mHideCustomVersions) return false;
        return !(mInstalledVersions == null || mInstalledVersions.length == 0);
    }

    private boolean isInstalledVersionSelected(int groupPosition){
        return groupPosition == 0 && areInstalledVersionsAvailable();
    }
}
