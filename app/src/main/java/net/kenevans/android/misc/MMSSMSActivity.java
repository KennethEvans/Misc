package net.kenevans.android.misc;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Manages a ListView of all the messages in the database.
 */
public class MMSSMSActivity extends AppCompatActivity implements IConstants {
    /**
     * The current position when ACTIVITY_DISPLAY_MESSAGE is requested. Used
     * with the resultCodes RESULT_PREV and RESULT_NEXT when they are returned.
     */
    private int mCurrentPosition;

    /**
     * The current id when ACTIVITY_DISPLAY_MESSAGE is requested. Used with the
     * resultCodes RESULT_PREV and RESULT_NEXT when they are returned.
     */
    private long mCurrentId;

    /**
     * The mIncrement for displaying the next message.
     */
    private long mIncrement = 0;

    /**
     * /**
     * The date multiplier to use to get ms. MMS message timestamps are in sec
     * not ms.
     */
    private static final Long DATE_MULTIPLIER = 1000L;

    /**
     * Enum to specify message type
     */
    private enum MessageType {SMS, MMS}

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

    /**
     * The sort order to use.
     */
    private Order mSortOrder = Order.TIME;

    private CustomListAdapter mListAdapter;
    private ListView mListView;

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

        // Set fast scroll
        mListView.setFastScrollEnabled(true);
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

