package com.houndify.sample;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import static com.houndify.sample.R.id.input_client_key;

public class ClientCredentialsUtil {

    // update these as needed; or configure through editor ui
//    private static final String DEFAULT_CLIENT_ID = "WhFjyiGsDyiQrRenyInQ7g==";
    private static final String DEFAULT_CLIENT_ID = "LUI0uySvB4LVsiMc1JV-HQ==";
    //    private static final String DEFAULT_CLIENT_KEY = "Jy1D8XENuBi05PuOELq8xjD2qXPya6-vMzObsPAt76cLMUXCqmwpawDkzv36l-mTL9xDv7t8m1JUZ7jl-Ly2yg==";
    private static final String DEFAULT_CLIENT_KEY = "B1LLmebF1xgz8GIlkOdet4pexie63zoenSXPBfObE2a-mp5hJ3bN0wVOMvlKlBFoAnIrsf5fZQmy_JmIUMCEvw==";

    public static void showEditPopup(final Context context) {
        View view =
                LayoutInflater.from(context).inflate(R.layout.edit_client_credentials_popup, null);

        final EditText id = view.findViewById(R.id.input_client_id);
//        id.setText(getDefaultSharedPreferences(context).getString(KEY_CLIENT_ID, null));
        id.setText(getClientId());

        final EditText key = view.findViewById(input_client_key);
//        key.setText(getDefaultSharedPreferences(context).getString(KEY_CLIENT_KEY, null));
        key.setText(getClientKey());

        final TextView link = view.findViewById(R.id.link);


        new AlertDialog.Builder(context).setTitle("Client API Credentials")
                .setView(view)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(

                        "Save", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String idString = id.getText().toString().trim();
                                if (idString.isEmpty()) {
                                    idString = null;
                                }
                                String keyString = key.getText().toString().trim();
                                if (keyString.isEmpty()) {
                                    keyString = null;
                                }
                                getDefaultSharedPreferences(context).edit()
                                        .putString(KEY_CLIENT_ID, idString)
                                        .putString(KEY_CLIENT_KEY, keyString)
                                        .apply();
                            }
                        })
                .create()
                .show();
    }

    private static final String KEY_CLIENT_ID = "client_id";
    private static final String KEY_CLIENT_KEY = "client_key";

    private static SharedPreferences getDefaultSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static String getClientId(Context context) {

        if (getDefaultSharedPreferences(context).getString(KEY_CLIENT_ID, DEFAULT_CLIENT_ID).isEmpty()) {
            return getClientId();
        } else {
            return getDefaultSharedPreferences(context).getString(KEY_CLIENT_ID, DEFAULT_CLIENT_ID);
        }

    }

    public static String getClientKey(Context context) {
        if (getDefaultSharedPreferences(context).getString(KEY_CLIENT_KEY, DEFAULT_CLIENT_KEY).isEmpty()) {
            return getClientKey();
        } else {
            return getDefaultSharedPreferences(context).getString(KEY_CLIENT_KEY, DEFAULT_CLIENT_KEY);
        }
    }

    public static String getClientId() {
        return DEFAULT_CLIENT_ID;
    }

    public static String getClientKey() {
        return DEFAULT_CLIENT_KEY;
    }
}
