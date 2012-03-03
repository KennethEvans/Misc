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
 * Manages a ListView of all the messages in the database specified by the URI field.
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

	/** The Uri to use. */
	public static final Uri uri = SMS_URI;

	/**
	 * The date multiplier to use to get ms. MMS message timestamps are in sec
	 * not ms.
	 */
	public static final Long dateMultiplier = 1L;

	/** Enum to specify the sort order. */
	enum Order {
		TIME(COL_DATE + " DESC"), ID(COL_ID + " DESC");
		public String sqlCommand;

		Order(String sqlCommand) {
			this.sqlCommand = sqlCommand;
		}
	}

	/** The sort order to use. */
	private Order sortOrder = Order.TIME;

	/** The static format string to use for formatting dates. */
	public static final String format = "MMM dd, yyyy HH:mm:ss Z";
	public static final SimpleDateFormat formatter = new SimpleDateFormat(
			format);

	private CustomCursorAdapter adapter;

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
	protected void onListItemClick(ListView lv, View view, int position, long id) {
		super.onListItemClick(lv, view, position, id);
		// Save the position when starting the activity
		currentPosition = position;
		Intent i = new Intent(this, DisplayMessageActivity.class);
		i.putExtra(COL_ID, id);
		i.putExtra(URI_KEY, getUri().toString());
		i.putExtra(DATE_MULTIPLIER_KEY, getDateMultiplier());
		// DEBUG
		Log.d(TAG, this.getClass().getSimpleName() + ".onListItemClick: "
				+ " position=" + position + " id=" + id + " uri="
				+ getUri().toString() + " dateMultiplier="
				+ getDateMultiplier());
		startActivityForResult(i, DISPLAY_MESSAGE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		// DEBUG
		Log.d(TAG, "onActivityResult: requestCode=" + requestCode
				+ " resultCode=" + resultCode + " currentPosition="
				+ currentPosition);
		if (requestCode == DISPLAY_MESSAGE) {
			// There will be no items until the list is refreshed
			refresh();
			ListAdapter adapter = getListAdapter();
			if (adapter == null) {
				return;
			}
			try {
				int count = adapter.getCount();
				Log.d(TAG, "onActivityResult: count=" + count);
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
				i.putExtra(COL_ID, id);
				i.putExtra(URI_KEY, getUri().toString());
				i.putExtra(DATE_MULTIPLIER_KEY, getDateMultiplier());
				Log.d(TAG, "onActivityResult: position=" + currentPosition
						+ " id=" + id);
				startActivityForResult(i, DISPLAY_MESSAGE);
			} catch (Exception ex) {
				Utils.excMsg(this, "Error displaying new message", ex);
			}
		}
	}

	@Override
	protected void onPause() {
		Log.d(TAG, this.getClass().getSimpleName()
				+ ".onPause: currentPosition=" + currentPosition);
		super.onPause();
	}

	@Override
	protected void onResume() {
		Log.d(TAG, this.getClass().getSimpleName()
				+ ".onResume(1): currentPosition=" + currentPosition);
		super.onResume();
	}

	/**
	 * Bring up a dialog to change the sort order.
	 */
	private void setOrder() {
		final CharSequence[] items = { getText(R.string.sort_time),
				getText(R.string.sort_id) };
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getText(R.string.sort_title));
		builder.setSingleChoiceItems(items, sortOrder == Order.TIME ? 0 : 1,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						dialog.dismiss();
						sortOrder = item == 0 ? Order.TIME : Order.ID;
						refresh();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * Format the date using the static format.
	 * 
	 * @param dateNum
	 * @return
	 * @see #format
	 */
	public static String formatDate(Long dateNum) {
		return formatDate(SMSActivity.formatter, dateNum);
	}

	/**
	 * Format the date using the given format.
	 * 
	 * @param formatter
	 * @param dateNum
	 * @return
	 * @see #format
	 */
	public static String formatDate(SimpleDateFormat formatter, Long dateNum) {
		// Consider using Date.toString() as it might be more locale
		// independent.
		if (dateNum == null) {
			return "<Unknown>";
		}
		if (dateNum == -1) {
			// Means the column was not found in the database
			return "<Date NA>";
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
		// TODO does calling this more than onece cause memory leaks or extra
		// work?
		// TODO Probably causes extra work but does insure a refresh.
		try {
			// First get the names of all the columns in the database
			Cursor cursor = getContentResolver().query(getUri(), null, null,
					null, null);
			String[] avaliableColumns = cursor.getColumnNames();
			cursor.close();

			// Make an array of the desired ones that are available
			String[] desiredColumns = { COL_ID, COL_ADDRESS, COL_DATE, COL_BODY };
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
					null, sortOrder.sqlCommand);
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
			Utils.excMsg(this, "Error finding messages", ex);
		}
	}

	/**
	 * Method used to test what is happening with a database.
	 * 
	 * @param testNum
	 *            Prefix to log message.
	 * @param cls
	 *            The calling class (will be part of the log message).
	 * @param context
	 *            The calling context. Used to get the content resolver if the
	 *            input cursor is null.
	 * @param cursor
	 *            The calling cursor or null to use a cursor with all columns.
	 * @param id
	 *            The _id.
	 * @param uri
	 *            The URI of the content database (will be part of the log
	 *            message).
	 */
	public static void test(int testNum, Class<?> cls, Context context,
			Cursor cursor, String id, Uri uri) {
		Cursor cursor1;
		if (cursor == null) {
			String selection = COL_ID + "=" + id;
			// String[] projection = { "*" };
			String[] projection = null;
			cursor1 = context.getContentResolver().query(uri, projection,
					selection, null, null);
			cursor1.moveToFirst();
		} else {
			cursor1 = cursor;
		}

		int indexId = cursor1.getColumnIndex(COL_ID);
		int indexDate = cursor1.getColumnIndex(COL_DATE);
		int indexAddress = cursor1.getColumnIndex(COL_ADDRESS);
		int indexThreadId = cursor1.getColumnIndex(COL_THREAD_ID);

		do {
			String id1 = cursor1.getString(indexId);
			String address = "<Address NA>";
			if (indexAddress > -1) {
				address = cursor1.getString(indexAddress);
			}
			Long dateNum = -1L;
			if (indexDate > -1) {
				dateNum = cursor1.getLong(indexDate);
			}
			String threadId = "<ThreadID NA>";
			if (indexThreadId > -1) {
				threadId = cursor1.getString(indexThreadId);
			}
			Log.d(TAG,
					testNum + " " + cls.getSimpleName() + ".test" + "id=(" + id
							+ "," + id1 + ") address=" + address + " dateNum="
							+ dateNum + " threadId=" + threadId + " uri=" + uri
							+ " cursor=(" + cursor1.getColumnCount() + ","
							+ cursor1.getCount() + "," + cursor1.getPosition()
							+ ")");
		} while (cursor == null && cursor1.moveToNext());

		if (cursor == null) {
			// Close the cursor if we created it here
			cursor1.close();
		}
	}

	/**
	 * @return The content provider URI used.
	 */
	public Uri getUri() {
		return uri;
	}

	/**
	 * @return The date multiplier to use.
	 */
	public Long getDateMultiplier() {
		return dateMultiplier;
	}

	private class CustomCursorAdapter extends CursorAdapter {
		private LayoutInflater inflater;
		private int indexDate;
		private int indexAddress;
		private int indexId;

		public CustomCursorAdapter(Context context, Cursor cursor) {
			super(context, cursor);
			inflater = LayoutInflater.from(context);
			indexId = cursor.getColumnIndex(COL_ID);
			indexDate = cursor.getColumnIndex(COL_DATE);
			indexAddress = cursor.getColumnIndex(COL_ADDRESS);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			TextView title = (TextView) view.findViewById(R.id.title);
			TextView subtitle = (TextView) view.findViewById(R.id.subtitle);
			String id = cursor.getString(indexId);
			String address = "<Address NA>";
			if (indexAddress > -1) {
				address = cursor.getString(indexAddress);
			}
			Long dateNum = -1L;
			if (indexDate > -1) {
				dateNum = cursor.getLong(indexDate) * getDateMultiplier();
			}
			title.setText(id + ": " + formatAddress(address));
			subtitle.setText(formatDate(dateNum));
			Log.d(TAG, getClass().getSimpleName() + ".bindView" + " id=" + id
					+ " address=" + address + " dateNum=" + dateNum
					+ " dateMultiplier=" + getDateMultiplier());
			// DEBUG
			if (id.equals(new Integer(76).toString())) {
				test(1, this.getClass(), SMSActivity.this, cursor, id, getUri());
				test(2, this.getClass(), SMSActivity.this, null, id, getUri());
			}
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return inflater.inflate(R.layout.list_row, null);
		}

	}

}
