package com.houndify.sample;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.ContactsContract;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class ContactManger {

    private static final int PERMISSION_REQUEST_CONTACT = 10001;

    public ContactManger() {
    }

    ContactManagerListner listner;

    public List<Contact> getContacts(Activity ctx) {
        List<Contact> list = new ArrayList<>();
        String phoneNumber = "";
        ContentResolver contentResolver = ctx.getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI,
                null,
                null,
                null,
                ContactsContract.Contacts.DISPLAY_NAME + " ASC");
        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                Contact info = new Contact();

                info.id = id;
                info.name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                if (cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {

//                    Cursor cursorInfo = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
//                            null,
//                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
//                            new String[]{id},
//                            null);

                    Cursor cursorInfo = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ? "
//                                    +
//                                    "AND " +
//                                    ContactsContract.CommonDataKinds.Phone.TYPE + " = " +
//                                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                            ,
                            new String[]{id},
                            null);

//                    InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(ctx.getContentResolver(),
//                            ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, new Long(id)));
//
//                    Uri person = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, new Long(id));
//                    Uri pURI = Uri.withAppendedPath(person, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
//
//                    Bitmap photo = null;
//                    if (inputStream != null) {
//                        photo = BitmapFactory.decodeStream(inputStream);
//                    }

                    while (cursorInfo.moveToNext()) {
                        phoneNumber = cursorInfo.getString(cursorInfo.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        info.phone = phoneNumber;
                        info.addPhoneNumber(phoneNumber);
                    }
                    cursorInfo.close();
                }

//                if (cursor.getInt(cursor.getColumnIndex("has_email")) > 0) {
//                    Cursor cursorInfo = contentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
//                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);
//
//                    while (cursorInfo.moveToNext()) {
//                        info.email = cursorInfo.getString(cursorInfo.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
//                    }
//                    cursorInfo.close();
//                }
                list.add(info);
            }
            cursor.close();
        }
        return list;
    }

    public void getContactsAsync(Activity activity, ContactManagerListner listner) {
        this.listner = listner;
        askForContactPermission(activity);
    }

    public void askForContactPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                        Manifest.permission.READ_CONTACTS)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setTitle("Contacts access needed");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setMessage("please confirm Contacts access");//TODO put real question
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @TargetApi(Build.VERSION_CODES.M)
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            activity.requestPermissions(
                                    new String[]
                                            {
                                                    Manifest.permission.READ_CONTACTS,
                                                    Manifest.permission.CALL_PHONE,
                                            }
                                    , PERMISSION_REQUEST_CONTACT);
                        }
                    });
                    builder.show();
                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.READ_CONTACTS},
                            PERMISSION_REQUEST_CONTACT);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            } else {
                getContact(activity);
            }
        } else {
            getContact(activity);
        }
    }

    private void getContact(Activity activity) {
        new ContactAsync(activity).execute();
    }

    public void onRequestPermissionsResult(Activity activity, int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CONTACT: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getContact(activity);
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {
                    if (listner != null) {
                        listner.onPermmissionDenied();
                    }
                    Toast.makeText(activity, "No permission for contacts", Toast.LENGTH_SHORT).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public void setListner(ContactManagerListner listner) {
        this.listner = listner;
    }

    public class ContactAsync extends AsyncTask<Void, Void, List<Contact>> {

        Activity mContext;

        public ContactAsync(Activity mContext) {
            this.mContext = mContext;
        }

        @Override
        protected List<Contact> doInBackground(Void... voids) {
            if (listner != null) {
                listner.onContactPickStart();
            }
            return getContacts(mContext);
        }


        @Override
        protected void onPostExecute(List<Contact> contacts) {
            if (listner != null) {
                listner.onContactPickFinish(contacts);
            }
        }
    }

    public static class Contact {

        String id;
        String name;
        String phone;
        List<String> phoneList = new ArrayList<>();
//        String email;

        public Contact(Contact contact) {
            this.id = contact.id;
            this.name = contact.name;
            this.phone = contact.phone;
            this.phoneList.clear();
            this.phoneList.addAll(contact.phoneList);
        }

        public Contact() {
        }

        public void addPhoneNumber(String phone) {
            phoneList.add(phone);
        }

        public List<String> getPhoneList() {
            return phoneList;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getPhone() {
            return phone;
        }

        @Override
        public String toString() {
            return "{" +
                    "name:'" + name + '\'' +
                    ", phone:'" + phone + '\'' +
                    ", phoneList:'" + phoneList.toString() + '\'' +
//                    ", email:'" + email + '\'' +
                    '}';
        }
    }


    public interface ContactManagerListner {
        void onContactPickStart();

        void onContactPickFinish(List<Contact> contacts);

        void onPermmissionDenied();

    }
}
