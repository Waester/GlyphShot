package com.github.glyphshot;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.IOException;
import java.io.InputStream;

public class FloatingWindow extends Service {

    private WindowManager mManager;
    private WindowManager.LayoutParams mParams;
    private LayoutInflater layoutInflater;
    private View mView;
    private int nThumbnails = 0;
    private SharedPreferences prefs;
    private SharedPreferences.Editor prefsEdit;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefsEdit = prefs.edit();

        mManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        mView = layoutInflater.inflate(R.layout.floating, null);
        mParams = new WindowManager.LayoutParams(getWidth(), getHeight(), prefs.getInt("xpos", 0), prefs.getInt("ypos", 0), WindowManager.LayoutParams.TYPE_SYSTEM_ERROR, WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        mParams.gravity = Gravity.TOP | Gravity.LEFT;
        mManager.addView(mView, mParams);

        ImageButton imageButton = (ImageButton) mView.findViewById(R.id.button_take);
        imageButton.setOnTouchListener(new View.OnTouchListener() {

            boolean isClick;
            float SCROLL_THRESHOLD = 100;
            float dX, dY;
            long startTime;
            long endTime;
            long elapsedTime;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        isClick = true;
                        startTime = SystemClock.elapsedRealtime();
                        dX = mParams.x - motionEvent.getRawX();
                        dY = mParams.y - motionEvent.getRawY();
                        break;

                    case MotionEvent.ACTION_UP:
                        if (isClick) {
                            endTime = SystemClock.elapsedRealtime();
                            elapsedTime = endTime - startTime;
                            if (elapsedTime < 500) {
                                new Thread() {
                                    @Override
                                    public void run() {
                                        addThumb(takeScreenshot());
                                    }
                                }.start();
                            } else {
                                removeAllThumbs();
                            }
                        } else {
                            prefsEdit.putInt("xpos", mParams.x);
                            prefsEdit.putInt("ypos", mParams.y);
                            prefsEdit.apply();
                        }
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if ((Math.abs(mParams.x - (motionEvent.getRawX() + dX)) > SCROLL_THRESHOLD || Math.abs(mParams.y - (motionEvent.getRawY() + dY)) > SCROLL_THRESHOLD)) {
                            if (isClick) {
                                isClick = false;
                            }
                            mParams.x = (int) (motionEvent.getRawX() + dX);
                            mParams.y = (int) (motionEvent.getRawY() + dY);
                            mManager.updateViewLayout(mView, mParams);
                        }
                        break;
                }
                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
        removeAllThumbs();
        mManager.removeView(mView);
        prefsEdit.putBoolean("enabled", false);
        prefsEdit.apply();
    }

    private int getHeight() {
        return getResources().getDimensionPixelSize(R.dimen.button_take_height) + (getResources().getDimensionPixelSize(R.dimen.padding_margin) * 2);
    }

    private int getWidth() {
        return getResources().getDimensionPixelSize(R.dimen.button_take_width) + (nThumbnails * (getResources().getDimensionPixelSize(R.dimen.thumb_width) + getResources().getDimensionPixelSize(R.dimen.padding_margin))) + (getResources().getDimensionPixelSize(R.dimen.padding_margin) * 2);
    }

    private Bitmap takeScreenshot() {
        InputStream inputStream = null;
        try {
            inputStream = Runtime.getRuntime().exec("su -c screencap -p").getInputStream();
        } catch (IOException ex) {
            System.out.println("Error: IOException");
            return null;
        }
        if (inputStream != null) {
            Bitmap decodeStream = BitmapFactory.decodeStream(inputStream);
            if (decodeStream == null) {
                System.out.println("Error: Bitmap");
                return null;
            }
            return decodeStream;
        }
        return null;
    }

    protected void addThumb(Bitmap bitmap) {
        if (bitmap != null) {
            final LinearLayout linearLayout = (LinearLayout) mView.findViewById(R.id.thumbnails);
            final ImageView imageView = (ImageView) layoutInflater.inflate(R.layout.thumb, linearLayout, false);

            Bitmap editBitmap = Bitmap.createBitmap(bitmap, 0, bitmap.getHeight() / 4, bitmap.getWidth(), (bitmap.getHeight() / 3) * 2);
            imageView.setImageBitmap(editBitmap);

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    ++nThumbnails;
                    linearLayout.addView(imageView);
                    mParams.width = getWidth();
                    mManager.updateViewLayout(mView, mParams);
                }
            });
        }
    }

    private void removeAllThumbs() {
        nThumbnails = 0;
        ((ViewGroup) mView.findViewById(R.id.thumbnails)).removeAllViews();
        mParams.width = getWidth();
        mManager.updateViewLayout(mView, mParams);
    }
}
