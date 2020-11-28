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
    /**
     * Tag to associate with log messages.
     */
    String TAG = "Misc";

    // Information
    /**
     * Key for information URL sent to InfoActivity.
     */
    String INFO_URL = "InformationURL";

    /**
     * Directory on the SD card for saving data for all apps that do save.
     */
    String SD_CARD_MISC_DIR = "Misc";

    // Preferences
    String PREF_MEDIA_MONKEY_DIRECTORY = "dataDirectory";
    String PREF_WIFI_SORT_ORDER = "wifiSortOrder";
    String PREF_APPDETAILS_APP_NAMES = "appdetailsAppNames";

    // Media Monkey
    /**
     * Directory on the SD card where the music is stored
     */
    String SD_CARD_MUSIC_DIRECTORY = "Music";

    // SMS database
    /**
     * The key for the URI used.
     */
    String URI_KEY = "URI";
    /**
     * The key for the date multiplier used.
     */
    String DATE_MULTIPLIER_KEY = "DateMultiplier";
    /**
     * The URI for messages. Has all messages.
     */
    Uri SMS_URI = Uri.parse("content://sms");
    /**
     * The URI for the inbox.
     */
    Uri SMS_INBOX_URI = Uri.parse("content://sms/inbox");
    /**
     * The URI for the outbox.
     */
    Uri SMS_OUTBOX_URI = Uri.parse("content://sms/outbox");
    /**
     * The URI for the sent messages.
     */
    Uri SMS_SENT_URI = Uri.parse("content://sms/sent");
    /**
     * SMS database column for the id. Identifies the row.
     */
    String COL_ID = "_id";
    /**
     * SMS database column for the address.
     */
    String COL_ADDRESS = "address";
    /**
     * SMS database column for the date.
     */
    String COL_DATE = "date";
    /**
     * MMS-SMS database variable for the normalized date. Only valid for sort
     * order.
     */
    String VAR_NORMALIZED_DATE = "normalized_date";
    /**
     * SMS database column for the body.
     */
    String COL_BODY = "body";
    /**
     * SMS database column for the thread ID.
     */
    String COL_THREAD_ID = "thread_id";
    /**
     * SMS-MMS database column for the Message Type (SMS or MMS).
     */
    String COL_CT_T = "ct_t";
    /**
     * MMS database column for the Mime Type.
     */
    String COL_CT = "ct";
    /**
     * MMS database column for the Mime data.
     */
    String COL_DATA = "_data";
    /**
     * MMS database column for the text.
     */
    String COL_TEXT = "text";
    /**
     * MMS database column for the message_type.
     */
    String COL_MESSAGE_TYPE = "message_type";
    /**
     * MMS database column for the message_count.
     */
    String COL_MESSAGE_COUNT = "message_count";
    /**
     * MMS database column for the has_attachment.
     */
    String COL_HAS_ATTACHMENT = "has_attachment";

    /**
     * Request code for displaying a call.
     */
    int DISPLAY_CALL = 0;
    /**
     * The URI for calls.
     */
    Uri CALLLOG_CALLS_URI = android.provider.CallLog
            .Calls.CONTENT_URI;
    /**
     * Callog.Calls database column for the id. Identifies the row.
     */
    String COL_NUMBER = "number";
    /**
     * Callog.Calls database column for the address.
     */
    String COL_DURATION = "duration";
    /**
     * Callog.Calls database column for the date.
     */
    String COL_NAME = "name";
    /**
     * Callog.Calls database column for the type.
     */
    String COL_TYPE = "type";
    /**
     * Callog.Calls database column for the raw_contact_id.
     */
    String COL_RAW_CONTACT_ID = "raw_contact_id";

    /**
     * The URI for MMS messages. Has all messages.
     */
    Uri MMS_URI = Uri.parse("content://mms");
    /**
     * The URI for MMS part where the body, etc. are found.
     */
    Uri MMS_PART_URI = Uri.parse("content://mms/part");
    /**
     * The URI for SMS and MMS messages. Wasn't successful.
     */
    Uri MMS__SMS_URI = Uri.parse("content://mms-sms");
    /**
     * The URI for MMS and SMS conversations. Simple form.
     */
    Uri MMS_SMS_CONVERSATIONS_URI_SIMPLE = Uri
            .parse("content://mms-sms/conversations?simple=true");
    /**
     * The URI for MMS and SMS conversations.
     */
    Uri MMS_SMS_CONVERSATIONS_URI = Uri
            .parse("content://mms-sms/conversations");
    /**
     * The URI for MMS and SMS complete conversations. Wasn't successful.
     */
    Uri MMS_SMS_COMPLETE_CONVERSATIONS_URI = Uri
            .parse("content://mms-sms/complete-conversations");

    // Date multipliers MMS messages have the time in seconds. SMS messages have
    // the time in ms.
    /**
     * Date multiplier for SMS messages.
     */
    long SMS_DATE_MULTIPLIER = 1L;
    /**
     * Date multiplier for MMS messages.
     */
    long MMS_DATE_MULTIPLIER = 1000L;

    // Conversations database
    // From SMS Fix Time:
    // The content://sms URI does not notify when a thread is deleted, so
    // instead we use the content://mms-sms/conversations URI for observing.
    // This provider, however, does not play nice when looking for and editing
    // the existing messages. So, we use the original content://sms URI for our
    // editing
    /**
     * The URI for conversations.
     */
    Uri SMS_CONVERSATIONS_URI = Uri
            .parse("content://mms-sms/conversations");

    // Messages
    /**
     * Request code for displaying a message.
     */
    int DISPLAY_MESSAGE = 0;
    /**
     * Request code for sending a spam message.
     */
    int SPAM_MESSAGE = 1;
    /**
     * Result code for creating a document.
     */
    int CREATE_DOCUMENT = 10;
    /**
     * Result code for DISPLAY_MESSAGE indicating the previous message.
     */
    int RESULT_PREV = 1000;
    /**
     * Result code for DISPLAY_MESSAGE indicating the next message.
     */
    int RESULT_NEXT = 1001;

    /**
     * The number (7726) used for forwarding spam.
     */
    String SPAM_NUMBER = "7726";
    // DEBUG
    // String SPAM_NUMBER = "5554";

    // Mapping
    /**
     * Value denoting a latitude in extras
     */
    String LATITUDE = "net.kenevans.android.misc.latitude";
    /**
     * Value denoting a latitude in extras
     */
    String LONGITUDE = "net.kenevans.android.misc.longitude";
    /**
     * Value denoting a SID in extras
     */
    String SID = "net.kenevans.android.misc.sid";
    /**
     * Value denoting a NID in extras
     */
    String NID = "net.kenevans.android.misc.nid";

    /**
     * KB/byte. Converts bytes to KB.
     */
    double KB = 1. / 1024.;
    /**
     * MB/byte. Converts bytes to MB.
     */
    double MB = 1. / (1024. * 1024.);
    /**
     * GB/byte. Converts bytes to GB.
     */
    double GB = 1. / (1024. * 1024. * 1024.);

}
