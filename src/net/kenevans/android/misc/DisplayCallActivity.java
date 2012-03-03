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

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

/**
 * Class to display a single message.
 */
public class DisplayCallActivity extends Activity implements IConstants {
	/** The Uri to use. */
	public Uri uri;

	private TextView mTitleTextView;
	private TextView mSubtitleTextView;
	private Long mRowId;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.displaycall);

		// Get the saved state
		// SharedPreferences prefs = getPreferences(MODE_PRIVATE);

		mTitleTextView = (TextView) findViewById(R.id.titleview);
		mSubtitleTextView = (TextView) findViewById(R.id.subtitleview);
		mSubtitleTextView.setMovementMethod(new ScrollingMovementMethod());

		mRowId = (savedInstanceState == null) ? null
				: (Long) savedInstanceState.getSerializable(COL_ID);
		Bundle extras = getIntent().getExtras();
		if (mRowId == null && extras != null) {
			mRowId = extras.getLong(COL_ID);
		}
		if (extras != null) {
			String uriPath = extras.getString(URI_KEY);
			if (uriPath != null) {
				uri = Uri.parse(uriPath);
			}
		}
		if (uri == null) {
			Utils.errMsg(this, "Null content provider database Uri");
			return;
		}
		mRowId = extras != null ? extras.getLong(COL_ID) : null;

		// Call refresh to set the contents
		refresh();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.displaycallsmenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
		case R.id.prev:
			navigate(RESULT_PREV);
			return true;
		case R.id.next:
			navigate(RESULT_NEXT);
			return true;
		}
		return false;
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Retain the offset so the user can use it again
		// SharedPreferences.Editor editor =
		// getPreferences(MODE_PRIVATE).edit();
		// editor.putInt("timeOffset", lastTimeOffset);
		// editor.putBoolean("dryrun", dryRun);
		// editor.commit();
	}

	@Override
	protected void onResume() {
		// Restore the offset so the user can use it again
		super.onResume();
		// SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		// lastTimeOffset = prefs.getInt("timeOffset", lastTimeOffset);
		// dryRun = prefs.getBoolean("dryrun", dryRun);
	}

	/**
	 * Sets the result code to send back to the calling Activity. One of:
	 * <ul>
	 * <li>RESULT_PREV
	 * <li>RESULT_NEXT
	 * </ul>
	 * 
	 * @param resultCode
	 *            The result code to send.
	 */
	private void navigate(int resultCode) {
		setResult(resultCode);
		finish();
	}

	/**
	 * Gets a new cursor and redraws the view. Closes the cursor after it is
	 * done with it.
	 */
	private void refresh() {
		try {
			// Only get the row with mRowId
			String selection = COL_ID + "=" + mRowId.longValue();

			// First get the names of all the columns in the database
			Cursor cursor = getContentResolver().query(uri, null, selection,
					null, null);
			String[] columns = cursor.getColumnNames();
			cursor.close();

			// Then get the columns for this row
			String sort = COL_DATE + " DESC";
			cursor = getContentResolver().query(uri, columns, selection, null,
					sort);
			int indexId = cursor.getColumnIndex(COL_ID);
			int indexDate = cursor.getColumnIndex(COL_DATE);
			int indexNumber = cursor.getColumnIndex(COL_NUMBER);
			int indexDuration = cursor.getColumnIndex(COL_DURATION);
			int indexType = cursor.getColumnIndex(COL_TYPE);
			int indexName = cursor.getColumnIndex(COL_NAME);
			Log.d(TAG, this.getClass().getSimpleName() + ".refresh: "
					+ " mRowId=" + mRowId + " uri=" + uri.toString());

			// There should only be one row returned, the last will be the most
			// recent if more are returned owing to the sort above
			boolean found = cursor.moveToFirst();
			if (!found) {
				mTitleTextView.setText("<Error>");
				mSubtitleTextView.setText("");
			} else {
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
				String title = id;
				// Indicate if more than one found
				if (cursor.getCount() > 1) {
					title += " [1/" + cursor.getCount() + "]";
				}
				title += ": "
						+ SMSActivity.formatAddress(number)
						+ " ("
						+ CallHistoryActivity.formatType(type)
						+ ") "
						+ name
						+ "\n"
						+ SMSActivity.formatDate(CallHistoryActivity.formatter,
								dateNum) + " Duration: "
						+ CallHistoryActivity.formatDuration(duration);
				String subTitle = "";
				Log.d(TAG, getClass().getSimpleName() + ".refresh" + " id="
						+ id + " address=" + number + " dateNum=" + dateNum);

				// Add all the fields in the database
				for (String colName : columns) {
					try {
						int index = cursor.getColumnIndex(colName);
						// Don't do a LF the first time
						if (subTitle.length() != 0) {
							subTitle += "\n";
						}
						// Don't print the body
						if (colName.equals("body")) {
							subTitle += colName + ": <"
									+ cursor.getString(index).length()
									+ " chars>";
						} else {
							subTitle += colName + ": "
									+ cursor.getString(index);
						}
					} catch (Exception ex) {
						// Shouldn't happen
						subTitle += colName + ": Not found";
					}
				}

				// Set the TextViews
				mTitleTextView.setText(title);
				mSubtitleTextView.setText(subTitle);

				// Debug
				// if (id.equals(new Integer(76).toString())) {
				// SMSActivity.test(3, this.getClass(), this, cursor, id, uri);
				// SMSActivity.test(4, this.getClass(), this, null, id, uri);
				// }
			}
			// We are through with the cursor
			cursor.close();
		} catch (Exception ex) {
			Utils.excMsg(this, "Error finding message", ex);
			if (mTitleTextView != null) {
				mTitleTextView.setText("<Error>");
			}
			if (mSubtitleTextView != null) {
				mSubtitleTextView.setText("");
			}
		}
	}

}
