package com.houndify.sample;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        openVoiceScreen();
    }


    private void openVoiceScreen() {
        Intent intent = new Intent(this, VoiceSearchActivity.class);
        startActivity(intent);
        finish();
    }

}
