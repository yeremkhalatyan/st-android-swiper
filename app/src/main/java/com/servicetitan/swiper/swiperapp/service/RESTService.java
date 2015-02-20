package com.servicetitan.swiper.swiperapp.service;

import com.servicetitan.swiper.swiperapp.model.CardData;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;

/**
 * Created by Admin on 2/13/2015.
 */
public class RESTService {

    private static final String SERVER_API = "http://192.168.0.107:3000/creditcard";
//    public static final String SERVICE_TITAN_APP = "http://192.168.0.104";
    public static final String SERVICE_TITAN_APP = "https://go.servicetitan.com/";

    private RESTService() {
    }

    public static int send(CardData cardData) throws IOException {

        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(SERVER_API);

        String json = JSONHelper.parseToJSON(cardData);

        StringEntity se = new StringEntity(json);
        httpPost.setEntity(se);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");

        HttpResponse httpResponse = httpclient.execute(httpPost);

        int result = httpResponse.getStatusLine().getStatusCode();

        return result;
    }
}
