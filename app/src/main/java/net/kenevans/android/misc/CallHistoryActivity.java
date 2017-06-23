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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.CallLog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Manages a ListView of all the calls in the database specified by the URI
 * field.
 */

/**
 * @author evans
 */
public class CallHistoryActivity extends ListActivity implements IConstants {
    /**
     * The current position when DISPLAY_CALL is requested. Used with the
     * resultCodes RESULT_PREV and RESULT_NEXT when they are returned.
     */
    private int mCurrentPosition;

    /**
     * The current id when DISPLAY_CALL is requested. Used with the resultCodes
     * RESULT_PREV and RESULT_NEXT when they are returned.
     */
    private long mCurrentId;

    /**
     * The increment for displaying the next call.
     */
    private long increment = 0;

    /**
     * The Uri to use for the database.
     */
    public static final Uri URI = CALLLOG_CALLS_URI;

    /**
     * Enum to specify the sort order.
     */
    enum Order {
        TIME(COL_DATE + " DESC"), ID(COL_ID + " DESC");
        public String sqlCommand;

        Order(String sqlCommand) {
            this.sqlCommand = sqlCommand;
        }
    }

    /**
     * Array of hard-coded mFilters
     */
    private Filter[] mFilters;
    /**
     * The current filter.
     */
    private int filter = 0;

    /**
     * Array of hard-coded sort orders
     */
    private SortOrder[] SORT_ORDERSs;
    /**
     * The current sort order.
     */
    private int mSortOrder = 0;

    /**
     * Template for the name of the file written to the root of the SD card
     */
    private static final String SAVE_FILE_NAME = "CallHistory.%s.csv";

    private CustomCursorAdapter mListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create mFilters here so getText is available
        mFilters = new Filter[]{
                new Filter(getText(R.string.filter_none), null),
                new Filter(getText(R.string.filter_known), COL_NAME
                        + " IS NOT NULL"),
                new Filter(getText(R.string.filter_unknown), COL_NAME
                        + " IS NULL"),
                new Filter(getText(R.string.filter_short_unknown_nonoutgoing),
                        COL_DURATION + "<=20" + " AND " + COL_NAME + " IS NULL"
                                + " AND " + COL_TYPE + "<>"
                                + CallLog.Calls.OUTGOING_TYPE),
                new Filter(getText(R.string.filter_10min), COL_DURATION
                        + ">600"),
                // Place holder to prevent reformatting
        };

        // Create sort orders here so getText is available
        SORT_ORDERSs = new SortOrder[]{
                new SortOrder(getText(R.string.sort_time), COL_DATE + " DESC"),
                new SortOrder(getText(R.string.sort_id), COL_ID + " DESC"),
                new SortOrder(getText(R.string.sort_name), COL_NAME),
                new SortOrder(getText(R.string.sort_duration), COL_DURATION
                        + " DESC"),
                new SortOrder(getText(R.string.sort_number), COL_NUMBER
                        + " DESC"),
                // Place holder to prevent reformatting
        };

        // Get the preferences here before refresh()
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        filter = prefs.getInt("filter", 0);
        if (filter < 0 || filter >= mFilters.length) {
            filter = 0;
        }
        mSortOrder = prefs.getInt("mSortOrder", 0);
        if (mSortOrder < 0 || mSortOrder >= SORT_ORDERSs.length) {
            mSortOrder = 0;
        }

