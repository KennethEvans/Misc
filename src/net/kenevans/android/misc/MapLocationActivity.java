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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class MapLocationActivity extends MapActivity implements IConstants {
	private List<Overlay> mapOverlays;
	private MapView mMapView;
	private MyLocationOverlay mMyLocation;
	// Defaults are 1600 Amphitheatre Pkwy, Mountain View, CA 94043
	/** The latitude to show. */
	private int mLatitude = 37422028;
	/** The longitude to show. */
	private int mLongitude = -122084068;
	/** The NID to show in the popup. */
	private int mNid = 0;
	/** The SID to show in the popup. */
	private int mSid = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.maplocation);

		// Check the intent to see if it came with a specified latitude and
		// longitude
		Intent intent = getIntent();
		if (intent.hasExtra(LATITUDE)) {
			mLatitude = intent.getExtras().getInt(LATITUDE);
		}
		if (intent.hasExtra(LONGITUDE)) {
			mLongitude = intent.getExtras().getInt(LONGITUDE);
		}
		if (intent.hasExtra(NID)) {
			mNid = intent.getExtras().getInt(NID);
		}
		if (intent.hasExtra(SID)) {
			mSid = intent.getExtras().getInt(SID);
		}
		Log.i(TAG, "onCreate: mLatitude=" + mLatitude + " mLongitude="
				+ mLongitude);

		mMapView = (MapView) findViewById(R.id.mapview);
		mMapView.setBuiltInZoomControls(true);

		// Navigate to the current point
		MapController mc = mMapView.getController();
		GeoPoint point = new GeoPoint(mLatitude, mLongitude);
		mc.animateTo(point);

		mapOverlays = mMapView.getOverlays();

		// Create the overlay
		MarkerItemizedOverlay markerOverlay = new MarkerItemizedOverlay(this,
				this.getResources().getDrawable(R.drawable.tower));
		createMarkerOverlay(markerOverlay);
		mapOverlays.add(markerOverlay);

		// Add the current location
		mMyLocation = new MyLocationOverlay(this,
				mMapView);
		mMyLocation.enableMyLocation();
		mapOverlays.add(mMyLocation);
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Avoid unnecessary processing
		if(mMyLocation != null) {
			mMyLocation.disableMyLocation();
		}
	}

	@Override
	protected void onResume() {
		// Restore the offset so the user can use it again
		super.onResume();
		// Start processing again
		if(mMyLocation != null) {
			mMyLocation.enableMyLocation();
		}
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	private void createMarkerOverlay(MarkerItemizedOverlay markerOverlay) {
		GeoPoint point = new GeoPoint(mLatitude, mLongitude);
		OverlayItem overlayItem = new OverlayItem(point, "Cell Tower", "NID="
				+ mNid + " SID=" + mSid + "\nLat="
				+ String.format("%.6f", mLatitude * 1.e-6) + " Lon="
				+ String.format("%.6f", mLongitude * 1.e-6));
		markerOverlay.addOverlay(overlayItem);
	}

}
