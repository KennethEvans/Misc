package net.kenevans.android.misc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class SwipeDetector implements View.OnTouchListener, IConstants {
    private Activity activity;
    static final int MIN_DISTANCE = 100;
    private float downX, downY, upX, upY;

    public SwipeDetector(final Activity activity) {
        this.activity = activity;
    }

    public void onSwipeLeft() {
    }

    public void onSwipeRight() {
    }

    public void onSwipeDown() {
    }

    public void onSwipeUp() {
    }


    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouch(View v, MotionEvent event) {
//        Log.i(TAG, "Swipe onTouch: Action=" + event.getAction());
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                downX = event.getX();
                downY = event.getY();
                Log.i(TAG, "Swipe ACTION_DOWN: X=" + downX + " Y+" + downY);
                return true;
            }
            case MotionEvent.ACTION_UP: {
                upX = event.getX();
                upY = event.getY();
                float deltaX = downX - upX;
                float deltaY = downY - upY;
                Log.i(TAG, "Swipe ACTION_UP: X=" + upX + " Y+" + upY
                + " deltaX=" + deltaX + " deltaY=" + deltaY);

                // Swipe horizontal?
                if (Math.abs(deltaX) > MIN_DISTANCE) {
                    // left or right
                    if (deltaX < 0) {
                        this.onSwipeRight();
                        return true;
                    }
                    if (deltaX > 0) {
                        this.onSwipeLeft();
                        return true;
                    }
//                } else {
//                    Log.i(TAG, "Swipe was only " + Math.abs(deltaX) + "
//                    long, "
//                            + "need at least " + MIN_DISTANCE);
                }

                // Swipe vertical?
                if (Math.abs(deltaY) > MIN_DISTANCE) {
                    // top or down
                    if (deltaY < 0) {
                        this.onSwipeDown();
                        return true;
                    }
                    if (deltaY > 0) {
                        this.onSwipeUp();
                        return true;
                    }
//                } else {
//                    Log.i(TAG, "Swipe was only " + Math.abs(deltaX) + "
//                    long," +
//                            " " +
//                            "need at least " + MIN_DISTANCE);
                }

                //     return true;
            }
        }
        return false;
    }
}