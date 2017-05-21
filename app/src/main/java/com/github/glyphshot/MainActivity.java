package com.github.glyphshot;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class MainActivity extends Activity {

    private Intent mIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIntent = new Intent(this, FloatingWindow.class);
    }

    @Override
    public void onResume() {
        super.onResume();
        startService(mIntent);
        finish();
    }
}
