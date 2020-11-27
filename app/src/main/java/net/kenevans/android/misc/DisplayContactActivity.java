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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
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
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Class to display a single message.
 */
public class DisplayContactActivity extends AppCompatActivity implements IConstants {
    /**
     * The content provider URI to use.
     */
    private Uri mUri;

    private TextView mTitleTextView;
    private TextView mSubtitleTextView;
    private TextView mContactTextView;
    private TextView mInfoTextView;
    private ImageView mImageView;
    private Long mRowId;

    /**
     * Called when the activity is first created.
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.displaycall);

        // Get the saved state
        // SharedPreferences prefs = getPreferences(MODE_PRIVATE);

        mTitleTextView = findViewById(R.id.titleview);
        mSubtitleTextView = findViewById(R.id.subtitleview);
        mContactTextView = findViewById(R.id.contactview);
        mInfoTextView = findViewById(R.id.infoview);
        mImageView = findViewById(R.id.imageview);

        // Swipe
        View.OnTouchListener listener =
                new SwipeDetector(DisplayContactActivity.this) {
                    @Override
                    public void onSwipeLeft() {
                        Log.d(TAG, "onSwipeLeft");
                        super.onSwipeLeft();
                        navigate(RESULT_PREV);
                    }

                    @Override
                    public void onSwipeRight() {
                        Log.d(TAG, "onSwipeRight");
                        super.onSwipeRight();
                        navigate(RESULT_NEXT);
                    }
                };
        ScrollView scrollView = findViewById(R.id.scrollview);
        scrollView.setOnTouchListener(listener);
        mTitleTextView.setOnTouchListener(listener);
        mSubtitleTextView.setOnTouchListener(listener);
        mInfoTextView.setOnTouchListener(listener);
        mImageView.setOnTouchListener(listener);

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
        }
        if (mUri == null) {
            Utils.errMsg(this, "Null content provider database URI");
            return;
        }
        mRowId = extras != null ? extras.getLong(COL_ID) : null;

        // Call refresh to set the contents
        refresh();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        // TODO
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
                deleteContacts();
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
     * @param resultCode The result code to send.
     */
    private void navigate(int resultCode) {
        setResult(resultCode);
        finish();
    }

