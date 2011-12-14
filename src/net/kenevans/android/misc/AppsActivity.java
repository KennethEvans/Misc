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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.ClipboardManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity to display information about installed apps.
 * 
 */
public class AppsActivity extends Activity implements IConstants {
	/** Name of the file written to the root of the SD card */
	private static final String sdCardFileName = "ApplicationInfo.txt";

	private TextView mTextView;
	public boolean doBuildInfo = false;
	//	public boolean doComponentList = false;
	public boolean doNonSystemApps = true;
	public boolean doSystemApps = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.apps);

		// requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		// Get the TextView
		mTextView = (TextView) findViewById(R.id.textview);
		// Make it scroll
		mTextView.setMovementMethod(new ScrollingMovementMethod());
		
		// refresh will be called in onResume
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.appsmenu, menu);
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
		case R.id.save:
			save();
			return true;
		case R.id.settings:
			setOptions();
			return true;
		case R.id.help:
			showHelp();
			return true;
		}
		return false;
	}

	@Override
	protected void onPause() {
		super.onPause();
		SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
		editor.putBoolean("doBuildInfo", doBuildInfo);
		editor.putBoolean("doNonSystemApps", doNonSystemApps);
		editor.putBoolean("doSystemApps", doSystemApps);
		editor.commit();
	}

	@Override
	protected void onResume() {
		super.onResume();
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		doBuildInfo = prefs.getBoolean("doBuildInfo", doBuildInfo);
		doNonSystemApps = prefs.getBoolean("doNonSystemApps", doNonSystemApps);
		doSystemApps = prefs.getBoolean("doSystemApps", doSystemApps);

		refresh();
	}

	/**
	 * Updates the application information. First sets a processing message,
	 * then calls an asynchronous task to get the information. Getting the
	 * information can take a long time, and there is no user indication
	 * something is happening otherwise.
	 */
	private void refresh() {
		try {
			mTextView.setText("Processing...");
			new RefreshTask().execute();
		} catch (Exception ex) {
			Utils.excMsg(this, "Error in Refresh", ex);
		}
	}

	/**
	 * Method that gets the application info to be displayed.
	 */
	private String getText() {
		String info;
		try {
			info = getAppsInfo();
		} catch (Exception ex) {
			Utils.excMsg(this, "Error in asyncRefresh", ex);
			info = "Error in asyncRefresh\n" + ex.getMessage();
		}
		return info;
	}

	/**
	 * An asynchronous task to get the application info, which can take a long
	 * time.
	 */
	private class RefreshTask extends AsyncTask<Void, Void, String> {
		@Override
		protected String doInBackground(Void... params) {
			return getText();
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			mTextView.setText(result);
		}
	}

	/**
	 * Copies the contents of the application information view to the clipboard.
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

	/**
	 * Saves the info to the SD card
	 */
	private void save() {
		BufferedWriter out = null;
		try {
			File sdCardRoot = Environment.getExternalStorageDirectory();
			if (sdCardRoot.canWrite()) {
				File file = new File(sdCardRoot, sdCardFileName);
				FileWriter writer = new FileWriter(file);
				out = new BufferedWriter(writer);
				CharSequence charSeq = mTextView.getText();
				out.write(charSeq.toString());
				if (charSeq.length() == 0) {
					Utils.warnMsg(this, "The file written is empty");
				}
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
		Toast.makeText(getApplicationContext(), "Wrote " + sdCardFileName,
				Toast.LENGTH_LONG).show();
	}

	/**
	 * Bring up a dialog to change the options.
	 */
	private void setOptions() {
		final CharSequence[] items = { "Build Information",
				"Downloaded Applications", "System Applications" };
		boolean[] states = { doBuildInfo, doNonSystemApps, doSystemApps };
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Settings");
		builder.setMultiChoiceItems(items, states,
				new DialogInterface.OnMultiChoiceClickListener() {
					public void onClick(DialogInterface dialogInterface,
							int item, boolean state) {
					}
				});
		builder.setPositiveButton("Okay",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						SparseBooleanArray checked = ((AlertDialog) dialog)
								.getListView().getCheckedItemPositions();
						doBuildInfo = checked.get(0);
						doNonSystemApps = checked.get(1);
						doSystemApps = checked.get(2);
						refresh();
					}
				});
		builder.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		builder.create().show();
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
					"file:///android_asset/appinfo.html");
			startActivity(intent);
		} catch (Exception ex) {
			Utils.excMsg(this, "Error showing Help", ex);
		}
	}

	/**
	 * Gets the Build information.
	 * 
	 * @return
	 */
	private String getBuildInfo() {
		StringBuffer buf = new StringBuffer();
		buf.append("VERSION.RELEASE=" + Build.VERSION.RELEASE + "\n");
		buf.append("VERSION.INCREMENTAL=" + Build.VERSION.INCREMENTAL + "\n");
		buf.append("VERSION.SDK=" + Build.VERSION.SDK + "\n");
		buf.append("BOARD=" + Build.BOARD + "\n");
		buf.append("BRAND=" + Build.BRAND + "\n");
		buf.append("DEVICE=" + Build.DEVICE + "\n");
		buf.append("FINGERPRINT=" + Build.FINGERPRINT + "\n");
		buf.append("HOST=" + Build.HOST + "\n");
		buf.append("ID=" + Build.ID + "\n");
		return buf.toString();
	}

