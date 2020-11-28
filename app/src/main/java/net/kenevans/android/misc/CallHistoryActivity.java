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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.CallLog;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

import static net.kenevans.android.misc.MessageUtils.formatDate;

/**
 * Manages a ListView of all the calls in the database specified by the URI
 * field.
 */
public class CallHistoryActivity extends AppCompatActivity implements IConstants {
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
    private long mIncrement = 0;

    /**
     * The Uri to use for the database.
     */
    private static final Uri URI = CALLLOG_CALLS_URI;

    /**
     * Enum to specify the sort order.
     */
    private enum Order {
        TIME(COL_DATE + " DESC"), ID(COL_ID + " DESC");
        public final String sqlCommand;

        Order(String sqlCommand) {
            this.sqlCommand = sqlCommand;
        }
    }

    private ListView mListView;

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
    private SortOrder[] sortOrders;
    /**
     * The current sort order.
     */
    private int mSortOrder = 0;

    /**
     * Template for the name of the file written to the root of the SD card
     */
    private static final String SAVE_FILE_NAME = "CallHistory.%s.csv";

    private CustomListAdapter mListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_view);
        mListView = findViewById(R.id.mainListView);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                onListItemClick(mListView, view, position, id);
            }
        });

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
        sortOrders = new SortOrder[]{
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
        mSortOrder = prefs.getInt("sortOrder", 0);
        if (mSortOrder < 0 || mSortOrder >= sortOrders.length) {
            mSortOrder = 0;
        }

        // Set fast scroll
        mListView.setFastScrollEnabled(true);

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

    protected void onListItemClick(ListView lv, View view, int position, long
            id) {
        // Save the position when starting the activity
        mCurrentPosition = position;
        mCurrentId = id;
        mIncrement = 0;
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
            mIncrement = 0;
            // Note that earlier items are at higher positions in the list
            if (resultCode == RESULT_PREV) {
                mIncrement = -1;
            } else if (resultCode == RESULT_NEXT) {
                mIncrement = 1;
            }
        } else if (requestCode == CREATE_DOCUMENT && resultCode == Activity.RESULT_OK) {
            Uri uri;
            if (intent != null) {
                uri = intent.getData();
                Log.d(TAG, "uri=" + uri);
                doSave(uri);
            }
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, this.getClass().getSimpleName()
                + ".onResume: mCurrentPosition=" + mCurrentPosition
                + " mCurrentId=" + mCurrentId + " mIncrement=" + mIncrement);
        super.onResume();
        refresh();
        // If mIncrement is set display a new message
        if (mIncrement != 0) {
            displayCall();
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
        final CharSequence[] items = new CharSequence[sortOrders.length];
        for (int i = 0; i < sortOrders.length; i++) {
            items[i] = sortOrders[i].name;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getText(R.string.sort_title));
        builder.setSingleChoiceItems(items, mSortOrder,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        dialog.dismiss();
                        if (item < 0 || item >= sortOrders.length) {
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
     * @param duration The given duration.
     * @return The formatted duration.
     */
    public static String formatDuration(String duration) {
        if (duration == null || duration.length() == 0) {
            return "<Unknown>";
        }
        int seconds;
        try {
            seconds = Integer.parseInt(duration);
        } catch (NumberFormatException ex) {
            return "<Invalid>";
        }

        int hours = seconds / 3600;
        seconds -= hours * 3600;
        int minutes = seconds / 60;
        seconds -= minutes * 60;

        return String.format(Locale.US, "%d:%02d:%02d", hours, minutes,
                seconds);
    }

    /**
     * Format the type as a string.
     *
     * @param type The given type.
     * @return The formatted type.
     */
    public static String formatType(int type) {
        if (type == CallLog.Calls.INCOMING_TYPE) {
            return "Incoming";
        } else if (type == CallLog.Calls.OUTGOING_TYPE) {
            return "Outgoing";
        } else if (type == CallLog.Calls.MISSED_TYPE) {
            return "Missed";
        } else if (type == CallLog.Calls.VOICEMAIL_TYPE) {
            return "Voicemail";
        } else if (type == CallLog.Calls.REJECTED_TYPE) {
            return "Rejected";
        } else if (type == CallLog.Calls.BLOCKED_TYPE) {
            return "Blocked";
        } else if (type == CallLog.Calls.ANSWERED_EXTERNALLY_TYPE) {
            return "Answered Externally";
        } else {
            return "<Unknown type>";
        }
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
            intent.putExtra(INFO_URL, "file:///android_asset/callhistory" +
                    ".html");
            startActivity(intent);
        } catch (Exception ex) {
            Utils.excMsg(this, "Error showing Help", ex);
        }
    }

    /**
     * Asks for the name of the save file
     */
    private void save() {
        try {
            File sdCardRoot = Environment.getExternalStorageDirectory();
            String format = "yyyy-MM-dd-HHmmss";
            SimpleDateFormat formatter = new SimpleDateFormat(format,
                    Locale.US);
            Date now = new Date();
            String fileName = String.format(SAVE_FILE_NAME,
                    formatter.format(now));
            Uri.Builder builder = new Uri.Builder();
            builder.path(sdCardRoot.getPath())
                    .appendPath(SD_CARD_MISC_DIR);
            Uri uri = builder.build();

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_TITLE, fileName);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
            }
            Log.d(TAG, this.getClass().getSimpleName()
                    + ".save: uri=" + uri);
            startActivityForResult(intent, CREATE_DOCUMENT);
        } catch (Exception ex) {
            Utils.excMsg(this, "Error requesting saving to SD card", ex);
        }
    }

    /**
     * Saves the info to the SD card
     */
    private void doSave(Uri uri) {
        FileWriter writer = null;
        BufferedWriter out = null;
        Cursor cursor = null;
        try {
            ParcelFileDescriptor pfd = getContentResolver().
                    openFileDescriptor(uri, "w");
            writer =
                    new FileWriter(pfd.getFileDescriptor());

            out = new BufferedWriter(writer);
            out.write("id," + "date," + "number," + "type,"
                    + "duration," + "name\n");

            // Get the database again to avoid traversing the ListView,
            // which only has visible items

            // Get a cursor
            String[] desiredColumns = {COL_ID, COL_NUMBER, COL_DATE,
                    COL_DURATION, COL_TYPE, COL_NAME};
            cursor = getContentResolver().query(getUri(),
                    desiredColumns, mFilters[filter].selection, null,
                    sortOrders[mSortOrder].sortOrder);
            int indexId = cursor.getColumnIndex(COL_ID);
            int indexDate = cursor.getColumnIndex(COL_DATE);
            int indexNumber = cursor.getColumnIndex(COL_NUMBER);
            int indexDuration = cursor.getColumnIndex(COL_DURATION);
            int indexType = cursor.getColumnIndex(COL_TYPE);
            int indexName = cursor.getColumnIndex(COL_NAME);

            // Loop over items
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String id = cursor.getString(indexId);
                String number = "<Number NA>";
                if (indexNumber > -1) {
                    number = cursor.getString(indexNumber);
                }
                long dateNum = -1L;
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
                        + ","
                        + "\""
                        + formatDate(MessageUtils.mediumFormatter,
                        dateNum) + "\","
                        + MessageUtils.formatAddress(number) + ","
                        + formatType(type) + ","
                        + formatDuration(duration) + "," + name +
                        "\n");
                cursor.moveToNext();
            }
        } catch (Exception ex) {
            Utils.excMsg(this, "Error finding calls", ex);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            try {
                if (writer != null) writer.close();
                if (out != null) out.close();
            } catch (Exception ex) {
                // No nothing
            }
        }
        Utils.infoMsg(this, "Wrote " + uri.getPath());
    }

    /**
     * Displays the call at the current position plus the current increment,
     * adjusting for being within range. Resets the increment to 0 after.
     */
    private void displayCall() {
        if (mListAdapter == null) {
            return;
        }
        try {
            int count = mListAdapter.getCount();
            Log.d(TAG, this.getClass().getSimpleName() + ".displayCall: " +
                    "count="
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
                Data data = mListAdapter.getData(mCurrentPosition);
                if (data == null) {
                    Utils.errMsg(this, "Error displaying message: Missing" +
                            " " +
                            "data for position " + mCurrentPosition);
                    return;
                }
                id = data.getId();
                if (id != mCurrentId) {
                    changed = true;
                }
            }
            // Determine the new mCurrentPosition
            Log.d(TAG, this.getClass().getSimpleName()
                    + ".displayCall: position=" + mCurrentPosition + " " +
                    "id=" + id
                    + " changed=" + changed);
            if (changed) {
                for (int i = 0; i < count; i++) {
                    Data data = mListAdapter.getData(mCurrentPosition);
                    if (data == null) {
                        Utils.errMsg(this, "Error displaying message: " +
                                "Missing" +
                                " " + "data for position " + mCurrentPosition);
                        return;
                    }
                    id = data.getId();
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

            // Display messages if a requested mIncrement is not possible
            if (mIncrement > 0) {
                mCurrentPosition += mIncrement;
                if (mCurrentPosition > count - 1) {
                    Toast.makeText(getApplicationContext(),
                            "At the last item in the list",
                            Toast.LENGTH_LONG)
                            .show();
                    mCurrentPosition = count - 1;
                }
            } else if (mIncrement < 0) {
                mCurrentPosition += mIncrement;
                if (mCurrentPosition < 0) {
                    Toast.makeText(getApplicationContext(),
                            "At the first item in the list",
                            Toast.LENGTH_LONG)
                            .show();
                    mCurrentPosition = 0;
                }
            }

            // Request the new call
            Data data = mListAdapter.getData(mCurrentPosition);
            if (data == null) {
                Utils.errMsg(this, "Error displaying message: Missing " +
                        "data for position " + mCurrentPosition);
                return;
            }
            mCurrentId = data.getId();
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
            mIncrement = 0;
        }
    }

    /**
     * Gets a new cursor and starts managing it.
     */
    private void refresh() {
        // Initialize the list view mAapter
        mListAdapter = new CustomListAdapter();
        mListView.setAdapter(mListAdapter);
    }

    /**
     * @return The content provider URI used.
     */
    private Uri getUri() {
        return URI;
    }

    /**
     * Class to manage a filter.
     */
    private static class Filter {
        private final CharSequence name;
        private final String selection;

        private Filter(CharSequence menuName, String selection) {
            this.name = menuName;
            this.selection = selection;
        }
    }

    /**
     * Class to manage a sort order.
     */
    private static class SortOrder {
        private final CharSequence name;
        private final String sortOrder;

        private SortOrder(CharSequence menuName, String sortOrder) {
            this.name = menuName;
            this.sortOrder = sortOrder;
        }
    }

    /**
     * Class to manage the data needed for an item in the ListView.
     */
    private static class Data {
        private final long id;
        private String number;
        private long dateNum = -1;
        private String duration;
        private int type;
        private String name;
        private boolean invalid = true;

        private Data(long id) {
            this.id = id;
        }

        private void setValues(String number, long dateNum, String duration,
                               int type, String name) {
            this.number = number;
            this.dateNum = dateNum;
            this.duration = duration;
            this.type = type;
            this.name = name;
            invalid = false;
        }

        private long getId() {
            return id;
        }

        private String getNumber() {
            return number;
        }

        private long getDateNum() {
            return dateNum;
        }

        private String getDuration() {
            return duration;
        }

        private int getType() {
            return type;
        }

        private String getName() {
            return name;
        }

        private boolean isInvalid() {
            return invalid;
        }
    }

    /**
     * ListView adapter class for this activity.
     */
    private class CustomListAdapter extends BaseAdapter {
        private String[] mDesiredColumns;
        private Data[] mDataArray;
        private final LayoutInflater mInflator;
        private int mIndexId;
        private int mIndexDate;
        private int mIndexType;
        private int mIndexNumber;
        private int mIndexDuration;
        private int mIndexName;

        private CustomListAdapter() {
            super();
            // DEBUG
//            Log.d(TAG, this.getClass().getSimpleName() + " Start");
//            Date start = new Date();
            mInflator = getLayoutInflater();
            Cursor cursor = null;
            int nItems = 0;
            try {
                // First get the names of all the columns in the database
                String[] availableColumns;
                cursor = getContentResolver().query(getUri(), null, null,
                        null, null);
                if (cursor == null) {
                    availableColumns = new String[0];
                } else {
                    availableColumns = cursor.getColumnNames();
                    cursor.close();
                }

                // Make an array of the desired ones that are available
                String[] desiredColumns = {COL_ID, COL_DATE,
                        COL_TYPE, COL_NUMBER, COL_DURATION, COL_NAME};
                ArrayList<String> list = new ArrayList<>();
                for (String col : desiredColumns) {
                    for (String col1 : availableColumns) {
                        if (col.equals(col1)) {
                            list.add(col);
                            break;
                        }
                    }
                }
                mDesiredColumns = new String[list.size()];
                list.toArray(mDesiredColumns);

                // Get the available columns from all rows
                cursor = getContentResolver().query(getUri(),
                        mDesiredColumns, mFilters[filter].selection, null,
                        sortOrders[mSortOrder].sortOrder);
                if (cursor == null) {
                    Utils.errMsg(CallHistoryActivity.this,
                            "ListAdapter: Error getting data: No items in" +
                                    " " +
                                    "database");
                    return;
                }

                mIndexId = cursor.getColumnIndex(COL_ID);
                mIndexDate = cursor.getColumnIndex(COL_DATE);
                mIndexType = cursor.getColumnIndex(COL_TYPE);
                mIndexNumber = cursor.getColumnIndex(COL_NUMBER);
                mIndexDuration = cursor.getColumnIndex(COL_DURATION);
                mIndexName = cursor.getColumnIndex(COL_NAME);

                int count = cursor.getCount();
                mDataArray = new Data[count];

                if (count <= 0) {
                    Utils.infoMsg(CallHistoryActivity.this, "No items in " +
                            "database");
                } else {
                    // Loop over items
                    if (cursor.moveToFirst()) {
                        while (!cursor.isAfterLast()) {
                            long id = cursor.getLong(mIndexId);
                            mDataArray[nItems] = new Data(id);
                            nItems++;
                            cursor.moveToNext();
                        }
                    }
                }
            } catch (Exception ex) {
                Utils.excMsg(CallHistoryActivity.this,
                        "ListAdapter: Error getting data", ex);
            } finally {
                try {
                    if (cursor != null) cursor.close();
                } catch (Exception ex) {
                    // Do nothing
                }
            }
            // DEBUG
//            Date end = new Date();
//            double elapsed = (end.getTime() - start.getTime()) / 1000.;
//            Log.d(TAG, "Elapsed time=" + elapsed);
            Log.d(TAG, "Data list created with " + nItems + " items");
        }

        private Data getData(int i) {
            if (mDataArray == null || i < 0 || i >= mDataArray.length) {
                return null;
            }
            return mDataArray[i];
        }

        @Override
        public int getCount() {
            return mDataArray == null ? 0 : mDataArray.length;
        }

        @Override
        public Object getItem(int i) {
            if (mDataArray == null || i < 0 || i >= mDataArray.length) {
                return null;
            }
            return mDataArray[i];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            // DEBUG
//            Log.d(TAG, this.getClass().getSimpleName() + ": i=" + i);
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.list_row, viewGroup,
                        false);
                viewHolder = new ViewHolder();
                viewHolder.title = view.findViewById(R.id.title);
                viewHolder.subTitle = view.findViewById(R.id
                        .subtitle);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            String titleText;
            String subTitleText;

            // Check if index is OK.
            if (i < 0 || i >= mDataArray.length) {
                titleText = "Error";
                subTitleText = "Bad view index" + i + " (Should be 0 to "
                        + mDataArray.length + ")";
                viewHolder.title.setText(titleText);
                viewHolder.subTitle.setText(subTitleText);
                return view;
            }
            Data data = mDataArray[i];
            if (data == null) {
                titleText = "Error";
                subTitleText = "Cannot find data for i=" + i;
                viewHolder.title.setText(titleText);
                viewHolder.subTitle.setText(subTitleText);
                return view;
            }

            if (data.isInvalid()) {
                // Get the values for this item
                Cursor cursor = getContentResolver().query(getUri(),
                        mDesiredColumns,
                        COL_ID + "=" + mDataArray[i].getId(), null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    String number = "<Number NA>";
                    if (mIndexNumber > -1) {
                        number = cursor.getString(mIndexNumber);
                    }
                    long dateNum = -1L;
                    if (mIndexDate > -1) {
                        dateNum = cursor.getLong(mIndexDate);
                    }
                    String duration = "<Duration NA>";
                    if (mIndexDuration > -1) {
                        duration = cursor.getString(mIndexDuration);
                    }
                    int type = -1;
                    if (mIndexType > -1) {
                        type = cursor.getInt(mIndexType);
                    }
                    String name = "Unknown";
                    if (mIndexName > -1) {
                        name = cursor.getString(mIndexName);
                        if (name == null) {
                            name = "Unknown";
                        }
                    }
                    data.setValues(number, dateNum, duration, type, name);
                }
                if (cursor != null) cursor.close();
            }
            mDataArray[i] = data;

            titleText = String.format(Locale.US, "%d", data.getId()) + ": " +
                    MessageUtils.formatAddress(data.getNumber())
                    + " (" + formatType(data.getType()) + ") " + data.getName();
            subTitleText = formatDate(MessageUtils.mediumFormatter,
                    data.getDateNum()) + " Duration: "
                    + formatDuration(data.getDuration());
            viewHolder.title.setText(titleText);
            viewHolder.subTitle.setText(subTitleText);
            return view;
        }
    }

    /**
     * Convenience class for managing views for a ListView row.
     */
    private static class ViewHolder {
        TextView title;
        TextView subTitle;
    }

}
