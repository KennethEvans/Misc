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
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Class to display a single MMS message.
 */
public class DisplayMMSActivity extends Activity implements IConstants {
    /**
     * Set this to not make any changes to the database.
     */
    private boolean mDryRun = true;
    /**
     * The current default value for the user's offset.
     */
    private static int mLastTimeOffset;
    /**
     * The Uri to use.
     */
    public Uri mUri;
    /**
     * The date multiplier to use to get ms. MMS message timestamps are in sec
     * not ms.
     */
    public Long mDateMultiplier = 1L;
    /**
     * Name of the file written to the root of the SD card
     */
    private static final String SAVE_FILE_NAME = "SavedMMSMessageText.txt";

    private TextView mTitleTextView;
    private TextView mSubtitleTextView;
    private TextView mBodyTextView;
    private TextView mInfoTextView;
    private ImageView mImageView;
    private Long mRowId;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.displaymmsmessage);

        // Use minus the users time offset as the initial suggested value
        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        mLastTimeOffset = calendar.getTimeZone().getOffset(now.getTime());

        // Get the saved state for lastOffset, otherwise resets to the above.
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        mLastTimeOffset = prefs.getInt("timeOffset", mLastTimeOffset);

        mTitleTextView = (TextView) findViewById(R.id.titleview);
        mSubtitleTextView = (TextView) findViewById(R.id.subtitleview);
        mSubtitleTextView.setMovementMethod(new ScrollingMovementMethod());
        mInfoTextView = (TextView) findViewById(R.id.infoview);
        mBodyTextView = (TextView) findViewById(R.id.bodyview);
        mBodyTextView.setMovementMethod(new ScrollingMovementMethod());
        mImageView = (ImageView) findViewById(R.id.imageview);

        // Buttons
        ImageButton button = (ImageButton) findViewById(R.id.upbutton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigate(RESULT_NEXT);
            }
        });
        button = (ImageButton) findViewById(R.id.downbutton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigate(RESULT_PREV);
            }
        });

        mRowId = (savedInstanceState == null) ? null
                : (Long) savedInstanceState.getSerializable(COL_ID);
        Bundle extras = getIntent().getExtras();
        if (mRowId == null && extras != null) {
            mRowId = extras.getLong(COL_ID);
        }
        if (extras != null) {
            String uriPath = extras.getString(URI_KEY);
            if (uriPath != null) {
                mUri = Uri.parse(uriPath);
            }
            mDateMultiplier = extras.getLong(DATE_MULTIPLIER_KEY);
        }
        if (mUri == null) {
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
        inflater.inflate(R.menu.displaymessagemenu, menu);
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
            case R.id.fixtime:
                fixTime();
                return true;
            case R.id.dryrun:
                toggleDryRun();
                return true;
            case R.id.reportspam:
                reportSpam();
                return true;
            case R.id.savetext:
                saveToTextFile(false);
                return true;
            case R.id.cleartext:
                saveToTextFile(true);
                return true;
            case R.id.delete:
                deleteMessage();
                return true;
            case R.id.help:
                showHelp();
                return true;
        }
        return false;
    }

    @Override
    protected void onPause() {
        Log.d(TAG, this.getClass().getSimpleName() + ".onPause: ");
        super.onPause();
        // Retain the offset so the user can use it again
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        editor.putInt("timeOffset", mLastTimeOffset);
        editor.putBoolean("dryrun", mDryRun);
        editor.commit();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, this.getClass().getSimpleName() + ".onResume: ");
        // Restore the offset so the user can use it again
        super.onResume();
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        mLastTimeOffset = prefs.getInt("timeOffset", mLastTimeOffset);
        mDryRun = prefs.getBoolean("dryrun", mDryRun);
    }

    /**
     * Gets the current time from the database, prompts the user for a time
     * offset, and changes the time in the data base unless the user cancels.
     */
    private void fixTime() {
        try {
            String[] columns = {COL_DATE};
            // Only get the row with mRowId
            String selection = COL_ID + "=" + mRowId.longValue();
            String sort = COL_DATE + " DESC";
            Cursor cursor = getContentResolver().query(mUri, columns, selection,
                    null, sort);

            int indexDate = cursor.getColumnIndex(COL_DATE);
            // There should only be one row returned, the last will be the most
            // recent if more are returned owing to the sort above
            boolean found = cursor.moveToFirst();
            if (!found) {
                cursor.close();
                Utils.errMsg(this, "Did not find message");
                return;
            }
            final Long curDate = cursor.getLong(indexDate) * mDateMultiplier;
            // We are through with the cursor
            cursor.close();

            // Make a TimeOffsetDialog to get the users value
            final TimeOffsetDialog dialog = new TimeOffsetDialog(this,
                    mLastTimeOffset);
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
                            Utils.errMsg(DisplayMMSActivity.this,
                                    "Got invalid value for the time offset");
                        } else {
                            // Save this value as the default
                            mLastTimeOffset = offset;
                            long newDate = curDate + offset;
                            if (mDryRun) {
                                Toast.makeText(
                                        getApplicationContext(),
                                        "Dry run:\n"
                                                + "Old Time="
                                                + MessageUtils
                                                .formatDate(curDate)
                                                + "\nNew Time="
                                                + MessageUtils
                                                .formatDate(newDate),
                                        Toast.LENGTH_LONG).show();
                            } else {
                                // The following change the database
                                ContentValues values = new ContentValues();
                                values.put(COL_DATE, newDate);
                                getContentResolver().update(mUri, values,
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
                            mLastTimeOffset = offset;
                        }
                    }
                }
            });
            dialog.show();
        } catch (Exception ex) {
            Utils.excMsg(this, "Error finding MMS message", ex);
        }
    }

    /**
     * Sets the result code to send back to the calling Activity. One of:
     * <ul>
     * <li>RESULT_PREV
     * <li>RESULT_NEXT
     * </ul>
     *
     * @param resultCode The result code to send.
     */
    private void navigate(int resultCode) {
        setResult(resultCode);
        finish();
    }

    /**
     * Deletes the message and navigates to the next message.
     */
    private void deleteMessage() {
        if (mDryRun) {
            Toast.makeText(getApplicationContext(),
                    "Dry run:\n" + "Message deleted", Toast.LENGTH_LONG).show();
        } else {
            try {
                // The following change the database
                getContentResolver().delete(mUri, "_id = " + mRowId, null);
                navigate(RESULT_NEXT);
            } catch (Exception ex) {
                Utils.excMsg(this, "Problem deleting message", ex);
            }
        }
    }

    /**
     * Reports spam by sending a message to SPAM_NUMBER. This version uses
     * Intent.ACTION_SENDTO, which brings up a list to select the
     * application to
     * use. It results in a copy of the sent message appearing in the database.
     * It does not attach any other information to the message.
     *
     * @see IConstants#SPAM_NUMBER
     */
    private void reportSpam() {
        // Note: Using SmsManager.getDefault() did not put the sent message in
        // the database
        try {
            // Get the message
            String msg = mBodyTextView.getText().toString();

            // Pop up a message
            Toast.makeText(
                    getApplicationContext(),
                    "First, forward the message to " + SPAM_NUMBER
                            + "\nPress the back button when done",
                    Toast.LENGTH_LONG).show();

            // Send the message using ACTION_SENDTO
            Uri uri = Uri.parse("smsto:" + SPAM_NUMBER);
            Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
            intent.putExtra("sms_body", msg);
            startActivityForResult(intent, SPAM_MESSAGE);
        } catch (Exception ex) {
            Utils.excMsg(this, "Problem reporting spam", ex);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        // DEBUG
        Log.d(TAG, this.getClass().getSimpleName()
                + ".onActivityResult: requestCode=" + requestCode
                + " resultCode=" + resultCode);
        if (requestCode == SPAM_MESSAGE) {
            // Handle activity to send spam message finished
            // Can't check if it was cancelled since it always seems to return 0
            // Now, send the number
            try {
                // Get the number
                String msg = mTitleTextView.getText().toString();

                // Remove the id
                int pos = msg.indexOf(":") + 2;
                if (pos > 1 && !(pos > msg.length())) {
                    msg = msg.substring(pos);
                }

                // Truncate at the linefeed
                pos = msg.indexOf("\n");
                msg = msg.substring(0, pos);

                // Pop up a message for the user
                Toast.makeText(
                        getApplicationContext(),
                        "Next, send the number"
                                + "\nPress the back button when done",
                        Toast.LENGTH_LONG).show();

                // Send the number using ACTION_SENDTO
                mUri = Uri.parse("smsto:" + SPAM_NUMBER);
                intent = new Intent(Intent.ACTION_SENDTO, mUri);
                intent.putExtra("sms_body", msg);
                startActivity(intent);
            } catch (Exception ex) {
                Utils.excMsg(this, "Problem sending spam number", ex);
            }
        }
    }

    /**
     * Saves the info to the SD card
     */
    private void saveToTextFile(boolean clear) {
        BufferedWriter out = null;
        try {
            File sdCardRoot = Environment.getExternalStorageDirectory();
            if (sdCardRoot.canWrite()) {
                File dir = new File(sdCardRoot, SD_CARD_MISC_DIR);
                if (dir.exists() && dir.isFile()) {
                    Utils.errMsg(this, "Cannot create directory: " + dir
                            + "\nA file with that name exists.");
                    return;
                }
                if (!dir.exists()) {
                    Log.d(TAG, this.getClass().getSimpleName()
                            + ": create: dir=" + dir.getPath());
                    boolean res = dir.mkdir();
                    if (!res) {
                        Utils.errMsg(this, "Cannot create directory: " + dir);
                        return;
                    }
                }
                File file = new File(dir, SAVE_FILE_NAME);
                // Append if clear is false
                FileWriter writer = new FileWriter(file, !clear);
                out = new BufferedWriter(writer);
                if (clear) {
                    out.write("");
                    Utils.infoMsg(this, "Cleared " + file.getPath());
                } else {
                    // Get the number
                    String msg = mTitleTextView.getText().toString();
                    // Remove the id
                    int pos = msg.indexOf(":") + 2;
                    if (pos > 1 && !(pos > msg.length())) {
                        msg = msg.substring(pos);
                    }
                    // String string = MessageUtils.formatSmsType(getSmsType())
                    // + msg + "\n" + mBodyTextView.getText().toString()
                    // + "\n\n";
                    String string = "MMS " + msg + "\n"
                            + mBodyTextView.getText().toString() + "\n\n";
                    out.append(string);
                    Utils.infoMsg(this, "Wrote " + file.getPath());
                }
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
                    "file:///android_asset/displaymessage.html");
            startActivity(intent);
        } catch (Exception ex) {
            Utils.excMsg(this, "Error showing Help", ex);
        }
    }

    /**
     * Toggles whether database changes are real or simulated.
     */
    private void toggleDryRun() {
        final CharSequence[] items = {getText(R.string.on),
                getText(R.string.off)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getText(R.string.dryRunTitle));
        builder.setSingleChoiceItems(items, mDryRun ? 0 : 1,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        dialog.dismiss();
                        mDryRun = item == 0 ? true : false;
                        String msg;
                        if (mDryRun) {
                            msg = "Time changes are simulated.\nDatabase will" +
                                    " not be changed.";
                        } else {
                            msg = "Time changes are real.\nDatabase will be " +
                                    "changed.";
                        }
                        Utils.infoMsg(DisplayMMSActivity.this, msg);
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Gets the MMS text for the given id.
     *
     * @param id
     * @return
     */
    public String getMmsText(String id) {
        Uri partURI = Uri.parse("content://mms/part/" + id);
        InputStream is = null;
        StringBuilder sb = new StringBuilder();
        try {
            is = getContentResolver().openInputStream(partURI);
            if (is != null) {
                InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                BufferedReader reader = new BufferedReader(isr);
                String temp = reader.readLine();
                while (temp != null) {
                    sb.append(temp);
                    temp = reader.readLine();
                }
            }
        } catch (IOException ex) {
            Log.e(TAG, getClass().getSimpleName() + ".getMmsText Exception: "
                    + ex.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
        return sb.toString();
    }

    /**
     * Gets the MMS bitmap for the given id.
     *
     * @param _id
     * @return
     */
    public Bitmap getMmsImage(String _id) {
        Uri partURI = Uri.parse("content://mms/part/" + _id);
        InputStream is = null;
        Bitmap bitmap = null;
        try {
            is = getContentResolver().openInputStream(partURI);
            bitmap = BitmapFactory.decodeStream(is);
        } catch (IOException ex) {
            Log.e(TAG, getClass().getSimpleName() + ".getMmsImage Exception: "
                    + ex.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
        return bitmap;
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
            Cursor cursor = getContentResolver().query(mUri, null, selection,
                    null, null);
            String[] columns = cursor.getColumnNames();
            cursor.close();

            // Then get the columns for this row
            String sort = COL_DATE + " DESC";
            cursor = getContentResolver().query(mUri, columns, selection, null,
                    sort);
            int indexId = cursor.getColumnIndex(COL_ID);
            int indexDate = cursor.getColumnIndex(COL_DATE);
            // The address and body are not in this database.
            // There is no SMS type.
            Log.d(TAG, this.getClass().getSimpleName() + ".refresh: "
                    + " mRowId=" + mRowId + " mUri=" + mUri.toString()
                    + " DATE_MULTIPLIER=" + mDateMultiplier);

            List<String> mimeList = new ArrayList<String>();
            Bitmap bitmap = null;

            // There should only be one row returned, the last will be the most
            // recent if more are returned owing to the sort above
            boolean found = cursor.moveToFirst();
            if (!found) {
                mTitleTextView.setText("<Error>");
                mSubtitleTextView.setText("");
                mBodyTextView.setText("Failed to find message " + mRowId);
            } else {
                String id = cursor.getString(indexId);
                Long dateNum = -1L;
                if (indexDate > -1) {
                    dateNum = cursor.getLong(indexDate) * mDateMultiplier;
                }
                // We need to get the address from another provider
                String address = "<Address NA>";
                String text = MessageUtils.getMmsAddress(this, id);
                if (text != null) {
                    address = text;
                }

                // We need to get the body from the part
                String body = "<Body NA>";
                String partSelection = "mid=" + id;
                Uri partUri = MMS_PART_URI;
                Cursor partCursor = getContentResolver().query(partUri, null,
                        partSelection, null, null);
                // DEBUG
                String debugString = "";
                int nPart = 0;

                if (partCursor.moveToFirst()) {
                    String data = null;
                    do {
                        // DEBUG
                        debugString += "\n\nPart " + nPart++;
                        debugString +=
                                MessageUtils.getColumnNamesAndValues
                                        (partCursor);

                        // Get the part ID
                        String partId = partCursor.getString(partCursor
                                .getColumnIndex(COL_ID));

                        // Determine the attachment type
                        String cType = partCursor.getString(partCursor
                                .getColumnIndex(COL_CT));
                        if (cType != null) {
                            mimeList.add(cType);
                        }
                        if ("text/plain".equals(cType)) {
                            text = null;
                            data = partCursor.getString(partCursor
                                    .getColumnIndex(COL_DATA));
                            if (data != null) {
                                text = getMmsText(partId);
                            } else {
                                text = partCursor.getString(partCursor
                                        .getColumnIndex(COL_TEXT));
                            }
                            if (text != null) {
                                if (body.equals("<Body NA>")) {
                                    body = "";
                                }
                                body += text + "\n";
                            }
                        } else if ("image/jpeg".equals(cType)
                                || "image/bmp".equals(cType)
                                || "image/gif".equals(cType)
                                || "image/jpg".equals(cType)
                                || "image/png".equals(cType)) {
                            // Use only the first one
                            if (bitmap == null) {
                                bitmap = getMmsImage(partId);
                            }
                        }
                    } while (partCursor.moveToNext());
                }
                if (partCursor != null) {
                    partCursor.close();
                }

                String title = id;
                // Indicate if more than one found
                if (cursor.getCount() > 1) {
                    title += " [1/" + cursor.getCount() + "]";
                }
                title += ": " + MessageUtils.formatAddress(address) + "\n"
                        + MessageUtils.formatDate(dateNum);
                String subTitle = "";
                Log.d(TAG, getClass().getSimpleName() + ".refresh" + " id="
                        + id + " address=" + address + " dateNum=" + dateNum
                        + " DATE_MULTIPLIER=" + mDateMultiplier);

                // Add the mime types
                subTitle += "Content Types" + "\n";
                for (String mimeType : mimeList) {
                    subTitle += "  " + mimeType + "\n";
                }
                subTitle += "\n";

                // Add all the addresses
                subTitle += "Associated Addresses\n";
                String[] addresses = MessageUtils.getAllMmsAddresses(this, id);
                for (String addr : addresses) {
                    subTitle += "  " + addr + "\n";
                }
                subTitle += "\n";

                // DEBUG
                // Add the part column names
                if (debugString != null) {
                    subTitle += "Part Data" + debugString + "\n";
                }

                // Add all the fields in the database
                subTitle += MessageUtils.getColumnNamesAndValues(cursor);

                // Set the TextViews
                mTitleTextView.setText(title);
                mSubtitleTextView.setText(subTitle);
                mBodyTextView.setText(body);

                // Set the info view
                String info = MessageUtils.formatDate(
                        MessageUtils.shortFormatter, dateNum)
                        + "\n"
                        + "MMS "
                        + MessageUtils.formatAddress(address);
                String contactName = MessageUtils.getContactNameFromNumber(
                        this, address);
                if (!contactName.equals("Unknown")) {
                    info += "\n" + contactName;
                }
                mInfoTextView.setText(info);

                // Set the image view
                if (mImageView != null && bitmap != null) {
                    mImageView.setImageBitmap(bitmap);
                }

                // DEBUG
                // if (id.equals(new Integer(76).toString())) {
                // SMSActivity.test(3, this.getClass(), this, cursor, id, mUri);
                // SMSActivity.test(4, this.getClass(), this, null, id, mUri);
                // }
            }
            // We are through with the cursor
            cursor.close();
        } catch (Exception ex) {
            String msg = "Error finding MMS message:\n" + ex.getMessage();
            Utils.excMsg(this, "Error finding MMS message", ex);
            String stackTrace = Utils.getStackTraceString(ex);
            Log.d(TAG,
                    getClass().getSimpleName() + ".refresh " + ex.getMessage()
                            + "\n" + stackTrace);

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

}
