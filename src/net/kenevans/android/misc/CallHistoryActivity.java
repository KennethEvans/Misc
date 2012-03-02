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

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.util.Log;
import android.view.LayoutInflater;
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
public class CallHistoryActivity extends ListActivity implements IConstants {
	/**
	 * The current position when ACTIVITY_DISPLAY_MESSAGE is requested. Used
	 * with the resultCodes RESULT_PREV and RESULT_NEXT when they are returned.
	 */
	private int currentPosition;

	/** The Uri to use. */
	public static final Uri uri = CALLLOG_CALLS_URI;

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

	// @Override
	// public boolean onCreateOptionsMenu(Menu menu) {
	// MenuInflater inflater = getMenuInflater();
	// inflater.inflate(R.menu.smsmenu, menu);
	// return true;
	// }
	//
	// @Override
	// public boolean onOptionsItemSelected(MenuItem item) {
	// int id = item.getItemId();
	// switch (id) {
	// case R.id.refresh:
	// refresh();
	// return true;
	// case R.id.order:
	// setOrder();
	// return true;
	// }
	// return false;
	// }

	@Override
	protected void onListItemClick(ListView lv, View view, int position, long id) {
		super.onListItemClick(lv, view, position, id);
		// Save the position when starting the activity
		currentPosition = position;
		Intent i = new Intent(this, DisplayCallActivity.class);
		i.putExtra(COL_ID, id);
		i.putExtra(URI_KEY, getUri().toString());
		// DEBUG
		Log.d(TAG, this.getClass().getSimpleName() + ".onListItemClick: "
				+ " position=" + position + " id=" + id + " uri="
				+ getUri().toString());
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
				Intent i = new Intent(this, DisplayCallActivity.class);
				i.putExtra(COL_ID, id);
				i.putExtra(URI_KEY, getUri().toString());
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

	// /**
	// * Bring up a dialog to change the sort order.
	// */
	// private void setOrder() {
	// final CharSequence[] items = { getText(R.string.orderByTime),
	// getText(R.string.orderById) };
	// AlertDialog.Builder builder = new AlertDialog.Builder(this);
	// builder.setTitle(getText(R.string.orderTitle));
	// builder.setSingleChoiceItems(items, sortOrder == Order.TIME ? 0 : 1,
	// new DialogInterface.OnClickListener() {
	// public void onClick(DialogInterface dialog, int item) {
	// dialog.dismiss();
	// sortOrder = item == 0 ? Order.TIME : Order.ID;
	// refresh();
	// }
	// });
	// AlertDialog alert = builder.create();
	// alert.show();
	// }

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
			String[] desiredColumns = { COL_ID, COL_NUMBER, COL_DATE,
					COL_DURATION, COL_TYPE };
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
			Utils.excMsg(this, "Error finding calls", ex);
		}
	}

	/**
	 * @return The content provider URI used.
	 */
	public Uri getUri() {
		return uri;
	}

	private class CustomCursorAdapter extends CursorAdapter {
		private LayoutInflater inflater;
		private int indexDate;
		private int indexNumber;
		private int indexId;
		private int indexDuration;
		private int indexType;

		public CustomCursorAdapter(Context context, Cursor cursor) {
			super(context, cursor);
			inflater = LayoutInflater.from(context);
			indexId = cursor.getColumnIndex(COL_ID);
			indexDate = cursor.getColumnIndex(COL_DATE);
			indexNumber = cursor.getColumnIndex(COL_NUMBER);
			indexDuration = cursor.getColumnIndex(COL_DURATION);
			indexType = cursor.getColumnIndex(COL_TYPE);
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
			title.setText(id + ": " + SMSActivity.formatAddress(number) + " ("
					+ formatType(type) + ") Duration: "
					+ formatDuration(duration));
			subtitle.setText(SMSActivity.formatDate(dateNum));
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
			return inflater.inflate(R.layout.list_row, null);
		}

	}

}
