package com.fortytwo.ft_hangouts;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HangoutsApp extends Application {

    private static final String PREFS_NAME = "ft_hangouts_prefs";
    private static final String KEY_LAST_BACKGROUND = "last_background_timestamp";

    private int startedActivitiesCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

            @Override
            public void onActivityStarted(Activity activity) {
                startedActivitiesCount++;

                if (startedActivitiesCount == 1) {
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    long lastBackground = prefs.getLong(KEY_LAST_BACKGROUND, -1);

                    if (lastBackground != -1) {
                        long elapsedMillis = System.currentTimeMillis() - lastBackground;
                        long minutes = (elapsedMillis / 1000) / 60;
                        long seconds = (elapsedMillis / 1000) % 60;

                        String formattedTimestamp = new SimpleDateFormat(
                                "HH:mm:ss dd.MM.yyyy", Locale.getDefault())
                                .format(new Date(lastBackground));

                        String message = getString(
                                R.string.toast_background_info,
                                formattedTimestamp, minutes, seconds
                        );

                        Toast.makeText(HangoutsApp.this, message, Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onActivityStopped(Activity activity) {
                startedActivitiesCount--;

                if (startedActivitiesCount == 0) {
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    prefs.edit()
                            .putLong(KEY_LAST_BACKGROUND, System.currentTimeMillis())
                            .apply();
                }
            }

            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
            @Override public void onActivityResumed(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
    }
}