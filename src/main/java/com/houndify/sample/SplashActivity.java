package com.houndify.sample;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
    }

    private void openVoiceScreen() {
        finish();
        startActivity(
                new Intent(Intent.ACTION_ASSIST)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .setPackage(BuildConfig.APPLICATION_ID)
        );

    }

    @Override
    protected void onResume() {
        super.onResume();
        openVoiceScreen();

    }
}
