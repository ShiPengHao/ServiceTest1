package com.yimeng.servicetest.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;

import com.yimeng.servicetest.utils.MyApp;
import com.yimeng.servicetest.utils.MyToast;

/**
 * Created by 依萌 on 2017/3/21.
 */

public class CountIntentService extends IntentService {

    public static final String DEFAULT_NAME = "CountIntentService";
    private boolean runningFlag;
    private LocalBroadcastManager mLocalBroadcastManager;

    public CountIntentService() {
        super(DEFAULT_NAME);
    }

    public CountIntentService(String name) {
        super(name);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        runningFlag = true;
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(MyApp.getContext());
        MyToast.showLog(this, "onCreate");
        // ts
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        runningFlag = false;
        mLocalBroadcastManager.sendBroadcast(new Intent(DEFAULT_NAME));
        MyToast.showLog(this, "onDestroy");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // 模拟操作：计数10次
        int count = 0;
        int MAX_COUNT = 10;
        do {
            SystemClock.sleep(1000);
            if (runningFlag) {
                MyToast.showLog(this, String.format("%s秒后我将自杀", MAX_COUNT - count++));
            }
        } while (runningFlag && count < MAX_COUNT);
    }
}
