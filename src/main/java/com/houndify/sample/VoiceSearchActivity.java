package com.houndify.sample;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hound.android.sdk.AsyncTextSearch;
import com.hound.android.sdk.Search;
import com.hound.android.sdk.TextSearchListener;
import com.hound.android.sdk.VoiceSearch;
import com.hound.android.sdk.VoiceSearchInfo;
import com.hound.android.sdk.VoiceSearchListener;
import com.hound.android.sdk.audio.SimpleAudioByteStreamSource;
import com.hound.android.sdk.util.HoundRequestInfoFactory;
import com.hound.android.sdk.util.UserIdFactory;
import com.hound.core.model.sdk.HoundRequestInfo;
import com.hound.core.model.sdk.HoundResponse;
import com.hound.core.model.sdk.PartialTranscript;
import com.houndify.sample.addressbook.Contact;
import com.houndify.sample.addressbook.PhoneEntry;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.mapzen.speakerbox.Speakerbox;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class VoiceSearchActivity extends AppCompatActivity {

    private TextView statusTextView;
    ProgressBar pb;
    private carbon.widget.ImageView btnSearch;
    private TextView contentTextView;

    private LocationManager locationManager;

    private boolean isContactSynced;
    private VoiceSearch voiceSearch;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_voice_search_phrase);
        speakerbox = new Speakerbox(getApplication());

        statusTextView = findViewById(R.id.status_text_view);
        contentTextView = findViewById(R.id.textView);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        pb = findViewById(R.id.pb);

        btnSearch = findViewById(R.id.btn_search);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*if (voiceSearch == null) {
                    startSearch(new SimpleAudioByteStreamSource());
                } else {
                    voiceSearch.stopRecording();
                }*/
                startSearch(new SimpleAudioByteStreamSource());

            }
        });

        Search.setDebug(true);

        checkPermission();

    }

    private ProgressDialog progressDialog;

    private ProgressDialog initProgerssDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        return progressDialog;
    }

    private void hideProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    public void showProgress(String message) {
        if (progressDialog == null) {
            progressDialog = initProgerssDialog();
        }
        if (progressDialog != null && !isFinishing()) {
            if (message != null) {
                progressDialog.setMessage(message);
            }
            progressDialog.show();
        } else {
            progressDialog = null;
        }
    }


    private void stopSearch() {
        if (voiceSearch != null) {
            voiceSearch.stopRecording();
            voiceSearch = null;
        }
    }

    private void startSearch(InputStream inputStream) {
        if (voiceSearch == null) {
            voiceSearch =
                    new VoiceSearch.Builder().setRequestInfo(getHoundRequestInfo())
                            .setClientId(ClientCredentialsUtil.getClientId(this))
                            .setClientKey(ClientCredentialsUtil.getClientKey(this))
                            .setListener(voiceListener)
                            .setAudioSource(inputStream)
                            .setInputLanguageIetfTag("en")
                            .build();
            voiceSearch.start();
        }


        statusTextView.setText("Listening...");
//        btnSearch.setText("");
    }

    private final Listener voiceListener = new Listener();

    /**
     * Listener to receive search state information and final search results.
     */
    private class Listener implements VoiceSearchListener {

        @Override
        public void onTranscriptionUpdate(final PartialTranscript transcript) {
            if (voiceSearch != null) {
                switch (voiceSearch.getState()) {
                    case STATE_STARTED:
                        statusTextView.setText("Listening...");
                        break;

                    case STATE_SEARCHING:
                        statusTextView.setText("Receiving...");
                        break;

                    default:
                        statusTextView.setText("Unknown");
                        break;
                }
            }
            contentTextView.setText("Transcription:\n" + transcript.getPartialTranscript());
        }

        /**
         * Successful response back from the server that is properly formed.
         *
         * @param response the main response model
         * @param info     miscellaneous information about the search
         */
        @Override
        public void onResponse(HoundResponse response, VoiceSearchInfo info) {
            stopSearch();
//            btnSearch.setText("");
            pb.setVisibility(View.INVISIBLE);
            hideProgress();

            if (response.getStatus().equals(HoundResponse.Status.OK)) {
                statusTextView.setText("Received response");
// We put pretty printing JSON on a separate thread as the server JSON can be
// quite large and will stutter the UI
// Not meant to be configuration change proof, this is just a demo
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String message, responseText;
                        JSONObject responseObject;
                        try {
                            responseObject = new JSONObject(info.getContentBody());
                            responseText = responseObject.toString();
                            message = responseObject.toString(2);
                            handleHoundifyResponse(responseObject);
                        } catch (final Exception ex) {
                            contentTextView.setText("Bad JSON\n\n" + response);
                            message = "Bad JSON\n\n" + response;
                            responseText = message;
                        }
                        System.out.println("RESPONSE : " + responseText);
                        final String finalMessage = message;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                contentTextView.setText(finalMessage);
                            }
                        });
                    }
                }).start();
            } else {
                statusTextView.setText("Request failed with: " + response.getErrorMessage());
                startListning(false);
            }

        }


        @Override
        public void onError(final Exception ex, final VoiceSearchInfo info) {
            voiceSearch = null;
            statusTextView.setText("Something went wrong");
            contentTextView.setText(exceptionToString(ex));
            startListning(false);
        }

        @Override
        public void onRecordingStopped() {
            statusTextView.setText("Receiving...");
            pb.setVisibility(View.VISIBLE);
//            showProgress("Receiving...");
            btnSearch.setEnabled(false);
        }

        @Override
        public void onAbort(final VoiceSearchInfo info) {
            voiceSearch = null;
            statusTextView.setText("Aborted");
            startListning(true);
        }
    }

    /**
     * Listener to receive search state information and final search results.
     *//*
    private class Listener implements VoiceSearch.RawResponseListener {

        @Override
        public void onTranscriptionUpdate(final PartialTranscript transcript) {
            switch (voiceSearch.getState()) {
                case STATE_STARTED:
                    statusTextView.setText("Listening...");
                    break;

                case STATE_SEARCHING:
                    statusTextView.setText("Receiving...");
                    break;

                default:
                    statusTextView.setText("Unknown");
                    break;
            }

            contentTextView.setText("Transcription:\n" + transcript.getPartialTranscript());
        }

        @Override
        public void onResponse(String rawResponse, VoiceSearchInfo voiceSearchInfo) {
            voiceSearch = null;

            statusTextView.setText("Received Response");

            String jsonString;
            try {
                jsonString = new JSONObject(rawResponse).toString(2);
            } catch (final JSONException ex) {
                jsonString = "Failed to parse content:\n" + rawResponse;
            }

            contentTextView.setText(jsonString);
            btnSearch.setText("Search");
        }

        @Override
        public void onError(final Exception ex, final VoiceSearchInfo info) {
            voiceSearch = null;

            statusTextView.setText("Something went wrong");
            contentTextView.setText(exceptionToString(ex));
        }

        @Override
        public void onRecordingStopped() {
            statusTextView.setText("Receiving...");
        }

        @Override
        public void onAbort(final VoiceSearchInfo info) {
            voiceSearch = null;
            statusTextView.setText("Aborted");
        }
    }*/;

    private HoundRequestInfo getHoundRequestInfo() {
        final HoundRequestInfo requestInfo = HoundRequestInfoFactory.getDefault(this);

        // Client App is responsible for providing a UserId for their users which is meaningful
        // to the client.
        requestInfo.setUserId(UserIdFactory.get(this));
        // Each request must provide a unique request ID.
        requestInfo.setRequestId(UUID.randomUUID().toString());

        // Attach location info if permission granted
        setLocation(requestInfo);


        requestInfo.setUseContactData(true);


        return requestInfo;
    }

    /**
     * This method configure the TTS on a HoundRequestInfo object.
     */
    private void configTTS(HoundRequestInfo requestInfo) {

        // Example for configuring Acapela TTS Voice Collection
        // https://www.houndify.com/domains/540c271e-cf06-4f33-ab26-731f0bd9b79d

        // choose the voice 'Laura'
        requestInfo.setExtraField("ResponseAudioVoice", "Laura");

        // response type for audio
        requestInfo.setExtraField("ResponseAudioShortOrLong", "Short");

        // https://docs.houndify.com/reference/VoiceParameters
        final ObjectNode voiceParameters = JsonNodeFactory.instance.objectNode();
        voiceParameters.put("Speed", 100);
        voiceParameters.put("Volume", 100);
        voiceParameters.put("Pitch", 100);
        requestInfo.setExtraField("AcapelaVoiceParameters", voiceParameters);

        // specify the expected audio encoding formats
        final ArrayNode encodings = JsonNodeFactory.instance.arrayNode();
        encodings.add("WAV");
        encodings.add("Speex");
        requestInfo.setExtraField("ResponseAudioAcceptedEncodings", encodings);
    }

    private void setLocation(final HoundRequestInfo requestInfo) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        final Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        if (lastKnownLocation != null) {
            requestInfo.setLatitude(lastKnownLocation.getLatitude());
            requestInfo.setLongitude(lastKnownLocation.getLongitude());
            requestInfo.setPositionHorizontalAccuracy((double) lastKnownLocation.getAccuracy());
        }
    }

    private static String exceptionToString(final Exception ex) {
        try {
            final StringWriter sw = new StringWriter(1024);
            final PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            pw.close();
            return sw.toString();
        } catch (final Exception e) {
            return "";
        }
    }

    public void callUser(String phoneNum) {

        Intent callIntent = new Intent(Intent.ACTION_CALL);

        callIntent.setData(Uri.parse("tel:" + phoneNum + ""));

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        startActivity(callIntent);
        finish();
    }

    private void handleHoundifyResponse(JSONObject resObj) {
        try {
            if (resObj.has("AllResults")) {
                JSONArray AllResults = resObj.getJSONArray("AllResults");
                if (AllResults.length() > 0) {

                    JSONObject result = AllResults.getJSONObject(0);

//                    if (!result.optString("CommandKind").equalsIgnoreCase("NoResultCommand")) {

                    if (result.optString("CommandKind").equalsIgnoreCase("PhoneCommand")) {

                        if (result.optString("PhoneCommandKind").equalsIgnoreCase("CallExactContact") ||
                                result.optString("PhoneCommandKind").equalsIgnoreCase("CallNumber")) {

                            String numberToCall = result.optString("Number");
                            String spokenResponseLong = "";
                            String writeResponseLong = "";

                            JSONObject ClientActionSucceededResult = result.optJSONObject("ClientActionSucceededResult");

                            if (ClientActionSucceededResult != null) {
                                spokenResponseLong = ClientActionSucceededResult.optString("SpokenResponseLong");
                                writeResponseLong = ClientActionSucceededResult.optString("WrittenResponseLong");
                            }

                            if (!numberToCall.isEmpty()) {
                                textToSpeechText(spokenResponseLong, numberToCall);
                                String finalWriteResponseLong = writeResponseLong;
                                runOnUiThread(() -> contentTextView.setText(finalWriteResponseLong));
                            }

                        }
//                        }

                    } /*else if (result.optString("CommandKind").equalsIgnoreCase("DisambiguateCommand")) {

                        if (result.optString("DisambiguateCommandKind").equalsIgnoreCase("CallOneContactNumberAmbiguous")) {

                            JSONObject RequiredFeaturesSupportedResult = result.optJSONObject("RequiredFeaturesSupportedResult");

                            String SpokenResponse = "";
                            String spokenResponseLong = "";
                            String writeResponseLong = "";
                            if (RequiredFeaturesSupportedResult != null) {
                                SpokenResponse = RequiredFeaturesSupportedResult.optString("SpokenResponse");
                                spokenResponseLong = RequiredFeaturesSupportedResult.optString("SpokenResponseLong");
                                writeResponseLong = RequiredFeaturesSupportedResult.optString("WrittenResponseLong");
                            }

                            textToSpeechTextOnNoResultCommand(SpokenResponse + " " + spokenResponseLong);

                            sampleResponse();

                        }

                    }*/ else {
                        String SpokenRsponseLong = result.optString("SpokenResponseLong");
                        if (!SpokenRsponseLong.isEmpty()) {
                            textToSpeechTextOnNoResultCommand(SpokenRsponseLong);
                        } else {
                            runOnUiThread(() -> startListning(false));
                        }
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sampleResponse() {



        /*
 {
  "FormatVersion": "1.0",
  "QueryID": "08fdc8b0-d718-a128-6f1d-d5a280e11457",
  "Disambiguation": {
    "NumToShow": 1,
    "ChoiceData": [
      {
        "Transcription": "call milin ocean",
        "ConfidenceScore": 0.6859999999999999,
        "FormattedTranscription": "call milin ocean",
        "FixedTranscription": "call milin ocean"
      }
    ]
  },
  "ServerGeneratedId": "08fdc8b0-d718-a128-6f1d-d5a280e11457",
  "RealTime": 3.66217,
  "RealSpeechTime": 3.655902,
  "NumToReturn": 1,
  "Status": "OK",
  "AllResults": [
    {
      "SpokenResponse": "Placing phone calls is not supported by this client.",
      "ConversationState": {
        "ConversationStateTime": 1613501759,
        "QueryEntities": {
          "Who": [
            {
              "FullName": "Milin Ocean",
              "FirstName": "Milin Ocean",
              "ContactEntries": [
                {
                  "FirstName": "Milin Ocean",
                  "PhoneEntries": [
                    {
                      "Category": "cell",
                      "Number": "9876543211"
                    }
                  ],
                  "TimesContacted": 0,
                  "IsFavorite": false,
                  "IsVisible": false,
                  "HoundAndroidContactID": "501",
                  "AndroidContactID": "501"
                },
                {
                  "FirstName": "Milin Ocean",
                  "PhoneEntries": [
                    {
                      "Category": "cell",
                      "Number": "+917778870778"
                    }
                  ],
                  "TimesContacted": 0,
                  "IsFavorite": false,
                  "IsVisible": false,
                  "HoundAndroidContactID": "401",
                  "AndroidContactID": "401"
                }
              ]
            }
          ]
        }
      },
      "RequiredFeaturesSupportedResult": {
        "SpokenResponse": "Which number for Milin Ocean?",
        "TemplateData": {
          "TemplateName": "VerticalTemplateList",
          "Items": [
            {
              "DividerBelow": true,
              "TemplateName": "DescriptiveText",
              "TemplateData": {
                "TemplateName": "DescriptiveText",
                "Title": "9876543211",
                "Section1": "9876543211",
                "ActionURIs": [
                  "hound://textsearch?q=the%20first%20one&slt=false"
                ]
              }
            },
            {
              "DividerBelow": true,
              "TemplateName": "DescriptiveText",
              "TemplateData": {
                "TemplateName": "DescriptiveText",
                "Title": "+917778870778",
                "Section1": "+917778870778",
                "ActionURIs": [
                  "hound://textsearch?q=the%20second%20one&slt=false"
                ]
              }
            }
          ]
        },
        "TemplateName": "VerticalTemplateList",
        "UserVisibleMode": "Disambiguate Number",
        "ConversationState": {
          "ConversationStateTime": 1613501759,
          "QueryEntities": {
            "Who": [
              {
                "FullName": "Milin Ocean",
                "FirstName": "Milin Ocean",
                "ContactEntries": [
                  {
                    "FirstName": "Milin Ocean",
                    "PhoneEntries": [
                      {
                        "Category": "cell",
                        "Number": "9876543211"
                      }
                    ],
                    "TimesContacted": 0,
                    "IsFavorite": false,
                    "IsVisible": false,
                    "HoundAndroidContactID": "501",
                    "AndroidContactID": "501"
                  },
                  {
                    "FirstName": "Milin Ocean",
                    "PhoneEntries": [
                      {
                        "Category": "cell",
                        "Number": "+917778870778"
                      }
                    ],
                    "TimesContacted": 0,
                    "IsFavorite": false,
                    "IsVisible": false,
                    "HoundAndroidContactID": "401",
                    "AndroidContactID": "401"
                  }
                ]
              }
            ]
          },
          "ResponseEntities": {
            "Who": [
              {
                "FullName": "Milin Ocean",
                "FirstName": "Milin Ocean",
                "ContactEntries": [
                  {
                    "FirstName": "Milin Ocean",
                    "PhoneEntries": [
                      {
                        "Category": "cell",
                        "Number": "9876543211"
                      }
                    ],
                    "TimesContacted": 0,
                    "IsFavorite": false,
                    "IsVisible": false,
                    "HoundAndroidContactID": "501",
                    "AndroidContactID": "501"
                  },
                  {
                    "FirstName": "Milin Ocean",
                    "PhoneEntries": [
                      {
                        "Category": "cell",
                        "Number": "+917778870778"
                      }
                    ],
                    "TimesContacted": 0,
                    "IsFavorite": false,
                    "IsVisible": false,
                    "HoundAndroidContactID": "401",
                    "AndroidContactID": "401"
                  }
                ]
              }
            ]
          },
          "Mode": "DialOrCallOneContactNumberAmbiguous",
          "Action": "Call",
          "Choices": {
            "Contacts": [
              {
                "FirstName": "Milin Ocean",
                "PhoneEntries": [
                  {
                    "Category": "cell",
                    "Number": "9876543211"
                  }
                ],
                "TimesContacted": 0,
                "IsFavorite": false,
                "IsVisible": false,
                "HoundAndroidContactID": "501",
                "AndroidContactID": "501"
              },
              {
                "FirstName": "Milin Ocean",
                "PhoneEntries": [
                  {
                    "Category": "cell",
                    "Number": "+917778870778"
                  }
                ],
                "TimesContacted": 0,
                "IsFavorite": false,
                "IsVisible": false,
                "HoundAndroidContactID": "401",
                "AndroidContactID": "401"
              }
            ],
            "Choices": [
              {
                "PhoneNumber": "9876543211",
                "PhoneEntries": [
                  {
                    "ContactIndex": 0,
                    "EntryIndex": 0
                  }
                ],
                "ToUserWrittenName": "9876543211",
                "ToUserSpokenName": "nine eight seven six five four three two one one",
                "FromUserNames": [

                ],
                "Categories": [

                ]
              },
              {
                "PhoneNumber": "+917778870778",
                "PhoneEntries": [
                  {
                    "ContactIndex": 1,
                    "EntryIndex": 0
                  }
                ],
                "ToUserWrittenName": "+917778870778",
                "ToUserSpokenName": "plus nine one seven seven seven eight eight seven zero seven seven eight",
                "FromUserNames": [

                ],
                "Categories": [

                ]
              }
            ]
          }
        },
        "LargeScreenHTML": "<div class='h-template h-descriptive-text'>\n  <div class=pure-g>\n    <div class=pure-u-1>\n      <h3 class='h-template-title h-descriptive-text-title'><a href='hound://textsearch?q=the%20first%20one&slt=false'>9876543211</a>\n      </h3>\n    </div>\n  </div>\n  <p class='h-template-p h-descriptive-text-section-1'>9876543211\n  </p>\n</div>\n<div class='h-template h-descriptive-text'>\n  <div class=pure-g>\n    <div class=pure-u-1>\n      <h3 class='h-template-title h-descriptive-text-title'><a href='hound://textsearch?q=the%20second%20one&slt=false'>+917778870778</a>\n      </h3>\n    </div>\n  </div>\n  <p class='h-template-p h-descriptive-text-section-1'>+917778870778\n  </p>\n</div>\n",
        "WrittenResponseLong": "I know 2 numbers for Milin Ocean -- 9876543211 and +917778870778.  Which number should I call?",
        "AutoListen": true,
        "WrittenResponse": "Which number for Milin Ocean?",
        "SmallScreenHTML": "<div class='h-template h-descriptive-text'>\n  <div class=pure-g>\n    <div class=pure-u-1>\n      <h3 class='h-template-title h-descriptive-text-title'><a href='hound://textsearch?q=the%20first%20one&slt=false'>9876543211</a>\n      </h3>\n    </div>\n  </div>\n  <p class='h-template-p h-descriptive-text-section-1'>9876543211\n  </p>\n</div>\n<div class='h-template h-descriptive-text'>\n  <div class=pure-g>\n    <div class=pure-u-1>\n      <h3 class='h-template-title h-descriptive-text-title'><a href='hound://textsearch?q=the%20second%20one&slt=false'>+917778870778</a>\n      </h3>\n    </div>\n  </div>\n  <p class='h-template-p h-descriptive-text-section-1'>+917778870778\n  </p>\n</div>\n",
        "SpokenResponseLong": "I know 2 numbers for Milin Ocean -- nine eight seven six five four three two one one and plus nine one seven seven seven eight eight seven zero seven seven eight.  Which number should I call?"
      },
      "Choices": {
        "Contacts": [
          {
            "FirstName": "Milin Ocean",
            "PhoneEntries": [
              {
                "Category": "cell",
                "Number": "9876543211"
              }
            ],
            "TimesContacted": 0,
            "IsFavorite": false,
            "IsVisible": false,
            "HoundAndroidContactID": "501",
            "AndroidContactID": "501"
          },
          {
            "FirstName": "Milin Ocean",
            "PhoneEntries": [
              {
                "Category": "cell",
                "Number": "+917778870778"
              }
            ],
            "TimesContacted": 0,
            "IsFavorite": false,
            "IsVisible": false,
            "HoundAndroidContactID": "401",
            "AndroidContactID": "401"
          }
        ],
        "Choices": [
          {
            "PhoneNumber": "9876543211",
            "PhoneEntries": [
              {
                "ContactIndex": 0,
                "EntryIndex": 0
              }
            ],
            "ToUserWrittenName": "9876543211",
            "ToUserSpokenName": "nine eight seven six five four three two one one",
            "FromUserNames": [

            ],
            "Categories": [

            ]
          },
          {
            "PhoneNumber": "+917778870778",
            "PhoneEntries": [
              {
                "ContactIndex": 1,
                "EntryIndex": 0
              }
            ],
            "ToUserWrittenName": "+917778870778",
            "ToUserSpokenName": "plus nine one seven seven seven eight eight seven zero seven seven eight",
            "FromUserNames": [

            ],
            "Categories": [

            ]
          }
        ]
      },
      "WrittenResponseLong": "Placing phone calls is not supported by this client.",
      "AutoListen": false,
      "WrittenResponse": "Placing phone calls is not supported by this client.",
      "DisambiguateCommandKind": "CallOneContactNumberAmbiguous",
      "RequiredFeatures": [
        "PlacePhoneCalls"
      ],
      "CommandKind": "DisambiguateCommand",
      "ChoiceList": [
        {
          "QueryText": "the first one",
          "PrimaryText": "9876543211",
          "SecondaryText": "9876543211"
        },
        {
          "QueryText": "the second one",
          "PrimaryText": "+917778870778",
          "SecondaryText": "+917778870778"
        }
      ],
      "SpokenResponseLong": "Placing phone calls is not supported by this client.",
      "ViewType": [
        "Native",
        "None"
      ]
    }
  ],
  "Format": "SoundHoundVoiceSearchResult",
  "ResultsAreFinal": [
    true
  ],
  "BuildInfo": {
    "User": "jenkinslave",
    "Date": "Wed Jan  6 11:47:09 PST 2021",
    "Machine": "f28kbm2.pnp.melodis.com",
    "GitCommit": "4ff69aeb92b0076d5fbbd4fff4a30dd02beb4c5c",
    "GitBranch": "origin/master",
    "BuildNumber": "6825",
    "Kind": "\"Low Fat\"",
    "Variant": "release"
  },
  "AudioLength": 4.31,
  "DomainUsage": [
    {
      "Domain": "Phone",
      "DomainUniqueID": "931bbb76-4661-466d-9991-011564200bb7",
      "CreditsUsed": 0
    },
    {
      "Domain": "Query Glue",
      "DomainUniqueID": "591bfa8a-7468-4e65-8632-dfa5ebf8f0f2",
      "CreditsUsed": 0
    }
  ]
}*/


    }

    private void startListning(boolean isPlayBeep) {
        Log.e("startListning", "isPlayBeep : " + isPlayBeep);
        printStackTrace("isPlayBeep");
        btnSearch.setEnabled(true);
        pb.setVisibility(View.INVISIBLE);
        hideProgress();
        if (isPlayBeep) {
            playPip();
        }
        btnSearch.post(new Runnable() {
            @Override
            public void run() {
                btnSearch.performClick();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
//        if (voiceSearch != null) {
//            voiceSearch.stopRecording();
//            voiceSearch = null;
//        }
    }

    @Override
    public void finish() {
        super.finish();
        hideProgress();
        stopSearch();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        checkPermission();
    }

    private void checkPermission() {
        Dexter.withContext(this)
                .withPermissions(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.MODIFY_AUDIO_SETTINGS,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.READ_SMS,
                        Manifest.permission.SEND_SMS
                ).withListener(new MultiplePermissionsListener() {
                                   @Override
                                   public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                                       if (multiplePermissionsReport.areAllPermissionsGranted()) {
                                           locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                                           if (isContactSynced) {
                                               startListning(true);
                                           } else {
                                               getContacts();
                                           }

                                       }
                                   }

                                   @Override
                                   public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                                       permissionToken.continuePermissionRequest();
                                   }
                               }
        ).check();
    }

    protected Speakerbox speakerbox;
    boolean isEnableSpeakFeature = true;

    private void textToSpeechTextOnNoResultCommand(String text) {
        showToast(text);
        if (isEnableSpeakFeature) {
            speakerbox.playAndOnDone(text, () -> startListning(false));
        }
    }

    private void textToSpeechText(String text) {
        showToast(text);
        if (isEnableSpeakFeature) {
            speakerbox.playAndOnError(text, () -> showToast("Something goes to wrong..."));
        }
    }

    private void textToSpeechText(String text, String number) {

        showToast(text);
        if (isEnableSpeakFeature) {
            speakerbox.playAndOnDone(text, () -> callUser(number));
        } else {
            callUser(number);
        }


    }

    private void showToast(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        runOnUiThread(() -> Toast.makeText(VoiceSearchActivity.this, text, Toast.LENGTH_SHORT).show());
    }

    private void playPip() {
//        ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
//        toneGen1.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 150);

        ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, 100);
        toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 200);

    }

    /*private void playBeep() {
        ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        toneGen1.startTone(ToneGenerator.TONE_PROP_BEEP2, 150);
    }*/

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        /*if (requestCode == PERMISSION_REQUEST_CALL_PHONE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getContacts();
                // permission was granted, yay! Do the
                // contacts-related task you need to do.
            } else {
                Toast.makeText(this, "No permission for contacts", Toast.LENGTH_SHORT).show();
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
            }
        } else {*/
        if (contactManger != null) {
            contactManger.onRequestPermissionsResult(VoiceSearchActivity.this, requestCode, permissions, grantResults);
        }
//        }
    }

    private AsyncTextSearch asyncTextSearch;

    // special text search for uploading contacts.  This realliy only needs to be done one time to store
    // the contacts information to the cloud.  You can subsequently run this if the contacts change and
    // you want to overwrite when is in the cloud.
    private void startTextSearchForContactsUpload() {
        if (asyncTextSearch != null) {
            return; // We are already searching
        }
        AsyncTextSearch.Builder builder = new AsyncTextSearch.Builder()
                .setRequestInfo(getHoundRequestInfoForContactsUpload())
                .setClientId(PrefManager.CLIENT_ID)
                .setClientKey(PrefManager.CLIENT_KEY)
                .setListener(new TextSearchListener() {
                    @Override
                    public void onResponse(HoundResponse response, VoiceSearchInfo info) {
                        if (response.getStatus().equals(HoundResponse.Status.OK)) {
                            System.out.println("Contact Synced Successfully");
                            statusTextView.setText("Contact Synced Successfully");
                            startListning(true);
                            String message;
                            try {
                                message = new JSONObject(info.getContentBody()).toString(2);
                            } catch (final JSONException ex) {

                                message = "Bad JSON\n\n" + response;
                            }

                            System.out.println("RESPONSE : " + message);
                        } else {
                            System.out.println("Request failed with: " + response.getErrorMessage());
                        }
                    }

                    @Override
                    public void onError(Exception e, VoiceSearchInfo info) {

                    }

                    @Override
                    public void onAbort(VoiceSearchInfo info) {

                    }
                })
                .setQuery("user_contacts_request");
        asyncTextSearch = builder.build();
        asyncTextSearch.start();
        statusTextView.setText("Uploading Contacts...");
        btnSearch.setEnabled(false);
//        pb.setVisibility(View.VISIBLE);
        showProgress("Uploading Contacts...");
    }

    private HoundRequestInfo getHoundRequestInfoForContactsUpload() {
        final HoundRequestInfo requestInfo = HoundRequestInfoFactory.getDefault(this);
        requestInfo.setUserId(UserIdFactory.get(this));
        requestInfo.setRequestId(UUID.randomUUID().toString());
// Attach location info if permission granted
        setLocation(requestInfo);
// requestInfo.setUseContactData(true);
        List<Contact> contacts = new ArrayList<>();

        Contact contact;
        List<PhoneEntry> phoneEntries;
        PhoneEntry phoneEntry;

        for (ContactManger.Contact phoneContact : phoneContacts) {
            contact = new Contact();

            contact.setContactId(phoneContact.id + "");
            contact.setLookupKey(phoneContact.id + "");
            contact.setFirstName(phoneContact.getName());
            contact.setLastName("");
            phoneEntries = new ArrayList<>();

            for (String numbers : phoneContact.phoneList) {
                phoneEntry = new PhoneEntry();
                phoneEntry.setCategory("cell");
                phoneEntry.setNumber(numbers);
                phoneEntries.add(phoneEntry);

            }
            contact.setPhoneEntries(phoneEntries);
            contacts.add(contact);

        }
        createAddContactsJsonNew(requestInfo, contacts);
        System.out.println("Request Info : " + requestInfo.toString());
        return requestInfo;
    }

    private static final String LOG_TAG = DashboardActivity.class.getSimpleName();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final int PERMISSION_REQUEST_CALL_PHONE = 10003;
    ContactManger contactManger = new ContactManger();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    // import com.fasterxml.jackson.databind.ObjectMapper;
    private void createAddContactsJsonNew(
            HoundRequestInfo requestInfo,
            final Collection<Contact> contacts) {
        ObjectNode clearRequest = JsonNodeFactory.instance.objectNode();
        ObjectNode addRequest = JsonNodeFactory.instance.objectNode();
        ArrayNode requestArray = JsonNodeFactory.instance.arrayNode();
        ArrayNode contactsArray = JsonNodeFactory.instance.arrayNode();
        System.out.println("Request Info : total contacts = " + contacts.size());
        try {
            for (Contact contact : contacts) {
                contactsArray.add(objectMapper.convertValue(contact, ObjectNode.class));
            }
            addRequest.put("RequestKind", "Add");
            addRequest.set("NewContacts", contactsArray);
            clearRequest.put("RequestKind", "Clear");
            requestArray.insert(0, clearRequest);
            requestArray.insert(1, addRequest);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
        requestInfo.setExtraField("UserContactsRequests", requestArray);
    }

    List<ContactManger.Contact> phoneContacts = new ArrayList<>();

    private void getContacts() {

        contactManger.getContactsAsync(this, new ContactManger.ContactManagerListner() {
            @Override
            public void onContactPickFinish(List<ContactManger.Contact> contacts) {
                phoneContacts.clear();
                phoneContacts.addAll(contacts);
                startTextSearchForContactsUpload();
                isContactSynced = true;
            }

            @Override
            public void onPermmissionDenied() {

            }

            @Override
            public void onContactPickStart() {


            }
        });
    }

    public void printStackTrace(String msg) {
        System.out.println("============== " + msg + " ================");
        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
            System.out.println(ste);
        }
        System.out.println("==============  ================");
    }
}
