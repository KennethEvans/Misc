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
 * Manages a ListView of all the messages in the database specified by the URI field.
 */
/**
 * @author evans
 * 
 */
public class MessageActivity extends ListActivity implements IConstants {
	/**
	 * The current position when ACTIVITY_DISPLAY_MESSAGE is requested. Used
	 * with the resultCodes RESULT_PREV and RESULT_NEXT when they are returned.
	 */
	private int currentPosition;

	/**
	 * The current id when ACTIVITY_DISPLAY_MESSAGE is requested. Used with the
	 * resultCodes RESULT_PREV and RESULT_NEXT when they are returned.
	 */
	private long currentId;

	/** The increment for displaying the next message. */
	private long increment = 0;

	/** The Uri to use. */
	public static final Uri uri = MMS_SMS_CONVERSATIONS_URI;

	/** Enum to specify the sort order. */
	enum Order {
		TIME(VAR_NORMALIZED_DATE + " DESC"), ID(COL_ID + " DESC");
		public String sqlCommand;

		Order(String sqlCommand) {
			this.sqlCommand = sqlCommand;
		}
	}

	/** The sort order to use. */
	private Order sortOrder = Order.TIME;

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
		currentId = id;
		increment = 0;
		displayMessage();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		// DEBUG
		Log.d(TAG, this.getClass().getSimpleName()
				+ ".onActivityResult: requestCode=" + requestCode
				+ " resultCode=" + resultCode + " currentPosition="
				+ currentPosition);
		if (requestCode == DISPLAY_MESSAGE) {
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
				+ ".onPause: currentPosition=" + currentPosition);
		Log.d(TAG, this.getClass().getSimpleName() + ".onPause: currentId="
				+ currentId);
		super.onPause();
	}

	@Override
	protected void onResume() {
		Log.d(TAG, this.getClass().getSimpleName()
				+ ".onResume: currentPosition=" + currentPosition
				+ " currentId=" + currentId + " increment=" + increment);
		super.onResume();
		// If increment is set display a new message
		if (increment != 0) {
			displayMessage();
		}
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
	 * Returns whether the given id corresponds to an MMS message.
	 * 
	 * @param id
	 * @return
	 */
	public boolean isMMS(long id) {
		String[] values = MessageUtils.getStringValues(this, id, uri,
				new String[] { COL_CT_T });
		return values != null
				&& "application/vnd.wap.multipart.related".equals(values[0]);
	}

	/**
	 * Displays the message at the current position plus the current increment,
	 * adjusting for being within range. Resets the increment to 0 after.
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
			if (currentPosition > count - 1 || currentPosition < 0) {
				changed = true;
			} else {
				id = adapter.getItemId(currentPosition);
				if (id != currentId) {
					changed = true;
				}
			}
			// Determine the new currentPosition
			Log.d(TAG, this.getClass().getSimpleName()
					+ ".displayMessage: position=" + currentPosition + " id="
					+ id + " changed=" + changed);
			if (changed) {
				for (int i = 0; i < count; i++) {
					id = adapter.getItemId(i);
					if (id == currentId) {
						currentPosition = i;
						break;
					}
				}
			}
			// currentPosition may still be invalid, check it is in range
			if (currentPosition < 0) {
				currentPosition = 0;
			} else if (currentPosition > count - 1) {
				currentPosition = count - 1;
			}

			// Display messages if a requested increment is not possible
			if (increment > 0) {
				currentPosition += increment;
				if (currentPosition > count - 1) {
					Toast.makeText(getApplicationContext(),
							"At the last item in the list", Toast.LENGTH_LONG)
							.show();
					currentPosition = count - 1;
				}
			} else if (increment < 0) {
				currentPosition += increment;
				if (currentPosition < 0) {
					Toast.makeText(getApplicationContext(),
							"At the first item in the list", Toast.LENGTH_LONG)
							.show();
					currentPosition = 0;
				}
			}

			// Request the new message from the appropriate activity
			currentId = adapter.getItemId(currentPosition);
			Intent i = null;
			if (isMMS(currentId)) {
				i = new Intent(this, DisplayMMSActivity.class);
				i.putExtra(COL_ID, currentId);
				i.putExtra(URI_KEY, MMS_URI.toString());
				i.putExtra(DATE_MULTIPLIER_KEY, MMS_DATE_MULTIPLIER);
				Log.d(TAG, this.getClass().getSimpleName()
						+ ".displayMessage: MMS");
			} else {
				i = new Intent(this, DisplaySMSActivity.class);
				i.putExtra(COL_ID, currentId);
				i.putExtra(URI_KEY, SMS_URI.toString());
				i.putExtra(DATE_MULTIPLIER_KEY, SMS_DATE_MULTIPLIER);
				Log.d(TAG, this.getClass().getSimpleName()
						+ ".displayMessage: SMS");
			}
			Log.d(TAG, this.getClass().getSimpleName()
					+ ".displayMessage: position=" + currentPosition
					+ " currentId=" + currentId);
			startActivityForResult(i, DISPLAY_MESSAGE);
		} catch (Exception ex) {
			Utils.excMsg(this, "Error displaying message", ex);
		} finally {
			// Reset increment
			increment = 0;
		}
	}

	/**
	 * Gets a new cursor and starts managing it.
	 */
	private void refresh() {
		try {
			// First get the names of all the columns in the database
			Cursor cursor = getContentResolver().query(getUri(), null, null,
					null, null);
			String[] avaliableColumns = cursor.getColumnNames();
			cursor.close();

			// Make an array of the desired ones that are available
			String[] desiredColumns = { COL_ID, COL_ADDRESS, COL_DATE,
					COL_BODY, COL_TYPE, COL_CT_T };
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
			String selection = null;
			cursor = getContentResolver().query(getUri(), columns, selection,
					null, sortOrder.sqlCommand);
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
	 * @return The content provider URI used.
	 */
	public Uri getUri() {
		return uri;
	}

	private class CustomCursorAdapter extends CursorAdapter {
		private LayoutInflater inflater;
		private int indexDate;
		private int indexAddress;
		private int indexId;
		private int indexType;
		private int indexCtt;

		public CustomCursorAdapter(Context context, Cursor cursor) {
			super(context, cursor);
			inflater = LayoutInflater.from(context);
			indexId = cursor.getColumnIndex(COL_ID);
			indexDate = cursor.getColumnIndex(COL_DATE);
			indexAddress = cursor.getColumnIndex(COL_ADDRESS);
			indexType = cursor.getColumnIndex(COL_TYPE);
			indexCtt = cursor.getColumnIndex(COL_CT_T);
			Log.d(TAG, this.getClass().getSimpleName() + " uri=" + getUri()
					+ " indexCtt=" + indexCtt);
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
				dateNum = cursor.getLong(indexDate);
			}
			int type = -1;
			if (indexType > -1) {
				type = cursor.getInt(indexType);
			}
			String ctt = "ct_t NA";
			String messageType = "";
			Long multiplier = SMS_DATE_MULTIPLIER;
			if (indexCtt > -1) {
				ctt = cursor.getString(indexCtt);
				// Doing it this way handles null ctt
				if ("application/vnd.wap.multipart.related".equals(ctt)) {
					messageType = "MMS ";
					multiplier = MMS_DATE_MULTIPLIER;
					// We need to get the address from another provider
					String text = MessageUtils.getMMSAddress(
							MessageActivity.this, id);
					if (text != null) {
						address = text;
					}
				} else {
					messageType = "SMS ";
					if (indexAddress > -1) {
						address = cursor.getString(indexAddress);
					}
				}
			}
			title.setText(id + ": " + messageType
					+ MessageUtils.formatSmsType(type)
					+ MessageUtils.formatAddress(address));
			subtitle.setText(MessageUtils.formatDate(dateNum * multiplier));
			// Log.d(TAG, getClass().getSimpleName() + ".bindView" + " id=" + id
			// + " address=" + address + " dateNum=" + dateNum
			// + " dateMultiplier=" + getDateMultiplier());
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return inflater.inflate(R.layout.list_row, null);
		}

	}

}
