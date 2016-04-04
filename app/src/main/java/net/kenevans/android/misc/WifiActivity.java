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

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages a ListView of Wifi networks and their properties.
 */

/**
 * @author evans
 */
public class WifiActivity extends ListActivity implements IConstants {
    /**
     * Enum to specify the sort order.
     */
    enum SortOrder {
        NONE, SSID, BSSID, LEVEL, FREQUENCY
    }

    private static int[] WIFI_STRENGTH_COLORS = {
            0xFFCD3700, 0xFFFFA500, Color.YELLOW, 0xFFBCEE68, Color.GREEN,
    };

    /**
     * The sort order to use.
     */
    private SortOrder sortOrder = SortOrder.NONE;

    /**
     * Adapter to manage the ListView.
     */
    private NetworkListAdapter mNetworkListdapter;

    /**
     * The BroadcastReceiver to get the scan results.
     */
    private BroadcastReceiver mReceiver;

    /**
     * The scan results.
     */
    private ArrayList<WifiNetwork> mNetworks;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the stored sort order
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        int sortOrderOrdinal = prefs.getInt(PREF_WIFI_SORT_ORDER, SortOrder.NONE.ordinal());
        if (sortOrderOrdinal >= 0 && sortOrderOrdinal < SortOrder.values().length) {
            sortOrder = SortOrder.values()[sortOrderOrdinal];
        }

