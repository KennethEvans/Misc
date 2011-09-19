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

import android.os.Handler;

/**
 * A class to provide a timer that runs a given Runnable when the timer expires
 * after the current interval passes and then sets it to run again after the
 * current interval.
 */
public class IntervalTimer {
	/** The interval after which to call the given Runnable. */
	private int interval;
	/** The handler that handles the Runnables */
	private Handler handler;
	/** A runnable that runs the given runnable and then starts the timer again. */
	private Runnable delegate;
	/** Whether the timer is started or not. */
	private boolean started = false;

	/**
	 * The Runnable that just has the code you wish to run and does nothing
	 * about resetting the timer to run again.
	 */
	private Runnable userRunnable;

	/**
	 * Returns if the timer is started or not.
	 * 
	 * @return
	 */
	public boolean getIsStarted() {
		return started;
	}

	/**
	 * Constructor that sets the interval.
	 * 
	 * @param interval
	 *            The interval for the timer.
	 */
	public IntervalTimer(int interval) {
		this.interval = interval;
		handler = new Handler();
		started = false;
	}

	/**
	 * Constructor that sets the interval and Runnable to run when it expires.
	 * The Runnable just has the code you wish to run and should do nothing
	 * about resetting the timer to run again.
	 * 
	 * @param interval
	 * @param runnable
	 */
	public IntervalTimer(int interval, Runnable runnable) {
		this.interval = interval;
		setUserRunnable(runnable);
		handler = new Handler();
		started = false;
	}

	/**
	 * Start the timer if not already started with a new interval and Runnable.
	 * The Runnable just has the code you wish to run and should do nothing
	 * about resetting the timer to run again.
	 * 
	 * @param interval
	 * @param runnable
	 */
	public void start(int interval, Runnable runnable) {
		if (started) {
			return;
		}
		this.interval = interval;
		setUserRunnable(runnable);
		handler.postDelayed(delegate, this.interval);
		started = true;
	}

	/**
	 * Start the timer if not already started.
	 */
	public void start() {
		if (started || delegate == null) {
			return;
		}
		handler.postDelayed(delegate, interval);
		started = true;
	}

	/**
	 * Stop the timer if not already stopped.
	 */
	public void stop() {
		if (!started || delegate == null) {
			return;
		}
		handler.removeCallbacks(delegate);
		started = false;
	}

	/**
	 * Set the Runnable that will be run when the timer expires. The Runnable
	 * just has the code you wish to run and should do nothing about resetting
	 * the timer to run again.
	 * 
	 * @param runnable
	 */
	public void setUserRunnable(Runnable runnable) {
		if (runnable == null) {
			// Reset the delegate so we don't run something that was supposed to
			// have been changed
			delegate = null;
			return;
		}
		userRunnable = runnable;
		// Use the delegate to run the given Runnable and then do
		// handler.postDelayed().
		delegate = new Runnable() {
			public void run() {
				userRunnable.run();
				handler.postDelayed(delegate, interval);
			}
		};
	}

	/**
	 * Get the current interval.
	 * 
	 * @return
	 */
	public int getInterval() {
		return interval;
	}

	/**
	 * Set a new interval.
	 * 
	 * @param interval
	 */
	public void setInterval(int interval) {
		this.interval = interval;
	}

}