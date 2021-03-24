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
        checkOverLayPermission();

    }


    public static int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 10001;
    public static int ACTION_VOICE_INPUT_SETTINGS_REQUEST_CODE = 10002;

    private void checkOverLayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                !Settings.canDrawOverlays(this)) {
            RequestPermission();
        } else {
            checkForDefaultApp();
        }
    }

    private void RequestPermission() {
        // Check if Android M or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Show alert dialog to the user saying a separate permission is needed
            // Launch the settings activity if the user prefers
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + BuildConfig.APPLICATION_ID));
            startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
        }
    }

    private void checkForDefaultApp() {
        String appName = getResources().getString(R.string.app_name);

        String title = "Set " + appName + " as the default assist app.";
        String message =
                "1.Tap on the 'Settings' button below\n" +
                        "2.Tap on the 'Assist app' section\n" +
                        "3.Tap on '" + appName + "' to set as default";

        ComponentName activity = getCurrentAssistWithReflection(getApplicationContext());
        if (activity == null || !activity.getPackageName().equalsIgnoreCase(BuildConfig.APPLICATION_ID)) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setMessage(message)
                    .setTitle(title)
                    .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS);
                            startActivityForResult(intent, ACTION_VOICE_INPUT_SETTINGS_REQUEST_CODE);
                        }
                    }).show();
        } else {
            openVoiceScreen();
        }
    }

    public ComponentName getCurrentAssistWithReflection(Context context) {
        try {
            Method myUserIdMethod = UserHandle.class.getDeclaredMethod("myUserId");
            myUserIdMethod.setAccessible(true);
            Integer userId = (Integer) myUserIdMethod.invoke(null);

            if (userId != null) {
                Constructor constructor = Class.forName("com.android.internal.app.AssistUtils").getConstructor(Context.class);
                Object assistUtils = constructor.newInstance(context);

                Method getAssistComponentForUserMethod = assistUtils.getClass().getDeclaredMethod("getAssistComponentForUser", int.class);
                getAssistComponentForUserMethod.setAccessible(true);
                return (ComponentName) getAssistComponentForUserMethod.invoke(assistUtils, userId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
