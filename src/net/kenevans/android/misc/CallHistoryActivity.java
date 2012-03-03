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

	/** Select short, unknown, non-outgoing calls. */
	private static String selectShortUnknownNotOutgoing = COL_DURATION + "<=20"
			+ " AND " + COL_NAME + " IS NULL" + " AND " + COL_TYPE + "<>"
			+ CallLog.Calls.OUTGOING_TYPE;
	/** Array of selection types. */
	private static String[] selectionTypes = { null,
			selectShortUnknownNotOutgoing };
	/** The current selection type. */
	private int selectionType = 0;
	/** The current selection. */
	private String selection = selectionTypes[selectionType];

	/** The static format string to use for formatting dates. */
	public static final String format = "MMM dd, yyyy HH:mm:ss";
	public static final SimpleDateFormat formatter = new SimpleDateFormat(
			format);

	/** Template for the name of the file written to the root of the SD card */
	private static final String sdCardFileNameTemplate = "CallHistory.%s.csv";

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
		}
		return false;
	}

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
		SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
		editor.putInt("selectionType", selectionType);
		editor.commit();
	}

	@Override
	protected void onResume() {
		Log.d(TAG, this.getClass().getSimpleName()
				+ ".onResume(1): currentPosition=" + currentPosition);
		super.onResume();
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		selectionType = prefs.getInt("selectionType", 0);
		if (selectionType < 0 || selectionType >= selectionTypes.length) {
			selectionType = 0;
		}
		selection = selectionTypes[selectionType];
	}

	/**
	 * Bring up a dialog to change the sort order.
	 */
	private void setFilter() {
		final CharSequence[] items = { getText(R.string.select_none),
				getText(R.string.select_short_unknown_nonoutgoing) };
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getText(R.string.selectTitle));
		builder.setSingleChoiceItems(items, selectionType,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						dialog.dismiss();
						if (item < 0 || item >= selectionTypes.length) {
							Utils.errMsg(CallHistoryActivity.this,
									"Invalid filter");
							selectionType = 0;
						} else {
							selectionType = item;
						}
						selection = selectionTypes[selectionType];
						switch (item) {
						case 0:
							selection = null;
							break;
						case 1:
							selection = selectShortUnknownNotOutgoing;
							break;
						default:
							selection = null;
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
			intent.putExtra(INFO_URL,
					"file:///android_asset/callhistory.html");
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
				String fileName = String.format(sdCardFileNameTemplate,
						formatter.format(now), now.getTime());
				File file = new File(sdCardRoot, fileName);
				FileWriter writer = new FileWriter(file);
				out = new BufferedWriter(writer);
				out.write("id\t" + "date\t" + "number\t" + "type\t"
						+ "duration\t" + "name\n");

				// Get the database again to avoid traversing the ListView,
				// which only has visible items
				Cursor cursor = null;
				try {
					// Get a cursor
					String[] desiredColumns = { COL_ID, COL_NUMBER, COL_DATE,
							COL_DURATION, COL_TYPE, COL_NAME };
					cursor = getContentResolver().query(getUri(),
							desiredColumns, selection, null,
							sortOrder.sqlCommand);
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
								+ SMSActivity.formatDate(
										CallHistoryActivity.formatter, dateNum)
								+ "\t" + SMSActivity.formatAddress(number)
								+ "\t" + formatType(type) + "\t"
								+ formatDuration(duration) + "\t" + name + "\n");
						cursor.moveToNext();
					}
				} catch (Exception ex) {
					out.write("Error finding calls\n" + ex.getMessage());
				} finally {
					if (cursor != null) {
						cursor.close();
					}
				}
				Utils.infoMsg(this, "Wrote " + fileName);
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
					COL_DURATION, COL_TYPE, COL_NAME };
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
			title.setText(id + ": " + SMSActivity.formatAddress(number) + " ("
					+ formatType(type) + ") " + name);
			subtitle.setText(SMSActivity.formatDate(formatter, dateNum)
					+ " Duration: " + formatDuration(duration));
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
