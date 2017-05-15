package com.yimeng.servicetest.utils;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

/**
 * 键盘
 */
public class KeyBoardUtils {

    private static final InputMethodManager IMM = (InputMethodManager) MyApp.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

    /**
     * 打卡软键盘
     *
     * @param view     一个控件引用
     */
    public static void openKeyboard(View view) {
        IMM.showSoftInput(view, InputMethodManager.RESULT_SHOWN);
        IMM.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }


    /**
     * 关闭软键盘
     *
     * @param view     一个控件引用
     */
    public static void closeKeyboard(View view) {
        IMM.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}

