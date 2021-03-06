//Copyright (c) 2011 Kenneth Evans
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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Class that has a ListView of Activities to be launched.
 */
public class MiscActivity extends AppCompatActivity {
    /**
     * Array of items used to populate the ListView when debugging.
     */
    private static final Data[] DEBUG_DATA = {
//            new Data("Track Towers", "Show the current tower location",
//                    MapLocationActivity.class),
            new Data("Network Information",
                    "Get information about the carrier", NetworkActivity.class),
            // new Data("Current Time",
            // "Get the current time in several formats",
            // CurrentTimeActivity.class),
            // new Data("WebView", "Test WebView", InfoActivity.class),
            new Data("Application Info", "Display Application Information",
                    ApplicationInfoActivity.class),
            new Data("MMS and SMS Messages",
                    "Display all SMS and MMS messsages",
                    MMSSMSActivity.class),
            new Data("SMS Messages",
                    "Display all messages in the SMS database",
                    SMSActivity.class),
            new Data("MMS Messages",
                    "Display all messages in the MMS database",
                    MMSActivity.class),
            // new Data("Messages", "Display all SMS and MMS messages",
            // MessageActivity.class),
            new Data("Call History", "Display call history",
                    CallHistoryActivity.class),
            new Data("Contacts", "Display contacts", ContactsActivity.class),
            new Data("App Details", "Display Settings page for selected Apps",
                    AppDetailsActivity.class),
            new Data("Wi-Fi", "Display Wi-Fi Networks", WifiActivity.class),
            // new Data("Fix Media Monkey Auto-Convert Files",
            // "Rename auto-converted .m4a music files to .mp3",
            // FixMediaMonkeyActivity.class),
            // new Data("Test", "Not implemented", null),
    };

    /**
     * Array of items used to populate the ListView for release.
     */
    private static final Data[] RELEASE_DATA = {
//            new Data("Track Towers", "Show the current tower location",
//                    MapLocationActivity.class),
            new Data("Network Information",
                    "Get information about the carrier", NetworkActivity.class),
            new Data("Application Info", "Display Application Information",
                    ApplicationInfoActivity.class),
            new Data("MMS and SMS Messages",
                    "Display all SMS and MMS messsages",
                    MMSSMSActivity.class),
            new Data("SMS Messages",
                    "Display all messages in the SMS database",
                    SMSActivity.class),
            new Data("MMS Messages",
                    "Display all messages in the MMS database",
                    MMSActivity.class),
            new Data("Call History", "Display call history",
                    CallHistoryActivity.class),
            new Data("Contacts", "Display contacts", ContactsActivity.class),
            new Data("App Details", "Display Settings page for selected App",
                    AppDetailsActivity.class),
            new Data("Wi-Fi", "Display Wi-Fi Networks", WifiActivity.class),
            // new Data("Fix Media Monkey Auto-Convert Files",
            // "Rename auto-converted .m4a music files to .mp3",
            // FixMediaMonkeyActivity.class),
            // Spacer to keep bracket on next line
    };

    /**
     * The Array of items actually used to populate the ListView.
     */
    private Data[] mData = RELEASE_DATA;
    private ListView mListView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_view);
        mListView = findViewById(R.id.mainListView);

        try {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setLogo(R.drawable.bluemouse);
            getSupportActionBar().setDisplayUseLogoEnabled(true);
        } catch (Exception ex) {
            // Do nothing
        }

        // Use different items when debugging
        if (Utils.isDebugBuild(this)) {
            mData = DEBUG_DATA;
        }

        // set the ListAdapter
        mListView.setAdapter(new MiscAdapter(this, mData));

        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos,
                                    long id) {
                if (pos < 0 || pos >= mData.length) {
                    return;
                }
                if (mData[pos].activityClass == null) {
                    Toast.makeText(getApplicationContext(),
                            mData[pos].title + " Selected", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Intent i = new Intent(MiscActivity.this,
                            mData[pos].activityClass);
                    try {
                        startActivity(i);
                    } catch (Exception ex) {
                        Utils.excMsg(MiscActivity.this,
                                "Error launching activity", ex);
                    }
                }
            }
        });
    }

    /**
     * A class to hold the mData used to define the contents of the list item
     * and
     * to implement the onItemClick handler.
     */
    private static class Data {
        private String title;
        private String subtitle;
        private Class<?> activityClass;

        private Data(String title, String subtitle, Class<?> activityClass) {
            this.title = title;
            this.subtitle = subtitle;
            this.activityClass = activityClass;
        }

    }

    /**
     * A custom ListView adapter for our implementation. Based on the efficient
     * list adapter in the SDK APIDemos list14.java.
     */
    private static class MiscAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        private Data[] data;

        private MiscAdapter(Context context, Data[] data) {
            this.data = data;
            // Cache the LayoutInflate to avoid asking for a new one each time.
            mInflater = LayoutInflater.from(context);
        }

        /**
         * The number of items in the list.
         *
         * @see android.widget.ListAdapter#getCount()
         */
        public int getCount() {
            return data.length;
        }

        /**
         * Since the mData comes from an array, just returning the index is
         * sufficient to get at the mData. If we were using a more complex mData
         * structure, we would return whatever object represents one row in the
         * list.
         *
         * @see android.widget.ListAdapter#getItem(int)
         */
        public Object getItem(int position) {
            return position;
        }

        /**
         * Use the array index as a unique id.
         *
         * @see android.widget.ListAdapter#getItemId(int)
         */
        public long getItemId(int position) {
            return position;
        }

        /**
         * Make a view to hold each row.
         *
         * @see android.widget.ListAdapter#getView(int, android.view.View,
         * android.view.ViewGroup)
         */
        public View getView(int pos, View convertView, ViewGroup parent) {
            // A ViewHolder keeps references to children views to avoid
            // unnecessary calls
            // to findViewById() on each row.
            ViewHolder holder;

            // When convertView is not null, we can reuse it directly, there is
            // no need
            // to reinflate it. We only inflate a new View when the convertView
            // supplied
            // by ListView is null.
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.list_row, parent,
                        false);

                // Creates a ViewHolder and store references to the two children
                // views
                // we want to bind mData to.
                holder = new ViewHolder();
                holder.title = convertView.findViewById(R.id.title);
                holder.subtitle = convertView
                        .findViewById(R.id.subtitle);

                convertView.setTag(holder);
            } else {
                // Get the ViewHolder back
                holder = (ViewHolder) convertView.getTag();
            }

            // Bind the mData efficiently with the holder.
            holder.title.setText(data[pos].title);
            holder.subtitle.setText(data[pos].subtitle);

            return convertView;
        }

        static class ViewHolder {
            TextView title;
            TextView subtitle;
        }
    }

}
