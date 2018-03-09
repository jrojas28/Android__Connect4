package com.altice_crt_b.connect4.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;

/**
 * Created by moise on 3/7/2018.
 */

public class BoardGridAdapter extends BaseAdapter {
    private Context mContext;
    private int layoutId;
    private ArrayList<View> views;

    public BoardGridAdapter(Context c, int lid) {
        mContext = c;
        layoutId = lid;
        views = new ArrayList<>();
    }

    public int getCount() {
        return 42;
    }

    public Object getItem(int position) {
        return views.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(mContext);

        if (convertView == null) {
            // if it's not recycled, initialize some attributes

            convertView = inflater.inflate(layoutId, parent, false);

            views.add(convertView);
        }

        return convertView;
    }
//    @Override
//    public boolean areAllItemsEnabled() {
//        if(layoutId == R.layout.box)
//            return false;
//        return true;
//    }
//
//    @Override
//    public boolean isEnabled(int position) {
//        // Return true for clickable, false for not
//        if(layoutId == R.layout.box)
//            return false;
//        return true;
//    }

    // references to our images

}