    /**
     * Original implementation of deleting a contact.
     */
    private void deleteContactOrig() {
        Utils.infoMsg(this, "Deleting contacts is not implemented yet!");
        // TODO The folowing code appears to work but the contact is not
        // deleted

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(
                "Are you sure you want to delete "
                        + "this contact from the contacts database? "
                        + "It cannot be undone.")
                .setCancelable(false)
                .setPositiveButton(getText(R.string.yes_label),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int
                                    id) {
                                try {
                                    // The following change the database
                                    getContentResolver().delete(mUri,
                                            "_id = " + mRowId, null);
                                    navigate(RESULT_NEXT);
                                } catch (Exception ex) {
                                    Utils.excMsg(DisplayContactActivity.this,
                                            "Problem deleting contact", ex);
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
     * Finds the raw contacts, presents a dialog to select which ones to
     * delete, and deletes the selected ones.
     */
    private void deleteContacts() {
        final List<RawContactInfo> rciList = new ArrayList<>();
        final List<CharSequence> items = new ArrayList<>();
        Cursor rawCursor = getContentResolver().query
                (ContactsContract.RawContacts.CONTENT_URI,
                        null,
                        ContactsContract.RawContacts.CONTACT_ID + "=?",
                        new String[]{String.valueOf(mRowId)}, null);
        String rawId, accountName;
        RawContactInfo rci;
        while (rawCursor.moveToNext()) {
            rawId = rawCursor
                    .getString(rawCursor
                            .getColumnIndex(ContactsContract.RawContacts
                                    ._ID));
            accountName = MessageUtils.getRawContactName(this, rawCursor);
            rci = new RawContactInfo(rawId, accountName);
            rciList.add(new RawContactInfo(rawId, accountName));
            items.add(rci.toString());
        }
        rawCursor.close();

        final ArrayList<Integer> selectedList = new ArrayList<>();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle(getText(R.string.sort_title));
        builder.setTitle("Raw Contacts to Delete");
        builder.setMultiChoiceItems(items.toArray(new CharSequence[0]), null,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,
                                        int which, boolean isChecked) {
                        if (isChecked) {
                            selectedList.add(which);
                        } else if (selectedList.contains(which)) {
                            selectedList.remove(Integer
                                    .valueOf(which));
                        }
                    }
                });
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                RawContactInfo rci;
                String msg = "";
                int nDeleted;
                int nDeletedTotal = 0;
                for (int i = 0; i < selectedList.size(); i++) {
                    rci = rciList.get(selectedList.get(i));
                    nDeleted = deleteRawContact(rci.getRawId());
                    if (nDeleted == 0) {
                        msg += rci + "\n    Failed\n";
                    } else if (nDeleted == 1) {
                        msg += rci + "\n    Deleted\n";
                        nDeletedTotal++;
                    } else {
                        msg += rci + "\n    Unexpected result\n";
                    }
                }
                msg += "Deleted " + nDeletedTotal + " of " + rciList.size() +
                        " raw contacts";
                Utils.infoMsg(DisplayContactActivity.this, msg);
                refresh();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface
                .OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getApplicationContext(), "Cancelled", Toast
                        .LENGTH_LONG).show();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /***
     * Deletes a raw contact with the given raw ID.
     *
     * @param rawId The raw id to delete.
     * @return Number of raw contacts deleted.
     */
    private int deleteRawContact(String rawId) {
        int deletedRawContacts;
        if (false) {
            // DEBUG
            deletedRawContacts = 1;
        } else {
            deletedRawContacts = getContentResolver().delete(ContactsContract
                            .RawContacts.CONTENT_URI, ContactsContract
                            .RawContacts
                            ._ID + " = ?",
                    new String[]{rawId});
        }
        return deletedRawContacts;
    }

    /**
     * Gets a new cursor and redraws the view. Closes the cursor after it is
     * done with it.
     */
    private void refresh() {
        try {
            // Only get the row with mRowId
            String selection = COL_ID + "=" + mRowId;

            // First get the names of all the columns in the database
            Cursor cursor = getContentResolver().query(mUri, null, selection,
                    null, null);
            String[] columns = cursor.getColumnNames();
            cursor.close();

            // Then get the columns for this row
            String sort = ContactsContract.Contacts.DISPLAY_NAME + " ASC";
            cursor = getContentResolver().query(mUri, columns, selection, null,
                    sort);
            int indexId = cursor.getColumnIndex(COL_ID);
            int indexName = cursor
                    .getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
            Log.d(TAG, this.getClass().getSimpleName() + ".refresh: "
                    + " mRowId=" + mRowId + " mUri=" + mUri.toString());

            // There should only be one row returned, the last will be the most
            // recent if more are returned owing to the sort above
            boolean found = cursor.moveToFirst();
            if (!found) {
                mTitleTextView.setText("<Error>");
                mSubtitleTextView.setText("");
                mInfoTextView.setText("");
                mContactTextView.setText("");
            } else {
                String id = cursor.getString(indexId);
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
                title += ": " + name;
                String subTitle = "";
                Log.d(TAG, getClass().getSimpleName() + ".refresh" + " id="
                        + id + " name=" + name);

                // Add all the fields in the database
                subTitle += MessageUtils.getColumnNamesAndValues(cursor);

                // Set the TextViews
                mTitleTextView.setText(title);
                mSubtitleTextView.setText(subTitle);

                // Set the info view
                String info = name;
                mInfoTextView.setText(info);

                // Set the contact view
                if (mContactTextView != null) {
                    String contactInfo;
                    if (name.length() > 0) {
                        contactInfo = MessageUtils.getContactInfo(this, name);
                    } else {
                        contactInfo = "Unknown Contact";
                    }
                    if (contactInfo != null) {
                        mContactTextView.setText(contactInfo);
                    }
                }

                // Set the image
                if (mImageView != null) {
                    long contactId = MessageUtils.getContactIdFromName(this,
                            name);
                    Bitmap bitmap = MessageUtils.loadContactPhoto(
                            getContentResolver(), contactId);
                    if (bitmap == null) {
                        // DEBUG
                        // bitmap =
                        // BitmapFactory.decodeFile
                        // ("/sdcard/Pictures/Art/Wildcat.jpg");
                        bitmap = BitmapFactory.decodeResource(getResources(),
                                R.drawable.android_icon);
                    }
                    if (bitmap != null) {
                        mImageView.setImageBitmap(bitmap);
                    }
                }
            }
            cursor.close();
        } catch (Exception ex) {
            Utils.excMsg(this, "Error finding contact", ex);
            if (mTitleTextView != null) {
                mTitleTextView.setText("<Error>");
            }
            if (mSubtitleTextView != null) {
                mSubtitleTextView.setText("");
            }
            if (mInfoTextView != null) {
                mInfoTextView.setText("");
            }
            if (mContactTextView != null) {
                mContactTextView.setText("");
            }
        }
    }

    /***
     * Class to manage a raw contact
     */
    private static class RawContactInfo {
        private String rawId;
        private String name;

        public RawContactInfo(String rawId, String name) {
            this.rawId = rawId;
            this.name = name;
        }

        @NonNull
        @Override
        public String toString() {
            return name + " (rawId=" + rawId + ")";
        }

        public String getRawId() {
            return rawId;
        }

        public String getName() {
            return name;
        }
    }
}