        // Make a BroadcastReceiver to get the scan results
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                int state = wifi.getWifiState();
                if (state != WifiManager.WIFI_STATE_ENABLED) {
                    Utils.errMsg(WifiActivity.this, "WiFi is not enabled");
                    return;
                }
                List<ScanResult> scanResults = wifi.getScanResults();
                // Make the ArrayList
                mNetworks = new ArrayList<WifiNetwork>(scanResults.size());
                int i = 0;
                for (ScanResult scanResult : scanResults) {
                    mNetworks.add(new WifiNetwork(i++, scanResult));
                }
                // Sort the arrays list
                Collections.sort(mNetworks);
                // Set the adapter
                mNetworkListdapter = new NetworkListAdapter();
                setListAdapter(mNetworkListdapter);
            }
        };
        registerReceiver(mReceiver, new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));


        // Call refresh to set the contents
        refresh();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.smsmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.refresh:
                refresh();
                return true;
            case R.id.order:
                setOrder();
                return true;
        }
        return false;
    }

    @Override
    protected void onListItemClick(ListView lv, View view, int position, long id) {
        super.onListItemClick(lv, view, position, id);
        WifiNetwork network = mNetworks.get(position);
        String msg = "Supports\n";
        String[] capabilities = network.getCapabilities().split("]");
        for (String string : capabilities) {
            if (string.startsWith("[") && string.length() > 1) {
                msg += "    " + string.substring(1) + "\n";
            }
        }
        Utils.infoMsg(this, msg);
    }

    @Override
    protected void onPause() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    /**
     * Bring up a dialog to change the sort order.
     */
    private void setOrder() {
        final CharSequence[] items = {getText(R.string.sort_none),
                getText(R.string.sort_ssid), getText(R.string.sort_bssid),
                getText(R.string.sort_level), getText(R.string.sort_frequency),};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getText(R.string.sort_title));
        builder.setSingleChoiceItems(items, sortOrder.ordinal(),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        dialog.dismiss();
                        switch (item) {
                            case 0:
                                sortOrder = SortOrder.NONE;
                                break;
                            case 1:
                                sortOrder = SortOrder.SSID;
                                break;
                            case 2:
                                sortOrder = SortOrder.BSSID;
                                break;
                            case 3:
                                sortOrder = SortOrder.LEVEL;
                                break;
                            case 4:
                                sortOrder = SortOrder.FREQUENCY;
                                break;
                        }
                        // Save the sort order
                        SharedPreferences.Editor editor = PreferenceManager
                                .getDefaultSharedPreferences(WifiActivity.this).edit();
                        editor.putInt(PREF_WIFI_SORT_ORDER, sortOrder.ordinal());
                        editor.commit();

                        // Sort the arrays list
                        Collections.sort(mNetworks);
                        // Refresh the networks
                        refresh();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Starts a scan for networks.
     */
    private void refresh() {
        try {
            setListAdapter(mNetworkListdapter);
            if (mNetworkListdapter != null) {
                // Clear the data to indicate we are waiting
                mNetworkListdapter.clear();
                // Refresh the View so it shows
                mNetworkListdapter.notifyDataSetChanged();
            }
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            wifiManager.startScan();
        } catch (Exception ex) {
            Utils.excMsg(this, "Error starting network scan", ex);
        }
    }

    /**
     * Determines channel from frequency.
     *
     * @param freq
     * @return
     */
    public static int convertFrequencyToChannel(int freq) {
        if (freq >= 2412 && freq <= 2484) {
            return (freq - 2412) / 5 + 1;
        } else if (freq >= 5170 && freq <= 5825) {
            return (freq - 5170) / 5 + 34;
        } else {
            return -1;
        }
    }

    /**
     * Class to manage one network from the ScanResult's.
     */
    private class WifiNetwork implements Comparable<WifiNetwork> {
        private int id;
        private String ssid;
        private String bssid;
        private int level;
        private int frequency;
        private String capabilities;

        /**
         * Constructor.
         *
         * @param i          The position in the original list.
         * @param scanResult The ScanResult at that position.
         */
        public WifiNetwork(int i, ScanResult scanResult) {
            this.id = id;
            this.ssid = scanResult.SSID;
            if (ssid.length() == 0) {
                this.ssid = "???";
            }
            this.bssid = scanResult.BSSID;
            this.level = scanResult.level;
            this.frequency = scanResult.frequency;
            this.capabilities = scanResult.capabilities;
        }

        public int getId() {
            return id;
        }

        public String getSsid() {
            return ssid;
        }

        public String getBssid() {
            return bssid;
        }

        public int getLevel() {
            return level;
        }

        public int getFrequency() {
            return frequency;
        }

        public String getCapabilities() {
            return capabilities;
        }

        /**
         * Calculate the channel.
         *
         * @return
         */
        public int getChannel() {
            return convertFrequencyToChannel(frequency);
        }

        /**
         * Get the number of bars out of 5.
         *
         * @return
         */
        public int getBars() {
            return WifiManager.calculateSignalLevel(level, 5);
        }


        @Override
        public int compareTo(WifiNetwork other) {
            switch (sortOrder) {
                case NONE:
                    return this.id - other.id;
                case FREQUENCY:
                    // Lowest first
                    return this.frequency - other.frequency;
                case LEVEL:
                    // Highest first
                    return other.level - this.level;
                case SSID:
                    return (this.ssid.toLowerCase()).compareTo(other.ssid.toLowerCase());
                case BSSID:
                    return this.bssid.compareTo(other.bssid);
            }
            return 0;
        }
    }

    // Adapter for managing the networks.
    private class NetworkListAdapter extends BaseAdapter {
        private LayoutInflater mInflator;

        public NetworkListAdapter() {
            super();
            mInflator = WifiActivity.this.getLayoutInflater();
        }

        public void clear() {
            mNetworks.clear();
        }

        @Override
        public int getCount() {
            return mNetworks.size();
        }

        @Override
        public Object getItem(int i) {
            return mNetworks.get(i);
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

            WifiNetwork network = mNetworks.get(i);
            int bars = network.getBars();
            String title = "";
            title += network.getSsid() + " " + network.getBssid();

            String subTitle = "";
            subTitle += bars + ((bars != 1) ? " bars " : " bar ") + network.getLevel()
                    + " db " + "Channel " + network.getChannel()
                    + " (" + network.getFrequency() + " MHz)";

            viewHolder.title.setText(title);
            viewHolder.subTitle.setText(subTitle);

            // Color the text according to the level
            if (bars >= 0 && bars < WIFI_STRENGTH_COLORS.length) {
                viewHolder.subTitle.setTextColor(WIFI_STRENGTH_COLORS[bars]);
            } else {
                viewHolder.subTitle.setTextColor(Color.WHITE);
            }

            return view;
        }
    }

    static class ViewHolder {
        TextView title;
        TextView subTitle;
    }

}
