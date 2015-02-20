package com.servicetitan.swiper.swiperapp;

import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;

/**
 * Created by Admin on 2/19/2015.
 */
public class SuccessfulActivity extends Activity {

    private static final long ACTIVITY_SHOW_DURATION = 3000;
    private static final long ACTIVITY_SHOW_STATE_CHECKER = 100;
    private static final String REMAIN_TIME_EXTRA = "remain_time_erxtra";

    private long remainTime;

    private CountDownTimer cdt;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_successful);

        if (savedInstanceState != null) {
            remainTime = savedInstanceState.getLong(REMAIN_TIME_EXTRA, ACTIVITY_SHOW_DURATION);
        } else {
            remainTime = ACTIVITY_SHOW_DURATION;
        }
    }
    @Override
    protected void onResume(){
        super.onResume();

        cdt = new CountDownTimer(remainTime, ACTIVITY_SHOW_STATE_CHECKER) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainTime = millisUntilFinished;
            }

            @Override
            public void onFinish() {
                finish();
            }
        };
        cdt.start();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putLong(REMAIN_TIME_EXTRA, remainTime);
    }

    @Override
    protected void onPause() {
        super.onPause();
        cdt.cancel();
    }

    @Override
    public void onBackPressed() {
        return;
    }
}
