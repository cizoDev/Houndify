package com.houndify.sample.addressbook;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;


@JsonSerialize
@JsonInclude(JsonInclude.Include.NON_NULL)

public class PhoneEntry {

    @JsonProperty("Category")
    String category;
    @JsonProperty("Number")
    String number;

    public String getCategory() {
        return category;
    }

    public void setCategory(final String category) {
        this.category = category;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(final String number) {
        this.number = number;
    }
}
