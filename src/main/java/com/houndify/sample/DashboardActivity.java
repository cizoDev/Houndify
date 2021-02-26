package com.houndify.sample;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hound.android.sdk.AsyncTextSearch;
import com.hound.android.sdk.TextSearchListener;
import com.hound.android.sdk.VoiceSearchInfo;
import com.hound.android.sdk.util.HoundRequestInfoFactory;
import com.hound.android.sdk.util.UserIdFactory;
import com.hound.core.model.sdk.HoundRequestInfo;
import com.hound.core.model.sdk.HoundResponse;
import com.houndify.sample.addressbook.Contact;
import com.houndify.sample.addressbook.PhoneEntry;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class DashboardActivity extends AppCompatActivity {


    private static final int PERMISSION_REQUEST_CALL_PHONE = 10003;
    ContactManger contactManger = new ContactManger();

    private static class Sample {
        final private CharSequence title;
        final private CharSequence subtitle;
        final private CharSequence[] features;
        final private Class activityClass;

        Sample(CharSequence title, CharSequence subtitle, CharSequence[] features,
               Class activityClass) {
            this.title = title;
            this.subtitle = subtitle;
            this.features = features;
            this.activityClass = activityClass;
        }
    }

    private static final List<Sample> SAMPLE_LIST = new ArrayList();

    static {
//        SAMPLE_LIST.add(new Sample("Houndify Voice Search with Phrase Spotter",
//                "Recommended Integration",
//                new CharSequence[]{
//                        "Wake-up phrase spotter for hands-free experience",
//                        "Phrase spotter threshold adjustment",
//                        "One-time initialization with Houndify class",
//                        "Interactive search panel with live transcriptions",
//                        "Response spoken with Android TTS engine",
//                },
//                HoundifyVoiceSearchWithPhraseSpotterActivity.class));

        SAMPLE_LIST.add(new Sample("Custom Voice Search",
                "",
                new CharSequence[]{
//                        "Custom voice search API",
//                        "Live transcriptions",
//                        "Access to raw JSON response",
                },
                VoiceSearchActivity.class));

//        SAMPLE_LIST.add(new Sample("Text Search",
//                "Text-based Search",
//                new CharSequence[]{
//                        "Text search API",
//                        "Access to raw JSON response",
//                }, TextSearchActivityNew.class));
    }

    private ListView listView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        listView = findViewById(R.id.listview);


        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE},
                    PERMISSION_REQUEST_CALL_PHONE);
        } else {
            getContacts();
        }


        listView.setAdapter(new SamplesAdapter());
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startActivity(new Intent(getApplicationContext(),
                        SAMPLE_LIST.get(position).activityClass));
            }
        });

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
        }, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (TextUtils.isEmpty(ClientCredentialsUtil.getClientId(this)) ||
                TextUtils.isEmpty(ClientCredentialsUtil.getClientKey(this))) {
            Toast.makeText(this,
                    "Client API Credentials missing.\nPlease update in settings.",
                    Toast.LENGTH_LONG)
                    .show();
        }
    }

    private MenuItem menuSettings;
    private MenuItem menuVersionInfo;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menuSettings = menu.add("Client API Credentials");
        menuSettings.setIcon(android.R.drawable.ic_menu_edit);
        menuVersionInfo = menu.add("Version Info");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item == menuSettings) {
            ClientCredentialsUtil.showEditPopup(this);
            return true;
        } else if (item == menuVersionInfo) {
            showVersionInfoDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class SamplesAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return SAMPLE_LIST.size();
        }

        @Override
        public Sample getItem(int position) {
            return SAMPLE_LIST.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView;

            if (convertView == null) {
                itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_sample, parent, false);
            } else {
                itemView = convertView;
            }

            Sample sample = getItem(position);

            TextView title = itemView.findViewById(R.id.title);
            title.setText(sample.title);

            TextView subtitle = itemView.findViewById(R.id.subtitle);
            subtitle.setText(sample.subtitle);

            TextView description = itemView.findViewById(R.id.description);

            StringBuilder stringBuilder = new StringBuilder();
            for (CharSequence feature : sample.features) {
                if (stringBuilder.length() > 0) {
                    stringBuilder.append('\n');
                }
                stringBuilder.append(" - ").append(feature);
            }

            description.setText(stringBuilder.toString());

            return itemView;
        }
    }

    private void showVersionInfoDialog() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("Sample App: ").append(BuildConfig.VERSION_NAME).append('\n');

        stringBuilder.append("Houndify Lib: ")
                .append(com.hound.android.voicesdk.BuildConfig.ARTIFACT_ID)
                .append(':')
                .append(com.hound.android.voicesdk.BuildConfig.VERSION_NAME)
                .append('\n');

        stringBuilder.append("Phrase Spotter Lib: ")
                .append(com.hound.android.libphs.BuildConfig.ARTIFACT_ID)
                .append(':')
                .append(com.hound.android.libphs.BuildConfig.VERSION_NAME);

        new AlertDialog.Builder(this).setTitle("Version Info")
                .setMessage(stringBuilder.toString())
                .create()
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CALL_PHONE) {
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
        } else {
            if (contactManger != null) {
                contactManger.onRequestPermissionsResult(DashboardActivity.this, requestCode, permissions, grantResults);
            }
        }
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
                            System.out.println("Received response...displaying the JSON");

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

    private LocationManager locationManager;
    private void setLocation(final HoundRequestInfo requestInfo) {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
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
    private static final String LOG_TAG = DashboardActivity.class.getSimpleName();
    private static final ObjectMapper objectMapper = new ObjectMapper();

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
            }

            @Override
            public void onPermmissionDenied() {

            }

            @Override
            public void onContactPickStart() {


            }
        });
    }

}
