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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

/**
 * Activity to display information about the network and carrier.
 * 
 */
public class NetworkActivity extends Activity implements IConstants {
	private int mLatitude = Integer.MAX_VALUE;
	private int mLongitude = Integer.MAX_VALUE;
	private int mSid = 0;
	private int mNid = 0;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.network);

		// Call refresh to set the contents
		refresh();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.networkmenu, menu);
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
		case R.id.map:
			showMap();
			return true;
		}
		return false;
	}

	@Override
	protected void onPause() {
		Log.i(TAG, "onPause: mLatitude=" + mLatitude + " mLongitude="
				+ mLongitude);
		super.onPause();
		// Retain the state
		SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
		editor.putInt("latitude", mLatitude);
		editor.putInt("longitude", mLongitude);
		editor.putInt("nid", mNid);
		editor.putInt("sid", mSid);
		editor.commit();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Restore the state
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		mLatitude = prefs.getInt("latitude", mLatitude);
		mLongitude = prefs.getInt("longitude", mLongitude);
		mNid = prefs.getInt("nid", mNid);
		mSid = prefs.getInt("sid", mSid);
		Log.i(TAG, "onResume: mLatitude=" + mLatitude + " mLongitude="
				+ mLongitude);
	}

	/**
	 * Gets information about the network.
	 * 
	 * @return
	 */
	private String getNetworkInfo() {
		String info = "Network Information\n";
		Date now = new Date();
		// This has the most hope of giving a result that is locale dependent.
		// At one time it had the UTC offset wrong, but seems to work now.
		info += now + "\n\n";
		// This allows more explicit formatting.
		// SimpleDateFormat formatter = new SimpleDateFormat(
		// "MMM dd, yyyy HH:mm:ss z");
		// info += formatter.format(now) + "\n\n";
		try {
			TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			if (tm == null) {
				info += "Could not get TelephonyManager";
				return info;
			}
			info += "Roaming: " + tm.isNetworkRoaming() + "\n";
			info += "Network Operator: " + tm.getNetworkOperator() + "\n";
			info += "Network Operator Name: " + tm.getNetworkOperatorName()
					+ "\n";
			int phoneType = tm.getPhoneType();
			String type = "Unknown";
			if (phoneType == TelephonyManager.PHONE_TYPE_NONE) {
				type = "None";
			} else if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
				type = "CDMA";
			} else if (phoneType == TelephonyManager.PHONE_TYPE_GSM) {
				type = "GSM";
			}
			info += "Phone Type: " + type + "\n";
			CellLocation cl = tm.getCellLocation();
			if (cl == null) {
				info += "Could not get Cell Location\n";
				return info;
			} else {
				info += cl.getClass().getSimpleName() + "\n";
			}
			if (cl instanceof CdmaCellLocation) {
				CdmaCellLocation cdmacl = (CdmaCellLocation) cl;
				// info += "CDMA Cell Location:\n";
				// info += "Location: " + cdmacl.toString() + "\n";
				mNid = cdmacl.getNetworkId();
				info += "Network ID: " + mNid + "\n";
				mSid = cdmacl.getSystemId();
				info += "System ID: " + mSid + "\n";
				info += "Base Station ID: " + cdmacl.getBaseStationId() + "\n";

				int loc = cdmacl.getBaseStationLatitude();
				info += "Base Station Lat: " + loc + " [" + locToDeg(loc)
						+ " deg]\n";
				mLatitude = locToGoogle(loc);

				loc = cdmacl.getBaseStationLongitude();
				info += "Base Station Lon: " + loc + " [" + locToDeg(loc)
						+ " deg]\n";
				mLongitude = locToGoogle(loc);
				Log.i(TAG, "getNetworkInfo: mLatitude=" + mLatitude
						+ " mLongitude=" + mLongitude);
				// Retain the state as these values
				SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE)
						.edit();
				editor.putInt("latitude", mLatitude);
				editor.putInt("longitude", mLongitude);
				editor.putInt("nid", mNid);
				editor.putInt("sid", mSid);
				editor.commit();
			} else if (cl instanceof GsmCellLocation) {
				GsmCellLocation gsmcl = (GsmCellLocation) cl;
				info += "GSM Cell Location:\n";
				info += "Cell ID: " + gsmcl.getCid() + "\n";
				info += "LAC: " + gsmcl.getLac() + "\n";
			}
			// List<NeighboringCellInfo> ncis = tm.getNeighboringCellInfo();
			// This doesn't appear useful for CDMA
			// if(ncis != null) {
			// for(NeighboringCellInfo nci : ncis) {
			// nci.getLac();
			// }
			// }
		} catch (Exception ex) {
			info += "Error getting Network Information:\n";
			info += ex.getMessage();
		}

		return info;
	}

	private void showMap() {
		if (mLongitude == Integer.MAX_VALUE || mLongitude == Integer.MAX_VALUE) {
			Utils.errMsg(this,
					"Current latitude and longitude values are invalid");
			return;
		}
		try {
			Log.i(TAG, "showMap: mLatitude=" + mLatitude + " mLongitude="
					+ mLongitude);
			// Start the MapLocationActivity
			Intent intent = new Intent();
			intent.setClass(this, MapLocationActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_SINGLE_TOP);
			intent.putExtra(LATITUDE, mLatitude);
			intent.putExtra(LONGITUDE, mLongitude);
			intent.putExtra(SID, mSid);
			intent.putExtra(NID, mNid);
			startActivity(intent);
		} catch (Exception ex) {
			Utils.excMsg(this, "Error showing map", ex);
		}
	}

	/**
	 * Input value is in units of .25 sec from -1296000 to 1296000 corresponding
	 * to -90 deg to 90 deg for latitude and from -2592000 to 2592000
	 * corresponding to -180 deg to 180 deg for longitude.
	 * 
	 * @param loc
	 *            Value from CdmaCellLocation.
	 * @return Value in degrees to six places or "Invalid" if lat is
	 *         Integer.MAX_VALUE.
	 */
	private String locToDeg(int loc) {
		if (loc == Integer.MAX_VALUE) {
			return "Invalid";
		}
		double val = 90. * loc / 1296000.;
		return String.format("%.6f", val);
	}

	/**
	 * Input value is in units of .25 sec from -1296000 to 1296000 corresponding
	 * to -90 deg to 90 deg for latitude and from -2592000 to 2592000
	 * corresponding to -180 deg to 180 deg for longitude.
	 * 
	 * @param loc
	 *            Value from CdmaCellLocation.
	 * @return Value suitable for GoogleMaps, which is 1.e6 times the value in
	 *         deg.
	 */
	private int locToGoogle(int loc) {
		if (loc == Integer.MAX_VALUE) {
			return loc;
		}
		int val = (int) Math.round(90.e6 * loc / 1296000.);
		return val;
	}

	/**
	 * Updates the network information.
	 */
	private void refresh() {
		try {
			TextView tv = (TextView) findViewById(R.id.textview);
			String info = getNetworkInfo();
			tv.setText(info);
		} catch (Exception ex) {
			Utils.excMsg(this, "Error in Refresh", ex);
		}
	}

	/**
	 * Copies the contents of the network information view to the clipboard.
	 */
	private void copyToClipboard() {
		try {
			ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			TextView tv = (TextView) findViewById(R.id.textview);
			cm.setText(tv.getText());
		} catch (Exception ex) {
			Utils.excMsg(this, "Error setting Clipboard", ex);
		}
	}

}
