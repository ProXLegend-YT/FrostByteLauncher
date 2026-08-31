package com.frostbyte.launcher.skins;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.frostbyte.launcher.R;
import com.frostbyte.launcher.authenticator.accounts.SkinHeadRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SkinGridAdapter extends BaseAdapter {

    public interface OnSkinPickedListener {
        void onSkinPicked(SkinEntry entry);
    }

    private final Context mContext;
    private final List<SkinEntry> mEntries = new ArrayList<>();
    private final OnSkinPickedListener mListener;
    private final Map<SkinEntry, Bitmap> mIconCache = new HashMap<>();
    private final Set<SkinEntry> mLoadInFlight = new HashSet<>();

    public SkinGridAdapter(Context context, OnSkinPickedListener listener) {
        mContext = context;
        mListener = listener;
    }

    public void setEntries(List<SkinEntry> entries) {
        mEntries.clear();
        mEntries.addAll(entries);
        notifyDataSetChanged();
    }

    public void addEntry(SkinEntry entry) {
        mEntries.add(0, entry);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mEntries.size();
    }

    @Override
    public SkinEntry getItem(int position) {
        return mEntries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.item_skin_grid, parent, false);
        }
        SkinEntry entry = mEntries.get(position);

        ImageView thumbnail = view.findViewById(R.id.skin_thumbnail);
        TextView name = view.findViewById(R.id.skin_name);
        name.setText(entry.displayName);
        view.setTag(entry);
        view.setOnClickListener(v -> {
            if (mListener != null) mListener.onSkinPicked((SkinEntry) v.getTag());
        });

        Bitmap cachedIcon = mIconCache.get(entry);
        if (cachedIcon != null) {
            thumbnail.setImageBitmap(cachedIcon);
            return view;
        }

        thumbnail.setImageResource(R.drawable.ic_px_ram); // placeholder until async load resolves

        if (mLoadInFlight.contains(entry)) return view;
        mLoadInFlight.add(entry);

        final View viewRef = view;
        new Thread(() -> {
            try {
                Bitmap skinBitmap = SkinManager.loadPreviewBitmap(mContext, entry);
                if (skinBitmap == null) return;
                Bitmap headIcon = new SkinHeadRenderer().render(96, skinBitmap);
                skinBitmap.recycle();
                if (headIcon == null) return;
                mIconCache.put(entry, headIcon);
                viewRef.post(() -> {
                    mLoadInFlight.remove(entry);
                    // Only apply if this recycled view is still showing the same entry
                    if (viewRef.getTag() == entry) {
                        thumbnail.setImageBitmap(headIcon);
                    }
                });
            } catch (Exception ignored) {
                // Leave the placeholder icon on failure (e.g. network error for remote skins)
                viewRef.post(() -> mLoadInFlight.remove(entry));
            }
        }).start();

        return view;
    }
}
