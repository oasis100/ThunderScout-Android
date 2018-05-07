/*
 * MIT License
 *
 * Copyright (c) 2016 - 2018 Luke Myers (FRC Team 980 ThunderBots)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.team980.thunderscout;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.service.quicksettings.TileService;
import android.support.multidex.MultiDexApplication;
import android.support.v7.app.AppCompatDelegate;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.team980.thunderscout.bluetooth.BluetoothServerService;
import com.team980.thunderscout.bluetooth.util.BluetoothQuickTileService;

import java.io.IOException;

import io.fabric.sdk.android.Fabric;

public class ThunderScout extends MultiDexApplication implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static boolean isInteger(String str) { //TODO use this for all the int checks
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onCreate() { //This isn't why loading is slow
        super.onCreate();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        if (!sharedPref.contains(getResources().getString(R.string.pref_device_name))) {
            sharedPref.edit().putString(getResources().getString(R.string.pref_device_name), Build.MANUFACTURER + " " + Build.MODEL).apply();
        }

        boolean runServer = sharedPref.getBoolean(getResources().getString(R.string.pref_enable_bluetooth_server), false);

        if (runServer) { //I must be launching multiple instances?
            startService(new Intent(this, BluetoothServerService.class));
        }

        sharedPref.registerOnSharedPreferenceChangeListener(this);

        if (BuildConfig.DEBUG) {
            //Disable Firebase Analytics on debug builds
            FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(false);

            //Init, but disable, Crashlytics on debug builds
            Crashlytics crashlyticsKit = new Crashlytics.Builder()
                    .core(new CrashlyticsCore.Builder()
                            .disabled(true).build())
                    .build();
            Fabric.with(this, crashlyticsKit);
        } else {
            //Firebase Analytics is based on user preference
            FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(sharedPref.getBoolean(getResources().getString(R.string.pref_enable_analytics), true));

            //Manually init Crashlytics based on user preference
            Crashlytics crashlyticsKit = new Crashlytics.Builder()
                    .core(new CrashlyticsCore.Builder()
                            .disabled(sharedPref.getBoolean(getResources().getString(R.string.pref_enable_crashlytics), false)).build())
                    .build();
            Fabric.with(this, crashlyticsKit);
        }

        if (FirebaseAuth.getInstance().getCurrentUser() != null
                && sharedPref.getBoolean(getResources().getString(R.string.pref_enable_push_notifications), true)) {
            FirebaseMessaging.getInstance().setAutoInitEnabled(true);
            FirebaseInstanceId.getInstance().getToken();
        } else {
            FirebaseMessaging.getInstance().setAutoInitEnabled(false);
            AsyncTask.execute(() -> {
                try {
                    FirebaseInstanceId.getInstance().deleteInstanceId();
                } catch (IOException ignored) {
                }
            });
        }

        if (sharedPref.getBoolean(getResources().getString(R.string.pref_app_theme), true)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getResources().getString(R.string.pref_enable_bluetooth_server))) {
            Boolean isServer = sharedPreferences.getBoolean(getResources().getString(R.string.pref_enable_bluetooth_server), false);

            if (isServer) {
                startService(new Intent(this, BluetoothServerService.class));
            } else {
                stopService(new Intent(this, BluetoothServerService.class));
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                TileService.requestListeningState(this, new ComponentName(this, BluetoothQuickTileService.class));
            }
        } else if (key.equals(getResources().getString(R.string.pref_enable_analytics)) && !BuildConfig.DEBUG) {
            FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(sharedPreferences.getBoolean(getResources().getString(R.string.pref_enable_analytics), true));
        }
    }
}
