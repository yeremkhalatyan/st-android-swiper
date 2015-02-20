package com.servicetitan.swiper.swiperapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.servicetitan.swiper.swiperapp.service.RESTService;

import java.util.List;


public class StartScreenActivity extends ActionBarActivity {

    private Button mStartSwiperStartAppButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_screen);

        mStartSwiperStartAppButton = (Button)findViewById(R.id.openServiceTitan);
        mStartSwiperStartAppButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(RESTService.SERVICE_TITAN_APP));

                PackageManager packageManager = getPackageManager();
                List activities = packageManager.queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
                boolean isIntentSafe = activities.size() > 0;
                if (isIntentSafe)
                    startActivity(intent);
                else {
                    showMessage(R.string.servic_titan_unavailable);
                }
                finish();
            }
        });
    }

    @Override
    public void onDestroy () {
        super.onDestroy();
    }

    private void showMessage(int resID) {
        Toast.makeText(StartScreenActivity.this, resID, Toast.LENGTH_SHORT).show();
    }
}