        // Call refresh to set the contents
        // Does not have to be done in resume
        refresh();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.callhistorymenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.refresh:
                refresh();
                return true;
            case R.id.save:
                save();
                return true;
            case R.id.help:
                showHelp();
                return true;
            case R.id.filter:
                setFilter();
                return true;
            case R.id.sort:
                setSortOrder();
                return true;
        }
        return false;
    }

    @Override
    protected void onListItemClick(ListView lv, View view, int position, long
            id) {
        super.onListItemClick(lv, view, position, id);
        // Save the position when starting the activity
        mCurrentPosition = position;
        mCurrentId = id;
        increment = 0;
        displayCall();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        // DEBUG
        Log.d(TAG, this.getClass().getSimpleName()
                + ".onActivityResult: requestCode=" + requestCode
                + " resultCode=" + resultCode + " mCurrentPosition="
                + mCurrentPosition);
        if (requestCode == DISPLAY_CALL) {
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
                + ".onPause: mCurrentPosition=" + mCurrentPosition);
        Log.d(TAG, this.getClass().getSimpleName() + ".onPause: mCurrentId="
                + mCurrentId);
        super.onPause();
        // We save the preferences in refresh
    }

    @Override
    protected void onResume() {
        Log.d(TAG, this.getClass().getSimpleName()
                + ".onResume: mCurrentPosition=" + mCurrentPosition
                + " mCurrentId=" + mCurrentId + " increment=" + increment);
        super.onResume();
        // If increment is set display a new call
        if (increment != 0) {
            displayCall();
        }
        // We get the preferences in onCreate since it is not necessary to do
        // refresh() here
    }

    /**
     * Bring up a dialog to change the filter order.
     */
    private void setFilter() {
        final CharSequence[] items = new CharSequence[mFilters.length];
        for (int i = 0; i < mFilters.length; i++) {
            items[i] = mFilters[i].name;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getText(R.string.filter_title));
        builder.setSingleChoiceItems(items, filter,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        dialog.dismiss();
                        if (item < 0 || item >= mFilters.length) {
                            Utils.errMsg(CallHistoryActivity.this,
                                    "Invalid filter");
                            filter = 0;
                        } else {
                            filter = item;
                        }
                        refresh();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Bring up a dialog to change the sort order.
     */
    private void setSortOrder() {
        final CharSequence[] items = new CharSequence[SORT_ORDERSs.length];
        for (int i = 0; i < SORT_ORDERSs.length; i++) {
            items[i] = SORT_ORDERSs[i].name;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getText(R.string.sort_title));
        builder.setSingleChoiceItems(items, mSortOrder,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        dialog.dismiss();
                        if (item < 0 || item >= SORT_ORDERSs.length) {
                            Utils.errMsg(CallHistoryActivity.this,
                                    "Invalid mSortOrder");
                            mSortOrder = 0;
                        } else {
                            mSortOrder = item;
                        }
                        refresh();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Format the duration to be hh:mm:ss.
     *
     * @param duration
     * @return
     */
    public static String formatDuration(String duration) {
        if (duration == null || duration.length() == 0) {
            return "<Unknown>";
        }
        int seconds = -1;
        try {
            seconds = Integer.parseInt(duration);
        } catch (NumberFormatException ex) {
            return "<Invalid>";
        }

        int hours = seconds / 3600;
        seconds -= hours * 3600;
        int minutes = seconds / 60;
        seconds -= minutes * 60;

        return String.format("%d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Format the type as a string.
     *
     * @param type
     * @return
     */
    public static String formatType(int type) {
        if (type == CallLog.Calls.INCOMING_TYPE) {
            return "Incoming";
        }
        if (type == CallLog.Calls.OUTGOING_TYPE) {
            return "Outgoing";
        }
        if (type == CallLog.Calls.MISSED_TYPE) {
            return "Missed";
        }
        return "<Unknown type>";
    }

    /**
     * Show the help.
     */
    private void showHelp() {
        try {
            // Start theInfoActivity
            Intent intent = new Intent();
            intent.setClass(this, InfoActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra(INFO_URL, "file:///android_asset/callhistory.html");
            startActivity(intent);
        } catch (Exception ex) {
            Utils.excMsg(this, "Error showing Help", ex);
        }
    }

    /**
     * Saves the info to the SD card
     */
    private void save() {
        BufferedWriter out = null;
        try {
            File sdCardRoot = Environment.getExternalStorageDirectory();
            if (sdCardRoot.canWrite()) {
                // Create the file name
                String format = "yyyy-MM-dd-HHmmss";
                SimpleDateFormat formatter = new SimpleDateFormat(format);
                Date now = new Date();
                File dir = new File(sdCardRoot, SD_CARD_MISC_DIR);
                if (dir.exists() && dir.isFile()) {
                    Utils.errMsg(this, "Cannot create directory: " + dir
                            + "\nA file with that name exists.");
                    return;
                }
                if (!dir.exists()) {
                    Log.d(TAG, this.getClass().getSimpleName()
                            + ": create: dir=" + dir.getPath());
                    boolean res = dir.mkdir();
                    if (!res) {
                        Utils.errMsg(this, "Cannot create directory: " + dir);
                        return;
                    }
                }
                String fileName = String.format(SAVE_FILE_NAME,
                        formatter.format(now), now.getTime());
                File file = new File(dir, fileName);
                FileWriter writer = new FileWriter(file);
                out = new BufferedWriter(writer);
                out.write("id\t" + "date\t" + "number\t" + "type\t"
                        + "duration\t" + "name\n");

                // Get the database again to avoid traversing the ListView,
                // which only has visible items
                Cursor cursor = null;
                try {
                    // Get a cursor
                    String[] desiredColumns = {COL_ID, COL_NUMBER, COL_DATE,
                            COL_DURATION, COL_TYPE, COL_NAME};
                    cursor = getContentResolver().query(getUri(),
                            desiredColumns, mFilters[filter].selection, null,
                            SORT_ORDERSs[mSortOrder].sortOrder);
                    int indexId = cursor.getColumnIndex(COL_ID);
                    int indexDate = cursor.getColumnIndex(COL_DATE);
                    int indexNumber = cursor.getColumnIndex(COL_NUMBER);
                    int indexDuration = cursor.getColumnIndex(COL_DURATION);
                    int indexType = cursor.getColumnIndex(COL_TYPE);
                    int indexName = cursor.getColumnIndex(COL_NAME);

                    // Loop over items
                    cursor.moveToFirst();
                    while (cursor.isAfterLast() == false) {
                        String id = cursor.getString(indexId);
                        String number = "<Number NA>";
                        if (indexNumber > -1) {
                            number = cursor.getString(indexNumber);
                        }
                        Long dateNum = -1L;
                        if (indexDate > -1) {
                            dateNum = cursor.getLong(indexDate);
                        }
                        String duration = "<Duration NA>";
                        if (indexDuration > -1) {
                            duration = cursor.getString(indexDuration);
                        }
                        int type = -1;
                        if (indexType > -1) {
                            type = cursor.getInt(indexType);
                        }
                        String name = "Unknown";
                        if (indexName > -1) {
                            name = cursor.getString(indexName);
                            if (name == null) {
                                name = "Unknown";
                            }
                        }
                        out.write(id
                                + "\t"
                                + MessageUtils.formatDate(
                                MessageUtils.mediumFormatter, dateNum)
                                + "\t" + MessageUtils.formatAddress(number)
                                + "\t" + formatType(type) + "\t"
                                + formatDuration(duration) + "\t" + name +
								"\n");
                        cursor.moveToNext();
                    }
                } catch (Exception ex) {
                    out.write("Error finding calls\n" + ex.getMessage());
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                Utils.infoMsg(this, "Wrote " + file.getPath());
            } else {
                Utils.errMsg(this, "Cannot write to SD card");
                return;
            }
        } catch (Exception ex) {
            Utils.excMsg(this, "Error saving to SD card", ex);
        } finally {
            try {
                out.close();
            } catch (Exception ex) {
                // Do nothing
            }
        }
    }

    /**
     * Displays the call at the current position plus the current increment,
     * adjusting for being within range. Resets the increment to 0 after.
     */
    private void displayCall() {
        ListAdapter adapter = getListAdapter();
        if (adapter == null) {
            return;
        }
        try {
            int count = adapter.getCount();
            Log.d(TAG, this.getClass().getSimpleName() + ".displayCall: count="
                    + count);
            if (count == 0) {
                Utils.infoMsg(this, "There are no items in the list");
                return;
            }
            // Check if the item is still at the same position in the list
            boolean changed = false;
            long id = -1;
            if (mCurrentPosition > count - 1 || mCurrentPosition < 0) {
                changed = true;
            } else {
                id = adapter.getItemId(mCurrentPosition);
                if (id != mCurrentId) {
                    changed = true;
                }
            }
            // Determine the new mCurrentPosition
            Log.d(TAG, this.getClass().getSimpleName()
                    + ".displayCall: position=" + mCurrentPosition + " id=" + id
                    + " changed=" + changed);
            if (changed) {
                for (int i = 0; i < count; i++) {
                    id = adapter.getItemId(i);
                    if (id == mCurrentId) {
                        mCurrentPosition = i;
                        break;
                    }
                }
            }
            // mCurrentPosition may still be invalid, check it is in range
            if (mCurrentPosition < 0) {
                mCurrentPosition = 0;
            } else if (mCurrentPosition > count - 1) {
                mCurrentPosition = count - 1;
            }

            // Display calls if a requested increment is not possible
            if (increment > 0) {
                mCurrentPosition += increment;
                if (mCurrentPosition > count - 1) {
                    Toast.makeText(getApplicationContext(),
                            "At the last item in the list", Toast.LENGTH_LONG)
                            .show();
                    mCurrentPosition = count - 1;
                }
            } else if (increment < 0) {
                mCurrentPosition += increment;
                if (mCurrentPosition < 0) {
                    Toast.makeText(getApplicationContext(),
                            "At the first item in the list", Toast.LENGTH_LONG)
                            .show();
                    mCurrentPosition = 0;
                }
            }

            // Request the new call
            mCurrentId = adapter.getItemId(mCurrentPosition);
            Intent i = new Intent(this, DisplayCallActivity.class);
            i.putExtra(COL_ID, mCurrentId);
            i.putExtra(URI_KEY, getUri().toString());
            Log.d(TAG, this.getClass().getSimpleName()
                    + ".displayCall: position=" + mCurrentPosition
                    + " mCurrentId=" + mCurrentId);
            startActivityForResult(i, DISPLAY_CALL);
        } catch (Exception ex) {
            Utils.excMsg(this, "Error displaying call", ex);
        } finally {
            // Reset increment
            increment = 0;
        }
    }

    /**
     * Gets a new cursor and starts managing it.
     */
    private void refresh() {
        // TODO is refresh necessary? See the Notepad example where it is used.
        // TODO does calling this more than once cause memory leaks or extra
        // work?
        // TODO Probably causes extra work but does insure a refresh.
        try {
            // First get the names of all the columns in the database
            Cursor cursor = getContentResolver().query(getUri(), null, null,
                    null, null);
            String[] avaliableColumns = cursor.getColumnNames();
            cursor.close();

            // Make an array of the desired ones that are available
            String[] desiredColumns = {COL_ID, COL_NUMBER, COL_DATE,
                    COL_DURATION, COL_TYPE, COL_NAME};
            ArrayList<String> list = new ArrayList<String>();
            for (String col : desiredColumns) {
                for (String col1 : avaliableColumns) {
                    if (col.equals(col1)) {
                        list.add(col);
                        break;
                    }
                }
            }
            String[] columns = new String[list.size()];
            list.toArray(columns);

            // Get the available columns from all rows using the current
            // selection
            cursor = getContentResolver().query(getUri(), columns,
                    mFilters[filter].selection, null,
                    SORT_ORDERSs[mSortOrder].sortOrder);
            startManagingCursor(cursor);

            // Manage the mListAdapter
            if (mListAdapter == null) {
                // Set a custom cursor mListAdapter
                mListAdapter = new CustomCursorAdapter(getApplicationContext(),
                        cursor);
                setListAdapter(mListAdapter);
            } else {
                // This should close the current cursor and start using the new
                // one, hopefully avoiding memory leaks.
                mListAdapter.changeCursor(cursor);
            }
        } catch (Exception ex) {
            Utils.excMsg(this, "Error finding calls", ex);
        }
        // Save the preferences
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        editor.putInt("filter", filter);
        editor.putInt("mSortOrder", mSortOrder);
        editor.commit();
    }

    /**
     * @return The content provider URI used.
     */
    public Uri getUri() {
        return URI;
    }

    /**
     * Class to manage a filter.
     */
    private static class Filter {
        private CharSequence name;
        private String selection;

        private Filter(CharSequence menuName, String selection) {
            this.name = menuName;
            this.selection = selection;
        }
    }

    /**
     * Class to manage a sort order.
     */
    private static class SortOrder {
        private CharSequence name;
        private String sortOrder;

        private SortOrder(CharSequence menuName, String sortOrder) {
            this.name = menuName;
            this.sortOrder = sortOrder;
        }
    }

    /**
     * CursorAdapter for the CallHistoryActivity ListView.
     */
    private class CustomCursorAdapter extends CursorAdapter {
        private LayoutInflater inflater;
        private int indexDate;
        private int indexNumber;
        private int indexId;
        private int indexDuration;
        private int indexType;
        private int indexName;

        public CustomCursorAdapter(Context context, Cursor cursor) {
            super(context, cursor);
            inflater = LayoutInflater.from(context);
            indexId = cursor.getColumnIndex(COL_ID);
            indexDate = cursor.getColumnIndex(COL_DATE);
            indexNumber = cursor.getColumnIndex(COL_NUMBER);
            indexDuration = cursor.getColumnIndex(COL_DURATION);
            indexType = cursor.getColumnIndex(COL_TYPE);
            indexName = cursor.getColumnIndex(COL_NAME);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView title = (TextView) view.findViewById(R.id.title);
            TextView subtitle = (TextView) view.findViewById(R.id.subtitle);
            String id = cursor.getString(indexId);
            String number = "<Number NA>";
            if (indexNumber > -1) {
                number = cursor.getString(indexNumber);
            }
            Long dateNum = -1L;
            if (indexDate > -1) {
                dateNum = cursor.getLong(indexDate);
            }
            String duration = "<Duration NA>";
            if (indexDuration > -1) {
                duration = cursor.getString(indexDuration);
            }
            int type = -1;
            if (indexType > -1) {
                type = cursor.getInt(indexType);
            }
            String name = "Unknown";
            if (indexName > -1) {
                name = cursor.getString(indexName);
                if (name == null) {
                    name = "Unknown";
                }
            }
            title.setText(id + ": " + MessageUtils.formatAddress(number) + " ("
                    + formatType(type) + ") " + name);
            subtitle.setText(MessageUtils.formatDate(
                    MessageUtils.mediumFormatter, dateNum)
                    + " Duration: "
                    + formatDuration(duration));
            Log.d(TAG, getClass().getSimpleName() + ".bindView" + " id=" + id
                    + " number=" + number + " dateNum=" + dateNum);
            // DEBUG
            // if (id.equals(new Integer(76).toString())) {
            // test(1, this.getClass(), PhoneHistoryActivity.this, cursor, id,
            // getUri());
            // test(2, this.getClass(), PhoneHistoryActivity.this, null, id,
            // getUri());
            // }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return inflater.inflate(R.layout.list_row, parent, false);
        }

    }

}
