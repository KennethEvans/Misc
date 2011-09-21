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

import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Class to display a single message.
 */
public class DisplayMessageActivity extends Activity {
	/** Set this to not make any changes to the database. */
	private boolean dryRun = true;
	/** The current default value for the user's offset. */
	private static int lastTimeOffset;
	private TextView mTitleTextView;
	private TextView mSubtitleTextView;
	private TextView mBodyTextView;
	private Long mRowId;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.displaymessage);

		// Use minus the users time offset as the initial suggested value
		Date now = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(now);
		lastTimeOffset = calendar.getTimeZone().getOffset(now.getTime());

		// Get the saved state for lastOffset, otherwise resets to the above.
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		lastTimeOffset = prefs.getInt("timeOffset", lastTimeOffset);

		mTitleTextView = (TextView) findViewById(R.id.titleview);
		mSubtitleTextView = (TextView) findViewById(R.id.subtitleview);
		mBodyTextView = (TextView) findViewById(R.id.bodyview);

		mRowId = (savedInstanceState == null) ? null
				: (Long) savedInstanceState.getSerializable(SMSActivity.COL_ID);
		if (mRowId == null) {
			Bundle extras = getIntent().getExtras();
			mRowId = extras != null ? extras.getLong(SMSActivity.COL_ID) : null;
		}

		// Call refresh to set the contents
		refresh();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.displaymessagemenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
		case R.id.prev:
			navigate(SMSActivity.RESULT_PREV);
			return true;
		case R.id.next:
			navigate(SMSActivity.RESULT_NEXT);
			return true;
		case R.id.fixtime:
			fixTime();
			return true;
		case R.id.dryrun:
			toggleDryRun();
			return true;
		case R.id.delete:
			deleteMessage();
			return true;
		}
		return false;
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Retain the offset so the user can use it again
		SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
		editor.putInt("timeOffset", lastTimeOffset);
		editor.putBoolean("dryrun", dryRun);
		editor.commit();
	}

	@Override
	protected void onResume() {
		// Restore the offset so the user can use it again
		super.onResume();
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		lastTimeOffset = prefs.getInt("timeOffset", lastTimeOffset);
		dryRun = prefs.getBoolean("dryrun", dryRun);
	}

	/**
	 * Gets the current time from the database, prompts the user for a time
	 * offset, and changes the time in the data base unless the user cancels.
	 */
	private void fixTime() {
		try {
			String[] columns = { SMSActivity.COL_DATE };
			// Only get the row with mRowId
			String selection = SMSActivity.COL_ID + "=" + mRowId.longValue();
			Cursor cursor = getContentResolver().query(SMSActivity.SMS_URI,
					columns, selection, null, null);

			int indexDate = cursor.getColumnIndex(SMSActivity.COL_DATE);
			// There should only be one row returned
			boolean found = cursor.moveToFirst();
			if (!found) {
				Utils.errMsg(this, "Did not find message");
				return;
			}
			final Long curDate = cursor.getLong(indexDate);
			// We are through with the cursor
			cursor.close();

			// Make a TimeOffsetDialog to get the users value
			final TimeOffsetDialog dialog = new TimeOffsetDialog(this,
					lastTimeOffset);
			// The title needs to be this long to keep the width reasonable
			// Wasn't able to fix this with resources
			dialog.setTitle(R.string.timeoffset_dialog_title);
			final Button okButton = dialog.getOkButton();
			okButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (v == okButton) {
						Integer offset = dialog.getTimeOffset();
						if (offset == null) {
							Utils.errMsg(DisplayMessageActivity.this,
									"Got invalid value for the time offset");
						} else {
							// Save this value as the default
							lastTimeOffset = offset;
							long newDate = curDate + offset;
							if (dryRun) {
								Toast.makeText(
										getApplicationContext(),
										"Dry run:\n"
												+ "Old Time="
												+ SMSActivity
														.formatDate(curDate)
												+ "\nNew Time="
												+ SMSActivity
														.formatDate(newDate),
										Toast.LENGTH_LONG).show();
							} else {
								// The following change the database
								ContentValues values = new ContentValues();
								values.put(SMSActivity.COL_DATE, newDate);
								getContentResolver().update(
										SMSActivity.SMS_URI, values,
										"_id = " + mRowId, null);
							}

							refresh();
						}
						dialog.dismiss();
					}
				}
			});
			final Button cancelButton = dialog.getCancelButton();
			cancelButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (v == cancelButton) {
						dialog.dismiss();
						// Save the current offset even if the user cancelled
						// This allows the user to define a new default offset
						Integer offset = dialog.getTimeOffset();
						if (offset != null) {
							lastTimeOffset = offset;
						}
					}
				}
			});
			dialog.show();
		} catch (Exception ex) {
			Utils.excMsg(this, "Error finding message", ex);
		}
	}

	/**
	 * Sets the result code to send back to the SMSACtivity. One of:
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
	 * Deletes the message and navigates to the next message.
	 */
	private void deleteMessage() {
		if (dryRun) {
			Toast.makeText(getApplicationContext(),
					"Dry run:\n" + "Message deleted", Toast.LENGTH_LONG).show();
		} else {
			try {
				// The following change the database
				getContentResolver().delete(SMSActivity.SMS_URI,
						"_id = " + mRowId, null);
				navigate(SMSActivity.RESULT_NEXT);
			} catch (Exception ex) {
				Utils.excMsg(this, "Problem deleting message", ex);
			}
		}
	}

	/**
	 * Gets a new cursor and redraws the view. Closes the cursor after it is
	 * done with it.
	 */
	private void refresh() {
		try {
			String[] columns = { SMSActivity.COL_ID, SMSActivity.COL_ADDRESS,
					SMSActivity.COL_DATE, SMSActivity.COL_BODY };
			// Only get the row with mRowId
			String selection = SMSActivity.COL_ID + "=" + mRowId.longValue();

			// // DEBUG Get all the column names
			// Cursor cursor1 = getContentResolver().query(SMSActivity.SMS_URI,
			// null, selection, null, null);
			// String[] names = cursor1.getColumnNames();
			// String info = "";
			// for(String name : names) {
			// info += name + "\n";
			// }
			// Log.d(Utils.getTAG(), "SMS Column Names\n" + info);
			// cursor1.close();

			Cursor cursor = getContentResolver().query(SMSActivity.SMS_URI,
					columns, selection, null, null);
			int indexId = cursor.getColumnIndex(SMSActivity.COL_ID);
			int indexDate = cursor.getColumnIndex(SMSActivity.COL_DATE);
			int indexAddress = cursor.getColumnIndex(SMSActivity.COL_ADDRESS);
			int indexBody = cursor.getColumnIndex(SMSActivity.COL_BODY);
			// There should only be one row returned
			boolean found = cursor.moveToFirst();
			if (!found) {
				mTitleTextView.setText("<Error>");
				mSubtitleTextView.setText("");
				mBodyTextView.setText("Failed to find message " + mRowId);
			} else {
				String id = cursor.getString(indexId);
				String address = cursor.getString(indexAddress);
				Long dateNum = cursor.getLong(indexDate);
				String body = cursor.getString(indexBody);
				mTitleTextView.setText(id + ": "
						+ SMSActivity.formatAddress(address));
				mSubtitleTextView.setText(SMSActivity.formatDate(dateNum));
				mBodyTextView.setText(body);
			}
			// We are through with the cursor
			cursor.close();
		} catch (Exception ex) {
			String msg = "Error finding message:\n" + ex.getMessage();
			Utils.excMsg(this, "Error finding message", ex);
			if (mBodyTextView != null) {
				mBodyTextView.setTextColor(0xffff0000);
				mBodyTextView.setText(msg);
			}
			if (mTitleTextView != null) {
				mTitleTextView.setText("<Error>");
			}
			if (mSubtitleTextView != null) {
				mSubtitleTextView.setText("");
			}
		}
	}

	/**
	 * Toggles whether database changes are real or simulated.
	 */
	private void toggleDryRun() {
		final CharSequence[] items = { getText(R.string.on),
				getText(R.string.off) };
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getText(R.string.dryRunTitle));
		builder.setSingleChoiceItems(items, dryRun ? 0 : 1,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						dialog.dismiss();
						dryRun = item == 0 ? true : false;
						String msg;
						if (dryRun) {
							msg = "Time changes are simulated.\nDatabase will not be changed.";
						} else {
							msg = "Time changes are real.\nDatabase will be changed.";
						}
						Utils.infoMsg(DisplayMessageActivity.this, msg);
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

}
