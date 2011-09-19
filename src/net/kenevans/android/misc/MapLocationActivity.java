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

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

/**
 * Class to plot tower and user locations. If the incoming Intent has extra
 * data, plot this data and do not update. Otherwise start a timer and update
 * the location and other data at the current interval.
 * 
 */
public class MapLocationActivity extends MapActivity implements IConstants {
	private MyLocationOverlay mMyLocation;
	/** Whether the map looks for updates to the tower location or not. */
	private boolean autoUpdate = true;
	/** The timer to use for updates. */
	private IntervalTimer mTimer;
	/** The update interval in ms. */
	private int mInterval = 10000;

	/** The last known value for the latitude. */
	private int mLatitude;
	/** The last known value for the longitude. */
	private int mLongitude;
	/** The last known value for the NID. */
	private int mNid;
	/** The last known value for the SID. */
	private int mSid;

	/** The last error message or null if none. */
	String lastError;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.maplocation);

		MapView mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);

		// Find the tower values
		int[] vals = getTowerValues();
		if (vals == null) {
			String msg = "Failed to get tower location";
			if (lastError != null) {
				msg += "\n" + lastError;
			}
			Utils.errMsg(this, msg);
		} else {
			mLatitude = vals[0];
			mLongitude = vals[1];
			mNid = vals[2];
			mSid = vals[3];
		}

		// Initialize the map with the current values.
		initializeMap();

		if (!autoUpdate) {
			// Animate to the current point
			mapView.getController().animateTo(
					new GeoPoint(mLatitude, mLongitude));
		} else {
			// Create the timer but don't start it. It will be started in
			// onResume.
			mTimer = new IntervalTimer(mInterval, new Runnable() {
				@Override
				public void run() {
					updateMap();
				}
			});
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.maplocationmenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
		case R.id.refresh:
			updateMap();
			return true;
		case R.id.details:
			showDetails();
			return true;
		}
		return false;
	}

	@Override
	protected void onPause() {
		Log.d(TAG, this.getClass().getSimpleName() + ": onPause: mLatitude="
				+ mLatitude + " mLongitude=" + mLongitude);
		super.onPause();
		// Avoid unnecessary processing
		if (mMyLocation != null) {
			mMyLocation.disableMyLocation();
		}
		if (mTimer != null) {
			mTimer.stop();
		}
	}

	@Override
	protected void onResume() {
		// Restore the offset so the user can use it again
		super.onResume();
		// Start processing again
		if (mMyLocation != null) {
			mMyLocation.enableMyLocation();
		}
		if (autoUpdate) {
			updateMap();
			if (mTimer != null) {
				mTimer.start();
			}
		}
		Log.d(TAG, this.getClass().getSimpleName() + ": onResume: mLatitude="
				+ mLatitude + " mLongitude=" + mLongitude);
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * Initializes the MapView the first time. Creates a MarkerItemizedOverlay
	 * as the first overlay and the user location overlay as the second overlay.
	 */
	private void initializeMap() {
		Log.d(TAG, "initializeMap: " + " mLatitude=" + mLatitude
				+ " mLongitude=" + mLongitude + " mNid=" + mNid + " mSid="
				+ mSid);

		// Get the MapView and overlays
		MapView mapView = null;
		List<Overlay> mapOverlays = null;
		try {
			mapView = (MapView) findViewById(R.id.mapview);
			mapOverlays = mapView.getOverlays();
		} catch (Exception ex) {
			Utils.excMsg(this, "Error getting MapView and its overlays", ex);
			return;
		}

		// Create the MarkerItemizedOverlay
		MarkerItemizedOverlay newMarkerOverlay = createMarkerOverlay(mLatitude,
				mLongitude, mNid, mSid);
		if (newMarkerOverlay != null) {
			mapOverlays.add(newMarkerOverlay);
		}

		// Create the user location overlay in the second position
		mMyLocation = new MyLocationOverlay(this, mapView);
		mMyLocation.enableMyLocation();
		mapOverlays.add(mMyLocation);
	}

	private int[] getTowerValues() {
		// Find new values
		TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		// Find the location
		if (tm == null) {
			popupMsg("Could not get TelephonyManager");
			return null;
		}
		int phoneType = tm.getPhoneType();
		if (phoneType != TelephonyManager.PHONE_TYPE_CDMA) {
			popupMsg("Only CDMA is supported");
			return null;
		}
		CellLocation cl = tm.getCellLocation();
		if (cl == null) {
			popupMsg("Could not get Cell Location");
			return null;
		}
		if (!(cl instanceof CdmaCellLocation)) {
			popupMsg("Cell Location is is not a CdmaCellLocation class");
			return null;
		}
		CdmaCellLocation cdmacl = (CdmaCellLocation) cl;
		int lat = NetworkActivity.locToGoogle(cdmacl.getBaseStationLatitude());
		int lon = NetworkActivity.locToGoogle(cdmacl.getBaseStationLongitude());
		int nid = cdmacl.getNetworkId();
		int sid = cdmacl.getSystemId();
		Log.d(TAG, "  New values: " + " lat=" + lat + " lon=" + lon + " nid="
				+ nid + " sid=" + sid);
		return new int[] { lat, lon, nid, sid };
	}

	/**
	 * Updates the map with the new tower location if the location has changed.
	 * This method may be called often with a short interval between calls.
	 */
	private void updateMap() {
		Log.d(TAG, "updateMap: " + " mLatitude=" + mLatitude + " mLongitude="
				+ mLongitude + " mNid=" + mNid + " mSid=" + mSid);

		// Find new tower values
		int[] vals = getTowerValues();
		if (vals == null) {
			return;
		}
		int lat = vals[0];
		int lon = vals[1];
		int nid = vals[2];
		int sid = vals[3];

		// Get the existing overlays if any
		MapView mapView = null;
		List<Overlay> mapOverlays = null;
		try {
			mapView = (MapView) findViewById(R.id.mapview);
			mapOverlays = mapView.getOverlays();
		} catch (Exception ex) {
			String msg = "Error getting MapView and its overlays";
			popupMsg(msg);
			return;
		}

		// Check if the values have changed
		try {
			if (lat == mLatitude && lon == mLongitude) {
				// No change
				Log.d(TAG, "  No change");
				return;
			}
			// Replace the old one
			Log.d(TAG, "  Replacing: " + " lat=" + mLatitude + " lon="
					+ mLongitude);
			mLatitude = lat;
			mLongitude = lon;
			mNid = nid;
			mSid = sid;
			MarkerItemizedOverlay newMarkerOverlay = createMarkerOverlay(lat,
					lon, nid, sid);
			if (newMarkerOverlay != null) {
				mapOverlays.set(0, newMarkerOverlay);
			}
			// Animate to this point
			mapView.getController().animateTo(new GeoPoint(lat, lon));
		} catch (Exception ex) {
			popupMsg("Error resetting MarkerItemizedOverlay");
			return;
		}
	}

	/**
	 * Utility to make a Toast message and write to the log. For use in the
	 * update method to present a short error indication but not cause error
	 * dialog storms.
	 * 
	 * @param msg
	 */
	private void popupMsg(String msg) {
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
		Log.e(TAG, this.getClass().getSimpleName() + ": " + msg);
		lastError = msg;
	}

	/**
	 * Bring up the NetworkActivity.
	 */
	private void showDetails() {
		try {
			Log.d(TAG, this.getClass().getSimpleName()
					+ ": showDetails: mLatitude=" + mLatitude + " mLongitude="
					+ mLongitude);
			// Start the NetworkActivity
			Intent intent = new Intent();
			intent.setClass(this, NetworkActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(intent);
		} catch (Exception ex) {
			Utils.excMsg(this, "Error showing details", ex);
		}
	}

	/**
	 * Create a new MarkerItemizedOverlay with the given parameters.
	 * 
	 * @param lat
	 *            Latitude in Google units.
	 * @param lon
	 *            Longitude in Google units
	 * @param nid
	 *            Network ID.
	 * @param sid
	 *            System ID.
	 * @return The new MarkerItemizedOverlay or null on failure.
	 */
	private MarkerItemizedOverlay createMarkerOverlay(int lat, int lon,
			int nid, int sid) {
		MarkerItemizedOverlay markerOverlay = null;
		try {
			markerOverlay = new MarkerItemizedOverlay(this, this.getResources()
					.getDrawable(R.drawable.tower));
			GeoPoint point = new GeoPoint(lat, lon);
			OverlayItem overlayItem = new OverlayItem(point, "Cell Tower",
					"NID=" + nid + " SID=" + sid + "\nLat="
							+ String.format("%.6f", lat * 1.e-6) + " Lon="
							+ String.format("%.6f", lon * 1.e-6));
			markerOverlay.addOverlay(overlayItem);
		} catch (Exception ex) {
			Utils.excMsg(this, "Cannot create a new MarkerItemizedOverlay", ex);
			return null;
		}
		return markerOverlay;
	}

}
