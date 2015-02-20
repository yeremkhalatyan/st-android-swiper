package com.servicetitan.swiper.swiperapp;

import android.app.Activity;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.servicetitan.swiper.swiperapp.model.CardData;
import com.servicetitan.swiper.swiperapp.service.JSONHelper;
import com.servicetitan.swiper.swiperapp.service.RESTService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import IDTech.MSR.XMLManager.StructConfigParameters;
import IDTech.MSR.uniMag.UniMagTools.uniMagReaderToolsMsg;
import IDTech.MSR.uniMag.UniMagTools.uniMagSDKTools;
import IDTech.MSR.uniMag.uniMagReader;
import IDTech.MSR.uniMag.uniMagReaderMsg;

public class SwipeActivity extends Activity implements uniMagReaderMsg, uniMagReaderToolsMsg {

    private static final int SWIPE_CARD_TIMEOUT_DURATION_MILLIS = 20000;
    private static final int SWIPE_CARD_INTERVAL = 1000;
    private static final int AFTER_DISCONNECT_INIT_READER_DELAY = 1000;
    private static final int SERVER_RESPONSE_OK = 200;

    private boolean isSendingFinished = true;
    private boolean isToConnect;
    private boolean isConnected;
    private boolean isSwipeStarted;

    private LinearLayout mSwiperStatusLayout;
    private ImageView mSwipeCardStatusImageView;
    private ProgressBar mTimeOutProgressBar;
    private TextView mSwipeCardStatusTextView;
    private Button mSwipeCardTryAgainButton;
    private Button mCancelButton;
    private String mStatusText;
    private int mStatusImageResID;

    private LinearLayout mCardDataLayout;
    private ProgressBar mSendingProgressBar;
    private TextView mSendingDataTextView;
    private TextView mPleaseWaitTextView;
    private ImageView mServerErrorImageView;
    private TextView mServerErrorTextView;
    private TextView mCardUserNameTextView;
    private TextView mCardNumberTextView;
    private Button mSendButton;

    private uniMagReader mUniMagReader;
    private String mCardDataStr;

    private CardData mCardData = new CardData();
    private Handler mHandler = new Handler();
    private HttpAsyncTask mHttpAsyncTask;

    private Runnable setSwiperStatusTextRunnable = new Runnable() {
        @Override
        public void run() {
            mSwipeCardStatusTextView.setText(mStatusText);
        }
    };
    private Runnable setServerStatusTextRunnable = new Runnable() {
        @Override
        public void run() {
            mServerErrorTextView.setText(mStatusText);
        }
    };
    private Runnable setSwiperStatusImageRunnable = new Runnable() {
        @Override
        public void run() {
            mSwipeCardStatusImageView.setImageResource(mStatusImageResID);
        }
    };
    private Runnable setServerStatusImageRunnable = new Runnable() {
        @Override
        public void run() {
            mServerErrorImageView.setImageResource(mStatusImageResID);
        }
    };

    private CountDownTimer mRemainingTimeSwipeDur = new CountDownTimer(SWIPE_CARD_TIMEOUT_DURATION_MILLIS, SWIPE_CARD_INTERVAL) {

        @Override
        public void onTick(long millisUntilFinished) {
            updateText(getString(R.string.swipe_card_dur_text) + " " +
                    millisUntilFinished / 1000 + " sec.", R.color.text_color);
            mTimeOutProgressBar.setVisibility(View.VISIBLE);
            mTimeOutProgressBar.setProgress((int) ((SWIPE_CARD_TIMEOUT_DURATION_MILLIS - millisUntilFinished) / 1000));
        }

        @Override
        public void onFinish() {
            updateText(getString(R.string.swipe_card_dur_text) +
                    " 0 sec.", R.color.text_color);
            mTimeOutProgressBar.setProgress((SWIPE_CARD_TIMEOUT_DURATION_MILLIS) / 1000);
        }
    };

