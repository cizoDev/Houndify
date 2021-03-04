package com.houndify.sample;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import com.hound.android.fd.DefaultRequestInfoFactory;
import com.hound.android.fd.Houndify;


public class PrefManager extends MultiDexApplication {
    private static PrefManager appClass;

    private static SharedPreferences mPref;
    private static SharedPreferences.Editor editor;

    public static String DEVICE_ID;

    public static String CLIENT_ID = "LUI0uySvB4LVsiMc1JV-HQ==";
    public static String CLIENT_KEY = "B1LLmebF1xgz8GIlkOdet4pexie63zoenSXPBfObE2a-mp5hJ3bN0wVOMvlKlBFoAnIrsf5fZQmy_JmIUMCEvw==";


    public static PrefManager getInstance() {
        return appClass;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        appClass = this;

        mPref = this.getSharedPreferences(getResources().getString(R.string.app_name), Context.MODE_PRIVATE);
        editor = mPref.edit();

        DEVICE_ID = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);

        final Houndify houndify = Houndify.get(this);

        houndify.setClientId(CLIENT_ID);
        houndify.setClientKey(CLIENT_KEY);
        houndify.setRequestInfoFactory(new DefaultRequestInfoFactory(this));

    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }


    public static void clearPrefrances() {
        editor.clear();
        editor.apply();
    }

    public boolean isContactSync() {
        return mPref.getBoolean("isSync", false);
    }

    public  void setContactSync(boolean contactSync) {
        editor.putBoolean("isSync", contactSync);
        editor.commit();
    }


}
