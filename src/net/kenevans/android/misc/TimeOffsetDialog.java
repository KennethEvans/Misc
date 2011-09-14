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

import android.app.Dialog;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;

/**
 * Dialog for the user to enter a time offset in minutes and seconds.
 */
public class TimeOffsetDialog extends Dialog {
	private Button okButton;
	private Button cancelButton;
	private EditText hourText;
	private EditText minuteText;

	/**
	 * Constructor.
	 * 
	 * @param context
	 * @param offset
	 *            The time offset in ms to use for the suggested value.
	 */
	public TimeOffsetDialog(Context context, int offset) {
		super(context);
		// Hide the title
		// requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.timeoffset);

		okButton = (Button) findViewById(R.id.ok_button);
		cancelButton = (Button) findViewById(R.id.cancel_button);
		hourText = (EditText) findViewById(R.id.hourEditText);
		// Determine the initial values from the given offset
		int hours = offset / 3600000;
		int minutes = (offset - hours * 3600000) / 60000;
		hourText.setText(Integer.toString(hours));
		minuteText = (EditText) findViewById(R.id.minuteEditText);
		minuteText.setText(Integer.toString(minutes));
	}

	/**
	 * Gets the time offset in ms from the hours and minuteas values in the
	 * EditTexts.
	 * 
	 * @return
	 */
	public Integer getTimeOffset() {
		Integer val = null;
		try {
			String hourStr = hourText.getText().toString();
			String minuteStr = minuteText.getText().toString();
			int hours = Integer.parseInt(hourStr);
			int minutes = Integer.parseInt(minuteStr);
			// Convert to ms
			val = 3600000 * hours + 60000 * minutes;
		} catch (Exception ex) {
			val = null;
		}
		return val;
	}

	public Button getOkButton() {
		return okButton;
	}

	public Button getCancelButton() {
		return cancelButton;
	}

}
