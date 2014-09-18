package com.photosynq.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PrefUtils {
    public static final String PREFS_LOGIN_USERNAME_KEY = "USERNAME" ;
    public static final String PREFS_LOGIN_PASSWORD_KEY = "PASSWORD" ;
    public static final String PREFS_AUTH_TOKEN_KEY = "AUTHTOKEN" ;
    public static final String PREFS_DEFAULT_VAL = "DEFAULT" ;
    public static final String PREFS_CURRENT_LOCATION = "CURRENT_LOCATION" ;
    public static final String PREFS_CONNECTION_ID = "CONNECTION_ID";
    public static final String PREFS_FIRST_RUN = "FIRST_RUN";
    public static final String PREFS_SAVE_SYNC_INTERVAL = "SAVE_SYNC_INTERVAL";
    public static final String PREFS_QUESTION_INDEX = "FREQ_OF_QUESTION";
    public static final String PREFS_FIXED_VALUE = "FIXED_VALUE";
    public static final String PREFS_BIO_KEY = "USER_BIO" ;
    public static final String PREFS_NAME_KEY = "NAME";
    public static final String PREFS_INSTITUTE_KEY = "INSTITUTE";
    public static final String PREFS_THUMB_URL_KEY = "THUMB_URL";
    // public static String PREFS_QUESTION_INDEX_VALUES = "QUESTION_INDEX_VALUES";

    /**
     * Called to save supplied value in shared preferences against given key.
     */
    public static void saveToPrefs(Context context, String key, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key,value);
        editor.commit();
    }

    /**
     * Called to retrieve required value from shared preferences, identified by given key.
     * Default value will be returned of no value found or error occurred.
     */
    public static String getFromPrefs(Context context, String key, String defaultValue) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            return sharedPrefs.getString(key, defaultValue);
        } catch (Exception e) {
             e.printStackTrace();
             return defaultValue;
        }
    }
}