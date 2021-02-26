package com.houndify.sample.addressbook;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.List;

/**
 * For description of fields/expectations, see:
 * <a href="http://houndify.com/OneContact.html">http://houndify.com/OneContact.html</a>
 */
@JsonSerialize
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)

public class Contact {

    @JsonProperty("AndroidContactID")
    String lookupKey;

    // TODO: Add this back when the new contact sync code is supported
    @JsonProperty("HoundAndroidContactID")
    String houndAndroidContactId;

    @JsonProperty("FirstName")
    String firstName;
    @JsonProperty("LastName")
    String lastName;
    @JsonProperty("SingleName")
    String singleName;

    @JsonProperty("NickNames")
    List<String> nickNames = new ArrayList<>();

    @JsonProperty("PhoneEntries")
    List<PhoneEntry> phoneEntries = new ArrayList<>();
    @JsonProperty("DefaultPhone")
    String defaultPhoneCategory;

    @JsonProperty("TimesContacted")
    int timesContacted;
    @JsonProperty("LastContacted")
    DateAndTime lastContacted;


    @JsonProperty("DefaultEmail")
    String defaultEmailCategory;

    @JsonProperty("IsFavorite")
    boolean favorite;
    @JsonProperty("IsVisible")
    boolean visible;


    public Contact() {
        // Empty for jackson
    }

    public String getLookupKey() {
        return lookupKey;
    }

    public void setLookupKey(final String lookupKey) {
        this.lookupKey = lookupKey;
    }

    //     TODO: Add this back when the new contact sync code is supported
///*
    public String getContactId() {
        return houndAndroidContactId;
    }

    public void setContactId(final String houndAndroidContactId) {
        this.houndAndroidContactId = houndAndroidContactId;
    }

    //*/
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public String getSingleName() {
        return singleName;
    }

    public void setSingleName(final String singleName) {
        this.singleName = singleName;
    }

    public List<String> getNickNames() {
        return nickNames;
    }

    public void setNickNames(final List<String> nickNames) {
        this.nickNames = nickNames;
    }

    public List<PhoneEntry> getPhoneEntries() {
        return phoneEntries;
    }

    public void setPhoneEntries(final List<PhoneEntry> phoneEntries) {
        this.phoneEntries = phoneEntries;
    }

    public String getDefaultPhoneCategory() {
        return defaultPhoneCategory;
    }

    public void setDefaultPhoneCategory(final String defaultPhoneCategory) {
        this.defaultPhoneCategory = defaultPhoneCategory;
    }

    public String getDefaultEmailCategory() {
        return defaultEmailCategory;
    }

    public void setDefaultEmailCategory(final String defaultEmailCategory) {
        this.defaultEmailCategory = defaultEmailCategory;
    }

    public DateAndTime getLastContacted() {
        return lastContacted;
    }

    public void setLastContacted(final DateAndTime lastContacted) {
        this.lastContacted = lastContacted;
    }

    public int getTimesContacted() {
        return timesContacted;
    }

    public void setTimesContacted(final int timesContacted) {
        this.timesContacted = timesContacted;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(final boolean favorite) {
        this.favorite = favorite;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(final boolean visible) {
        this.visible = visible;
    }

}