    protected void onListItemClick(ListView lv, View view, int position, long
            id) {
        Log.d(TAG, this.getClass().getSimpleName() + ": onListItemClick: " +
                "position=" + position + " id=" + id);
        Data data = mListAdapter.getData(position);
        if (data == null) return;
        Log.d(TAG,
                "data: id=" + data.getId() + " " + data.getAddress());
        // Save the position when starting the activity
        mCurrentPosition = position;
        mCurrentId = data.getId();
        mIncrement = 0;
        displayMessage();
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
        if (requestCode == DISPLAY_MESSAGE) {
            mIncrement = 0;
            // Note that earlier items are at higher positions in the list
            if (resultCode == RESULT_PREV) {
                mIncrement = -1;
            } else if (resultCode == RESULT_NEXT) {
                mIncrement = 1;
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
            displayMessage();
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, this.getClass().getSimpleName()
                + ".onPause: mCurrentPosition=" + mCurrentPosition);
        Log.d(TAG, this.getClass().getSimpleName() + ".onPause: mCurrentId="
                + mCurrentId);
        super.onPause();
    }

    /**
     * Bring up a dialog to change the sort order.
     */
    private void setOrder() {
        final CharSequence[] items = {getText(R.string.sort_time),
                getText(R.string.sort_id)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getText(R.string.sort_title));
        builder.setSingleChoiceItems(items, mSortOrder == Order.TIME ? 0 : 1,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        dialog.dismiss();
                        mSortOrder = item == 0 ? Order.TIME : Order.ID;
                        refresh();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Displays the message at the current position plus the current mIncrement,
     * adjusting for being within range. Resets the mIncrement to 0 after.
     */
    private void displayMessage() {
        if (mListAdapter == null) {
            return;
        }
        try {
            int count = mListAdapter.getCount();
            Log.d(TAG, this.getClass().getSimpleName()
                    + ".displayMessage: count=" + count + " mCurrentId=" +
                    mCurrentId + " mCurrentPosition=" + mCurrentPosition);
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
                    Utils.errMsg(this, "Error displaying message: Missing " +
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
                    + ".displayMessage: position=" + mCurrentPosition + " id="
                    + id + " changed=" + changed);
            if (changed) {
                for (int i = 0; i < count; i++) {
                    Data data = mListAdapter.getData(mCurrentPosition);
                    if (data == null) {
                        Utils.errMsg(this, "Error displaying message: Missing" +
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
                            "At the last item in the list", Toast.LENGTH_LONG)
                            .show();
                    mCurrentPosition = count - 1;
                }
            } else if (mIncrement < 0) {
                mCurrentPosition += mIncrement;
                if (mCurrentPosition < 0) {
                    Toast.makeText(getApplicationContext(),
                            "At the first item in the list", Toast.LENGTH_LONG)
                            .show();
                    mCurrentPosition = 0;
                }
            }

            // Request the new message
            Data data = mListAdapter.getData(mCurrentPosition);
            if (data == null) {
                Utils.errMsg(this, "Error displaying message: Missing " +
                        "data for position " + mCurrentPosition);
                return;
            }
            mCurrentId = data.getId();
            Intent i;
            if (data.getType() == MessageType.SMS) {
                i = new Intent(this, DisplaySMSActivity.class);
                i.putExtra(URI_KEY, SMS_URI.toString());
            } else if (data.getType() == MessageType.MMS) {
                i = new Intent(this, DisplayMMSActivity.class);
                i.putExtra(URI_KEY, MMS_URI.toString());
            } else {
                Utils.errMsg(MMSSMSActivity.this,
                        "Invalid message type: " + data.getType());
                return;
            }
            i.putExtra(COL_ID, mCurrentId);
            Log.d(TAG, this.getClass().getSimpleName()
                    + ".displayMessage: position=" + mCurrentPosition
                    + " mCurrentId=" + mCurrentId);
            startActivityForResult(i, DISPLAY_MESSAGE);
        } catch (Exception ex) {
            Utils.excMsg(this, "Error displaying message", ex);
        } finally {
            // Reset mIncrement
            mIncrement = 0;
        }
    }

    /**
     * Gets a new cursor and starts managing it.
     */
    private void refresh() {
        // Initialize the list view mListAdapter
        mListAdapter = new CustomListAdapter();
        mListView.setAdapter(mListAdapter);
    }

    /**
     * Class to manage the data needed for an item in the ListView.
     */
    private static class Data {
        private long id;
        private String address;
        private long dateNum;
        private MessageType type;
        private boolean invalid = true;

        /**
         * Constructor.
         *
         * @param id The message id.
         */
        private Data(long id, long dateNum, MessageType type) {
            this.id = id;
            this.dateNum = dateNum;
            this.type = type;
        }

        /**
         * Sets the values for these parameters..
         *
         * @param address The address.
         */
        private void setValues(String address) {
            this.address = address;
            invalid = false;
        }

        @NonNull
        @Override
        public String toString() {
            return "Data {id=" + id + ", dateNum=" + dateNum + ", type=" + type + "}";
        }

        public long getId() {
            return id;
        }

        private String getAddress() {
            return address;
        }

        private long getDateNum() {
            return dateNum;
        }

        private boolean isInvalid() {
            return invalid;
        }

        public MessageType getType() {
            return type;
        }

        public void setType(MessageType type) {
            this.type = type;
        }
    }

    /**
     * ListView adapter class for this activity.
     */
    private class CustomListAdapter extends BaseAdapter {
        private List<Data> mDataList;

        private final LayoutInflater mInflator;

        private CustomListAdapter() {
            super();
            // DEBUG
            Log.d(TAG, this.getClass().getSimpleName() + " Start");
            mInflator = getLayoutInflater();
            mDataList = new ArrayList<>();
            Cursor cursor = null;
            long id = -1;
            int indexId = -1;
            int indexDate = -1;
            long dateNum;
            int nSMS = 0;
            int nItems = 0;
            MessageType type;
            String[] projection = new String[]{COL_ID, COL_DATE};
            // First do SMS
            type = MessageType.SMS;
            try {
                cursor = getContentResolver().query(SMS_URI, projection,
                        null, null, null);
                if (cursor == null) {
                    Utils.errMsg(MMSSMSActivity.this,
                            "ListAdapter: Error getting data: No SMS items in" +
                                    " database");

                } else {
                    indexId = cursor.getColumnIndex(COL_ID);
                    indexDate = cursor.getColumnIndex(COL_DATE);
                    int count = cursor.getCount();
                    if (count <= 0) {
                        Utils.infoMsg(MMSSMSActivity.this, "No items in " +
                                "database");
                    } else {
                        // Loop over items
                        if (cursor.moveToFirst()) {
                            while (!cursor.isAfterLast()) {
                                id = cursor.getLong(indexId);
                                dateNum = -1L;
                                if (indexDate > -1) {
                                    dateNum = cursor.getLong(indexDate);
                                }
                                mDataList.add(new Data(id, dateNum, type));
                                nItems++;
                                cursor.moveToNext();
                            }
                        }
                    }
                }
                nSMS = nItems;
                if (cursor != null) cursor.close();

                // Then do MMS
                type = MessageType.MMS;
                cursor = getContentResolver().query(MMS_URI, projection,
                        null, null, null);
                if (cursor == null) {
                    Utils.errMsg(MMSSMSActivity.this,
                            "ListAdapter: Error getting data: No items in " +
                                    "database");
                }
                indexId = cursor.getColumnIndex(COL_ID);
                int count = cursor.getCount();
                if (count <= 0) {
                    Utils.infoMsg(MMSSMSActivity.this, "No items in MMS " +
                            "database");
                } else {
                    // Loop over items
                    if (cursor.moveToFirst()) {
                        while (!cursor.isAfterLast()) {
                            id = cursor.getLong(indexId);
                            dateNum = -1L;
                            if (indexDate > -1) {
                                // Date multiplier needed for MMS, not SMS
                                dateNum =
                                        cursor.getLong(indexDate) * DATE_MULTIPLIER;
                            }
                            mDataList.add(new Data(id, dateNum, type));
                            nItems++;
                            cursor.moveToNext();
                        }
                    }
                }
                if (cursor != null) cursor.close();

                // Sort them
                Collections.sort(mDataList, new Comparator<Data>() {
                    @Override
                    public int compare(Data data1, Data data2) {
//                        if (data1 == null && data2 == null) {
//                            return 0;
//                        } else if (data1 == null) {
//                            return -1;
//                        } else if (data2 == null) {
//                            return 1;
//                        }
                        // Note these are in reverse order
                        if (mSortOrder == Order.ID) {
                            return Long.compare(data2.getId(), data1.getId());
                        } else {
                            return Long.compare(data2.getDateNum(),
                                    data1.getDateNum());
                        }
                    }
                });
            } catch (Exception ex) {
                Utils.excMsg(MMSSMSActivity.this,
                        "ListAdapter: Error getting " + type
                                + " data at id=" + id, ex);
                String stackTrace = Log.getStackTraceString(ex);
                Log.e(TAG, "ListAdapter: Error getting " + type
                        + " data at id=" + id, ex);
            } finally {
                try {
                    if (cursor != null) cursor.close();
                } catch (Exception ex) {
                    // Do nothing
                }
            }
            Log.d(TAG, "Data list created: processed " + nItems + " items"
                    + " mDataList.length=" + mDataList.size()
                    + " nSMS=" + nSMS + " nMMS=" + (nItems - nSMS));
        }

        private Data getData(int i) {
            if (mDataList == null || i < 0 || i >= mDataList.size()) {
                return null;
            }
            return mDataList.get(i);
        }

        @Override
        public int getCount() {
            return mDataList == null ? 0 : mDataList.size();
        }

        @Override
        public Object getItem(int i) {
            if (mDataList == null || i < 0 || i >= mDataList.size()) {
                return null;
            }
            return mDataList.get(i);
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
            try {
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
                if (i < 0 || i >= mDataList.size()) {
                    titleText = "Error";
                    subTitleText = "Bad view index" + i + " (Should be 0 " +
                            "to "
                            + mDataList.size() + ")";
                    viewHolder.title.setText(titleText);
                    viewHolder.subTitle.setText(subTitleText);
                    return view;
                }
                Data data = mDataList.get(i);
                if (data == null) {
                    titleText = "Error";
                    subTitleText = "Cannot find data for i=" + i;
                    viewHolder.title.setText(titleText);
                    viewHolder.subTitle.setText(subTitleText);
                    return view;
                }

                // Only calculate what is needed (i.e visible)
                // Speeds up tremendously over calculating everything before
                if (data.isInvalid()) {
                    long id;
                    String address;
                    String idStr;
                    long dateNum;
                    int indexId = -1;
                    int indexAddress = -1;
                    int indexDate = -1;
                    Cursor cursor = null;
                    if (data.getType() == MessageType.SMS) {
                        // SMS
//                        Log.d(TAG, "SMS: data.getType()=" + data.getType());
                        String[] projection = new String[]{COL_ID, COL_ADDRESS};
                        cursor = getContentResolver().query(SMS_URI,
                                projection,
                                COL_ID + "=" + data.getId(), null, null);
//                        Log.d(TAG,
//                                "getView: SMS id=" + data.getId() + "
//                                cursor" +
//                                        "=" + cursor);
                        if (cursor != null && cursor.moveToFirst()) {
                            indexAddress =
                                    cursor.getColumnIndex(COL_ADDRESS);
                            address = "<Address NA>";
                            if (indexAddress > -1) {
                                address = cursor.getString(indexAddress);
                            }
                            data.setValues(address);
                        }
                        if (cursor != null) cursor.close();
                    } else {
                        // MMS
//                        Log.d(TAG, "MMS: data.getType()=" + data.getType());
                        String[] projection = new String[]{COL_ID};
                        cursor = getContentResolver().query(MMS_URI,
                                projection,
                                COL_ID + "=" + data.getId(), null, null);
//                        Log.d(TAG,
//                                "getView: MMS id=" + data.getId() + "
//                                cursor" +
//                                        "=" + cursor);
                        if (cursor != null && cursor.moveToFirst()) {
                            // We need to get the address from another
                            // provider
                            indexId = cursor.getColumnIndex(COL_ID);
                            address = "<Address NA>";
                            idStr = cursor.getString(indexId);
                            Log.d(TAG,
                                    "indexId=" + indexId + "indexDate=" + indexDate);
                            // Determine if From or To
                            String fromAddr =
                                    MessageUtils.getMmsAddress(MMSSMSActivity.this, 137, idStr);
                            String toAddr =
                                    MessageUtils.getMmsAddress(MMSSMSActivity.this, 151, idStr);
                            if (fromAddr != null) {
                                if (!fromAddr.equals("insert-address-token")) {
                                    address = "From: " + fromAddr;
                                } else {
                                    // Is outgoing
                                    if (toAddr != null) {
                                        address = "To: " + toAddr;
                                    }
                                }
                            }
                            data.setValues(address);
                        }
                        if (cursor != null) cursor.close();
                    }
                }

                titleText =
                        String.format(Locale.US, "%d", data.getId()) + ": "
                                + data.getType() + " "
                                + MessageUtils.formatAddress(data.getAddress());
                subTitleText =
                        MessageUtils.formatDate(data.getDateNum());
//                try {
//                    subTitleText = Long.toString(data.getDateNum());
//                } catch (Exception ex) {
//                    subTitleText = "<error>";
//                }
                String contactName = MessageUtils.getContactNameFromNumber(
                        MMSSMSActivity.this, data.getAddress());
                if (contactName != null && !contactName.equals("Unknown")) {
                    titleText += " " + contactName;
                }
//                Log.d(TAG, "titleText=" + titleText);
//                Log.d(TAG, "subTitleText=" + subTitleText);
                viewHolder.title.setText(titleText);
                viewHolder.subTitle.setText(subTitleText);
            } catch (
                    Exception ex) {
                Utils.excMsg(MMSSMSActivity.this, "Error getting View", ex);
            }
//            Log.d(TAG, "view=" + view);
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
