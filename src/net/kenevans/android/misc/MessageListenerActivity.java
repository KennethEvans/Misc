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

import java.util.Date;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.text.ClipboardManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

public class MessageListenerActivity extends Activity implements IConstants {
	public static final String SMS_MESSAGE_INFO = "net.kenevans.android.misc.SMS_MESSAGE_INFO";

	BroadcastReceiver mReceiver;

	private TextView mTextView;
	private String mContents;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.messagelistener);

		// Get the TextView
		mTextView = (TextView) findViewById(R.id.textview);
		// Make it scroll
		mTextView.setMovementMethod(new ScrollingMovementMethod());

		// Make a BroadcastReceiver
		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				// The time this method was called
				Date now = new Date();

				// DEBUG
				Log.i(TAG, "BroadcastReceiver.onReceive: mContents.length()="
						+ ((mContents == null) ? "null" : mContents.length()));
				Bundle extras = intent.getExtras();
				if (extras == null) {
					return;
				}

				Object[] pdus = (Object[]) extras.get("pdus");

				String info = "----------------\n";
				for (int i = 0; i < pdus.length; i++) {
					SmsMessage message = SmsMessage
							.createFromPdu((byte[]) pdus[i]);
					info += "From: "
							+ SMSActivity.formatAddress(message
									.getOriginatingAddress()) + "\n";
					info += "Time received: "
							+ SMSActivity.formatDate(now.getTime()) + "\n";
					info += "Timestamp: "
							+ SMSActivity.formatDate(message
									.getTimestampMillis()) + "\n";
					info += "TimestampMillis: " + message.getTimestampMillis()
							+ "\n";

					// DEBUG
					Log.i(TAG,
							"BroadcastReceiver.onReceive: date="
									+ SMSActivity.formatDate(message
											.getTimestampMillis()));

					info += "ServiceCenterAddress: "
							+ message.getServiceCenterAddress() + "\n";
					// info += "Body:\n" + message.getMessageBody() + "\n";
					info += "Display Body:\n" + message.getDisplayMessageBody()
							+ "\n";
					mContents += info;

					// Save the state now in case we are paused when the event
					// comes in
					SharedPreferences.Editor editor = getPreferences(
							MODE_PRIVATE).edit();
					editor.putString("contents", mContents);
					editor.commit();

					mTextView.setText(mContents);
				}
			}
		};
		registerReceiver(mReceiver, new IntentFilter(
				"android.provider.Telephony.SMS_RECEIVED"));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.messagelistenermenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
		case R.id.clear:
			clearDisplay();
			return true;
		case R.id.copy:
			copyToClipboard();
			return true;
		}
		return false;
	}

	@Override
	protected void onPause() {
		super.onPause();
		// DEBUG
		Log.i(TAG, this.getClass().getSimpleName()
				+ ".onPause: mContents.length()="
				+ ((mContents == null) ? "null" : mContents.length()));
		super.onPause();
		// Retain the contents
		SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
		editor.putString("contents", mContents);
		editor.commit();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Restore the contents
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		mContents = prefs.getString("contents", mContents);
		resetDisplay();

		// DEBUG
		Log.i(TAG, this.getClass().getSimpleName()
				+ ".onResume: mContents.length()="
				+ ((mContents == null) ? "null" : mContents.length()));
	}

	@Override
	protected void onDestroy() {
		// Unregister the receiver (Not sure if this is necessary)
		try {
			if (mReceiver != null) {
				Log.i(TAG, this.getClass().getSimpleName() + ": "
						+ "unregisterReceiver");
				unregisterReceiver(mReceiver);
				mReceiver = null;
			}
		} catch (Exception ex) {
			Log.i(TAG, this.getClass().getSimpleName() + ": "
					+ " Problem with unregisterReceiver\n" + ex.getMessage());
		}
		super.onDestroy();
	}

	/**
	 * Resets the received information from mContents.
	 */
	private void resetDisplay() {
		try {
			if (mContents == null) {
				clearDisplay();
				mContents = mTextView.getText().toString();
			} else {
				mTextView.setText(mContents);
			}
		} catch (Exception ex) {
			Utils.excMsg(this, "Error setting the display", ex);
		}
	}

	/**
	 * Clears the received information.
	 */
	private void clearDisplay() {
		try {
			mTextView.setText(getText(R.string.messagelistener_title) + "\n\n");
			mContents = mTextView.getText().toString();
		} catch (Exception ex) {
			Utils.excMsg(this, "Error clearing the display", ex);
		}
	}

	/**
	 * Copies the contents of the current time view to the clipboard.
	 */
	private void copyToClipboard() {
		try {
			ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			TextView tv = (TextView) findViewById(R.id.textview);
			cm.setText(tv.getText());
		} catch (Exception ex) {
			Utils.excMsg(this, "Error setting clipboard", ex);
		}
	}

}
