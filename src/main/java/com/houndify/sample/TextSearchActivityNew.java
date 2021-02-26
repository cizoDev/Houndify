package com.houndify.sample;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
import com.hound.android.sdk.VoiceSearchInfo;
import com.hound.android.sdk.VoiceSearchState;
import com.hound.android.sdk.util.HoundRequestInfoFactory;
import com.hound.android.sdk.util.UserIdFactory;
import com.hound.core.model.sdk.HoundRequestInfo;
import com.hound.core.model.sdk.HoundResponse;
import com.houndify.sample.addressbook.Contact;
import com.houndify.sample.addressbook.PhoneEntry;
import com.mapzen.speakerbox.Speakerbox;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class TextSearchActivityNew extends AppCompatActivity {
    private static final String LOG_TAG = TextSearchActivityNew.class.getSimpleName();


    protected Speakerbox speakerbox;
    boolean isEnableSpeakFeature = false;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private TextView textView;
    private carbon.widget.TextView button;
    private EditText editText;
    private AsyncTextSearch asyncTextSearch;
    private LocationManager locationManager;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Search.setDebug(false);
        setContentView(R.layout.activity_textsearch);
        speakerbox = new Speakerbox(getApplication());
        textView = findViewById(R.id.textView);
        button = findViewById(R.id.button);
        editText = findViewById(R.id.editText);
//        editText.setText("call Milin Patel");
//        editText.setText("What is the weather");
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Contacts must be uploaded in their own text search one time in order to store them
        // on the Houndify server.  Future queries related to contacts can then use this information. e.g. "call milin patel"

//        startTextSearchForContactsUpload();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (asyncTextSearch == null) {
                    resetUIState();
                    startTextSearchFromUser();
                } else {
// voice search has already started.
                    if (asyncTextSearch.getState() == VoiceSearchState.STATE_STARTED) {
                        asyncTextSearch.abort();
                    }
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (asyncTextSearch != null) {
            asyncTextSearch.abort();
        }
    }

    private HoundRequestInfo getHoundRequestInfo() {
        final HoundRequestInfo requestInfo = HoundRequestInfoFactory.getDefault(this);
        requestInfo.setUserId(UserIdFactory.get(this));
        requestInfo.setRequestId(UUID.randomUUID().toString());
// Attach location info if permission granted
        setLocation(requestInfo);
        requestInfo.setUseContactData(true);

        try {
            System.out.println("Request Info : " + objectMapper.writeValueAsString(requestInfo));
        } catch (Exception ex) {
            Log.d(LOG_TAG, "Failed trying to write RequestInfo");
        }
        return requestInfo;
    }

    private HoundRequestInfo getHoundRequestInfoForContactsUpload() {
        final HoundRequestInfo requestInfo = HoundRequestInfoFactory.getDefault(this);
        requestInfo.setUserId(UserIdFactory.get(this));
        requestInfo.setRequestId(UUID.randomUUID().toString());
// Attach location info if permission granted
        setLocation(requestInfo);
// requestInfo.setUseContactData(true);
        List<Contact> contacts = new ArrayList<>();
        Contact contact = new Contact();
        contact.setContactId(SystemClock.currentThreadTimeMillis() + "");
        contact.setLookupKey(SystemClock.currentThreadTimeMillis() + "");
        contact.setFirstName("Milin");
        contact.setLastName("Patel");
        List<PhoneEntry> phoneEntries = new ArrayList<>();
        PhoneEntry phoneEntry = new PhoneEntry();
        phoneEntry.setCategory("cell");
        phoneEntry.setNumber("8980606000");
        phoneEntries.add(phoneEntry);
        contact.setPhoneEntries(phoneEntries);
        contacts.add(contact);
        createAddContactsJsonNew(requestInfo, contacts);
        System.out.println("Request Info : " + requestInfo.toString());
        ;
        return requestInfo;
    }

    private void setLocation(final HoundRequestInfo requestInfo) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            return;
        }
        final Location lastKnownLocation =
                locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        if (lastKnownLocation != null) {
            requestInfo.setLatitude(lastKnownLocation.getLatitude());
            requestInfo.setLongitude(lastKnownLocation.getLongitude());
            requestInfo.setPositionHorizontalAccuracy((double) lastKnownLocation.getAccuracy());
        }
    }

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
                .setListener(textSearchListener)
                .setQuery("user_contacts_request");
        asyncTextSearch = builder.build();
        asyncTextSearch.start();
    }

    // text search with the query provided via UI
    private void startTextSearchFromUser() {
        if (asyncTextSearch != null) {
            return; // We are already searching
        }
        AsyncTextSearch.Builder builder = new AsyncTextSearch.Builder()
                .setRequestInfo(getHoundRequestInfo())
                .setClientId(PrefManager.CLIENT_ID)
                .setClientKey(PrefManager.CLIENT_KEY)
                .setListener(textSearchListener)
                .setQuery(editText.getText().toString());
        asyncTextSearch = builder.build();
        textView.setText("Waiting for response...");
        button.setText("Stop Search");
        asyncTextSearch.start();
    }

    private void resetUIState() {
        button.setEnabled(true);
        button.setText("Submit text");
    }

    private final TextSearchListener textSearchListener = new TextSearchListener() {
        @Override
        public void onResponse(final HoundResponse response, final VoiceSearchInfo info) {
            asyncTextSearch = null;
            resetUIState();
// Make sure the request succeeded with OK
            if (response.getStatus().equals(HoundResponse.Status.OK)) {
                textView.setText("Received response...displaying the JSON");
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
                            textView.setText("Bad JSON\n\n" + response);
                            message = "Bad JSON\n\n" + response;
                            responseText = message;
                        }
                        System.out.println("RESPONSE : " + responseText);
                        final String finalMessage = message;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText(finalMessage);
                            }
                        });
                    }
                }).start();
            } else {
                textView.setText("Request failed with: " + response.getErrorMessage());
            }
        }

        @Override
        public void onError(final Exception ex, final VoiceSearchInfo info) {
            asyncTextSearch = null;
            resetUIState();
            textView.setText(exceptionToString(ex));
        }

        @Override
        public void onAbort(final VoiceSearchInfo info) {
            asyncTextSearch = null;
            resetUIState();
            textView.setText("Aborted");
        }
    };

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

    private void createAddContactsJson(HoundRequestInfo requestInfo, final Collection<Contact> contacts) {
        ObjectNode clearRequest = JsonNodeFactory.instance.objectNode();
        ObjectNode addRequest = JsonNodeFactory.instance.objectNode();
        ArrayNode requestArray = JsonNodeFactory.instance.arrayNode();
        ArrayNode contactsArray = JsonNodeFactory.instance.arrayNode();
        try {
            for (Contact contact : contacts) {
// contactsArray.add(ContactJsonFactory.fromContact(contact));
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

    // import com.fasterxml.jackson.databind.ObjectMapper;
    private void createAddContactsJsonNew(HoundRequestInfo requestInfo, final
    Collection<Contact> contacts) {
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

    public void callUser(String phoneNum) {

        Intent callIntent = new Intent(Intent.ACTION_CALL);

        callIntent.setData(Uri.parse("tel:" + phoneNum + ""));

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        startActivity(callIntent);
    }


    private void handleHoundifyResponse(JSONObject resObj) {


        try {
            if (resObj.has("AllResults")) {
                JSONArray AllResults = resObj.getJSONArray("AllResults");
                if (AllResults.length() > 0) {

                    JSONObject result = AllResults.getJSONObject(0);

                    if (!result.optString("CommandKind").equalsIgnoreCase("NoResultCommand")) {

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
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            textView.setText(finalWriteResponseLong);
                                        }
                                    });
                                }

                            }
                        }

                    } else {
                        textToSpeechText(result.optString("SpokenResponseLong"));
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void textToSpeechText(String text) {
        if (isEnableSpeakFeature) {
            speakerbox.playAndOnError(text, () -> showToast("Something goes to wrong..."));
        } else {
            showToast(text);
        }
    }


    private void textToSpeechText(String text, String number) {

        if (isEnableSpeakFeature) {
            speakerbox.playAndOnDone(text, () -> callUser(number));
        } else {
            callUser(number);
            showToast(text);
        }


    }

    private void showToast(String text) {
        runOnUiThread(() -> Toast.makeText(TextSearchActivityNew.this, text, Toast.LENGTH_SHORT).show());
    }


    //<editor-fold desc="NoResultCommand Response Sample">
    /*



    {
  "Format": "SoundHoundVoiceSearchResult",
  "FormatVersion": "1.0",
  "Status": "OK",
  "NumToReturn": 1,
  "AllResults": [
    {
      "CommandKind": "NoResultCommand",
      "SpokenResponse": "",
      "SpokenResponseLong": "Didn't get that!",
      "WrittenResponse": "Didn't get that!",
      "WrittenResponseLong": "Didn't get that!",
      "AutoListen": false,
      "ViewType": [
        "Native",
        "None"
      ],
      "TranscriptionSearchURL": "http:\/\/www.google.com\/#q=call%20Milin%20Patel"
    }
  ],
  "Disambiguation": {
    "NumToShow": 1,
    "ChoiceData": [
      {
        "Transcription": "call Milin Patel",
        "ConfidenceScore": 0.6859999999999999,
        "FormattedTranscription": "call Milin Patel",
        "FixedTranscription": ""
      }
    ]
  },
  "ResultsAreFinal": [
    true
  ],
  "DomainUsage": [

  ],
  "BuildInfo": {
    "User": "jenkinslave",
    "Date": "Wed Jan  6 11:47:09 PST 2021",
    "Machine": "f28kbm2.pnp.melodis.com",
    "GitCommit": "4ff69aeb92b0076d5fbbd4fff4a30dd02beb4c5c",
    "GitBranch": "origin\/master",
    "BuildNumber": "6825",
    "Kind": "\"Low Fat\"",
    "Variant": "release"
  },
  "QueryID": "b2a088e6-91aa-8068-8e3c-b6525f112b9c",
  "ServerGeneratedId": "b2a088e6-91aa-8068-8e3c-b6525f112b9c"
}


    * */
    //</editor-fold>

    //<editor-fold desc="PhoneCommand - Contact Response Sample">
    /*

{
  "Format": "SoundHoundVoiceSearchResult",
  "FormatVersion": "1.0",
  "Status": "OK",
  "NumToReturn": 1,
  "AllResults": [
    {
      "CommandKind": "PhoneCommand",
      "SpokenResponse": "Placing phone calls is not supported by this client.",
      "SpokenResponseLong": "Placing phone calls is not supported by this client.",
      "WrittenResponse": "Placing phone calls is not supported by this client.",
      "WrittenResponseLong": "Placing phone calls is not supported by this client.",
      "AutoListen": false,
      "ConversationState": {
        "ConversationStateTime": 1612176696,
        "History": [
          {
            "ConversationStateTime": 1612176502,
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
          }
        ],
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
      "ViewType": [
        "Native",
        "None"
      ],
      "ClientActionSucceededResult": {
        "SpokenResponse": "Calling Milin Ocean.",
        "SpokenResponseLong": "Calling Milin Ocean.",
        "WrittenResponse": "Calling Milin Ocean (+917778870778).",
        "WrittenResponseLong": "Calling Milin Ocean (+917778870778).",
        "ConversationState": {
          "ConversationStateTime": 1612176696,
          "History": [
            {
              "ConversationStateTime": 1612176502,
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
            }
          ],
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
        "ViewType": [
          "Native"
        ]
      },
      "ClientActionFailedResult": {
        "SpokenResponse": "Failed trying to call Milin Ocean.",
        "SpokenResponseLong": "Failed trying to call Milin Ocean.",
        "WrittenResponse": "Failed trying to call Milin Ocean (+917778870778).",
        "WrittenResponseLong": "Failed trying to call Milin Ocean (+917778870778).",
        "ConversationState": {
          "ConversationStateTime": 1612176696,
          "History": [
            {
              "ConversationStateTime": 1612176502,
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
            }
          ],
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
        }
      },
      "PhoneCommandKind": "CallExactContact",
      "Number": "+917778870778",
      "Contacts": [
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
  ],
  "Disambiguation": {
    "NumToShow": 1,
    "ChoiceData": [
      {
        "Transcription": "call Milin ocean",
        "ConfidenceScore": 0.6859999999999999,
        "FormattedTranscription": "call Milin ocean",
        "FixedTranscription": "call Milin ocean"
      }
    ]
  },
  "ResultsAreFinal": [
    true
  ],
  "DomainUsage": [
    {
      "Domain": "Phone",
      "DomainUniqueID": "931bbb76-4661-466d-9991-011564200bb7",
      "CreditsUsed": 1
    },
    {
      "Domain": "Query Glue",
      "DomainUniqueID": "591bfa8a-7468-4e65-8632-dfa5ebf8f0f2",
      "CreditsUsed": 0
    }
  ],
  "BuildInfo": {
    "User": "jenkinslave",
    "Date": "Wed Jan 6 11:47:09 PST 2021",
    "Machine": "f28kbm2.pnp.melodis.com",
    "GitCommit": "4ff69aeb92b0076d5fbbd4fff4a30dd02beb4c5c",
    "GitBranch": "origin\/master",
    "BuildNumber": "6825",
    "Kind": "\"Low Fat\"",
    "Variant": "release"
  },
  "QueryID": "a51d971b-0e88-e983-4375-a20ba7d28f87",
  "ServerGeneratedId": "a51d971b-0e88-e983-4375-a20ba7d28f87"
}

    * */
    //</editor-fold>

    //<editor-fold desc="PhoneCommand - Number Response Sample">
    /*


{
  "Format": "SoundHoundVoiceSearchResult",
  "FormatVersion": "1.0",
  "Status": "OK",
  "NumToReturn": 1,
  "AllResults": [
    {
      "CommandKind": "PhoneCommand",
      "SpokenResponse": "Placing phone calls is not supported by this client.",
      "SpokenResponseLong": "Placing phone calls is not supported by this client.",
      "WrittenResponse": "Placing phone calls is not supported by this client.",
      "WrittenResponseLong": "Placing phone calls is not supported by this client.",
      "AutoListen": false,
      "ConversationState": {
        "ConversationStateTime": 1612178214,
        "History": [
          {
            "ConversationStateTime": 1612176696,
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
          {
            "ConversationStateTime": 1612176502,
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
          }
        ]
      },
      "ViewType": [
        "Native",
        "None"
      ],
      "ClientActionSucceededResult": {
        "SpokenResponse": "Calling eight nine eight zero six zero six zero zero zero.",
        "SpokenResponseLong": "Calling eight nine eight zero six zero six zero zero zero.",
        "WrittenResponse": "Calling 8980606000.",
        "WrittenResponseLong": "Calling 8980606000.",
        "ConversationState": {
          "ConversationStateTime": 1612178214,
          "History": [
            {
              "ConversationStateTime": 1612176696,
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
            {
              "ConversationStateTime": 1612176502,
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
            }
          ]
        }
      },
      "ClientActionFailedResult": {
        "SpokenResponse": "Failed trying to call eight nine eight zero six zero six zero zero zero.",
        "SpokenResponseLong": "Failed trying to call eight nine eight zero six zero six zero zero zero.",
        "WrittenResponse": "Failed trying to call 8980606000.",
        "WrittenResponseLong": "Failed trying to call 8980606000.",
        "ConversationState": {
          "ConversationStateTime": 1612178214,
          "History": [
            {
              "ConversationStateTime": 1612176696,
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
            {
              "ConversationStateTime": 1612176502,
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
            }
          ]
        }
      },
      "PhoneCommandKind": "CallNumber",
      "Number": "8980606000",
      "NumberDerivation": {
        "DerivationKind": "ExplicitPhoneNumber",
        "SpokenForm": "eight nine eight zero six zero six zero zero zero",
        "WrittenForm": "8980606000",
        "Digits": "8980606000"
      }
    }
  ],
  "Disambiguation": {
    "NumToShow": 1,
    "ChoiceData": [
      {
        "Transcription": "call 8980606000",
        "ConfidenceScore": 0.6859999999999999,
        "FormattedTranscription": "call 8980606000",
        "FixedTranscription": "call 8980606000"
      }
    ]
  },
  "ResultsAreFinal": [
    true
  ],
  "DomainUsage": [
    {
      "Domain": "Phone Number by Digits",
      "DomainUniqueID": "14d4f18e-7da7-4297-9291-b06987372df8",
      "CreditsUsed": 0
    },
    {
      "Domain": "Phone",
      "DomainUniqueID": "931bbb76-4661-466d-9991-011564200bb7",
      "CreditsUsed": 1
    },
    {
      "Domain": "Query Glue",
      "DomainUniqueID": "591bfa8a-7468-4e65-8632-dfa5ebf8f0f2",
      "CreditsUsed": 0
    }
  ],
  "BuildInfo": {
    "User": "jenkinslave",
    "Date": "Wed Jan 6 11:47:09 PST 2021",
    "Machine": "f28kbm2.pnp.melodis.com",
    "GitCommit": "4ff69aeb92b0076d5fbbd4fff4a30dd02beb4c5c",
    "GitBranch": "origin\/master",
    "BuildNumber": "6825",
    "Kind": "\"Low Fat\"",
    "Variant": "release"
  },
  "QueryID": "d0b51bc3-c684-de5e-7e0e-9813e8b78a6d",
  "ServerGeneratedId": "d0b51bc3-c684-de5e-7e0e-9813e8b78a6d"
}


    * */
    //</editor-fold>

}