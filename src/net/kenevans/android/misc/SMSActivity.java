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

import java.text.SimpleDateFormat;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
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
 * Manages a ListView of all the messages in the SMS database.
 */
/**
 * @author evans
 * 
 */
public class SMSActivity extends ListActivity implements IConstants {
	/**
	 * The current position when ACTIVITY_DISPLAY_MESSAGE is requested. Used
	 * with the resultCodes RESULT_PREV and RESULT_NEXT when they are returned.
	 */
	private int currentPosition;

	/** The static format string to use for formatting dates. */
	public static final String format = "MMM dd, yyyy HH:mm:ss Z";
	public static final SimpleDateFormat formatter = new SimpleDateFormat(
			format);

	private Cursor cursor;
	private CustomCursorAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set the current position to 0
		// currentPosition = 0;

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
		}
		return false;
	}

	@Override
	protected void onListItemClick(ListView lv, View view, int position, long id) {
		super.onListItemClick(lv, view, position, id);
		// Save the position when starting the activity
		currentPosition = position;
		Intent i = new Intent(this, DisplayMessageActivity.class);
		i.putExtra(SMSActivity.ROW_ID, id);
		// DEBUG
		Log.i(TAG, "onListItemClick: position=" + position + " id=" + id);
		startActivityForResult(i, ACTIVITY_DISPLAY_MESSAGE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		// DEBUG
		Log.i(TAG, "onActivityResult: requestCode=" + requestCode
				+ " resultCode=" + resultCode + " currentPosition="
				+ currentPosition);
		if (requestCode == ACTIVITY_DISPLAY_MESSAGE) {
			// There will be no items until the list is refreshed
			refresh();
			ListAdapter adapter = getListAdapter();
			if (adapter == null) {
				return;
			}
			try {
				int count = adapter.getCount();
				Log.i(TAG, "onActivityResult: count=" + count);
				if (count == 0) {
					Utils.infoMsg(this, "There are no items in the list");
					return;
				}
				// Note that earlier items are at higher positions in the list
				if (resultCode == RESULT_PREV) {
					if (currentPosition >= count - 1) {
						Toast.makeText(getApplicationContext(),
								"At the last item in the list",
								Toast.LENGTH_LONG).show();
						currentPosition = count - 1;
					} else {
						currentPosition++;
					}
				} else if (resultCode == RESULT_NEXT) {
					if (currentPosition <= 0) {
						Toast.makeText(getApplicationContext(),
								"At the first item in the list",
								Toast.LENGTH_LONG).show();

						currentPosition = 0;
					} else {
						currentPosition--;
					}
				} else {
					// Is something else like RESULT_CANCELLED
					return;
				}
				// Request the new message
				long id = adapter.getItemId(currentPosition);
				Intent i = new Intent(this, DisplayMessageActivity.class);
				i.putExtra(SMSActivity.ROW_ID, id);
				Log.i(TAG, "onActivityResult: position=" + currentPosition
						+ " id=" + id);
				startActivityForResult(i, ACTIVITY_DISPLAY_MESSAGE);

				// Retain the offset. Doesn't seem to get done if we don't do
				// this.
				SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE)
						.edit();
				editor.putInt("currentPosition", currentPosition);
				editor.commit();
			} catch (Exception ex) {
				Utils.excMsg(this, "Error displaying new message", ex);
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Retain the offset so the user can use it again
		SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
		editor.putInt("currentPosition", currentPosition);
		editor.commit();
	}

	@Override
	protected void onResume() {
		// Restore the offset so the user can use it again
		super.onResume();
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		currentPosition = prefs.getInt("currentPosition", currentPosition);
	}

	/**
	 * Format the date using the static format.
	 * 
	 * @param dateNum
	 * @return
	 * @see #format
	 */
	public static String formatDate(Long dateNum) {
		// Consider using Date.toString() as it might be more locale
		// independent.
		if (dateNum == null) {
			return "<Unknown>";
		}
		// Consider using Date.toString()
		// It might be more locale independent.
		// return new Date(dateNum).toString();

		// Include the dateNum
		// return dateNum + " " + formatter.format(dateNum);

		return formatter.format(dateNum);
	}

	/**
	 * Format the number returned for the address to make it more presentable.
	 * 
	 * @param address
	 * @return
	 */
	public static String formatAddress(String address) {
		String retVal = address;
		if (address == null || address.length() == 0) {
			return "<Unknown>";
		}
		// Check if it is all digits
		int len = address.length();
		boolean isNumeric = true;
		for (int i = 0; i < len; i++) {
			if (!Character.isDigit(address.charAt(i))) {
				isNumeric = false;
				break;
			}
		}
		if (!isNumeric) {
			return address;
		}
		// Is all digits
		if (len == 11) {
			retVal = address.substring(0, 1) + "-" + address.substring(1, 4)
					+ "-" + address.substring(4, 7) + "-"
					+ address.substring(7, 11);
		} else if (len == 10) {
			retVal = address.substring(0, 3) + "-" + address.substring(3, 6)
					+ "-" + address.substring(6, 10);
		} else if (len == 7) {
			retVal = address.substring(0, 3) + "-" + address.substring(3, 7);
		}
		return retVal;
	}

	/**
	 * Gets a new cursor and starts managing it.
	 */
	private void refresh() {
		// TODO is refresh necessary? See the Notepad example where it is used.
		// TODO does calling this more than one cause memory leaks or extra
		// work?
		// TODO Probably causes extra work but does insure a refresh.
		try {
			String[] columns = { ROW_ID, COL_ADDRESS, COL_DATE, COL_BODY };
			// Get all rows
			cursor = getContentResolver().query(SMS_URI, columns, null, null,
					COL_DATE + " DESC");
			// editingCursor = getContentResolver().query(editingURI, columns,
			// "type=?", new String[] { "1" }, "_id DESC");
			startManagingCursor(cursor);

			// Manage the adapter
			if (adapter == null) {
				// Set a custom cursor adapter
				adapter = new CustomCursorAdapter(getApplicationContext(),
						cursor);
				setListAdapter(adapter);
			} else {
				// This should close the current cursor and start using the new
				// one, hopefully avoiding memory leaks.
				adapter.changeCursor(cursor);
			}
		} catch (Exception ex) {
			Utils.excMsg(this, "Error finding message", ex);
		}
	}

	private class CustomCursorAdapter extends CursorAdapter {
		private LayoutInflater inflater;
		private int indexDate;
		private int indexAddress;
		private int indexId;

		public CustomCursorAdapter(Context context, Cursor cursor) {
			super(context, cursor);
			inflater = LayoutInflater.from(context);
			indexId = cursor.getColumnIndex(ROW_ID);
			indexDate = cursor.getColumnIndex(COL_DATE);
			indexAddress = cursor.getColumnIndex(COL_ADDRESS);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			TextView title = (TextView) view.findViewById(R.id.title);
			TextView subtitle = (TextView) view.findViewById(R.id.subtitle);
			String id = cursor.getString(indexId);
			String address = cursor.getString(indexAddress);
			Long dateNum = cursor.getLong(indexDate);
			title.setText(id + ": " + formatAddress(address));
			subtitle.setText(formatDate(dateNum));
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return inflater.inflate(R.layout.list_row, null);
		}

	}

}