    private Runnable initReaderRunnable = new Runnable() {
        @Override
        public void run() {
            initializeReader(uniMagReader.ReaderType.SHUTTLE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swipe);

        mSwiperStatusLayout = (LinearLayout)findViewById(R.id.swiperStatusLayout);
        mSwipeCardStatusImageView = (ImageView) findViewById(R.id.swipe_card_status_imageView);
        mTimeOutProgressBar = (ProgressBar) findViewById(R.id.timeoutProgressBar);
        mTimeOutProgressBar.setMax(SWIPE_CARD_TIMEOUT_DURATION_MILLIS / 1000);
        mSwipeCardStatusTextView = (TextView) findViewById(R.id.swipe_card_status_textView);
        mSwipeCardTryAgainButton = (Button) findViewById(R.id.swipe_card_try_again_Button);
        mSwipeCardTryAgainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initializeReader(uniMagReader.ReaderType.SHUTTLE);
                if (mUniMagReader.startSwipeCard()) {
                    mUniMagReader.setTimeoutOfSwipeCard(SWIPE_CARD_TIMEOUT_DURATION_MILLIS);
                }
            }
        });
        mCancelButton = (Button) findViewById(R.id.cancel_button);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mCardDataLayout = (LinearLayout) findViewById(R.id.cardDataLayout);
        mSendingProgressBar = (ProgressBar) findViewById(R.id.sendProgressBar);
        mSendingDataTextView = (TextView) findViewById(R.id.sending_data_textView);
        mPleaseWaitTextView = (TextView) findViewById(R.id.please_wait_textView);
        mServerErrorImageView = (ImageView) findViewById(R.id.server_error_imageView);
        mServerErrorTextView = (TextView) findViewById(R.id.server_error_textView);
        mCardUserNameTextView = (TextView) findViewById(R.id.card_user_name);
        mCardNumberTextView = (TextView) findViewById(R.id.card_number);
        mSendButton = (Button) findViewById(R.id.send_button);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ////// send
                if (isInternetConnected()) {
                    mSwipeCardStatusImageView.setVisibility(View.GONE);
                    mHttpAsyncTask = new HttpAsyncTask();
                    mHttpAsyncTask.execute();
                } else {
                    showMessage(getString(R.string.no_internet_connection));
                }
            }
        });

        initializeReader(uniMagReader.ReaderType.SHUTTLE);
    }

    @Override
    protected void onPause() {
        if (mUniMagReader != null)
            mUniMagReader.stopSwipeCard();
        super.onPause();
    }

    private void initializeReader(uniMagReader.ReaderType type) {
        if (mUniMagReader != null) {
            mUniMagReader.unregisterListen();
            mUniMagReader.release();
            mUniMagReader = null;
        }
        mUniMagReader = new uniMagReader(this, this, type);
        mUniMagReader.setVerboseLoggingEnable(true);


        String fileNameWithPath = null;
        try {
            fileNameWithPath = getXMLFileFromRaw("idt_unimagcfg_default.xml");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!isFileExist(fileNameWithPath)) {
            fileNameWithPath = null;
        }

        mUniMagReader.setXMLFileNameWithPath(fileNameWithPath);
//        mUniMagReader.loadingConfigurationXMLFile(true);
        new LoadXMLAsyncTask().execute();

        //Initializing SDKTool for firmware update
        uniMagSDKTools firmwareUpdateTool = new uniMagSDKTools(this, this);
        firmwareUpdateTool.setUniMagReader(mUniMagReader);
        mUniMagReader.setSDKToolProxy(firmwareUpdateTool.getSDKToolProxy());
    }

    private class LoadXMLAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void[] params) {
            mUniMagReader.loadingConfigurationXMLFile(true);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mUniMagReader.registerListen();
        }
    }
    private boolean isFileExist(String path) {
        if (path == null)
            return false;
        File file = new File(path);
        return file.exists();
    }

    private String getXMLFileFromRaw(String fileName) throws IOException {
        //the target filename in the application path
        String fileNameWithPath = fileName;
        InputStream in = null;
        FileOutputStream out = null;
        try {
            in = getResources().openRawResource(R.raw.idt_unimagcfg_default);
            int length = in.available();
            byte[] buffer = new byte[length];
            in.read(buffer);
            deleteFile(fileNameWithPath);
            out = openFileOutput(fileNameWithPath, MODE_PRIVATE);
            out.write(buffer);
            // to refer to the application path
            File fileDir = this.getFilesDir();

            fileNameWithPath = fileDir.getPath();//fileDir.getParent() + java.io.File.separator + fileDir.getName();
            fileNameWithPath += java.io.File.separator + fileName;

        } catch (Exception e) {
            e.printStackTrace();
            fileNameWithPath = null;
        } finally {
            if (null != in) {
                in.close();
            }
            if (null != out) {
                out.close();
            }
        }

        return fileNameWithPath;
    }

    private void showMessage(String str) {
        Toast.makeText(SwipeActivity.this, str, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        if (mUniMagReader != null)
            mUniMagReader.release();
        super.onDestroy();
    }

    public boolean isInternetConnected() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private void updateText(String statusText, int colorID) {
        mStatusText = statusText;
        mSwipeCardStatusTextView.setVisibility(View.VISIBLE);
        mSwipeCardStatusTextView.setTextColor(getResources().getColor(colorID));
        mHandler.post(setSwiperStatusTextRunnable);
    }

    private void updateServerText(String statusText) {
        mStatusText = statusText;
        mServerErrorImageView.setVisibility(View.VISIBLE);
        mHandler.post(setServerStatusTextRunnable);
    }

    private void updateImage(int resId) {
        mStatusImageResID = resId;
        mSwipeCardStatusImageView.setVisibility(View.VISIBLE);
        mHandler.post(setSwiperStatusImageRunnable);
    }

    private void updateImage(int resId, int left, int top, int right, int bottom) {
        LayoutParams lp = (LayoutParams) mServerErrorImageView.getLayoutParams();
        lp.setMargins(left, top, right, bottom);
        mServerErrorImageView.setLayoutParams(lp);
        mServerErrorImageView.setVisibility(View.VISIBLE);
        mStatusImageResID = resId;
        mHandler.post(setServerStatusImageRunnable);
    }

    private int isCardDataViewsVisible() {
        return mCardDataLayout.getVisibility();
    }

    private void showSwiperStatusViews() {
        mSwiperStatusLayout.setVisibility(View.VISIBLE);
    }
    private void hideSwiperStatusViews() {
        mSwiperStatusLayout.setVisibility(View.GONE);
    }

    private void showCardDataViews() {
        mCardDataLayout.setVisibility(View.VISIBLE);
        mSendButton.setEnabled(true);
        mServerErrorImageView.setVisibility(View.GONE);
        mSendingProgressBar.setVisibility(View.GONE);
        mSendingDataTextView.setVisibility(View.GONE);
        mPleaseWaitTextView.setVisibility(View.GONE);
        mServerErrorTextView.setVisibility(View.GONE);
    }
    private void hideCardDataViews() {
        mCardDataLayout.setVisibility(View.GONE);
    }

    @Override
    public void onReceiveMsgToConnect() {
        if (isSendingFinished) {
            isToConnect = true;
            hideCardDataViews();
            showSwiperStatusViews();
            updateText(getString(R.string.to_connect), R.color.text_color);
            updateImage(R.drawable.connect_swiper);
        }
    }

    @Override
    public void onReceiveMsgConnected() {

        if (isSendingFinished) {
            isConnected = true;
            if (mUniMagReader.startSwipeCard()) {
                mUniMagReader.setTimeoutOfSwipeCard(SWIPE_CARD_TIMEOUT_DURATION_MILLIS);
            }

            updateText(getString(R.string.connected), R.color.text_color);
        }
    }

    @Override
    public void onReceiveMsgDisconnected() {

        isToConnect = false;
        isConnected = false;
        isSwipeStarted = false;

        if (mRemainingTimeSwipeDur != null)
            mRemainingTimeSwipeDur.cancel();
        if (isSendingFinished) {
            if (isCardDataViewsVisible() == View.VISIBLE) {
                hideCardDataViews();
                showSwiperStatusViews();
            }
            updateText(getString(R.string.connect_swiper), R.color.text_color);
            updateImage(R.drawable.connect_swiper);
            mTimeOutProgressBar.setVisibility(View.GONE);
            mSwipeCardTryAgainButton.setVisibility(View.GONE);
        }
        mHandler.postDelayed(initReaderRunnable, AFTER_DISCONNECT_INIT_READER_DELAY);
    }

    @Override
    public void onReceiveMsgTimeout(String strTimeoutMsg) {
        if (isToConnect && isConnected && isSwipeStarted) {
            //swipe card timeout
            mTimeOutProgressBar.setVisibility(View.GONE);
            updateText(getString(R.string.swipe_card_not_swiped_timeout_text), R.color.text_error_color);
            updateImage(R.drawable.swipe_card_error);
            mSwipeCardTryAgainButton.setVisibility(View.VISIBLE);
            if (mRemainingTimeSwipeDur != null)
                mRemainingTimeSwipeDur.cancel();

        } else if (isToConnect) {
            //swipe reader connect timeout
            updateText(getString(R.string.swipe_card_not_connected_timeout_text), R.color.text_error_color);
            updateImage(R.drawable.connect_swiper_error);
        }
    }

    @Override
    public void onReceiveMsgToSwipeCard() {
        mRemainingTimeSwipeDur.start();
        updateImage(R.drawable.swipe_card);
        mSwipeCardTryAgainButton.setVisibility(View.GONE);
        mTimeOutProgressBar.setVisibility(View.GONE);
        isSwipeStarted = true;
    }

    @Override
    public void onReceiveMsgCommandResult(int commandID, byte[] cmdReturn) {
    }

    @Override
    public void onReceiveMsgProcessingCardData() {
        updateText(getString(R.string.swipe_card_processing_text), R.color.text_color);
        if (mRemainingTimeSwipeDur != null) {
            mRemainingTimeSwipeDur.cancel();
        }
        mTimeOutProgressBar.setVisibility(View.GONE);
        mCancelButton.setText(R.string.please_wait_text);
        mCancelButton.setEnabled(false);
    }

    @Override
    public void onReceiveMsgCardData(byte flagOfCardData, byte[] cardData) {
        boolean isNumberOk = false;
        boolean isNameSurNameOk = false;
        if (cardData.length > 5)
            if (cardData[0] == 0x25 && cardData[1] == 0x45) {
                updateText(getString(R.string.swipe_card_error), R.color.text_error_color);
                mSwipeCardTryAgainButton.setVisibility(View.VISIBLE);
                if (mRemainingTimeSwipeDur != null)
                    mRemainingTimeSwipeDur.cancel();
                updateImage(R.drawable.swipe_card_error);
                return;
            }

        byte flag = (byte) (flagOfCardData & 0x04);
        if (flag == 0x00 || flag == 0x04)
            mCardDataStr = new String(cardData);

        Pattern pattern = Pattern.compile("\\d{4}\\*{8}\\d{4}");
        Matcher matcher = pattern.matcher(mCardDataStr);
        String number;
        if (matcher.find()) {
            number = matcher.group(0);
            mCardData.setCardNumber(number);
            isNumberOk = true;
        }

        pattern = Pattern.compile("[A-Z]+/[A-Z]+");
        matcher = pattern.matcher(mCardDataStr);
        String name_surname;
        if (matcher.find()) {
            name_surname = matcher.group(0);
            String[] name_surname_arr = name_surname.split("/");
            mCardData.setName(name_surname_arr[0]);
            mCardData.setSurName(name_surname_arr[1]);

            JSONHelper.parseToJSON(mCardData);
            isNameSurNameOk = true;
        }
//Card data is received properly
        if (isNumberOk && isNameSurNameOk) {

            //updateText(getString(R.string.swipe_card_data_received_ok_text));
            hideSwiperStatusViews();
            showCardDataViews();

            Runnable setUserNameRunnable = new Runnable() {
                @Override
                public void run() {
                    mCardUserNameTextView.setText("Name: " + mCardData.getName()
                            + " " + mCardData.getSurName());
                }
            };
            mHandler.post(setUserNameRunnable);
            Runnable setNumberRunnable = new Runnable() {
                @Override
                public void run() {
                    mCardNumberTextView.setText("Card Number: " + mCardData.getCardNumber());
                }
            };
            mHandler.post(setNumberRunnable);

        } else {
            updateText(getString(R.string.swipe_card_data_received_cancel_text), R.color.text_error_color);
            updateImage(R.drawable.swipe_card_error);
            mSwipeCardTryAgainButton.setVisibility(View.VISIBLE);
        }

        mCancelButton.setText(R.string.cancel_button);
        mCancelButton.setEnabled(true);
    }

    @Override
    public void onReceiveMsgToCalibrateReader() {
    }

    @Override
    public void onReceiveMsgSDCardDFailed(String strMSRData) {

    }

    @Override
    public void onReceiveMsgFailureInfo(int index, String strMessage) {

    }

    @Override
    public void onReceiveMsgAutoConfigProgress(int progressValue) {

    }

    @Override
    public void onReceiveMsgAutoConfigProgress(int percent, double result, String profileName) {

    }

    @Override
    public void onReceiveMsgAutoConfigCompleted(StructConfigParameters profile) {

    }

    @Override
    public boolean getUserGrant(int nType, String strMessage) {
        boolean getUserGranted;
        switch (nType) {
            case uniMagReaderMsg.typeToPowerupUniMag:
                //pop up dialog to get the user grant
                getUserGranted = true;
                break;
            case uniMagReaderMsg.typeToUpdateXML:
                //pop up dialog to get the user grant
                getUserGranted = true;
                break;
            case uniMagReaderMsg.typeToOverwriteXML:
                //pop up dialog to get the user grant
                getUserGranted = true;
                break;
            case uniMagReaderMsg.typeToReportToIdtech:
                //pop up dialog to get the user grant
                getUserGranted = true;
                break;
            default:
                getUserGranted = false;
                break;
        }
        return getUserGranted;
    }


    @Override
    public void onReceiveMsgUpdateFirmwareProgress(int i) {

    }

    @Override
    public void onReceiveMsgUpdateFirmwareResult(int i) {

    }

    @Override
    public void onReceiveMsgChallengeResult(int i, byte[] bytes) {

    }

    private class HttpAsyncTask extends AsyncTask<String, Void, Integer> {

        private void changeSendingViwsVisibility(int visibility) {
            mSendingProgressBar.setVisibility(visibility);
            mSendingDataTextView.setVisibility(visibility);
            mPleaseWaitTextView.setVisibility(visibility);
        }

        @Override
        protected void onPreExecute() {
            changeSendingViwsVisibility(View.VISIBLE);
            mServerErrorImageView.setVisibility(View.GONE);
            mServerErrorTextView.setVisibility(View.GONE);
            mSendButton.setEnabled(false);
            mCancelButton.setEnabled(false);
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(String... args) {
            isSendingFinished = false;
            int result = 0;
            try {
                result =  RESTService.send(mCardData);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(Integer result) {
            if (result == SERVER_RESPONSE_OK) {
                //showMessage(getString(R.string.request_ok));
                startActivity(new Intent(SwipeActivity.this, SuccessfulActivity.class));
                finish();
            } else {
                updateServerText(getString(R.string.server_temp_unavailable));
                updateImage(R.drawable.server_error_icon, 0, 20, 0, 0);
                mServerErrorImageView.setVisibility(View.VISIBLE);
                mServerErrorTextView.setVisibility(View.VISIBLE);
                changeSendingViwsVisibility(View.GONE);
                mSendButton.setEnabled(true);
                mCancelButton.setEnabled(true);
            }

            isSendingFinished = true;
        }
    }
}
