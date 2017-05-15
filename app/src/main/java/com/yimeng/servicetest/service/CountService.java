package com.yimeng.servicetest.service;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.Nullable;

import com.yimeng.servicetest.activity.MainActivity;
import com.yimeng.servicetest.utils.MyToast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 计数服务，提供如下几种服务
 * 1.通过binder调用接口函数实现与客户端双向通信，场景是模拟启动一个计数器的启动、暂停、继续、停止操作，bind时自动启动，比如音乐播放器
 * 2.通过messenger的消息队列实现与客户端双向通信，场景是简单的对同一个内存变量进行操作，然后把变化回传所有需要监听的客户端
 * 3.start服务时，启动binder实现中的计数器
 * 4.通过aidl调用接口函数实现与客户端双向通信，场景是简单的对同一个内存变量进行操作，然后把变化回传所有注册监听的客户端
 */

public class CountService extends Service {

    private boolean runningFlag = false;

    private Messenger mMessenger;
    private MyCountBinder mBinder;
    private MyCountHandler mCountHandler;
    private MyABinder mABinder;

    /**
     * 供方式1和3使用的接口，客户端可以控制和监听计数过程（含结束事件）
     */
    private class MyCountBinder extends Binder implements ICounter {
        private int aiCounter = 20;
        private boolean pauseFlag = false;
        /**
         * 维护所有的监听器
         */
        private ArrayList<OnCountListener> listeners = new ArrayList<>();

        /**
         * 开始计数
         */
        public void startCount() {
            MyToast.showLog(CountService.this, "startCount");
            if (runningFlag) {
                return;
            }
            runningFlag = true;
            new Thread() {
                @Override
                public void run() {
                    do {
                        if (pauseFlag) {
                            continue;
                        }
                        SystemClock.sleep(1000);
                        if (pauseFlag) {
                            continue;
                        }
                        if (runningFlag) {
                            int temp = aiCounter--;
                            MyToast.showLog(CountService.this, String.format("自杀倒计时:%s", temp));
                            for (int i = 0; i < listeners.size(); i++) {
                                listeners.get(i).onCount(temp);
                            }
                        }
                    } while (runningFlag && aiCounter > 0);
                    stopCount(aiCounter == 0);
                }
            }.start();

        }

        public void stopCount(boolean bySelf) {
            String str = "stopCount";
            if (bySelf) {
                stopSelf();
                str += "bySelf";
            }
            MyToast.showLog(CountService.this, str);
            if (bySelf) {
                for (int i = 0; i < listeners.size(); i++) {
                    listeners.get(i).onOver();
                }
            }
        }

        public void pauseCount() {
            MyToast.showLog(CountService.this, "pauseCount");
            pauseFlag = true;
        }

        public void continueCount() {
            MyToast.showLog(CountService.this, "continueCount");
            pauseFlag = false;
        }

        @Override
        public void registerCountListener(OnCountListener onCountListener) {
            listeners.add(onCountListener);
        }
    }

    /**
     * 供方式二使用的handler，客户端可以注册、反注册数据变化监听
     */
    private class MyCountHandler extends Handler {
        private int aiCounter = 0;
        private HashSet<Messenger> mClientMessengers = new HashSet<>();
        private Bundle mBundle = new Bundle();

        MyCountHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MainActivity.WHAT_ADD_CLIENT:
                    mClientMessengers.add(msg.replyTo);
                    break;
                case MainActivity.WHAT_REMOVE_CLIENT:
                    mClientMessengers.remove(msg.replyTo);
                    break;
                case MainActivity.WHAT_COUNT:
                    aiCounter++;
                    notifyChange();
                    break;
            }
        }

        /**
         * 通知需要双向通信的客户端数据已更新，注意，这里传递的数据必须实现Parcelable接口或者以bundle的形式放入data中，因为bundle已实现此接口
         */
        private void notifyChange() {
            MyToast.showLog(CountService.this, "MyCountHandler:notifyChange");
            for (Messenger mClientMessenger : mClientMessengers) {
                Message message = Message.obtain();
                message.what = MainActivity.WHAT_FRESH;
                mBundle.putInt("number", aiCounter);
                message.setData(mBundle);
                try {
                    mClientMessenger.send(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 供方式3使用的ipc binder，客户端可以注册、反注册数据变化监听，必须处理线程安全问题
     */
    private class MyABinder extends CountAIDL.Stub {
        // 使用线程安全的数据类型
        private AtomicInteger atomicInteger = new AtomicInteger(0);
        // 使用线程安全的远程回调监听，泛型是IInterface的子类，也就是说，也必须是aidl接口而不是普通接口
        private RemoteCallbackList<OnChangeListenerAIDL> callbackList = new RemoteCallbackList<>();

        @Override
        public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString) throws RemoteException {

        }

        @Override
        public void add() throws RemoteException {
            MyToast.showLog(CountService.this, "CountAIDL:add");
            int newValue = atomicInteger.addAndGet(1);
            int i = callbackList.beginBroadcast();
            while (i > 0) {
                i--;
                callbackList.getBroadcastItem(i).onChange(newValue);
            }
            callbackList.finishBroadcast();
        }

        @Override
        public void addListener(OnChangeListenerAIDL listener) throws RemoteException {
            MyToast.showLog(CountService.this, "addListener");
            callbackList.register(listener);
        }

        @Override
        public void removeListener(OnChangeListenerAIDL listener) throws RemoteException {
            MyToast.showLog(CountService.this, "removeListener");
            callbackList.unregister(listener);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        MyToast.showLog(this, "onBind");
        // 验证权限
        if (intent == null || checkCallingOrSelfPermission("com.yimeng.servicetest.service.CountIntentService") == PackageManager.PERMISSION_DENIED) {
            return null;
        }
        // 获取意图类型，分别提供不同的接口服务
        int type = intent.getIntExtra(MainActivity.TYPE_INTENT, MainActivity.TYPE_BINDER);
        switch (type) {
            case MainActivity.TYPE_BINDER:// 单进程binder服务
                mBinder.startCount();
                return mBinder;
            case MainActivity.TYPE_MESSENGER:// ipc messenger
                return mMessenger.getBinder();
            case MainActivity.TYPE_AIDL:// ipc aidl
                return mABinder;
        }
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        MyToast.showLog(this, "onUnbind");
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MyToast.showLog(this, "onStartCommand");
        mBinder.startCount();// start方式启动服务，默认使用单进程binder方式，也可以什么都不做
        return START_STICKY;
    }

    /**
     * 启动时将这几种服务方式全部准备好
     */
    @Override
    public void onCreate() {
        MyToast.showLog(this, "onCreate");
        mBinder = new MyCountBinder();
        mCountHandler = new MyCountHandler(Looper.myLooper());
        mMessenger = new Messenger(mCountHandler);
        mABinder = new MyABinder();
    }

    @Override
    public void onDestroy() {
        MyToast.showLog(this, "onDestroy");
        // 释放资源
        runningFlag = false;// 停止binder实现中任务线程的run方法
        mCountHandler.removeCallbacksAndMessages(null);// 清空messenger实现时的handler消息
        mABinder.callbackList.kill();// 禁用aidl回调
    }

}
