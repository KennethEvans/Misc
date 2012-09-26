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

// Based on code from http://stackoverflow.com/questions/8133417/android-get-free-size-of-internal-external-memory

package net.kenevans.android.misc;

import java.io.File;

import android.os.Environment;
import android.os.StatFs;

public class MemoryUtils {
	private static final double KB = 1. / 1024.;
	private static final double MB = 1. / (1024. * 1024.);
	private static final double GB = 1. / (1024. * 1024. * 1024.);

	public static boolean externalMemoryAvailable() {
		return Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED);
	}

	public static String getAvailableInternalMemorySize() {
		File path = Environment.getDataDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getAvailableBlocks();
		return formatSize(availableBlocks * blockSize);
	}

	public static String getTotalInternalMemorySize() {
		File path = Environment.getDataDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long totalBlocks = stat.getBlockCount();
		return formatSize(totalBlocks * blockSize);
	}

	public static String getAvailableExternalMemorySize() {
		if (externalMemoryAvailable()) {
			File path = Environment.getExternalStorageDirectory();
			StatFs stat = new StatFs(path.getPath());
			long blockSize = stat.getBlockSize();
			long availableBlocks = stat.getAvailableBlocks();
			return formatSize(availableBlocks * blockSize);
		} else {
			return "0";
		}
	}

	public static String getTotalExternalMemorySize() {
		if (externalMemoryAvailable()) {
			File path = Environment.getExternalStorageDirectory();
			StatFs stat = new StatFs(path.getPath());
			long blockSize = stat.getBlockSize();
			long totalBlocks = stat.getBlockCount();
			return formatSize(totalBlocks * blockSize);
		} else {
			return "0";
		}
	}

	public static String formatSize(long size) {
		String suffix = null;
		if (size >= 1024) {
			suffix = " KB";
			size /= 1024;
			if (size >= 1024) {
				suffix = " MB";
				size /= 1024;
			}
		}
		StringBuilder resultBuffer = new StringBuilder(Long.toString(size));
		int commaOffset = resultBuffer.length() - 3;
		while (commaOffset > 0) {
			resultBuffer.insert(commaOffset, ',');
			commaOffset -= 3;
		}
		if (suffix != null)
			resultBuffer.append(suffix);
		return resultBuffer.toString();
	}

	public static String getMemoryInfo() {
		StringBuffer buf = new StringBuffer();

		// Internal
		File path = Environment.getDataDirectory();
		StatFs stat = new StatFs(path.getPath());
		int blockSize = stat.getBlockSize();
		double totalBlocks = (double) stat.getBlockCount() * blockSize;
		double availableBlocks = (double) stat.getAvailableBlocks() * blockSize;
		double freeBlocks = (double) stat.getFreeBlocks() * blockSize;
		String format = ": %.0f KB = %.2f MB = %.2f GB\n";
		buf.append("Internal Memory\n");
		buf.append(String.format("  Total" + format, totalBlocks * KB,
				totalBlocks * MB, totalBlocks * GB));
		buf.append(String.format("  Free" + format, freeBlocks * KB, freeBlocks
				* MB, freeBlocks * GB));
		buf.append(String.format("  Available" + format, availableBlocks * KB,
				availableBlocks * MB, availableBlocks * GB));
		buf.append(String.format("  Block Size: %d Bytes\n", blockSize));

		// External Memory
		buf.append("External Memory\n");
		if (!externalMemoryAvailable()) {
			buf.append("  No Extenal Memory\n");
		} else {
			path = Environment.getExternalStorageDirectory();
			stat = new StatFs(path.getPath());
			blockSize = stat.getBlockSize();
			totalBlocks = (double) stat.getBlockCount() * blockSize;
			availableBlocks = (double) stat.getAvailableBlocks() * blockSize;
			freeBlocks = (double) stat.getFreeBlocks() * blockSize;
			buf.append(String.format("  Total" + format, totalBlocks * KB,
					totalBlocks * MB, totalBlocks * GB));
			buf.append(String.format("  Free" + format, freeBlocks * KB,
					freeBlocks * MB, freeBlocks * GB));
			buf.append(String.format("  Available" + format, availableBlocks
					* KB, availableBlocks * MB, availableBlocks * GB));
			buf.append(String.format("  Block Size: %d Bytes\n", blockSize));
		}

		return buf.toString();
	}

}
