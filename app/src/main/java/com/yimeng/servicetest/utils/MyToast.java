package com.yimeng.servicetest.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import android.widget.Toast;

/**
 * 静态吐司
 */
public class MyToast {

    private static Toast toast;

    /**
     * 在主线程toast一个内容
     *
     * @param content 内容
     */
    private static void showToast(String content) {
        long id = Thread.currentThread().getId();
        if (id != Looper.getMainLooper().getThread().getId()) {
            return;
        }
        if (toast == null) {
            toast = Toast.makeText(MyApp.getContext(), content, Toast.LENGTH_SHORT);
        } else {
            toast.setText(content);
        }
        toast.show();
    }

    /**
     * 显示调试信息
     */
    public static void showLog(Context context, String str) {
        str = String.format("app:%s,线程id:%s,%s", MyApp.getContext().getPackageName(), Thread.currentThread().getId(), str);
        if (context instanceof Activity) {
            showToast(str);
        }else {
            MyLog.i(context.toString(), str);
        }
    }

    /**
     * 显示调试信息
     */
    public static void showLog(Context context, int resId) {
        showLog(context, MyApp.getContext().getString(resId));
    }

}
