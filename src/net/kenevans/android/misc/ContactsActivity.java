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
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Manages a ListView of all the contacts in the database specified by the URI field.
 */
/**
 * @author evans
 * 
 */
public class ContactsActivity extends ListActivity implements IConstants {
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
	public static final Uri uri = ContactsContract.Contacts.CONTENT_URI;

	/** Enum to specify the sort order. */
	enum Order {
		NAME(ContactsContract.Contacts.DISPLAY_NAME + " ASC"), ID(COL_ID
				+ " ASC");
		public String sqlCommand;

		Order(String sqlCommand) {
			this.sqlCommand = sqlCommand;
		}
	}

	/** The sort order to use. */
	private Order sortOrder = Order.NAME;

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
		displayContact();
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
			displayContact();
		}
	}

	/**
	 * Bring up a dialog to change the sort order.
	 */
	private void setOrder() {
		final CharSequence[] items = { getText(R.string.sort_name),
				getText(R.string.sort_id) };
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getText(R.string.sort_title));
		builder.setSingleChoiceItems(items, sortOrder == Order.NAME ? 0 : 1,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						dialog.dismiss();
						sortOrder = item == 0 ? Order.NAME : Order.ID;
						refresh();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * Displays the message at the current position plus the current increment,
	 * adjusting for being within range. Resets the increment to 0 after.
	 */
	private void displayContact() {
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

			// Request the new message
			currentId = adapter.getItemId(currentPosition);
			Intent i = new Intent(this, DisplayContactActivity.class);
			i.putExtra(COL_ID, currentId);
			i.putExtra(URI_KEY, getUri().toString());
			Log.d(TAG, this.getClass().getSimpleName()
					+ ".displayMessage: position=" + currentPosition
					+ " currentId=" + currentId);
			startActivityForResult(i, DISPLAY_MESSAGE);
		} catch (Exception ex) {
			Utils.excMsg(this, "Error displaying contact", ex);
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
			String[] desiredColumns = { COL_ID,
					ContactsContract.Contacts.DISPLAY_NAME };
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
			Utils.excMsg(this, "Error finding contacts", ex);
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
		private int indexName;
		private int indexId;

		public CustomCursorAdapter(Context context, Cursor cursor) {
			super(context, cursor);
			inflater = LayoutInflater.from(context);
			indexId = cursor.getColumnIndex(COL_ID);
			indexName = cursor
					.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			TextView title = (TextView) view.findViewById(R.id.title);
			ImageView imageView = (ImageView) view.findViewById(R.id.imageview);
			String id = cursor.getString(indexId);
			String displayName = "Unknown";
			if (indexName > -1) {
				displayName = cursor.getString(indexName);
			}
			title.setText(id + ": " + displayName);
			// Set the image
			if (imageView != null) {
				long contactId = MessageUtils.getContactIdFromName(
						ContactsActivity.this, displayName);
				Bitmap bitmap = MessageUtils.loadContactPhoto(
						getContentResolver(), contactId);
				if (bitmap == null) {
					// DEBUG
					// bitmap =
					// BitmapFactory.decodeFile("/sdcard/Pictures/Art/Wildcat.jpg");
					bitmap = BitmapFactory.decodeResource(getResources(),
							R.drawable.android_icon);
				}
				if (bitmap != null) {
					imageView.setImageBitmap(bitmap);
				}
			}
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return inflater.inflate(R.layout.list_row_image, null);
		}

	}

}