//	/**
//	 * Return whether the given ResolveInfo represents a system package or not.
//	 * 
//	 * @param ri
//	 * @return
//	 */
//	private boolean isSystemPackage(ResolveInfo ri) {
//		return ((ri.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) ? true
//				: false;
//	}

	/**
	 * Return whether the given PackgeInfo represents a system package or not.
	 * User-installed packages (Market or otherwise) should not be denoted as
	 * system packages.
	 * 
	 * @param pkgInfo
	 * @return
	 */
	private boolean isSystemPackage(PackageInfo pkgInfo) {
		return ((pkgInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) ? true
				: false;
	}

	/**
	 * Gets a List of PInfo's for the installed packages.
	 * 
	 * @param getSysPackages
	 * @return
	 */
	private ArrayList<PInfo> getInstalledApps(boolean getSysPackages) {
		ArrayList<PInfo> res = new ArrayList<PInfo>();
		List<PackageInfo> packages = getPackageManager()
				.getInstalledPackages(0);
		for (int i = 0; i < packages.size(); i++) {
			PackageInfo pkg = packages.get(i);
			if ((!getSysPackages) && (pkg.versionName == null)) {
				continue;
			}
			PInfo newInfo = new PInfo(pkg);
			res.add(newInfo);
		}
		return res;
	}

//	private List<String> getComponentList(String action, String category) {
//		Intent intent = new Intent(action);
//		intent.addCategory(category);
//
//		List<ResolveInfo> ril = getPackageManager().queryIntentActivities(
//				intent, PackageManager.MATCH_DEFAULT_ONLY);
//		List<String> componentList = new ArrayList<String>();
//		String componentString;
//		for (ResolveInfo ri : ril) {
//			if (ri.activityInfo != null) {
//				componentString =
//				// ri.activityInfo.packageName
//				// +
//				(isSystemPackage(ri) ? "S " : "") + ri.activityInfo.name;
//				componentList.add(componentString);
//
//			}
//		}
//		return componentList;
//	}

	/**
	 * Gets information about the applications.
	 * 
	 * @return
	 */
	private String getAppsInfo() {
		String info = "Application Information\n";

		// Date
		Date now = new Date();
		// This has the most hope of giving a result that is locale dependent.
		// At one time it had the UTC offset wrong, but seems to work now.
		info += now + "\n\n";
		// This allows more explicit formatting.
		// SimpleDateFormat formatter = new SimpleDateFormat(
		// "MMM dd, yyyy HH:mm:ss z");
		// info += formatter.format(now) + "\n\n";

		// Build information
		if (doBuildInfo) {
			info += "Build Information\n\n";
			info += getBuildInfo() + "\n";
		}

//		// Installed component list
//		if (doComponentList) {
//			info += "Launcher Components\n\n";
//			List<String> components = getComponentList(Intent.ACTION_MAIN,
//					Intent.CATEGORY_LAUNCHER);
//			if (components == null) {
//				info += "<null>\n";
//			} else if (components.isEmpty()) {
//				info += "<none>\n";
//			} else {
//				for (String component : components) {
//					info += component + "\n";
//				}
//				info += "\n";
//			}
//		}

		// Non-system applications information
		if (doNonSystemApps) {
			info += "Downloaded Applications\n\n";
			try {
				// false = no system packages
				ArrayList<PInfo> apps = getInstalledApps(false);
				final int max = apps.size();
				PInfo app;
				for (int i = 0; i < max; i++) {
					app = apps.get(i);
					if (!app.isSystem) {
						info += app.prettyPrint() + "\n";
					}
				}
			} catch (Exception ex) {
				info += "Error getting Application Information:\n";
				info += ex.getMessage();
			}
		}

		// System applications information
		if (doSystemApps) {
			info += "System Applications\n\n";
			try {
				// false = no system packages
				ArrayList<PInfo> apps = getInstalledApps(false);
				final int max = apps.size();
				PInfo app;
				for (int i = 0; i < max; i++) {
					app = apps.get(i);
					if (app.isSystem) {
						info += app.prettyPrint() + "\n";
					}
				}
			} catch (Exception ex) {
				info += "Error getting Application Information:\n";
				info += ex.getMessage();
			}
		}

		// setProgressBarIndeterminateVisibility(false);

		return info;
	}

	/**
	 * Class to manage a single PackageInfo.
	 * 
	 */
	class PInfo {
		private String appname = "";
		private String pname = "";
		private String versionName = "";
		boolean isSystem;

		// private int versionCode = 0;
		// private Drawable icon;

		public PInfo(PackageInfo pkg) {
			appname = pkg.applicationInfo.loadLabel(getPackageManager())
					.toString();
			pname = pkg.packageName;
			versionName = pkg.versionName;
			// versionCode = pkg.versionCode;
			isSystem = isSystemPackage(pkg);
			// icon = pkg.applicationInfo.loadIcon(getPackageManager());
		}

		private String prettyPrint() {
			String info = "";
			info += appname + "\n";
			info += pname + "\n";
			info += "Version: " + versionName + "\n";
			// info += "Version code: " + versionCode + "\n";
			Log.d(TAG, info);
			return info;
		}

		public boolean isSystem() {
			return isSystem;
		}

	}

}
