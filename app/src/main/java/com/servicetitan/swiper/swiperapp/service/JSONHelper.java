package com.servicetitan.swiper.swiperapp.service;

import android.util.Log;

import com.servicetitan.swiper.swiperapp.model.CardData;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Admin on 2/11/2015.
 */
public class JSONHelper {
    private final static String TAG = "JSONHelper";

    private static final String NAME_JSON_KEY = "card_data";
    private static final String C_NAME_JSON_KEY = "name";
    private static final String C_SURNAME_JSON_KEY = "surname";
    private static final String C_NUMBER_JSON_KEY = "cardnumber";

    private static JSONObject mJSONObjectName = new JSONObject();
    private static JSONObject mJSONObjectCard = new JSONObject();

    private JSONHelper() {}

    public static String parseToJSON(CardData cDate) {
        try {
            mJSONObjectCard.put(C_NAME_JSON_KEY, cDate.getName());
            mJSONObjectCard.put(C_SURNAME_JSON_KEY, cDate.getSurName());
            mJSONObjectCard.put(C_NUMBER_JSON_KEY, cDate.getCardNumber());

            mJSONObjectName.put(NAME_JSON_KEY, mJSONObjectCard);

            Log.d(TAG, mJSONObjectName.toString());
            return mJSONObjectName.toString();

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void sendJSONToServer() {
        //sending code here
    }
}
