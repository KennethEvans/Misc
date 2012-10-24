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

import java.io.InputStream;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Class that contains general purpose message utilities.
 * 
 */
public class MessageUtils implements IConstants {
	/** The static format string to use for formatting dates. */
	public static final String longFormat = "MMM dd, yyyy HH:mm:ss Z";
	public static final SimpleDateFormat longFormatter = new SimpleDateFormat(
			longFormat);

	/** The static format string to use for formatting dates. */
	public static final String mediumFormat = "MMM dd, yyyy HH:mm:ss";
	public static final SimpleDateFormat mediumFormatter = new SimpleDateFormat(
			mediumFormat);

	/** The static short format string to use for formatting dates. */
	public static final String shortFormat = "M/d/yy h:mm a";
	public static final SimpleDateFormat shortFormatter = new SimpleDateFormat(
			shortFormat);

	/**
	 * Format the date using the static format.
	 * 
	 * @param dateNum
	 * @return
	 * @see #longFormat
	 */
	public static String formatDate(Long dateNum) {
		return formatDate(longFormatter, dateNum);
	}

	/**
	 * Format the date using the given format.
	 * 
	 * @param formatter
	 * @param dateNum
	 * @return
	 * @see #longFormat
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
	 * Format the type as a string.
	 * 
	 * @param type
	 * @return
	 */
	public static String formatSmsType(int type) {
		// TODO Find where these constants are defined
		// Internally in Telephony.BaseSmsColumns
		// Internally in TextBasedSmsColumns
		switch (type) {
		case 0:
			return "All ";
		case 1:
			return "From ";
		case 2:
			return "To ";
		case 3:
			return "Draft ";
		case 4:
			return "Outbox ";
		case 5:
			return "Failed ";
		case 6:
			return "Queued ";
		default:
			return "";
		}
	}

	/**
	 * Format the MMS address type type as a string.
	 * 
	 * @param type
	 * @return
	 */
	public static String formatMmsAddressType(String type) {
		// TODO Find where these constants are defined
		if (type.equals("151")) {
			return "To ";
		} else if (type.equals("137")) {
			return "From ";
		} else {
			return "Type " + type + " ";
		}
	}

