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
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
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
    private static final String[] DEFAULT_APP_NAMES = {
            "net.kenevans.android.misc",
            "com.android.providers.downloads",
    };

    /***
     * List of values to be added to the ListView.
     */
    private List<AppDetails> mAppDetails;

    /**
     * Adapter to manage the ListView.
     */
    private AppDetailsListAdapter mAppDetailsListdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the preferences
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        Gson gson = new Gson();
        String json = prefs.getString(PREF_APPDETAILS_APP_NAMES, "");
        String[] appNames = gson.fromJson(json, String[].class);
        createAppDetailsFromAppNames(appNames);
    }

    @Override
    protected void onListItemClick(ListView lv, View view, int position, long
            id) {
        super.onListItemClick(lv, view, position, id);
        AppDetails appDetails = (AppDetails) getListView().getItemAtPosition
                (position);
        String appName = appDetails.getAppName();
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
        // Call refresh to set the contents
        refresh();
    }

    private void refresh() {
        // Create the list values
        if (mAppDetails == null || mAppDetails.size() == 0) {
            // Create the list from the default names
            mAppDetails = new ArrayList<AppDetails>();
            for (String appName : DEFAULT_APP_NAMES) {
                mAppDetails.add(new AppDetails(appName));
            }
            // Store them in preferences
            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(this);
            SharedPreferences.Editor prefsEditor = PreferenceManager
                    .getDefaultSharedPreferences(this).edit();
            String[] appNames = createAppNamesFromAppDetails();
            if (appNames != null) {
                Gson gson = new Gson();
                String json = gson.toJson(appNames);
                prefsEditor.putString(PREF_APPDETAILS_APP_NAMES, json);
                prefsEditor.commit();
            }
        }
        if (mAppDetailsListdapter == null) {
            // Set the adapter
            mAppDetailsListdapter = new AppDetailsListAdapter();
            setListAdapter(mAppDetailsListdapter);
        }
    }

    /***
     * Creates mAppDetails from the given String array as fetched from
     * Preferences.
     *
     * @param appNames Array of appNames.
     */
    private void createAppDetailsFromAppNames(String[] appNames) {
        if (appNames == null) {
            mAppDetails = null;
            return;
        }
        mAppDetails = new ArrayList<AppDetails>(appNames.length);
        for (String appName : appNames) {
            mAppDetails.add(new AppDetails(appName));
        }
    }

    /***
     * Creates an array of String's from mAppDetails to use for saving
     * preferences.
     *
     * @return
     */
    private String[] createAppNamesFromAppDetails() {
        if (mAppDetails == null) {
            return new String[0];
        }
        ArrayList<String> appNamesList = new ArrayList<String>(mAppDetails
                .size());
        for (AppDetails appDetails : mAppDetails) {
            appNamesList.add(appDetails.appName);
        }
        return appNamesList.toArray(new String[appNamesList.size()]);
    }


    /**
     * Class to manage one network from the ScanResult's.
     */
    private class AppDetails {
        private String appName;
        private String versionName;
        private String packageName;

        /***
         * CTOR
         *
         * @param appName String like net.kenevans.android.misc.
         */
        public AppDetails(String appName) {
            List<PackageInfo> packages = getPackageManager()
                    .getInstalledPackages(0);
            this.appName = appName;
            for (PackageInfo info : packages) {
                if (appName.equals(info.packageName)) {
                    this.packageName = info.applicationInfo.loadLabel
                            (getPackageManager()).toString();
                    this.versionName = info.versionName;
                    break;
                }
            }
        }

        public String getAppName() {
            return appName;
        }

        public String getVersionName() {
            return versionName;
        }

        public String getPackageName() {
            return packageName;
        }
    }

    // Adapter for managing the networks.
    private class AppDetailsListAdapter extends BaseAdapter {
        private LayoutInflater mInflator;

        public AppDetailsListAdapter() {
            super();
            mInflator = AppDetailsActivity.this.getLayoutInflater();
        }

        public void clear() {
            mAppDetails.clear();
        }

        @Override
        public int getCount() {
            return mAppDetails.size();
        }

        @Override
        public Object getItem(int i) {
            return mAppDetails.get(i);
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

            AppDetails appDetails = mAppDetails.get(i);
            String appName = appDetails.getAppName();
            String packageName = appDetails.getPackageName();
            String versionName = appDetails.getVersionName();
            String title = "<Not Found>";
            String subTitle = appName;
            if (packageName != null) {
                title = packageName;
            }
            if (versionName != null) {
                subTitle += "\nVersion: " + versionName;
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
