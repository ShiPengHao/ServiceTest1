package com.yimeng.servicetest.utils;

import android.app.Application;
import android.content.Context;

/**
 * Created by 依萌 on 2017/3/21.
 */

public class MyApp extends Application {

    private static MyApp context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
    }

    public static Context getContext(){
        return context;
    }
}
