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

import android.net.Uri;

/**
 * Holds constant values used by several classes in the application.
 */
public interface IConstants {
	// Log tag
	/** Tag to associate with log messages. */
	public static final String TAG = "Misc";

	// Information
	/** Key for information URL sent to InfoActivity. */
	public static final String INFO_URL = "InformationURL";
	
	// Preferences
	public static final String PREF_MEDIA_MONKEY_DIRECTORY = "dataDirectory";
	
	// Media Monkey
	/** Directory on the SD card where the music is stored */
	public static final String SD_CARD_MUSIC_DIRECTORY = "Music";


	// SMS database
	/** The key for the URI used. */
	public static final String URI_KEY = "URI";
	/** The key for the date multiplier used. */
	public static final String DATE_MULTIPLIER_KEY = "DateMultiplier";
	/** The URI for messages. Has all messages. */
	public static final Uri SMS_URI = Uri.parse("content://sms");
	/** The URI for the inbox. */
	public static final Uri SMS_INBOX_URI = Uri.parse("content://sms/inbox");
	/** The URI for the outbox. */
	public static final Uri SMS_OUTBOX_URI = Uri.parse("content://sms/outbox");
	/** The URI for the sent messages. */
	public static final Uri SMS_SENT_URI = Uri.parse("content://sms/sent");
	/** SMS database column for the id. Identifies the row. */
	public static final String COL_ID = "_id";
	/** SMS database column for the address. */
	public static final String COL_ADDRESS = "address";
	/** SMS database column for the date. */
	public static final String COL_DATE = "date";
	/**
	 * MMS-SMS database variable for the normalized date. Only valid for sort
	 * order.
	 */
	public static final String VAR_NORMALIZED_DATE = "normalized_date";
	/** SMS database column for the body. */
	public static final String COL_BODY = "body";
	/** SMS database column for the thread ID. */
	public static final String COL_THREAD_ID = "thread_id";
	/** SMS-MMS database column for the Message Type (SMS or MMS). */
	public static final String COL_CT_T = "ct_t";
	/** MMS database column for the Mime Type. */
	public static final String COL_CT = "ct";
	/** MMS database column for the Mime data. */
	public static final String COL_DATA = "_data";
	/** MMS database column for the text. */
	public static final String COL_TEXT = "text";

	/** Request code for displaying a call. */
	public static final int DISPLAY_CALL = 0;
	/** The URI for calls. */
	public static final Uri CALLLOG_CALLS_URI = android.provider.CallLog.Calls.CONTENT_URI;
	/** Callog.Calls database column for the id. Identifies the row. */
	public static final String COL_NUMBER = "number";
	/** Callog.Calls database column for the address. */
	public static final String COL_DURATION = "duration";
	/** Callog.Calls database column for the date. */
	public static final String COL_NAME = "name";
	/** Callog.Calls database column for the type. */
	public static final String COL_TYPE = "type";
	/** Callog.Calls database column for the raw_contact_id. */
	public static final String COL_RAW_CONTACT_ID = "raw_contact_id";

	/** The URI for MMS messages. Has all messages. */
	public static final Uri MMS_URI = Uri.parse("content://mms");
	/** The URI for MMS part where the body, etc. are found. */
	public static final Uri MMS_PART_URI = Uri.parse("content://mms/part");
	/** The URI for SMS and MMS messages. Wasn't successful. */
	public static final Uri MMS__SMS_URI = Uri.parse("content://mms-sms");
	/** The URI for MMS and SMS conversations. */
	public static final Uri MMS_SMS_CONVERSATIONS_URI = Uri
			.parse("content://mms-sms/conversations");
	/** The URI for MMS and SMS complete conversations. Wasn't successful. */
	public static final Uri MMS_SMS_COMPLETE_CONVERSATIONS_URI = Uri
			.parse("content://mms-sms/complete-conversations");

	// Date multipliers MMS messages have the time in seconds. SMS messages have
	// the time in ms.
	/** Date multiplier for SMS messages. */
	public long SMS_DATE_MULTIPLIER = 1L;
	/** Date multiplier for MMS messages. */
	public long MMS_DATE_MULTIPLIER = 1000L;

	// Conversations database
	// From SMS Fix Time:
	// The content://sms URI does not notify when a thread is deleted, so
	// instead we use the content://mms-sms/conversations URI for observing.
	// This provider, however, does not play nice when looking for and editing
	// the existing messages. So, we use the original content://sms URI for our
	// editing
	/** The URI for conversations. */
	public static final Uri SMS_CONVERSATIONS_URI = Uri
			.parse("content://mms-sms/conversations");

	// Messages
	/** Request code for displaying a message. */
	public static final int DISPLAY_MESSAGE = 0;
	/** Request code for sending a spam message. */
	public static final int SPAM_MESSAGE = 1;
	/** Result code for DISPLAY_MESSAGE indicating the previous message. */
	public static final int RESULT_PREV = 1000;
	/** Result code for DISPLAY_MESSAGE indicating the next message. */
	public static final int RESULT_NEXT = 1001;

	/** The number (7726) used for forwarding spam. */
	public static final String SPAM_NUMBER = "7726";
	// DEBUG
	// public static final String SPAM_NUMBER = "5554";

	// Mapping
	/** Value denoting a latitude in extras */
	public static final String LATITUDE = "net.kenevans.android.misc.latitude";
	/** Value denoting a latitude in extras */
	public static final String LONGITUDE = "net.kenevans.android.misc.longitude";
	/** Value denoting a SID in extras */
	public static final String SID = "net.kenevans.android.misc.sid";
	/** Value denoting a NID in extras */
	public static final String NID = "net.kenevans.android.misc.nid";

	/** KB/byte. Converts bytes to KB. */
	public static final double KB = 1. / 1024.;
	/** MB/byte. Converts bytes to MB. */
	public static final double MB = 1. / (1024. * 1024.);
	/** GB/byte. Converts bytes to GB. */
	public static final double GB = 1. / (1024. * 1024. * 1024.);

}
