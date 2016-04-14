//Copyright (c) 2016 Kenneth Evans
//
//Permission is hereby granted, free of charge, to any person obtaining
//a copy of this software and associated documentation files (the
//"Software"), to deal in the Software without restriction, including
//without limitation the rights to use, copy, modify, merge, publish,
//distribute, sublicense, and/or sell copies of the Software, and to
//permit persons to whom the Software is furnished to do so, subject to
//the following conditions:
//
//The above copyright notice and this permission notice shall be included
//in all copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
//EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
//MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
//IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
//CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
//TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
//SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package net.kenevans.android.misc;

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a ListView of applications and shows their details settings.
 */

/**
 * @author evans
 */
public class AppDetailsActivity extends ListActivity implements IConstants {
    /***
     * List of app names to show in the ListView.
     */
    private static final String[] mAppNames = {
            "net.kenevans.android.misc",
            "com.android.providers.downloads",
    };

    /***
     * List of values to be added to the ListView.
     */
    private List<String> mListValues;

    /**
     * Adapter to manage the ListView.
     */
    private AppDetailsListAdapter mAppDetailsListdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create the list of values for the ListView
        mListValues = new ArrayList<String>();
        for (String string : mAppNames) {
            mListValues.add(string);
        }
        // Set the adapter
        mAppDetailsListdapter = new AppDetailsListAdapter();
        setListAdapter(mAppDetailsListdapter);
    }

    @Override
    protected void onListItemClick(ListView lv, View view, int position, long
            id) {
        super.onListItemClick(lv, view, position, id);
        String appName = (String) getListView().getItemAtPosition(position);
        Intent intent = new Intent(android.provider.Settings
                .ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.parse("package:" + appName));
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    // Adapter for managing the networks.
    private class AppDetailsListAdapter extends BaseAdapter {
        private LayoutInflater mInflator;

        public AppDetailsListAdapter() {
            super();
            mInflator = AppDetailsActivity.this.getLayoutInflater();
        }

        public void clear() {
            mListValues.clear();
        }

        @Override
        public int getCount() {
            return mListValues.size();
        }

        @Override
        public Object getItem(int i) {
            return mListValues.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup parent) {
            // // DEBUG
            // Log.d(TAG, "getView: " + i);
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.list_row, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.title = (TextView) view
                        .findViewById(R.id.title);
                viewHolder.subTitle = (TextView) view
                        .findViewById(R.id.subtitle);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            String appName = mListValues.get(i);
            String title = "<Not Found>";
            String subTitle = appName;
            List<PackageInfo> packages = getPackageManager()
                    .getInstalledPackages(0);

            for (PackageInfo info : packages) {
                if (appName.equals(info.packageName)) {
                    title = info.applicationInfo.loadLabel
                            (getPackageManager()).toString();
                    subTitle += "\nVersion: " + info.versionName;
                    break;
                }
            }

            viewHolder.title.setText(title);
            viewHolder.subTitle.setText(subTitle);

            return view;
        }
    }

    static class ViewHolder {
        TextView title;
        TextView subTitle;
    }

}
