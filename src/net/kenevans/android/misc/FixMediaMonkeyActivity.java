package net.kenevans.android.misc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class FixMediaMonkeyActivity extends Activity implements IConstants {
	private EditText mEditText;
	private TextView mResultView;
	private int nFiles = 0;
	private int nConverted = 0;
	private int nM4a = 0;
	private int nInvalid = 0;
	private int nErrors = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fixmediamonkey);

		// Initialize the count button with a listener for click events
		Button button = (Button) findViewById(R.id.countbutton);
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				renameFiles(true);
			}
		});

		// Initialize the rename button with a listener for click events
		button = (Button) findViewById(R.id.renamebutton);
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				renameFiles(false);
			}
		});

		mEditText = (EditText) findViewById(R.id.directoryview);
		File suggestedMusicDir = getSuggestedMusicDirectory();
		if (mEditText != null && suggestedMusicDir != null) {
			mEditText.setText(suggestedMusicDir.getPath());
		}
		mResultView = (TextView) findViewById(R.id.resultview);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.fixmediamonkeymenu, menu);
		return true;
	}

	/**
	 * Gets the suggested Music directory
	 * 
	 * @return
	 */
	public File getSuggestedMusicDirectory() {
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		String musicDirName = prefs
				.getString(PREF_MEDIA_MONKEY_DIRECTORY, null);
		File musicDir = null;
		if (musicDirName != null) {
			musicDir = new File(musicDirName);
		} else {
			File sdCardRoot = Environment.getExternalStorageDirectory();
			if (sdCardRoot != null) {
				musicDir = new File(sdCardRoot, SD_CARD_MUSIC_DIRECTORY);
				// Change the stored value (even if it is null)
				SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE)
						.edit();
				editor.putString(PREF_MEDIA_MONKEY_DIRECTORY,
						musicDir.getPath());
				editor.commit();
			}
		}
		if (musicDir == null) {
			Utils.errMsg(this, "Music directory is null");
			return null;
		}
		if (!musicDir.exists()) {
			Utils.errMsg(this, "Cannot find directory: " + musicDir);
			return null;
		}
		return musicDir;
	}

	/**
	 * Renames the .m4a file to .mp3.
	 */
	private void renameFiles(boolean countOnly) {
		if (mEditText == null || mResultView == null) {
			return;
		}
		nFiles = 0;
		nConverted = 0;
		nM4a = 0;
		nInvalid = 0;
		nErrors = 0;
		mResultView.setText("");
		String msg;
		Editable musicDirName = mEditText.getText();
		File musicDir = new File(musicDirName.toString());
		if (!musicDir.exists()) {
			msg = "Directory does not exist:\n" + musicDir.getPath();
			Utils.errMsg(this,
					"Directory does not exist:\n" + musicDir.getPath());
			mResultView.setText(msg);
			return;
		}

		// Save the last directory
		SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
		editor.putString(PREF_MEDIA_MONKEY_DIRECTORY, musicDir.getPath());

		// Process the files
		process(musicDir, countOnly);

		// Show results
		if (countOnly) {
			msg = "There are " + nInvalid + " invalid of " + nM4a
					+ " .m4a files of " + nFiles + " files total in "
					+ musicDir.getPath() + ".";
		} else {
			msg = "Converted " + nConverted + " of " + nInvalid
					+ " invalid of " + nM4a + " .m4a files of " + nFiles
					+ " files total in " + musicDir.getPath() + ".";
		}
		if (nErrors > 0) {
			msg += "\nEncountered " + nErrors + "Input/Output errors.";
		}
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
		mResultView.append(msg);
		editor.commit();
	}

	/**
	 * Iteratively process directories and files, renaming those that have
	 * extension .m4a.
	 * 
	 * @param file
	 * @return
	 */
	private boolean process(File file, boolean countOnly) {
		String newName = null;
		File newFile = null;
		int len = 0;
		boolean ok = false;
		if (file.isDirectory()) {
			// Is a directory, process the files in it
			File[] files = file.listFiles();
			for (File file1 : files) {
				if (!process(file1, countOnly)) {
					return false;
				}
				;
			}
			// No files
			return true;
		} else {
			// Is a file
			nFiles++;
			String ext = Utils.getExtension(file);
			if (ext.equals("m4a")) {
				nM4a++;
				if (!isMp3(file)) {
					// Is a presumably valid .m4a file
					return true;
				}
				nInvalid++;
				if (countOnly) {
					return true;
				}
				// Is a .m4a file with MP3 internals
				newName = file.getPath();
				len = newName.length();
				newFile = new File(newName.substring(0, len - 3) + "mp3");
				// Set ok = true here instead of rename to test
				ok = file.renameTo(newFile);
				if (!ok) {
					String msg = "Could not rename " + file.getPath();
					mResultView.append(msg + "\n");
				} else {
					nConverted++;
				}
				return ok;
			}
			return true;
		}
	}

	/**
	 * Determine if a file is MP3 by checking if it starts with ID3. Should work
	 * when comparing to M4A.
	 * 
	 * @param file
	 * @return
	 */
	private boolean isMp3(File file) {
		boolean res = false;
		BufferedInputStream is = null;
		byte[] buffer = new byte[] { 0, 0, 0 };
		try {
			is = new BufferedInputStream(new FileInputStream(file));
			// int nRead = is.read(buffer, 0, 3);
			is.read(buffer, 0, 3);
			if (buffer[0] == 'I' && buffer[1] == 'D' && buffer[2] == '3') {
				res = true;
			}
		} catch (Exception ex) {
			nErrors++;
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception ex) {
					// do nothing
				}
			}
		}
		return res;
	}

}
