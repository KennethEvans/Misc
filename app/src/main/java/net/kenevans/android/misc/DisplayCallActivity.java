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
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Class to display a single message.
 */
public class DisplayCallActivity extends Activity implements IConstants {
    /**
     * Set this to not make any changes to the database.
     */
    private boolean mDryRun = false;

    /**
     * The Uri to use.
     */
    public Uri uri;

    private TextView mTitleTextView;
    private TextView mSubtitleTextView;
    private TextView mContactTextView;
    private TextView mInfoTextView;
    private ImageView mImageView;
    private Long mRowId;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.displaycall);

        // Get the saved state
        // SharedPreferences prefs = getPreferences(MODE_PRIVATE);

        mTitleTextView = (TextView) findViewById(R.id.titleview);
        mSubtitleTextView = (TextView) findViewById(R.id.subtitleview);
        mContactTextView = (TextView) findViewById(R.id.contactview);
        mInfoTextView = (TextView) findViewById(R.id.infoview);
        mImageView = (ImageView) findViewById(R.id.imageview);

        // mSubtitleTextView.setMovementMethod(new ScrollingMovementMethod());
        // mContactTextView.setMovementMethod(new ScrollingMovementMethod());

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
        // editor.putBoolean("dryrun", mDryRun);
        // editor.commit();
    }

    @Override
    protected void onResume() {
        // Restore the offset so the user can use it again
        super.onResume();
        // SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        // lastTimeOffset = prefs.getInt("timeOffset", lastTimeOffset);
        // mDryRun = prefs.getBoolean("dryrun", mDryRun);
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
    private void deleteCall() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(
                "Are you sure you want to delete "
                        + "this call from the call log database? "
                        + "It cannot be undone.")
                .setCancelable(false)
                .setPositiveButton(getText(R.string.yes_label),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int
                                    id) {
                                if (mDryRun) {
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
                                        // getContentResolver().update(mUri,
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
                            public void onClick(DialogInterface dialog, int
									id) {
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
            Log.d(TAG, this.getClass().getSimpleName() + ".refresh: "
                    + " mRowId=" + mRowId + " mUri=" + uri.toString());

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
                        + MessageUtils.formatAddress(number)
                        + " ("
                        + CallHistoryActivity.formatType(type)
                        + ") "
                        + name
                        + "\n"
                        + MessageUtils.formatDate(MessageUtils.mediumFormatter,
                        dateNum) + " Duration: "
                        + CallHistoryActivity.formatDuration(duration);
                String subTitle = "";
                Log.d(TAG, getClass().getSimpleName() + ".refresh" + " id="
                        + id + " address=" + number + " dateNum=" + dateNum);

                // Add all the fields in the database
                subTitle += MessageUtils.getColumnNamesAndValues(cursor);

                // Set the TextViews
                mTitleTextView.setText(title);
                mSubtitleTextView.setText(subTitle);

                // Set the info view
                String info = MessageUtils.formatDate(
                        MessageUtils.shortFormatter, dateNum) + "\n" + name;
                mInfoTextView.setText(info);

                // Set the contact view
                if (mContactTextView != null) {
                    String contactInfo = null;
                    if (name != null && name.length() > 0) {
                        contactInfo = MessageUtils.getContactInfo(this, name);
                    } else {
                        contactInfo = "Unknown Contact";
                    }
                    if (contactInfo != null) {
                        mContactTextView.setText(contactInfo);
                    }
                }

                // Set the image view
                if (mImageView != null) {
                    long contactId = MessageUtils.getContactIdFromName(this, name);
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
                        mImageView.setImageBitmap(bitmap);
                    }
                }
            }

            // We are through with the cursor
            cursor.close();
        } catch (Exception ex) {
            Utils.excMsg(this, "Error finding call", ex);
            if (mTitleTextView != null) {
                mTitleTextView.setText("<Error>");
            }
            if (mSubtitleTextView != null) {
                mSubtitleTextView.setText("");
            }
        }
    }

}
