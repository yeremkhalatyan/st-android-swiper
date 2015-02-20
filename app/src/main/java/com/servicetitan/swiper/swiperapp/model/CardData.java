package com.servicetitan.swiper.swiperapp.model;

/**
 * Created by Admin on 2/11/2015.
 */
public class CardData {

    private String mName;
    private String mSurName;

    private String mCardNumber;

    public CardData() {

    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getSurName() {
        return mSurName;
    }

    public void setSurName(String surName) {
        mSurName = surName;
    }

    public String getCardNumber() {
        return mCardNumber;
    }

    public void setCardNumber(String cardNumber) {
        mCardNumber = cardNumber;
    }
}
