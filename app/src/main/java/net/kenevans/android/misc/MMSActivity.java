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

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
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
import android.widget.CursorAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Manages a ListView of all the messages in the database specified by the
 * URI field.
 */

/**
 * @author evans
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
    public static final Uri URI
            = MMS_URI;

    /**
     * The date multiplier to use to get ms. MMS message timestamps are in sec
     * not ms.
     */
    public static final Long DATE_MULTIPLIER
            = 1000L;

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
     * The sort order to use.
     */
    private Order mSortOrder = Order.TIME;

    private CustomCursorAdapter mListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
    protected void onListItemClick(ListView lv, View view, int position, long
            id) {
        super.onListItemClick(lv, view, position, id);
        // Save the position when starting the activity
        mCurrentPosition = position;
        mCurrentId = id;
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
    protected void onPause() {
        Log.d(TAG, this.getClass().getSimpleName()
                + ".onPause: mCurrentPosition=" + mCurrentPosition);
        Log.d(TAG, this.getClass().getSimpleName() + ".onPause: mCurrentId="
                + mCurrentId);
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, this.getClass().getSimpleName()
                + ".onResume: mCurrentPosition=" + mCurrentPosition
                + " mCurrentId=" + mCurrentId + " mIncrement=" + mIncrement);
        super.onResume();
        // If mIncrement is set display a new message
        if (mIncrement != 0) {
            displayMessage();
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
                    + ".displayMessage: position=" + mCurrentPosition + " id="
                    + id + " changed=" + changed);
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
            mCurrentId = adapter.getItemId(mCurrentPosition);
            Intent i = new Intent(this, DisplayMMSActivity.class);
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
            String[] desiredColumns = {COL_ID, COL_ADDRESS, COL_DATE,
                    COL_BODY, COL_TYPE};
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

            // Get the available columns from all rows
            // String selection = COL_ID + "<=76" + " OR " + COL_ID + "=13";
            String selection = null;
            cursor = getContentResolver().query(getUri(), columns, selection,
                    null, mSortOrder.sqlCommand);
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
            Utils.excMsg(this, "Error finding MMS messages", ex);
        }
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

    private class CustomCursorAdapter extends CursorAdapter {
        private LayoutInflater inflater;
        private int indexDate;
        private int indexId;
        private int indexType;

        public CustomCursorAdapter(Context context, Cursor cursor) {
            super(context, cursor);
            inflater = LayoutInflater.from(context);
            indexId = cursor.getColumnIndex(COL_ID);
            indexDate = cursor.getColumnIndex(COL_DATE);
            indexType = cursor.getColumnIndex(COL_TYPE);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView title = (TextView) view.findViewById(R.id.title);
            TextView subtitle = (TextView) view.findViewById(R.id.subtitle);
            String id = cursor.getString(indexId);
            // We need to get the address from another provider
            String address = "<Address NA>";
            String text = MessageUtils.getMmsAddress(MMSActivity.this, id);
            if (text != null) {
                address = text;
            }
            Long dateNum = -1L;
            if (indexDate > -1) {
                dateNum = cursor.getLong(indexDate) * getDateMultiplier();
            }
            int type = -1;
            if (indexType > -1) {
                type = cursor.getInt(indexType);
            }
            String titleText = id + ": " + MessageUtils.formatSmsType(type)
                    + MessageUtils.formatAddress(address);
            String contactName = MessageUtils.getContactNameFromNumber(
                    MMSActivity.this, address);
            if (!contactName.equals("Unknown")) {
                titleText += " " + contactName;
            }
            title.setText(titleText);
            subtitle.setText(MessageUtils.formatDate(dateNum));
            // Log.d(TAG, getClass().getSimpleName() + ".bindView" + " id=" + id
            // + " address=" + address + " dateNum=" + dateNum
            // + " DATE_MULTIPLIER=" + getDateMultiplier());
            // DEBUG
            // if (id.equals(new Integer(76).toString())) {
            // test(1, this.getClass(), SMSActivity.this, cursor, id, getUri());
            // test(2, this.getClass(), SMSActivity.this, null, id, getUri());
            // }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return inflater.inflate(R.layout.list_row, parent, false);
        }

    }

}
