package com.houndify.sample;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
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

import org.json.JSONException;
import org.json.JSONObject;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class TextSearchActivity extends AppCompatActivity {
    private static final String LOG_TAG = TextSearchActivity.class.getSimpleName();
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
        textView = findViewById(R.id.textView);
        button = findViewById(R.id.button);
        editText = findViewById(R.id.editText);
        editText.setText("What is the weather");
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Contacts must be uploaded in their own text search one time in order to store them
        // on the Houndify server.  Future queries related to contacts can then use this information. e.g. "call milin patel"
        startTextSearchForContactsUpload();

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
// requestInfo.setUseContactData(true);
        System.out.println("Request Info : " + requestInfo.toString());;
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
        System.out.println("Request Info : " + requestInfo.toString());;
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
                .setClientId(ClientCredentialsUtil.getClientId(this))
                .setClientKey(ClientCredentialsUtil.getClientKey(this))
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
                .setClientId(ClientCredentialsUtil.getClientId(this))
                .setClientKey(ClientCredentialsUtil.getClientKey(this))
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
                        String message;
                        try {
                            message = new JSONObject(info.getContentBody()).toString(2);
                        } catch (final JSONException ex) {
                            textView.setText("Bad JSON\n\n" + response);
                            message = "Bad JSON\n\n" + response;
                        }
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
    private void createAddContactsJson(HoundRequestInfo requestInfo, final Collection<Contact>
            contacts) {
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
        ObjectMapper objectMapper = new ObjectMapper();
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
}