	/**
	 * Gets the values in the given Context for each of the given columns for
	 * the given id from the database given by the Uri . The value will be "NA"
	 * if it is not available.
	 * 
	 * @param context
	 *            The calling context.
	 * @param id
	 *            The row id.
	 * @param uri
	 *            The Uri for the database.
	 * @param columns
	 *            An array of columns.
	 * @return An array of the values for each column or null on failure.
	 */
	public static String[] getStringValues(Context context, long id, Uri uri,
			String[] columns) {
		if (columns == null) {
			return null;
		}
		int nCols = columns.length;
		String[] values = new String[nCols];
		if (nCols == 0) {
			return values;
		}
		for (int i = 0; i < nCols; i++) {
			values[i] = "NA";
		}
		Cursor cursor = null;
		try {
			// Get the available columns from all rows
			String selection = COL_ID + "=" + id;
			cursor = context.getContentResolver().query(uri, columns,
					selection, null, null);
			cursor.moveToFirst();
			int index;
			for (int i = 0; i < nCols; i++) {
				index = cursor.getColumnIndex(columns[i]);
				if (index > -1) {
					values[i] = cursor.getString(index);
				}
			}
		} catch (Exception ex) {
			Log.e(TAG, MessageActivity.class.getSimpleName()
					+ ".getStringValues Exception: " + ex.getMessage());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return values;
	}

	/**
	 * Gets the MMS address for the given id.
	 * 
	 * @param context
	 *            The calling context.
	 * @param id
	 * @return
	 */
	public static String getMmsAddress(Context context, String id) {
		String addrSelection = "type=137 AND msg_id=" + id;
		String uriStr = MessageFormat.format("content://mms/{0}/addr", id);
		Uri uriAddress = Uri.parse(uriStr);
		String[] columns = { "address" };
		Cursor cursor = context.getContentResolver().query(uriAddress, columns,
				addrSelection, null, null);
		// DEBUG
		// String[] columnNames = addrCursor.getColumnNames();
		Log.d(TAG, MessageUtils.class.getSimpleName() + ".getMmsAddress: "
				+ uriAddress.toString() + " count=" + cursor.getCount());
		// for (String string : columnNames) {
		// Log.d(TAG, string);
		// }

		// String address = null;
		// String val;
		// if (addrCursor.moveToFirst()) {
		// do {
		// val = addrCursor
		// .getString(addrCursor.getColumnIndex("address"));
		// if (val != null) {
		// try {
		// Long.parseLong(val.replace("-", ""));
		// address = val;
		// } catch (NumberFormatException ex) {
		// if (address == null) {
		// address = val;
		// }
		// }
		// }
		// } while (addrCursor.moveToNext());
		// }

		String address = "";
		String val;
		// String type;
		if (cursor.moveToFirst()) {
			do {
				val = cursor.getString(cursor.getColumnIndex("address"));
				// type = addrCursor
				// .getString(addrCursor.getColumnIndex("type"));
				// address += val + " " + type + "\n";
				if (val != null) {
					address = val;
					// Use the first one found if more than one
					break;
				}
			} while (cursor.moveToNext());
		}
		if (cursor != null) {
			cursor.close();
		}
		Log.d(TAG, MessageUtils.class.getSimpleName() + ".getMmsAddress: "
				+ " address=" + address);
		return address;
	}

	/**
	 * Gets all the MMS address for the given id, one per line in the form: <br>
	 * "type address contact_name"
	 * 
	 * @param context
	 *            The calling context.
	 * @param id
	 * @return
	 */
	public static String[] getAllMmsAddresses(Context context, String id) {
		String selection = "msg_id=" + id;
		String uriStr = MessageFormat.format("content://mms/{0}/addr", id);
		Uri uri = Uri.parse(uriStr);
		String[] columns = { "address", "type", "contact_id" };
		Cursor cursor = context.getContentResolver().query(uri, columns,
				selection, null, null);
		Log.d(TAG, MessageUtils.class.getSimpleName() + ".getAllMmsAddresses: "
				+ uri.toString() + " count=" + cursor.getCount()
				+ " columnCount=" + cursor.getColumnCount());
		int indexAddr = cursor.getColumnIndex("address");
		int indexType = cursor.getColumnIndex("type");
		int indexId = cursor.getColumnIndex("contact_id");

		List<String> list = new ArrayList<String>();
		String addr;
		String type;
		String contactId;
		String contactName = "";
		String thisPhoneNumber = compressPhoneNumber(getThisPhoneNumber(context));
		if (cursor.moveToFirst()) {
			do {
				addr = type = contactId = "";
				if (indexAddr > -1) {
					addr = cursor.getString(indexAddr);
				}
				if (indexType > -1) {
					type = cursor.getString(indexType);
				}
				if (indexId > -1) {
					contactId = cursor.getString(indexId);
				}
				if (contactId == null) {
					// Try to get it from the number
					contactName = getContactNameFromNumber(context, addr);
				} else {
					contactName = getContactName(context, contactId);
				}
				if (contactName.equals("Unknown")) {
					// Check if it is this phone as a last ditch resort
					if (compressPhoneNumber(addr).equals(thisPhoneNumber)) {
						contactName = " This Phone";
					} else {
						contactName = "";
					}
				} else {
					contactName = " " + contactName;
				}
				list.add(formatMmsAddressType(type) + formatAddress(addr) + " "
						+ type + contactName);
			} while (cursor.moveToNext());
		}
		if (cursor != null) {
			cursor.close();
		}
		String[] addresses = new String[list.size()];
		list.toArray(addresses);
		return addresses;
	}

	/**
	 * Get a String with the column names and values for the given cursor.
	 * 
	 * @param cursor
	 * @return
	 */
	public static String getColumnNamesAndValues(Cursor cursor) {
		String info = "";
		String[] columnNames = cursor.getColumnNames();
		for (String name : columnNames) {
			try {
				int index = cursor.getColumnIndex(name);
				// Don't do a LF the first time
				if (info.length() != 0) {
					info += "\n";
				}
				// Don't print the body
				if (name.equals("body")) {
					info += name + ": <" + cursor.getString(index).length()
							+ " chars>";
				} else {
					info += name + ": " + cursor.getString(index);
				}
			} catch (Exception ex) {
				// Shouldn't happen
				info += name + ": Not found";
				continue;
			}
		}
		return info;
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
	 * Finds the contact name given the contact ID.
	 * 
	 * @param context
	 *            The calling context.
	 * @param contactId
	 * @return
	 */
	public static String getContactName(Context context, String contactId) {
		String displayName = "Unknown";
		if (contactId == null) {
			Log.d(TAG, MessageUtils.class.getSimpleName() + ".getContactName: "
					+ "contactId is null");
			return displayName;
		}
		String selection = COL_ID + "=" + contactId;
		String[] columns = { ContactsContract.Contacts.DISPLAY_NAME };
		Cursor cursor = context.getContentResolver().query(
				ContactsContract.Contacts.CONTENT_URI, columns, selection,
				null, null);
		Log.d(TAG, MessageUtils.class.getSimpleName() + ".getContactName: "
				+ "contactId=" + contactId + " count=" + cursor.getCount());
		if (cursor.getCount() == 0) {
			cursor.close();
			return displayName;
		}
		String val;
		while (cursor.moveToNext()) {
			val = cursor.getString(cursor
					.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
			if (val != null) {
				// Return the first one if there is more than one
				displayName = val;
				break;
			}
		}
		Log.d(TAG, MessageUtils.class.getSimpleName() + ".getContactName: "
				+ "displayName=" + displayName);
		return displayName;
	}

	/**
	 * Get contact information about the given name.
	 * 
	 * @param context
	 *            The calling context.
	 * @param name
	 * @return The information or null on failure.
	 */
	public static String getContactInfo(Context context, String name) {
		if (name == null || name.length() == 0) {
			return null;
		}
		String info = "Contact Information for " + name + "\n";

		// DEBUG
		// Cursor cursor1 = getContentResolver().query(
		// ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
		// info += "count=" + cursor1.getCount() + "\n";
		// String[] columnNames = cursor1.getColumnNames();
		// for (String string : columnNames) {
		// info += string + "\n";
		// }
		// info += "\n";
		// if (cursor1.moveToFirst()) {
		// do {
		// String displayName = cursor1
		// .getString(cursor1
		// .getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
		// info += displayName + "\n";
		// if (name.equals(displayName)) {
		// info += "!!! Matches " + name + "\n";
		// }
		// } while (cursor1.moveToNext());
		// }
		// if (cursor1 != null) {
		// cursor1.close();
		// }
		// info += "\n";

		// Using a selection stopped working
		// String selection = ContactsContract.Contacts.DISPLAY_NAME + "=\""
		// + name + "\"";
		String selection = null;
		String[] columns = { COL_ID, ContactsContract.Contacts.DISPLAY_NAME,
				ContactsContract.Contacts.PHOTO_ID,
				ContactsContract.Contacts.HAS_PHONE_NUMBER };
		Cursor cursor = context.getContentResolver().query(
				ContactsContract.Contacts.CONTENT_URI, columns, selection,
				null, null);
		if (cursor.getCount() == 0) {
			info += name + " not found\n";
			cursor.close();
			return info;
		}
		int indexId = cursor.getColumnIndex(COL_ID);
		int indexPhotoId = cursor
				.getColumnIndex(ContactsContract.Contacts.PHOTO_ID);
		// int indexTmesContacted = cursor
		// .getColumnIndex(ContactsContract.Contacts.TIMES_CONTACTED);
		// int indexLastTimeContacted = cursor
		// .getColumnIndex(ContactsContract.Contacts.LAST_TIME_CONTACTED);

		String displayName = "Not found";
		boolean found = false;
		if (cursor.moveToFirst()) {
			do {
				displayName = cursor
						.getString(cursor
								.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				if (name.equals(displayName)) {
					found = true;
					break;
				}
			} while (cursor.moveToNext());
		}
		if (!found) {
			// The name was not found
			cursor.close();
			return null;
		}

		// The cursor should be positioned at the right place
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
			Cursor pCursor = context.getContentResolver().query(
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
				info += "  " + MessageUtils.getPhoneType(phoneType) + ": "
						+ phoneNumber + "\n";
			}
			pCursor.close();
		}

		// Email
		info += "Email Addresses:\n";
		Cursor emailCur = context.getContentResolver().query(
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
			info += "  " + MessageUtils.getEmailType(emailType) + ": " + email
					+ "\n";
		}
		emailCur.close();

		cursor.close();
		return info;
	}

	/**
	 * Gets the id for the given name.
	 * 
	 * @param context
	 *            The calling context.
	 * @param name
	 * @return The id or -1 on failure.
	 */
	public static long getContactIdFromName(Context context, String name) {
		if (name == null || name.length() == 0) {
			return -1L;
		}
		// Using a selection stopped working
		// String selection = ContactsContract.Contacts.DISPLAY_NAME + "=\""
		// + name + "\"";
		// String[] columns = { COL_ID, };
		// Cursor cursor = getContentResolver().query(
		// ContactsContract.Contacts.CONTENT_URI, columns,
		// selection, null, null);
		// if (cursor.getCount() == 0) {
		// return -1L;
		// }
		// cursor.moveToNext();
		// long id = cursor.getLong(cursor.getColumnIndex(COL_ID));

		String selection = null;
		String[] columns = { COL_ID, ContactsContract.Contacts.DISPLAY_NAME };
		Cursor cursor = context.getContentResolver().query(
				ContactsContract.Contacts.CONTENT_URI, columns, selection,
				null, null);
		if (cursor.getCount() == 0) {
			return -1L;
		}
		long id = -1L;
		String displayName = null;
		if (cursor.moveToFirst()) {
			do {
				displayName = cursor
						.getString(cursor
								.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				if (name.equals(displayName)) {
					id = cursor.getLong(cursor.getColumnIndex(COL_ID));
					break;
				}
			} while (cursor.moveToNext());
		}
		cursor.close();
		return id;
	}

	/**
	 * Gets the id for the given name.
	 * 
	 * @param context
	 *            The calling context.
	 * @param number
	 * @return The id or -1 on failure.
	 */
	public static long getContactIdFromNumber(Context context, String number) {
		if (number == null || number.length() == 0) {
			return -1L;
		}
		// Get rid of everything but numerals for comparison
		String number1 = compressPhoneNumber(number);
		if (number1 == null || number1.length() == 0) {
			return -1L;
		}
		String selection = null;
		String[] columns = { ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
				ContactsContract.CommonDataKinds.Phone.NUMBER };
		// Look in the phone database
		Cursor cursor = context.getContentResolver().query(
				ContactsContract.CommonDataKinds.Phone.CONTENT_URI, columns,
				selection, null, null);
		int indexId = cursor
				.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
		int indexNumber = cursor
				.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
		if (indexId < 0 || indexNumber < 0 || cursor.getCount() == 0) {
			return -1L;
		}
		long id = -1L;
		long lVal;
		String number2;
		while (cursor.moveToNext()) {
			number2 = cursor
					.getString(cursor
							.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
			if (number2 == null) {
				continue;
			}
			number2 = compressPhoneNumber(number2);
			lVal = cursor.getLong(indexId);
			Log.d(TAG, MessageUtils.class.getSimpleName()
					+ ".getContactIdFromNumber: " + "number1,number2="
					+ number1 + "," + number2);
			if (number1.equals(number2)) {
				// Get the first one
				id = lVal;
				break;
			}
		}
		cursor.close();
		return id;
	}

	/**
	 * Gets the contact name give a phone number.
	 * 
	 * @param context
	 *            The calling context.
	 * @param number
	 * @return
	 */
	public static String getContactNameFromNumber(Context context, String number) {
		String name = "Unknown";
		if (number == null || number.length() == 0) {
			return name;
		}
		// Get rid of everything but numerals for comparison
		String number1 = compressPhoneNumber(number);
		if (number1 == null || number1.length() == 0) {
			return name;
		}
		long id = getContactIdFromNumber(context, number1);
		if (id > -1) {
			name = getContactName(context, Long.toString(id));
		}
		if (name.equals("Unknown")) {
			// Check if it is this phone as a last ditch resort
			String thisPhoneNumber = compressPhoneNumber(getThisPhoneNumber(context));
			if (number1.equals(thisPhoneNumber)) {
				name = " This Phone";
			}
		}
		return name;
	}

	/**
	 * Gets the number of this phone.
	 * 
	 * @param context
	 *            The calling context.
	 * @return
	 */
	public static String getThisPhoneNumber(Context context) {
		TelephonyManager tMgr = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		return tMgr.getLine1Number();
	}

	/**
	 * Compresses a phone number leaving only numerals and eliminating a leading
	 * 1 if there are 11 numerals.
	 * 
	 * @param number
	 * @return
	 */
	public static String compressPhoneNumber(String number) {
		if (number == null) {
			return null;
		}
		String compressed = number.replaceAll("[^0-9]", "");
		if (compressed.length() == 11 && compressed.startsWith("1")) {
			compressed = compressed.substring(1);
		}
		return compressed;
	}

}
