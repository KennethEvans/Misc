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

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.StatFs;
import android.provider.DocumentsContract;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity to display information about installed apps.
 */
public class ApplicationInfoActivity extends AppCompatActivity implements IConstants {
    /**
     * Template for the name of the file written to the root of the SD card
     */
    private static final String SAVE_FILE_NAME = "ApplicationInfo.%s.txt";

    private TextView mTextView;
    public boolean doBuildInfo = false;
    public boolean doMemoryInfo = false;
    public boolean doNonSystemApps = true;
    public boolean doSystemApps = false;
    public boolean doPreferredApplications = false;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.apps);

        // requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        // Get the TextView
        mTextView = findViewById(R.id.textview);

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
                // DEBUG install issue
                // test();
                showHelp();
                return true;
        }
        return false;
    }

    // // DEBUG install issue
    // /**
    // * A test routine that tries to install Zen Bound 2, which is failing.
    // */
    // protected void test() {
    // String fileName =
    // "humblebundle/downloads/zenbound2-png-humble
    // -1339542477_22344689c133319c.apk";
    // File sdCardRoot = Environment.getExternalStorageDirectory();
    // File file = new File(sdCardRoot, fileName);
    //
    // Intent intent = new Intent(Intent.ACTION_VIEW);
    // intent.setDataAndType(Uri.fromFile(file),
    // "application/vnd.android.package-archive");
    // Log.d(TAG, this.getClass().getSimpleName()
    // + ": Starting install intent for " + fileName);
    // startActivityForResult(intent, 1000);
    // }
    //
    // // DEBUG install issue
    // @Override
    // protected void onActivityResult(int requestCode, int resultCode,
    // Intent intent) {
    // super.onActivityResult(requestCode, resultCode, intent);
    // Log.d(TAG, this.getClass().getSimpleName()
    // + ".onActivityResult: requestCode=" + requestCode
    // + " resultCode=" + resultCode);
    // }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        editor.putBoolean("doBuildInfo", doBuildInfo);
        editor.putBoolean("doMemoryInfo", doMemoryInfo);
        editor.putBoolean("doPreferredApplications", doPreferredApplications);
        editor.putBoolean("doNonSystemApps", doNonSystemApps);
        editor.putBoolean("doSystemApps", doSystemApps);
        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        doBuildInfo = prefs.getBoolean("doBuildInfo", doBuildInfo);
        doMemoryInfo = prefs.getBoolean("doMemoryInfo", doMemoryInfo);
        doPreferredApplications = prefs.getBoolean("doPreferredApplications",
                doPreferredApplications);
        doNonSystemApps = prefs.getBoolean("doNonSystemApps", doNonSystemApps);
        doSystemApps = prefs.getBoolean("doSystemApps", doSystemApps);

        refresh();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        // DEBUG
        Log.d(TAG, this.getClass().getSimpleName()
                + ".onActivityResult: requestCode=" + requestCode
                + " resultCode=" + resultCode);
        if (requestCode == CREATE_DOCUMENT && resultCode == Activity.RESULT_OK) {
            Uri uri;
            if (intent != null) {
                uri = intent.getData();
                List<String> segments = uri.getPathSegments();
                Uri.Builder builder = new Uri.Builder();
                for (int i = 0; i < segments.size() - 1; i++) {
                    builder.appendPath(segments.get(i));
                }
                Uri parent = builder.build();
                Log.d(TAG, "uri=" + uri + " parent=" + parent);
                doSave(uri);
            }
        }
    }

    /**
     * Asks for the name of the save file
     */
    private void save() {
        try {
            Date now = new Date();
            String format = "yyyy-MM-dd-HHmmss";
            SimpleDateFormat formatter = new SimpleDateFormat(format,
                    Locale.US);
            String fileName = String.format(SAVE_FILE_NAME,
                    formatter.format(now));

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TITLE, fileName);
//            if (Build.VERSION.SDK_INT >= 26) {
//                // This doesn't work yet.
//                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
//            }
            startActivityForResult(intent, CREATE_DOCUMENT);
        } catch (Exception ex) {
            Utils.excMsg(this, "Error requesting saving to SD card", ex);
        }
    }

    /**
     * Does the actual writing for the save.
     *
     * @param uri The Uri to use for writing.
     */
    private void doSave(Uri uri) {
        FileOutputStream writer = null;
        try {
            Charset charset = StandardCharsets.UTF_8;
            ParcelFileDescriptor pfd = getContentResolver().
                    openFileDescriptor(uri, "w");
            writer =
                    new FileOutputStream(pfd.getFileDescriptor());
            CharSequence charSeq = mTextView.getText();
            byte[] bytes = charSeq.toString().getBytes(charset);
            writer.write(bytes);
            if (charSeq.length() == 0) {
                Utils.warnMsg(this, "The file written is empty");
            }
            Utils.infoMsg(this, "Wrote " + uri.getPath());
        } catch (Exception ex) {
            Utils.excMsg(this, "Error saving to SD card", ex);
        } finally {
            try {
                if (writer != null) writer.close();
            } catch (Exception ex) {
                // Do nothing
            }
        }
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
     * Copies the contents of the application information view to the
     * clipboard.
     */
    private void copyToClipboard() {
        try {
            android.content.ClipboardManager cm = (android.content
                    .ClipboardManager) getSystemService
                    (CLIPBOARD_SERVICE);
            TextView tv = findViewById(R.id.textview);
            android.content.ClipData clip = android.content.ClipData
                    .newPlainText("AppInfo", tv.getText());
            cm.setPrimaryClip(clip);
        } catch (Exception ex) {
            Utils.excMsg(this, "Error setting Clipboard", ex);
        }
    }

    /**
     * Bring up a dialog to change the options.
     */
    private void setOptions() {
        final CharSequence[] items = {"Build Information",
                "Memory Information", "Preferred Applications",
                "Downloaded Applications", "System Applications"};
        boolean[] states = {doBuildInfo, doMemoryInfo,
                doPreferredApplications, doNonSystemApps, doSystemApps};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings");
        builder.setMultiChoiceItems(items, states,
                new DialogInterface.OnMultiChoiceClickListener() {
                    public void onClick(DialogInterface dialogInterface,
                                        int item, boolean state) {
                    }
                });
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                SparseBooleanArray checked = ((AlertDialog) dialog)
                        .getListView().getCheckedItemPositions();
                doBuildInfo = checked.get(0);
                doMemoryInfo = checked.get(1);
                doPreferredApplications = checked.get(2);
                doNonSystemApps = checked.get(3);
                doSystemApps = checked.get(4);
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
            intent.putExtra(INFO_URL, "file:///android_asset/appinfo.html");
            startActivity(intent);
        } catch (Exception ex) {
            Utils.excMsg(this, "Error showing Help", ex);
        }
    }

    /**
     * Gets the Build information.
     *
     * @return The information.
     */
    private String getBuildInfo() {
        return ("VERSION.RELEASE=" + Build.VERSION.RELEASE + "\n") +
                "VERSION.INCREMENTAL=" + Build.VERSION.INCREMENTAL + "\n" +
                "VERSION.SDK=" + Build.VERSION.SDK_INT + "\n" +
                "BOARD=" + Build.BOARD + "\n" +
                "BRAND=" + Build.BRAND + "\n" +
                "DEVICE=" + Build.DEVICE + "\n" +
                "FINGERPRINT=" + Build.FINGERPRINT + "\n" +
                "HOST=" + Build.HOST + "\n" +
                "ID=" + Build.ID + "\n";
    }

    /**
     * Gets the Memory information.
     *
     * @return The information.
     */
    public String getMemoryInfo() {
        StringBuilder buf = new StringBuilder();

        // Internal Memory
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize;
        double total, available, free;
        blockSize = stat.getBlockSizeLong();
        total = (double) stat.getBlockCountLong() * blockSize;
        available = (double) stat.getAvailableBlocksLong() * blockSize;
        free = (double) stat.getFreeBlocksLong() * blockSize;
        double used = total - available;
        String format = ": %.0f KB = %.2f MB = %.2f GB\n";
        buf.append("Internal Memory\n");
        buf.append(String.format(Locale.US, "  Total" + format, total * KB,
                total * MB,
                total * GB));
        buf.append(String.format(Locale.US, "  Used" + format, used * KB,
                used * MB, used
                        * GB));
        buf.append(String.format(Locale.US, "  Available" + format, available
                        * KB,
                available * MB, available * GB));
        buf.append(String.format(Locale.US, "  Free" + format, free * KB,
                free * MB, free
                        * GB));
        buf.append(String.format(Locale.US, "  Block Size: %d Bytes\n",
                blockSize));

        // External Memory
        buf.append("\nExternal Memory\n");
        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            buf.append("  No External Memory\n");
        } else {
            path = Environment.getExternalStorageDirectory();
            stat = new StatFs(path.getPath());
            blockSize = stat.getBlockSizeLong();
            total = (double) stat.getBlockCountLong() * blockSize;
            available = (double) stat.getAvailableBlocksLong() * blockSize;
            free = (double) stat.getFreeBlocksLong() * blockSize;
            used = total - available;
            buf.append(String.format(Locale.US, "  Total" + format, total * KB,
                    total * MB, total * GB));
            buf.append(String.format(Locale.US, "  Used" + format, used * KB,
                    used * MB,
                    used * GB));
            buf.append(String.format(Locale.US, "  Available" + format,
                    available * KB,
                    available * MB, available * GB));
            buf.append(String.format(Locale.US, "  Free" + format, free * KB,
                    free * MB,
                    free * GB));
            buf.append(String.format(Locale.US, "  Block Size: %d Bytes\n",
                    blockSize));
        }

        // RAM
        MemoryInfo mi = new MemoryInfo();
        ActivityManager activityManager = (ActivityManager) getSystemService
                (ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        available = (double) mi.availMem;
        double threshold = (double) mi.threshold;
        boolean low = mi.lowMemory;
        buf.append("\nRAM\n");
        Long ram = getTotalRAM();
        if (ram == null) {
            buf.append("  Total: Not found\n");
        } else {
            total = ram / KB;
            used = total - available;
            buf.append(String.format(Locale.US, "  Total" + format, total * KB,
                    total * MB, total * GB));
            buf.append(String.format(Locale.US, "  Used" + format, used * KB,
                    used * MB, used * GB));
        }
        buf.append(String.format(Locale.US, "  Available" + format,
                available * KB, available * MB, available * GB));
        buf.append(String.format(Locale.US, "  Threshold" + format,
                threshold * KB, threshold * MB, threshold * GB));
        if (low) {
            buf.append("  Memory is low\n");
        }

        return buf.toString();
    }

    /**
     * Gets the total ram by parsing /proc/meminfo.
     *
     * @return The memory in KB or null on failure.
     */
    public static Long getTotalRAM() {
        String path = "/proc/meminfo";
        String[] tokens;
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            // Assume it is in the first line and assume it is in kb
            String line = br.readLine();
            tokens = line.split("\\s+");
            return Long.parseLong(tokens[1]);
        } catch (Exception ex) {
            return null;
        }
        // Do nothing
    }

    // /**
    // * Get info on the preferred (launch by default) applications.
    // *
    // * @return
    // */
    // public String getPreferredAppInfo() {
    // List<PackageInfo> packages = getPackageManager()
    // .getInstalledPackages(0);
    // List<IntentFilter> filters = new ArrayList<IntentFilter>();
    // List<ComponentName> activities = new ArrayList<ComponentName>();
    // String info = "";
    // int nPref = 0, nFilters = 0, nActivities = 0;
    // PackageInfo pkg = null;
    // for (int i = 0; i < packages.size(); i++) {
    // pkg = packages.get(i);
    // nPref = getPackageManager().getPreferredActivities(filters,
    // activities, pkg.packageName);
    // nFilters = filters.size();
    // nActivities = activities.size();
    // if (nPref > 0 || nFilters > 0 || nActivities > 0) {
    // // This is a launch by default package
    // info += "\n" + pkg.packageName + "\n";
    // for (IntentFilter filter : filters) {
    // info += "IntentFilter:\n";
    // for (int j = 0; j < filter.countActions(); j++) {
    // info += "    action: " + filter.getAction(j) + "\n";
    // }
    // for (int j = 0; j < filter.countCategories(); j++) {
    // info += "    category: " + filter.getCategory(j) + "\n";
    // }
    // for (int j = 0; j < filter.countDataTypes(); j++) {
    // info += "    type: " + filter.getDataType(j) + "\n";
    // }
    // for (int j = 0; j < filter.countDataAuthorities(); j++) {
    // info += "    data authority: "
    // + filter.getDataAuthority(j) + "\n";
    // }
    // for (int j = 0; j < filter.countDataPaths(); j++) {
    // info += "    data path: " + filter.getDataPath(j)
    // + "\n";
    // }
    // for (int j = 0; j < filter.countDataSchemes(); j++) {
    // info += "    data path: " + filter.getDataScheme(j)
    // + "\n";
    // }
    // // for (ComponentName activity : activities) {
    // // info += "activity="
    // // + activity.flattenToString() + "\n";
    // // }
    // }
    // }
    // }
    // return info;
    // }

    /**
     * Return whether the given PackageInfo represents a system package or not.
     * User-installed packages (Market or otherwise) should not be denoted as
     * system packages.
     *
     * @param pkgInfo The PackageInfo.
     * @return The information.
     */
    private boolean isSystemPackage(PackageInfo pkgInfo) {
        return (pkgInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    /**
     * Gets a List of PInfo's for the installed packages.
     *
     * @param getSysPackages Whether to get system packages or not.
     * @return The list of PInfo.
     */
    private ArrayList<PInfo> getInstalledApps(boolean getSysPackages) {
        ArrayList<PInfo> res = new ArrayList<>();
        List<PackageInfo> packages = getPackageManager()
                .getInstalledPackages(0);
        PInfo newInfo;
        PackageInfo pkg;
        for (int i = 0; i < packages.size(); i++) {
            pkg = packages.get(i);
            if ((!getSysPackages) && (pkg.versionName == null)) {
                continue;
            }
            newInfo = new PInfo(pkg);
            res.add(newInfo);
        }
        Collections.sort(res);
        return res;
    }

    /**
     * Gets a List of preferred packages.
     *
     * @return The list of preferred packages.
     */
    private ArrayList<PInfo> getPreferredApps() {
        ArrayList<PInfo> res = new ArrayList<>();
        // This returns nothing
        // List<PackageInfo> packages = getPackageManager()
        // .getPreferredPackages(0);
        List<PackageInfo> packages = getPackageManager()
                .getInstalledPackages(0);
        Log.d(TAG,
                this.getClass().getSimpleName()
                        + ".getPreferredApps: installed packages size="
                        + packages.size());

        List<IntentFilter> filters = new ArrayList<>();
        List<ComponentName> activities = new ArrayList<>();
        PInfo newInfo;
        PackageInfo pkg;
        int nPref, nFilters, nActivities;
        for (int i = 0; i < packages.size(); i++) {
            pkg = packages.get(i);
            if (pkg.versionName == null) {
                continue;
            }
            nPref = getPackageManager().getPreferredActivities(filters,
                    activities, pkg.packageName);
            nFilters = filters.size();
            nActivities = activities.size();
//            Log.d(TAG, pkg.packageName + " nPref=" + nPref + " nFilters="
//                    + nFilters + " nActivities=" + nActivities);
            if (nPref > 0 || nFilters > 0 || nActivities > 0) {
                newInfo = new PInfo(pkg);
                newInfo.setInfo("");
                // newInfo.setInfo(pkg.packageName + " nPref=" + nPref
                // + " nFilters=" + nFilters + " nActivities="
                // + nActivities + "\n");
                for (IntentFilter filter : filters) {
                    // newInfo.appendInfo("IntentFilter: " + " actions="
                    // + filter.countActions() + " categories="
                    // + filter.countCategories() + " types="
                    // + filter.countDataTypes() + " authorities="
                    // + filter.countDataAuthorities() + " paths="
                    // + filter.countDataPaths() + " schemes="
                    // + filter.countDataSchemes() + "\n");
                    newInfo.appendInfo("IntentFilter:\n");
                    for (int j = 0; j < filter.countActions(); j++) {
                        newInfo.appendInfo("    action: " + filter.getAction(j)
                                + "\n");
                    }
                    for (int j = 0; j < filter.countCategories(); j++) {
                        newInfo.appendInfo("    category: "
                                + filter.getCategory(j) + "\n");
                    }
                    for (int j = 0; j < filter.countDataTypes(); j++) {
                        newInfo.appendInfo("    type: " + filter.getDataType(j)
                                + "\n");
                    }
                    for (int j = 0; j < filter.countDataAuthorities(); j++) {
                        newInfo.appendInfo("    data authority: "
                                + filter.getDataAuthority(j) + "\n");
                    }
                    for (int j = 0; j < filter.countDataPaths(); j++) {
                        newInfo.appendInfo("    data path: "
                                + filter.getDataPath(j) + "\n");
                    }
                    for (int j = 0; j < filter.countDataSchemes(); j++) {
                        newInfo.appendInfo("    data path: "
                                + filter.getDataScheme(j) + "\n");
                    }
                    // for (ComponentName activity : activities) {
                    // newInfo.appendInfo("activity="
                    // + activity.flattenToString() + "\n");
                    // }
                }
                res.add(newInfo);
            }
        }
        Collections.sort(res);
        return res;
    }

    // private void testReader() {
    // // Open a file with Adobe Reader
    // File file = new File("/storage/extSdCard/PDF/Images Book.pdf");
    // Intent intent = new Intent(Intent.ACTION_VIEW);
    // intent.setDataAndType(Uri.fromFile(file), "application/pdf");
    // intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    // // startActivity(intent);
    //
    // Log.d(TAG, this.getClass().getSimpleName() + ".testReader: "
    // + "intent: " + intent);
    //
    // // Log.d(TAG, "intent URI: " + intent.toURI());
    // final List<ResolveInfo> list = getPackageManager()
    // .queryIntentActivities(intent, 0);
    // Log.d(TAG, "Packages:");
    // for (ResolveInfo rInfo : list) {
    // String pkgName = rInfo.activityInfo.applicationInfo.packageName;
    // Log.d(TAG, "  " + pkgName);
    // }
    //
    // ResolveInfo rDefault = getPackageManager().resolveActivity(intent,
    // PackageManager.MATCH_DEFAULT_ONLY);
    // if (rDefault == null) {
    // Log.d(TAG, " Default=null");
    // } else {
    // Log.d(TAG, " Default="
    // + rDefault.activityInfo.applicationInfo.packageName);
    // }
    // }

    /**
     * Gets information about the applications.
     *
     * @return The information.
     */
    private String getAppsInfo() {
        String info = "Application Information\n";

        // DEBUG
        // info +=
        // "\n\n------------------------------------------------------\n";
        // info += getPreferredAppInfo();
        // info += "------------------------------------------------------\n\n";

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

        // Memory information
        if (doMemoryInfo) {
            info += "Memory Information\n\n";
            info += getMemoryInfo() + "\n";
        }

        if (doPreferredApplications) {
            info += "Preferred Applications (Launch by Default)\n\n";
            try {
                // false = no system packages
                ArrayList<PInfo> apps = getPreferredApps();
                final int max = apps.size();
                PInfo app;
                for (int i = 0; i < max; i++) {
                    app = apps.get(i);
                    // No "\n" here
                    info += app.prettyPrint();
                }
            } catch (Exception ex) {
                info += "Error gettingPreferred Applications:\n\n";
                info += ex.getMessage() + "\n\n";
                Log.d(TAG, "Error gettingPreferred Applications:", ex);
            }
        }

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
                info += ex.getMessage() + "\n\n";
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
                info += "Error getting System Application Information:\n";
                info += ex.getMessage() + "\n\n";
            }
        }

        // setProgressBarIndeterminateVisibility(false);

        return info;
    }

    /**
     * Class to manage a single PackageInfo.
     */
    class PInfo implements Comparable<PInfo> {
        private final String appname;
        private final String pname;
        private final String versionName;
        private String info = null;
        boolean isSystem;

        // private int versionCode = 0;
        // private Drawable icon;

        PInfo(PackageInfo pkg) {
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
            if (this.info != null) {
                info += this.info + "\n";
            }
            // info += "Version code: " + versionCode + "\n";
            // DEBUG
            // Log.d(TAG, info);
            return info;
        }

        public boolean isSystem() {
            return isSystem;
        }

        @Override
        public int compareTo(PInfo another) {
            // TODO Auto-generated method stub
            return this.appname.compareTo(another.appname);
        }

        public String getInfo() {
            return info;
        }

        public void setInfo(String info) {
            this.info = info;
        }

        void appendInfo(String info) {
            this.info += info;
        }

    }

}
