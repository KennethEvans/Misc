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

import java.io.InputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Class to display a single message.
 */
public class DisplayCallActivity extends Activity implements IConstants {
	/** Set this to not make any changes to the database. */
	private boolean dryRun = false;

	/** The Uri to use. */
	public Uri uri;

	private TextView mTitleTextView;
	private TextView mSubtitleTextView;
	private TextView mContactTextView;
	private ImageView mImageView;
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
		mContactTextView = (TextView) findViewById(R.id.contactview);
		mImageView = (ImageView) findViewById(R.id.imageview);

		// mSubtitleTextView.setMovementMethod(new ScrollingMovementMethod());
		// mContactTextView.setMovementMethod(new ScrollingMovementMethod());

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
		case R.id.delete:
			deleteCall();
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
	 * Gets a bitmap of a contact photo
	 * 
	 * @param cr
	 * @param id
	 * @return
	 */
	public static Bitmap loadContactPhoto(ContentResolver cr, long id) {
		if (id < 0 || cr == null) {
			return null;
		}
		Uri uri = ContentUris.withAppendedId(
				ContactsContract.Contacts.CONTENT_URI, id);
		InputStream input = ContactsContract.Contacts
				.openContactPhotoInputStream(cr, uri);
		if (input == null) {
			return null;
		}
		return BitmapFactory.decodeStream(input);
	}

	/**
	 * Gets a String representation for the given Phone type.
	 * 
	 * @param type
	 *            One of the ContactsContract.CommonDataKinds.Phone.TYPE_xxx
	 *            types.
	 * @return
	 */
	public static String getPhoneType(int type) {
		String stringType = null;
		switch (type) {
		case ContactsContract.CommonDataKinds.Phone.TYPE_ASSISTANT:
			stringType = "Assistant";
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_CALLBACK:
			stringType = "Callback";
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_CAR:
			stringType = "Car";
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_COMPANY_MAIN:
			stringType = "Company Main";
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME:
			stringType = "Fax Home";
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK:
			stringType = "Fax Work";
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
			stringType = "Home";
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_ISDN:
			stringType = "ISDN";
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_MAIN:
			stringType = "Main";
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_MMS:
			stringType = "MMS";
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
			stringType = "Mobile";
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER:
			stringType = "Other";
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER_FAX:
			stringType = "Other Fax";
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_PAGER:
			stringType = "Pager";
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_RADIO:
			stringType = "Radio";
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_TELEX:
			stringType = "TELEX";
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_TTY_TDD:
			stringType = "TTY TDD";
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
			stringType = "Work";
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE:
			stringType = "Work Mobile";
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER:
			stringType = "Work Pager";
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM:
			stringType = "Custom";
			break;
		default:
			stringType = "Type " + type;
		}
		return stringType;
	}

	/**
	 * Gets a String representation for the Email given type.
	 * 
	 * @param type
	 *            One of the ContactsContract.CommonDataKinds.Email.TYPE_xxx
	 *            types.
	 * @return
	 */
	public static String getEmailType(int type) {
		String stringType = null;
		switch (type) {
		case ContactsContract.CommonDataKinds.Email.TYPE_HOME:
			stringType = "Home";
			break;
		case ContactsContract.CommonDataKinds.Email.TYPE_MOBILE:
			stringType = "Mobile";
			break;
		case ContactsContract.CommonDataKinds.Email.TYPE_WORK:
			stringType = "Work";
			break;
		case ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM:
			stringType = "Custom";
			break;
		case ContactsContract.CommonDataKinds.Email.TYPE_OTHER:
			stringType = "Other";
			break;
		default:
			stringType = "Type " + type + "";
		}
		return stringType;
	}

