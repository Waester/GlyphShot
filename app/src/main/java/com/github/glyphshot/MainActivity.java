package com.github.glyphshot;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class MainActivity extends Activity {

    private Intent mIntent;
    private SharedPreferences prefs;
    private SharedPreferences.Editor prefsEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIntent = new Intent(this, FloatingWindow.class);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefsEdit = prefs.edit();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!prefs.getBoolean("enabled", false)) {
            startService(mIntent);
            prefsEdit.putBoolean("enabled", true);
        } else {
            stopService(mIntent);
            prefsEdit.putBoolean("enabled", false);
        }
        prefsEdit.apply();
        finish();
    }
}
