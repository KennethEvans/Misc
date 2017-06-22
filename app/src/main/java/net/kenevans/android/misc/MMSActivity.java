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

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

import static android.R.attr.type;
import static net.kenevans.android.misc.MessageUtils.formatDate;

/**
 * Manages a ListView of all the messages in the database specified by the
 * URI field.
 */
public class MMSActivity extends ListActivity implements IConstants {
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
     * The Uri to use.
     */
    public static final Uri URI = MMS_URI;

    /**
     * The date multiplier to use to get ms. MMS message timestamps are in sec
     * not ms.
     */
    public static final Long DATE_MULTIPLIER
            = 1000L;

    /**
     * Enum to specify the sort order.
     */
    private enum Order {
        TIME(COL_DATE + " DESC"), ID(COL_ID + " DESC");
        public String sqlCommand;

        Order(String sqlCommand) {
            this.sqlCommand = sqlCommand;
        }
    }

    /**
     * The sort order to use.
     */
    private Order mSortOrder = Order.TIME;

    private CustomListAdapter mListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    protected void onListItemClick(ListView lv, View view, int position, long
            id) {
        Log.d(TAG, this.getClass().getSimpleName() + ": onListItemClick: " +
                "position="
                + position + " id=" + id);
        Data data = mListAdapter.getData(position);
        if (data == null) return;
        Log.d(TAG, "data: id=" + data.getId() + " " + data.getAddress());
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
                mIncrement = 1;
            } else if (resultCode == RESULT_NEXT) {
                mIncrement = -1;
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
        if (mListAdapter != null) {
            mListAdapter.clear();
        }
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
                id = mListAdapter.getData(mCurrentPosition).getId();
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
                    id = mListAdapter.getData(mCurrentPosition).getId();
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
            mCurrentId = mListAdapter.getData(mCurrentPosition).getId();
            Intent i = new Intent(this, DisplaySMSActivity.class);
            i.putExtra(COL_ID, mCurrentId);
            i.putExtra(URI_KEY, getUri().toString());
            i.putExtra(DATE_MULTIPLIER_KEY, getDateMultiplier());
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
        // Initialize the list view mAapter
        mListAdapter = new CustomListAdapter();
        setListAdapter(mListAdapter);
    }

    /**
     * @return The content provider URI used.
     */
    public Uri getUri() {
        return URI;
    }

    /**
     * @return The date multiplier to use.
     */
    public Long getDateMultiplier() {
        return DATE_MULTIPLIER;
    }

    /**
     * Class to manage the data needed for an item in the ListView.
     */
    private static class Data {
        private long id;
        private String address;
        private long dateNum = -1;
        private int type;

        private Data(long id, String address, long dateNum, int type) {
            this.id = id;
            this.address = address;
            this.dateNum = dateNum;
            this.type = type;
        }

        private long getId() {
            return id;
        }

        private String getAddress() {
            return address;
        }

        private long getDateNum() {
            return dateNum;
        }

        private int getType() {
            return type;
        }
    }

    /**
     * ListView adapter class for this activity.
     */
    private class CustomListAdapter extends BaseAdapter {
        private ArrayList<Data> mData;
        private LayoutInflater mInflator;
        private int indexId;
        private int indexDate;
        private int indexType;

        private CustomListAdapter() {
            super();
            mData = new ArrayList<>();
            mInflator = getLayoutInflater();
            Cursor cursor = null;
            int nItems = 0;
            try {
                // First get the names of all the columns in the database
                cursor = getContentResolver().query(getUri(), null, null,
                        null, null);
                String[] avaliableColumns = cursor.getColumnNames();
                cursor.close();

                // Make an array of the desired ones that are available
                String[] desiredColumns = {COL_ID, COL_ADDRESS, COL_DATE,
                        COL_BODY, COL_TYPE};
                ArrayList<String> list = new ArrayList<>();
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

                // Get the available columns from all rows
                // String selection = COL_ID + "<=76" + " OR " + COL_ID + "=13";
                cursor = getContentResolver().query(getUri(), columns,
                        null, null, mSortOrder.sqlCommand);

                indexId = cursor.getColumnIndex(COL_ID);
                indexDate = cursor.getColumnIndex(COL_DATE);
                indexType = cursor.getColumnIndex(COL_TYPE);

                // Loop over items
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    nItems++;
                    long id = cursor.getLong(indexId);
                    // We need to get the address from another provider
                    String address = "<Address NA>";
                    String text = MessageUtils.getMmsAddress(MMSActivity
                            .this, String.format(Locale.US, "%d", id));
                    if (text != null) {
                        address = text;
                    }
                    Long dateNum = -1L;
                    if (indexDate > -1) {
                        dateNum = cursor.getLong(indexDate) *
                                getDateMultiplier();
                    }
                    int type = -1;
                    if (indexType > -1) {
                        type = cursor.getInt(indexType);
                    }
                    addData(new Data(id, address, dateNum, type));
                    cursor.moveToNext();
                }
                if (cursor != null) cursor.close();
            } catch (Exception ex) {
                Utils.excMsg(MMSActivity.this,
                        "Error getting data", ex);
            } finally {
                try {
                    if (cursor != null) cursor.close();
                } catch (Exception ex) {
                    // Do nothing
                }
            }
            Log.d(TAG, "Data list created with " + nItems + " items");
        }

        private void addData(Data data) {
            if (!mData.contains(data)) {
                mData.add(data);
            }
        }

        private Data getData(int position) {
            return mData.get(position);
        }

        private void clear() {
            mData.clear();
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Object getItem(int i) {
            return mData.get(i);
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
                view = mInflator.inflate(R.layout.list_row, viewGroup,
                        false);
                viewHolder = new ViewHolder();
                viewHolder.title = (TextView) view.findViewById(R.id.title);
                viewHolder.subTitle = (TextView) view.findViewById(R.id
                        .subtitle);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            Data data = mData.get(i);
            String titleText = String.format(Locale.US, "%d", data.getId()) +
                    ": " + MessageUtils.formatSmsType(type)
                    + MessageUtils.formatAddress(data.getAddress());
            String contactName = MessageUtils.getContactNameFromNumber(
                    MMSActivity.this, data.getAddress());
            if (!contactName.equals("Unknown")) {
                titleText += " " + contactName;
            }
            viewHolder.title.setText(titleText);
            viewHolder.subTitle.setText(formatDate(data.getDateNum()));
            return view;
        }
    }

    /**
     * Convience class for managing views for a ListView row.
     */
    private static class ViewHolder {
        TextView title;
        TextView subTitle;
    }

}
