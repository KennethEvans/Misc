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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

public class CurrentTimeActivity extends Activity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.currenttime);

		// Call refresh to set the contents
		refresh();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.currenttimemenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
		case R.id.refresh:
			refresh();
			return true;
		case R.id.copy:
			copyToClipboard();
			return true;
		}
		return false;
	}

	/**
	 * Gets the current time in several formats.
	 * 
	 * @return
	 */
	private String getTimeInfo() {
		String info = "Using Date.toString()\n";
		Date now = new Date();
		info += now + "\n\n";

		info += "Using Calendar.getTime()\n";
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(now);
		double offset = calendar.getTimeZone().getOffset(now.getTime()) / 3600000.;
		String stringOffset = String.format("%.2f", offset);
		info += calendar.getTime() + "\n";
		info += "Time Zone Offset: " + stringOffset + " hr\n\n";

		String format = "MMM dd, yyyy HH:mm:ss";
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		offset = formatter.getTimeZone().getOffset(now.getTime()) / 3600000.;
		stringOffset = String.format("%.2f", offset);
		info += "Using format: " + format + "\n";
		info += formatter.format(now) + "\n";
		info += "Time Zone Offset: " + stringOffset + " hr\n\n";

		format = "MMM dd, yyyy HH:mm:ss z";
		formatter = new SimpleDateFormat(format);
		offset = formatter.getTimeZone().getOffset(now.getTime()) / 3600000.;
		stringOffset = String.format("%.2f", offset);
		info += "Using format: " + format + "\n";
		info += formatter.format(now) + "\n";
		info += "Time Zone Offset: " + stringOffset + " hr\n\n";

		format = "MMM dd, yyyy HH:mm:ss Z";
		formatter = new SimpleDateFormat(format);
		offset = formatter.getTimeZone().getOffset(now.getTime()) / 3600000.;
		stringOffset = String.format("%.2f", offset);
		info += "Using format: " + format + "\n";
		info += formatter.format(now) + "\n";
		info += "Time Zone Offset: " + stringOffset + " hr\n\n";

		return info;
	}

	/**
	 * Updates the information.
	 */
	private void refresh() {
		try {
			TextView tv = (TextView) findViewById(R.id.textview);
			String info = getTimeInfo();
			tv.setText(info);
		} catch (Exception ex) {
			Utils.excMsg(this, "Error in Refresh", ex);
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
