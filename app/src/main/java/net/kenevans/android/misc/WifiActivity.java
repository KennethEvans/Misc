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
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Manages a ListView of all the contacts in the database specified by the URI field.
 */

/**
 * @author evans
 */
public class WifiActivity extends ListActivity implements IConstants {
    /**
     * The current position when ACTIVITY_DISPLAY_MESSAGE is requested. Used
     * with the resultCodes RESULT_PREV and RESULT_NEXT when they are returned.
     */
    private int currentPosition;

    /**
     * The current id when ACTIVITY_DISPLAY_MESSAGE is requested. Used with the
     * resultCodes RESULT_PREV and RESULT_NEXT when they are returned.
     */
    private long currentId;

    /**
     * The increment for displaying the next message.
     */
    private long increment = 0;

    /**
     * Enum to specify the sort order.
     */
    enum Order {
        NAME(ContactsContract.Contacts.DISPLAY_NAME + " ASC"), ID(COL_ID
                + " ASC");
        public String sqlCommand;

        Order(String sqlCommand) {
            this.sqlCommand = sqlCommand;
        }
    }

    /**
     * The sort order to use.
     */
    private Order sortOrder = Order.NAME;

    private NetworkListAdapter mNetworkListdapter;

    /**
     * The BroadcastReceiver to get the scan results.
     */
    private BroadcastReceiver mReceiver;

    /**
     * The scan results.
     */
    private List<ScanResult> mScanResults;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                mScanResults = wifi.getScanResults();
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
        // Save the position when starting the activity
        currentPosition = position;
        currentId = id;
        increment = 0;
        displayNetwork();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        // DEBUG
        Log.d(TAG, this.getClass().getSimpleName()
                + ".onActivityResult: requestCode=" + requestCode
                + " resultCode=" + resultCode + " currentPosition="
                + currentPosition);
        if (requestCode == DISPLAY_MESSAGE) {
            increment = 0;
            // Note that earlier items are at higher positions in the list
            if (resultCode == RESULT_PREV) {
                increment = 1;
            } else if (resultCode == RESULT_NEXT) {
                increment = -1;
            }
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, this.getClass().getSimpleName()
                + ".onPause: currentPosition=" + currentPosition);
        Log.d(TAG, this.getClass().getSimpleName() + ".onPause: currentId="
                + currentId);
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, this.getClass().getSimpleName()
                + ".onResume: currentPosition=" + currentPosition
                + " currentId=" + currentId + " increment=" + increment);
        super.onResume();
        registerReceiver(mReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        // If increment is set display a new message
        if (increment != 0) {
            displayNetwork();
        }
    }

    /**
     * Bring up a dialog to change the sort order.
     */
    private void setOrder() {
        final CharSequence[] items = {getText(R.string.sort_name),
                getText(R.string.sort_id)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getText(R.string.sort_title));
        builder.setSingleChoiceItems(items, sortOrder == Order.NAME ? 0 : 1,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        dialog.dismiss();
                        sortOrder = item == 0 ? Order.NAME : Order.ID;
                        refresh();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Displays the message at the current position plus the current increment,
     * adjusting for being within range. Resets the increment to 0 after.
     */
    private void displayNetwork() {
        ListAdapter adapter = getListAdapter();
        if (adapter == null) {
            return;
        }
        try {
            int count = adapter.getCount();
            Log.d(TAG, this.getClass().getSimpleName()
                    + ".displayMessage: count=" + count);
            if (count == 0) {
                Utils.infoMsg(this, "There are no items in the list");
                return;
            }
            // Check if the item is still at the same position in the list
            boolean changed = false;
            long id = -1;
            if (currentPosition > count - 1 || currentPosition < 0) {
                changed = true;
            } else {
                id = adapter.getItemId(currentPosition);
                if (id != currentId) {
                    changed = true;
                }
            }
            // Determine the new currentPosition
            Log.d(TAG, this.getClass().getSimpleName()
                    + ".displayMessage: position=" + currentPosition + " id="
                    + id + " changed=" + changed);
            if (changed) {
                for (int i = 0; i < count; i++) {
                    id = adapter.getItemId(i);
                    if (id == currentId) {
                        currentPosition = i;
                        break;
                    }
                }
            }
            // currentPosition may still be invalid, check it is in range
            if (currentPosition < 0) {
                currentPosition = 0;
            } else if (currentPosition > count - 1) {
                currentPosition = count - 1;
            }

            // Display messages if a requested increment is not possible
            if (increment > 0) {
                currentPosition += increment;
                if (currentPosition > count - 1) {
                    Toast.makeText(getApplicationContext(),
                            "At the last item in the list", Toast.LENGTH_LONG)
                            .show();
                    currentPosition = count - 1;
                }
            } else if (increment < 0) {
                currentPosition += increment;
                if (currentPosition < 0) {
                    Toast.makeText(getApplicationContext(),
                            "At the first item in the list", Toast.LENGTH_LONG)
                            .show();
                    currentPosition = 0;
                }
            }

            // Request the new message
            currentId = adapter.getItemId(currentPosition);
//            Intent i = new Intent(this, DisplayWifiActivity.class);
//            i.putExtra(COL_ID, currentId);
//            i.putExtra(URI_KEY, getUri().toString());
//            Log.d(TAG, this.getClass().getSimpleName()
//                    + ".displayMessage: position=" + currentPosition
//                    + " currentId=" + currentId);
//            startActivityForResult(i, DISPLAY_MESSAGE);
        } catch (Exception ex) {
            Utils.excMsg(this, "Error displaying contact", ex);
        } finally {
            // Reset increment
            increment = 0;
        }
    }

    /**
     * Gets a new cursor and starts managing it.
     */
    private void refresh() {
        try {
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            wifiManager.startScan();
        } catch (Exception ex) {
            Utils.excMsg(this, "Error finding contacts", ex);
        }
    }

    // Adapter for holding sessions
    private class NetworkListAdapter extends BaseAdapter {
        private LayoutInflater mInflator;

        public NetworkListAdapter() {
            super();
            mInflator = WifiActivity.this.getLayoutInflater();
        }

        public ScanResult getSession(int position) {
            return mScanResults.get(position);
        }

        public void clear() {
            mScanResults.clear();
        }

        @Override
        public int getCount() {
            return mScanResults.size();
        }

        @Override
        public Object getItem(int i) {
            return mScanResults.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            // // DEBUG
            // Log.d(TAG, "getView: " + i);
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.list_row, null);
                viewHolder = new ViewHolder();
                viewHolder.title = (TextView) view
                        .findViewById(R.id.title);
                viewHolder.subTitle = (TextView) view
                        .findViewById(R.id.subtitle);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            ScanResult scanResult = mScanResults.get(i);
            // Set the name
            String[] resultStrings = scanResult.toString().split(",");
            String ssid = resultStrings[0];
            String bssid = resultStrings[1];
            String level = resultStrings[4];
            String frequency = resultStrings[5];
            String title = "";
            title += ssid + " " + bssid;
            String subTitle = "";
            subTitle += level + " db " + frequency + " MHz";
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
