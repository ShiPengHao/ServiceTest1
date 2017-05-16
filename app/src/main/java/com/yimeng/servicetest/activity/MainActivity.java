package com.yimeng.servicetest.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.yimeng.servicetest.R;
import com.yimeng.servicetest.service.CountAIDL;
import com.yimeng.servicetest.service.CountIntentService;
import com.yimeng.servicetest.service.CountService;
import com.yimeng.servicetest.service.ICounter;
import com.yimeng.servicetest.service.OnChangeListenerAIDL;
import com.yimeng.servicetest.utils.MyApp;
import com.yimeng.servicetest.utils.MyToast;

/**
 * 测试CountService的几种服务方式
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TYPE_INTENT = "TYPE_INTENT";
    public static final int TYPE_BINDER = 100;
    public static final int TYPE_MESSENGER = 101;
    public static final int TYPE_AIDL = 102;

    public static final int WHAT_COUNT = 100;
    public static final int WHAT_FRESH = 101;
    public static final int WHAT_REMOVE_CLIENT = 102;
    public static final int WHAT_ADD_CLIENT = 103;

    private TextView tv_is;
    private Intent mIntentServiceIntent;
    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mIntentServiceReceiver;
    private IntentFilter mIntentServiceFilter;
    private TextView tv_s;
    private TextView tv_bs;
    private TextView tv_bs_pause;
    private TextView tv_bs_messenger;
    private TextView tv_bs_messenger_count;
    private TextView tv_bs_aidl;
    private TextView tv_bs_aidl_count;
    private Intent mBinderIntent;
    private Intent mMessengerIntent;
    private Intent mAIDLIntent;
    private ServiceConnection mBinderConn;
    private ServiceConnection mMessengerConn;
    private ServiceConnection mAIDLConn;
    private ICounter mServiceCounter;
    private Messenger mMessengerService;
    private CountAIDL mAIDLService;
    private Messenger mMessengerClient;
    private Handler mHandler;
    private OnChangeListenerAIDL mChangeListenerAIDL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //test push
        initView();
        initData();
        setListener();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 停止服务
        stopIntentService();
        stopCountService();
        // 取消监听
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * 初始化view
     */
    private void initView() {
        tv_is = (TextView) findViewById(R.id.tv_is);
        tv_s = (TextView) findViewById(R.id.tv_s);
        tv_bs = (TextView) findViewById(R.id.tv_bs);
        tv_bs_pause = (TextView) findViewById(R.id.tv_bs_pause);
        tv_bs_messenger = (TextView) findViewById(R.id.tv_bs_messenger);
        tv_bs_messenger_count = (TextView) findViewById(R.id.tv_bs_messenger_count);
        tv_bs_aidl = (TextView) findViewById(R.id.tv_bs_aidl);
        tv_bs_aidl_count = (TextView) findViewById(R.id.tv_bs_aidl_count);
    }

    /**
     * 准备数据
     */
    private void initData() {
        // IntentService
        mIntentServiceIntent = new Intent(MyApp.getContext(), CountIntentService.class);
        // binder
        mBinderIntent = new Intent(MyApp.getContext(), CountService.class).putExtra(TYPE_INTENT, TYPE_BINDER);
        // messenger
        mMessengerIntent = new Intent("com.yimeng.servicetest.service.CountIntentService").putExtra(TYPE_INTENT, TYPE_MESSENGER);
        // aidl
        mAIDLIntent = new Intent("com.yimeng.servicetest.service.CountIntentService").putExtra(TYPE_INTENT, TYPE_AIDL);
    }

    /**
     * 设置监听
     */
    private void setListener() {
        tv_is.setOnClickListener(this);
        tv_s.setOnClickListener(this);
        tv_bs.setOnClickListener(this);
        tv_bs_pause.setOnClickListener(this);
        tv_bs_messenger.setOnClickListener(this);
        tv_bs_messenger_count.setOnClickListener(this);
        tv_bs_aidl.setOnClickListener(this);
        tv_bs_aidl_count.setOnClickListener(this);
        // 以注册单进程广播的方式监听IntentService的任务结束事件
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(MyApp.getContext());
        mIntentServiceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                tv_is.setText(R.string.intent_service_start);
                mLocalBroadcastManager.unregisterReceiver(mIntentServiceReceiver);
            }
        };
        mIntentServiceFilter = new IntentFilter(CountIntentService.DEFAULT_NAME);
        // 为messenger方式提供一个客户端messenger用于接收服务数据
        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case WHAT_FRESH:// 服务维护的数据有更新
                        MyToast.showLog(MainActivity.this, String.format("新值是:%s", msg.getData().getInt("number")));
                        return true;
                }
                return false;
            }
        });
        mMessengerClient = new Messenger(mHandler);
        // 为aidl方式提供一个监听，接收服务数据
        mChangeListenerAIDL = new OnChangeListenerAIDL.Stub() {
            @Override
            public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString) throws RemoteException {

            }

            @Override
            public void onChange(int number) throws RemoteException {
                MyToast.showLog(MainActivity.this, "onchange:" + number);
            }
        };
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_is:
                String str = tv_is.getText().toString();
                if (getString(R.string.intent_service_start).equalsIgnoreCase(str)) {
                    startIntentService();
                } else if (getString(R.string.intent_service_stop).equalsIgnoreCase(str)) {
                    stopIntentService();
                }
                break;

            case R.id.tv_s:
                str = tv_s.getText().toString();
                if (getString(R.string.service_start).equalsIgnoreCase(str)) {
                    startCountService();
                } else if (getString(R.string.service_stop).equalsIgnoreCase(str)) {
                    stopCountService();
                }
                break;

            case R.id.tv_bs:
                str = tv_bs.getText().toString();
                if (getString(R.string.bind_service).equalsIgnoreCase(str)) {
                    bindServiceBinder();
                } else if (getString(R.string.unbind_service).equalsIgnoreCase(str)) {
                    unbindServiceBinder();
                }
                break;

            case R.id.tv_bs_pause:
                if (mServiceCounter == null) {
                    MyToast.showLog(this, R.string.service_not_bound);
                    return;
                }
                str = tv_bs_pause.getText().toString();
                if (getString(R.string.count_pause).equalsIgnoreCase(str)) {
                    pauseCountBinder();
                } else if (getString(R.string.count_continue).equalsIgnoreCase(str)) {
                    continueCountBinder();
                }
                break;

            case R.id.tv_bs_messenger:
                str = tv_bs_messenger.getText().toString();
                if (getString(R.string.bind_service_messenger).equalsIgnoreCase(str)) {
                    bindServiceMessenger();
                } else if (getString(R.string.unbind_service_messenger).equalsIgnoreCase(str)) {
                    unbindServiceMessenger();
                }
                break;

            case R.id.tv_bs_messenger_count:
                countMessenger();
                break;

            case R.id.tv_bs_aidl:
                str = tv_bs_aidl.getText().toString();
                if (getString(R.string.bind_service_aidl).equalsIgnoreCase(str)) {
                    bindServiceAIDL();
                } else if (getString(R.string.unbind_service_aidl).equalsIgnoreCase(str)) {
                    unbindServiceAIDL();
                }
                break;

            case R.id.tv_bs_aidl_count:
                countAIDL();
                break;
        }
    }

    private boolean threadSwitch;

    /**
     * 计数+1
     */
    private void countAIDL() {
        if (mAIDLService == null) {
            MyToast.showLog(this, R.string.service_not_bound);
            return;
        }
        if (threadSwitch) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        mAIDLService.add();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        } else {
            try {
                mAIDLService.add();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        threadSwitch = !threadSwitch;
    }

    /**
     * 以messenger的方式绑定CountService
     */
    private void bindServiceAIDL() {
        if (mAIDLConn == null) {
            mAIDLConn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, final IBinder iBinder) {
                    if (iBinder == null) {
                        unbindServiceAIDL();
                        return;
                    }
                    MyToast.showLog(MainActivity.this, "onServiceConnected");
                    mAIDLService = CountAIDL.Stub.asInterface(iBinder);
                    try {
                        // 监听远程连接，挂掉，则解绑服务，刷新界面
                        iBinder.linkToDeath(new IBinder.DeathRecipient() {
                            @Override
                            public void binderDied() {
                                MyToast.showLog(MainActivity.this, "binderDied");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        unbindServiceAIDL();
                                    }
                                });
                            }
                        }, 0);
                        // 添加监听
                        mAIDLService.addListener(mChangeListenerAIDL);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                }
            };
        }
        if (bindService(mAIDLIntent, mAIDLConn, BIND_AUTO_CREATE)) {
            tv_bs_aidl.setText(R.string.unbind_service_aidl);
            tv_bs.setEnabled(false);
            tv_bs_messenger.setEnabled(false);
        }
    }

    /**
     * 解绑aidl service，重置aidl引用和连接对象
     */
    private void unbindServiceAIDL() {
        if (mAIDLService != null && mAIDLService.asBinder().pingBinder()) {
            try {
                mAIDLService.removeListener(mChangeListenerAIDL);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        if (mAIDLConn != null) {
            unbindService(mAIDLConn);
        }
        mAIDLService = null;
        mAIDLConn = null;
        tv_bs_aidl.setText(R.string.bind_service_aidl);
        tv_bs.setEnabled(true);
        tv_bs_messenger.setEnabled(true);
    }

    /**
     * 计数+1
     */
    private void countMessenger() {
        if (mMessengerService == null) {
            MyToast.showLog(this, R.string.service_not_bound);
            return;
        }
        Message message = Message.obtain();
        message.what = WHAT_COUNT;
        try {
            mMessengerService.send(message);
        } catch (RemoteException e) {
            MyToast.showLog(this, "RemoteException");
            e.printStackTrace();
        }
    }

    /**
     * 以messenger的方式绑定CountService
     */
    private void bindServiceMessenger() {
        if (mMessengerConn == null) {
            mMessengerConn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    if (iBinder == null) {
                        unbindServiceMessenger();
                        return;
                    }
                    MyToast.showLog(MainActivity.this, "onServiceConnected");
                    mMessengerService = new Messenger(iBinder);
                    try {
                        // 监听远程连接，挂掉，则解绑服务，刷新界面
                        iBinder.linkToDeath(new IBinder.DeathRecipient() {
                            @Override
                            public void binderDied() {
                                MyToast.showLog(MainActivity.this, "binderDied");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        unbindServiceMessenger();
                                    }
                                });
                            }
                        }, 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    // 注册监听
                    Message message = Message.obtain();
                    message.what = WHAT_ADD_CLIENT;
                    message.replyTo = mMessengerClient;
                    try {
                        mMessengerService.send(message);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                }
            };
        }
        if (bindService(mMessengerIntent, mMessengerConn, BIND_AUTO_CREATE)) {
            tv_bs_messenger.setText(R.string.unbind_service_messenger);
            tv_bs.setEnabled(false);
            tv_bs_aidl.setEnabled(false);
        }
    }

    /**
     * 解绑messenger service，重置messenger引用和连接对象
     */
    private void unbindServiceMessenger() {
        if (mMessengerService != null && mMessengerService.getBinder().pingBinder()) {
            Message message = Message.obtain();
            message.what = WHAT_REMOVE_CLIENT;
            message.replyTo = mMessengerClient;
            try {
                mMessengerService.send(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mMessengerService = null;
        if (mMessengerConn != null) {
            unbindService(mMessengerConn);
            mMessengerConn = null;
        }
        tv_bs_messenger.setText(R.string.bind_service_messenger);
        tv_bs.setEnabled(true);
        tv_bs_aidl.setEnabled(true);
    }

    /**
     * 暂停计数
     */
    private void pauseCountBinder() {
        mServiceCounter.pauseCount();
        tv_bs_pause.setText(R.string.count_continue);
    }

    /**
     * 继续计数
     */
    private void continueCountBinder() {
        mServiceCounter.continueCount();
        tv_bs_pause.setText(R.string.count_pause);
    }

    /**
     * 以binder的通信方式绑定CountService，设置监听接口，用来接收计数任务数据和任务结束信息
     */
    private void bindServiceBinder() {
        if (mBinderConn == null) {
            mBinderConn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    if (iBinder == null) {
                        unbindServiceBinder();
                        return;
                    }
                    MyToast.showLog(MainActivity.this, "onServiceConnected");
                    mServiceCounter = (ICounter) iBinder;
                    // 注册监听
                    mServiceCounter.registerCountListener(new ICounter.OnCountListener() {
                        @Override
                        public void onOver() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    MyToast.showLog(MainActivity.this, "onOver:计数任务结束");
                                    unbindServiceBinder();
                                }
                            });
                        }

                        @Override
                        public void onCount(final int number) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    MyToast.showLog(MainActivity.this, String.format("自杀倒计时:%s", number));
                                }
                            });
                        }
                    });
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                    MyToast.showLog(MainActivity.this, "onServiceDisconnected");
                }
            };
        }
        if (bindService(mBinderIntent, mBinderConn, BIND_AUTO_CREATE)) {
            tv_bs.setText(R.string.unbind_service);
            tv_bs_messenger.setEnabled(false);
            tv_bs_aidl.setEnabled(false);
        }
    }

    /**
     * 解绑service，重置binder引用和连接对象
     */
    private void unbindServiceBinder() {
        if (mBinderConn != null) {
            unbindService(mBinderConn);
        }
        mBinderConn = null;
        mServiceCounter = null;
        tv_bs.setText(R.string.bind_service);
        tv_bs_pause.setText(R.string.count_pause);
        tv_bs_messenger.setEnabled(true);
        tv_bs_aidl.setEnabled(true);
    }

    /**
     * 开启普通service
     */
    private void startCountService() {
        startService(mBinderIntent);
        tv_s.setText(R.string.service_stop);
    }

    /**
     * 停止普通service，这里是解绑所有自己的绑定之后尝试停止服务，感觉无卵用，画蛇添足
     */
    private void stopCountService() {
        unbindServiceBinder();
        unbindServiceMessenger();
        unbindServiceAIDL();
        if (stopService(mBinderIntent)) {
            tv_s.setText(R.string.service_start);
        }
    }


    /**
     * 启动intent service
     */
    private void startIntentService() {
        mLocalBroadcastManager.registerReceiver(mIntentServiceReceiver, mIntentServiceFilter);
        if (null != startService(mIntentServiceIntent)) {
            tv_is.setText(R.string.intent_service_stop);
        }
    }

    /**
     * 停止intent service
     */
    private void stopIntentService() {
        if (mIntentServiceReceiver != null) {
            mLocalBroadcastManager.unregisterReceiver(mIntentServiceReceiver);
        }
        if (mIntentServiceIntent != null) {
            if (stopService(mIntentServiceIntent)) {
                tv_is.setText(R.string.intent_service_start);
            }
        }
    }
}