	/**
	 * Get contact information about the given name.
	 * 
	 * @param name
	 * @return The information or null on failure.
	 */
	private String getContactInfo(String name) {
		if (name == null || name.length() == 0) {
			return null;
		}
		String info = "Contact Information for " + name + "\n";
		String selection = ContactsContract.Contacts.DISPLAY_NAME + "=\""
				+ name + "\"";
		Cursor cursor = getContentResolver().query(
				ContactsContract.Contacts.CONTENT_URI, null, selection, null,
				null);
		if (cursor.getCount() == 0) {
			info += name + " not found\n";
			return info;
		}
		int indexId = cursor.getColumnIndex(COL_ID);
		int indexDisplayName = cursor
				.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
		int indexPhotoId = cursor
				.getColumnIndex(ContactsContract.Contacts.PHOTO_ID);
		// int indexTmesContacted = cursor
		// .getColumnIndex(ContactsContract.Contacts.TIMES_CONTACTED);
		// int indexLastTimeContacted = cursor
		// .getColumnIndex(ContactsContract.Contacts.LAST_TIME_CONTACTED);

		cursor.moveToNext();
		String id = cursor.getString(indexId);
		info += "_id: " + id + "\n";
		String photoId = "Not found";
		if (indexPhotoId > -1) {
			photoId = cursor.getString(indexPhotoId);
			if (photoId == null) {
				photoId = "Not found";
			}
		}
		info += ContactsContract.Contacts.PHOTO_ID + ": " + photoId + "\n";
		String displayName = "Not found";
		if (indexDisplayName > -1) {
			displayName = cursor.getString(indexDisplayName);
			if (displayName == null) {
				displayName = "Not found";
			}
		}
		// These are not kept track of on the EVO 3D
		// info += ContactsContract.Contacts.DISPLAY_NAME + ": " + displayName
		// + "\n";
		// String timesContacted = "Not found";
		// if (indexTmesContacted > -1) {
		// timesContacted = cursor.getString(indexTmesContacted);
		// if (timesContacted == null) {
		// timesContacted = "Not found";
		// }
		// }
		// info += ContactsContract.Contacts.TIMES_CONTACTED + ": "
		// + timesContacted + "\n";
		// String lastTimeContacted = "Not found";
		// if (indexTmesContacted > -1) {
		// lastTimeContacted = cursor.getString(indexTmesContacted);
		// if (lastTimeContacted == null) {
		// lastTimeContacted = "Not found";
		// }
		// }
		// info += ContactsContract.Contacts.LAST_TIME_CONTACTED + ": "
		// + lastTimeContacted + "\n";

		// Phones
		info += "Phones:\n";
		if (Integer.parseInt(cursor.getString(cursor
				.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
			Cursor pCursor = getContentResolver().query(
					ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
					ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
					new String[] { id }, null);
			while (pCursor.moveToNext()) {
				String phoneNumber = pCursor
						.getString(pCursor
								.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
				int phoneType = pCursor
						.getInt(pCursor
								.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
				info += "  " + getPhoneType(phoneType) + ": " + phoneNumber
						+ "\n";
			}
			pCursor.close();
		}

		// Email
		info += "Email Addresses:\n";
		Cursor emailCur = getContentResolver().query(
				ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
				ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
				new String[] { id }, null);
		while (emailCur.moveToNext()) {
			// This would allow you get several email addresses
			// if the email addresses were stored in an array
			String email = emailCur
					.getString(emailCur
							.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
			int emailType = emailCur
					.getInt(emailCur
							.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE));
			info += "  " + getEmailType(emailType) + ": " + email + "\n";
		}
		emailCur.close();

		cursor.close();
		return info;
	}

	/**
	 * Gets the id for the given name.
	 * 
	 * @param name
	 * @return The id or -1 on failure.
	 */
	private long getContactId(String name) {
		if (name == null || name.length() == 0) {
			return -1;
		}
		String selection = ContactsContract.Contacts.DISPLAY_NAME + "=\""
				+ name + "\"";
		String[] desiredColumns = { COL_ID, };
		Cursor cursor = getContentResolver().query(
				ContactsContract.Contacts.CONTENT_URI, desiredColumns,
				selection, null, null);
		if (cursor.getCount() == 0) {
			return -1;
		}
		int indexId = cursor.getColumnIndex(COL_ID);
		cursor.moveToNext();
		long id = cursor.getLong(indexId);
		cursor.close();
		return id;
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
	 * Deletes the message and navigates to the next message.
	 */
	private void deleteCall() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(
				"Are you sure you want to delete "
						+ "this call from the call log database? "
						+ "It cannot be undone.")
				.setCancelable(false)
				.setPositiveButton(getText(R.string.yes_label),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								if (dryRun) {
									Toast.makeText(getApplicationContext(),
											"Dry run:\n" + "Message deleted",
											Toast.LENGTH_LONG).show();
									navigate(RESULT_NEXT);
								} else {
									try {
										// The following change the database
										getContentResolver().delete(uri,
												"_id = " + mRowId, null);
										navigate(RESULT_NEXT);
										// This was used temporarily to change
										// one record
										// ContentValues values = new
										// ContentValues();
										// values.put(COL_NAME, "Susan Semrod");
										// Long idd = 790l;
										// getContentResolver().update(uri,
										// values,
										// "_id = " + idd, null);
									} catch (Exception ex) {
										Utils.excMsg(DisplayCallActivity.this,
												"Problem deleting call", ex);
									}
								}
							}
						})
				.setNegativeButton(getText(R.string.cancel_label),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
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
			int indexRawContactId = cursor.getColumnIndex(COL_RAW_CONTACT_ID);
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
				long rawContactId = -1;
				if (indexType > -1) {
					rawContactId = cursor.getLong(indexRawContactId);
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

				// Set the contact view
				if (mContactTextView != null) {
					String contactInfo = null;
					if (name != null && name.length() > 0) {
						contactInfo = getContactInfo(name);
					} else {
						contactInfo = "Unknown Contact";
					}
					if (contactInfo != null) {
						mContactTextView.setText(contactInfo);
					}
				}

				// Set the image view
				if (mImageView != null) {
					long contactId = getContactId(name);
					Bitmap bitmap = loadContactPhoto(getContentResolver(),
							contactId);
					if (bitmap == null) {
						// bitmap =
						// BitmapFactory.decodeFile("/sdcard/test2.png");
						bitmap = BitmapFactory.decodeResource(getResources(),
								R.drawable.android_icon);
					}
					if (bitmap != null) {
						mImageView.setImageBitmap(bitmap);
					}
				}
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
