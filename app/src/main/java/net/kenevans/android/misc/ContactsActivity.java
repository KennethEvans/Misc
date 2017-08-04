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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

import static android.R.attr.id;

/**
 * Manages a ListView of all the contacts in the database specified by the
 * URI field.
 */
public class ContactsActivity extends ListActivity implements IConstants {
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
    private static final Uri URI
            = ContactsContract.Contacts.CONTENT_URI;

    /**
     * Enum to specify the sort order.
     */
    private enum Order {
        NAME(ContactsContract.Contacts.DISPLAY_NAME + " ASC"), ID(COL_ID
                + " ASC");
        public final String sqlCommand;

        Order(String sqlCommand) {
            this.sqlCommand = sqlCommand;
        }
    }

    /**
     * The sort order to use.
     */
    private Order mSortOrder = Order.NAME;

    private CustomListAdapter mListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set fast scroll
        getListView().setFastScrollEnabled(true);
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
                "position=" + position + " id=" + id);
        Data data = mListAdapter.getData(position);
        if (data == null) return;
        Log.d(TAG, "data: id=" + data.getId() + " " + data.getName());
        // Save the position when starting the activity
        mCurrentPosition = position;
        mCurrentId = data.getId();
        mIncrement = 0;
        displayContact();
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
            displayContact();
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
        final CharSequence[] items = {getText(R.string.sort_name),
                getText(R.string.sort_id)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getText(R.string.sort_title));
        builder.setSingleChoiceItems(items, mSortOrder == Order.NAME ? 0 : 1,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        dialog.dismiss();
                        mSortOrder = item == 0 ? Order.NAME : Order.ID;
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
    private void displayContact() {
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
            Intent i = new Intent(this, DisplayContactActivity.class);
            i.putExtra(COL_ID, mCurrentId);
            i.putExtra(URI_KEY, getUri().toString());
            Log.d(TAG, this.getClass().getSimpleName()
                    + ".displayMessage: position=" + mCurrentPosition
                    + " currentId=" + mCurrentId);
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
        setListAdapter(mListAdapter);
    }

    /**
     * @return The content provider URI used.
     */
    private Uri getUri() {
        return URI;
    }

    /**
     * Class to manage the data needed for an item in the ListView.
     */
    private static class Data {
        private final long id;
        private String name;
        private boolean invalid = true;

        private Data(long id) {
            this.id = id;
        }

        private void setValues(String name) {
            this.name = name;
            invalid = false;
        }

        private long getId() {
            return id;
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
                String[] desiredColumns = {COL_ID, ContactsContract.Contacts
                        .DISPLAY_NAME};
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
                cursor = getContentResolver().query(getUri(), mDesiredColumns,
                        null, null, mSortOrder.sqlCommand);
                if (cursor == null) {
                    Utils.errMsg(ContactsActivity.this,
                            "ListAdapter: Error getting data: No items in " +
                                    "database");
                    return;
                }

                mIndexId = cursor.getColumnIndex(COL_ID);
                mIndexName = cursor.getColumnIndex(ContactsContract.Contacts
                        .DISPLAY_NAME);

                int count = cursor.getCount();
                mDataArray = new Data[count];

                if (count <= 0) {
                    Utils.infoMsg(ContactsActivity.this, "No items in " +
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
                Utils.excMsg(ContactsActivity.this,
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
                view = mInflator.inflate(R.layout.list_row_image, viewGroup,
                        false);
                viewHolder = new ViewHolder();
                viewHolder.title = (TextView) view.findViewById(R.id.title);
                viewHolder.imageView = (ImageView) view.findViewById(R.id
                        .imageview);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            String titleText;

            // Check if index is OK.
            if (i < 0 || i >= mDataArray.length) {
                titleText = "Error";
                viewHolder.title.setText(titleText);
                return view;
            }
            Data data = mDataArray[i];
            if (data == null) {
                titleText = "Error";
                viewHolder.title.setText(titleText);
                return view;
            }

            if (data.isInvalid()) {
                // Get the values for this item
                Cursor cursor = getContentResolver().query(getUri(),
                        mDesiredColumns,
                        COL_ID + "=" + mDataArray[i].getId(), null, mSortOrder
                                .sqlCommand);
                if (cursor != null && cursor.moveToFirst()) {
                    String displayName = "Unknown";
                    if (mIndexName > -1) {
                        displayName = cursor.getString(mIndexName);
                    }
                    data.setValues(displayName);
                }
                if (cursor != null) cursor.close();
            }
            mDataArray[i] = data;

            titleText = String.format(Locale.US, "%d", data.getId()) +
                    ": " + data.getName();
            viewHolder.title.setText(titleText);

            if (viewHolder.imageView != null) {
                long contactId = MessageUtils.getContactIdFromName(
                        ContactsActivity.this, data.getName());
                Bitmap bitmap = MessageUtils.loadContactPhoto(
                        getContentResolver(), contactId);
                if (bitmap == null) {
                    // DEBUG
                    // bitmap =
                    // BitmapFactory.decodeFile("/sdcard/Pictures/Art/Wildcat
                    // .jpg");
                    bitmap = BitmapFactory.decodeResource(getResources(),
                            R.drawable.android_icon);
                }
                if (bitmap != null) {
                    viewHolder.imageView.setImageBitmap(bitmap);
                } else {
                    viewHolder.imageView.setImageDrawable(null);
                }
            }

            return view;
        }
    }

    /**
     * Convenience class for managing views for a ListView row.
     */
    private static class ViewHolder {
        TextView title;
        ImageView imageView;
    }


}
