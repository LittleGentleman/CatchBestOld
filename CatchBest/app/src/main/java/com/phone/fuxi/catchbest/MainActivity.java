package com.phone.fuxi.catchbest;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.util.LruCache;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.callback.Callback;
import com.callback.DialogButtonEnum;
import com.callback.PermissionUsage;
import com.callback.QuestPermissionListener;
import com.catchbest.CameraBean;
import com.catchbest.KSJ_BAYERMODE;
import com.catchbest.KSJ_TRIGGRMODE;
import com.catchbest.KSJ_WB_MODE;
import com.catchbest.cam;
import com.drawer.Resourceble;
import com.drawer.SlideMenuItem;
import com.drawer.ViewAnimator;
import com.util.JsonUtil;
import com.util.SharedPreferencesManager;
import com.view.BayerModePopupwindow;
import com.view.ConfirmDialog;
import com.view.ExposurePopupwindow;
import com.view.FieldDialog;
import com.view.GainPopupwindow;
import com.view.MenuButton;
import com.view.SwitchButton;
import com.view.TriggerPopupwindow;
import com.view.WhiteBalancePopupwindow;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.catchbest.KSJ_PARAM.KSJ_BLUE;
import static com.catchbest.KSJ_PARAM.KSJ_EXPOSURE;
import static com.catchbest.KSJ_PARAM.KSJ_GREEN;
import static com.catchbest.KSJ_PARAM.KSJ_MIRROR;
import static com.catchbest.KSJ_PARAM.KSJ_RED;


public class MainActivity extends BaseActivity implements SurfaceHolder.Callback, View.OnClickListener
        , GainPopupwindow.RedGainChangeListener
        , GainPopupwindow.GreenGainChangeListener
        , GainPopupwindow.BlueGainChangeListener
        , ExposurePopupwindow.ExposureChangeListener
        , WhiteBalancePopupwindow.WhiteBalanceSelectListener
        , TriggerPopupwindow.TriggerModeListener
        , ViewAnimator.ViewAnimatorListener
        , BayerModePopupwindow.BayerModeListener {

    private SurfaceView surfaceView;
    private SwitchButton sb_switch;
    private MenuButton btn_menu;
    private TextView tv_fps;

    private DrawerLayout drawerLayout;
    private LinearLayout linearLayout;
    private ActionBarDrawerToggle drawerToggle;
    private ViewAnimator viewAnimator;
    private List<SlideMenuItem> list = new ArrayList<>();

    private GainPopupwindow gainPopup;
    private ExposurePopupwindow exposurePopup;
    private WhiteBalancePopupwindow whiteBalancePopupwindow;
    private TriggerPopupwindow triggerPopupwindow;
    private BayerModePopupwindow bayerModePopupwindow;
    private FieldDialog fieldDialog;

    //    private int bayerMode = 12;//10,11(倒像)，12，13,14,15
    private int currentWB = KSJ_WB_MODE.KSJ_SWB_AUTO_ONCE.ordinal();
    private int currentTrigger = KSJ_TRIGGRMODE.KSJ_TRIGGER_INTERNAL.ordinal();
    private int currentBayer = KSJ_BAYERMODE.KSJ_BGGR_BGR32_FLIP.ordinal();

    private boolean isMirror = false;
    private boolean isLut = true;


    private SurfaceHolder m_PreviewHolder;

    private CameraBean surfaceCamera;
    private String json;

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public int m_nwidth;
    public int m_nheight;
    public int m_x;
    public int m_y;


    public ImageView[] m_imgvw;

    cam ksjcam;
    Lock m_Lock;

    boolean captureThreadGo = false;//相机开启状态  true：开启  false：停止
//    Handler hd = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            super.handleMessage(msg);
//            switch (msg.what) {
//                case 0:
//                    long currentTime = System.currentTimeMillis();
//                    if (currentTime - startTime >= 3000) {
//                        startTime = currentTime;
//                        fbs = count / 3.0;
//                        b = new BigDecimal(fbs);
//                        tv_fps.setText("fps:" + b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//                        count = 0;
//                    } else {
//                        currentTime = System.currentTimeMillis();
//                    }
//                    m_imgvw[msg.arg1].setImageBitmap((Bitmap) msg.obj);
//                    count++;
//                    break;
//            }
//        }
//    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initListener();
//        Toast.makeText(MainActivity.this, "checkSuFile==" + checkSuFile() + ",checkAccess===" + checkAccess(), Toast.LENGTH_SHORT).show();
        if(!checkAccess()) {
            if(upgradeRootPermission("/dev/bus/usb/")) {
                //root模式
                initData();
                if (ksjcam.m_devicecount <= 0) {
                    return;
                }
                checkPermission();
            }else {
                sb_switch.setStatus(false);
                ConfirmDialog dialog = new ConfirmDialog(this, new Callback() {
                    @Override
                    public void callback(DialogButtonEnum position) {

                    }
                });
                dialog.setContent("请Root您的系统再进行测试。");
                dialog.show();
            }
        } else {
            //root模式
            initData();
            if (ksjcam.m_devicecount <= 0) {
                return;
            }
            checkPermission();
        }
//        if (checkSuFile()) {
////            Toast.makeText(MainActivity.this, "已经Root", Toast.LENGTH_SHORT).show();
//            //root模式
//            upgradeRootPermission("/dev/bus/usb/");
//
//        } else {
////            Toast.makeText(MainActivity.this, "没有Root权限", Toast.LENGTH_SHORT).show();
//            if (checkAccess()) {
//                //非root模式
//                usbPermission();
////        Toast.makeText(MainActivity.this, "fd==" + fd, Toast.LENGTH_SHORT).show();
//            } else {
//                sb_switch.setStatus(false);
//                ConfirmDialog dialog = new ConfirmDialog(this, new Callback() {
//                    @Override
//                    public void callback(DialogButtonEnum position) {
//
//                    }
//                });
//                dialog.setContent("请Root您的系统再进行测试。");
//                dialog.show();
//            }
//
//        }


    }

    @Override
    protected void onResume() {
        super.onResume();
//        if (checkSuFile()) {
//            //root模式
//            initData();
//            if (ksjcam.m_devicecount <= 0) {
//                return;
//            }
//            checkPermission();
//        } else {
//            //非root模式
//            if (checkAccess()) {
//                if (ksjcam != null) {
////            Toast.makeText(MainActivity.this, "onResume", Toast.LENGTH_SHORT).show();
//                    initData();
//                    if (ksjcam.m_devicecount == 0 || fd == -1) {
//                        return;
//                    }
//                    checkPermission();
//                }
//            }
//
//        }


    }

    private void initView() {
        gainPopup = new GainPopupwindow(this);
        exposurePopup = new ExposurePopupwindow(this);
        whiteBalancePopupwindow = new WhiteBalancePopupwindow(this);
        triggerPopupwindow = new TriggerPopupwindow(this);
        bayerModePopupwindow = new BayerModePopupwindow(this);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        m_PreviewHolder = surfaceView.getHolder();
        sb_switch = (SwitchButton) findViewById(R.id.sb_switch);
        btn_menu = (MenuButton) findViewById(R.id.btn_menu);
        tv_fps = (TextView) findViewById(R.id.tv_fps);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.setScrimColor(Color.TRANSPARENT);
        linearLayout = (LinearLayout) findViewById(R.id.left_drawer);
        linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.closeDrawers();
            }
        });

        setActionBar();
        createMenuList();
        viewAnimator = new ViewAnimator<>(this, list, drawerLayout, this);


    }

    private void initListener() {
        sb_switch.setOnClickListener(this);
        m_PreviewHolder.addCallback(this);

        btn_menu.setOnClickListener(this);

        gainPopup.setRedChangeListener(this);
        gainPopup.setGreenChangeListener(this);
        gainPopup.setBlueChangeListener(this);
        exposurePopup.setExposureChangeListener(this);
        whiteBalancePopupwindow.setWhiteBalanceSelectListener(this);
        triggerPopupwindow.setTriggerModeListener(this);
        bayerModePopupwindow.setBayerModeListener(this);
    }

    private void initData() {
        int[] widtharray = new int[1];
        int[] heightarray = new int[1];
        int[] xarray = new int[1];
        int[] yarray = new int[1];

        int[] deviceTypeArray = new int[1];
        int[] serialsArray = new int[1];
        int[] firmwareVersionArray = new int[1];

//        if (checkSuFile()) {
            //root模式
            ksjcam = new cam();
            ksjcam.Init();
//        }


        m_Lock = new ReentrantLock();

        ksjcam.m_devicecount = ksjcam.DeviceGetCount();

        Log.e("zhanwei", "ksjcam.m_devicecount = " + String.valueOf(ksjcam.m_devicecount));
        json = (String) SharedPreferencesManager.getParam(this, "CAMERA_SURFACE", "");
        if (json.equals("")) {
            surfaceCamera = new CameraBean();
            surfaceCamera.setStartX(0);
            surfaceCamera.setStartY(0);
            surfaceCamera.setWidth(2592);
            surfaceCamera.setHeight(1944);
            surfaceCamera.setBayerMode(KSJ_BAYERMODE.KSJ_BGGR_BGR32_FLIP.ordinal());
            surfaceCamera.setRedGain(48);
            surfaceCamera.setGreenGain(48);
            surfaceCamera.setBlueGain(48);
            surfaceCamera.setTriggerMode(KSJ_TRIGGRMODE.KSJ_TRIGGER_INTERNAL.ordinal());
            surfaceCamera.setWhiteBalance(KSJ_WB_MODE.KSJ_SWB_AUTO_ONCE.ordinal());
            surfaceCamera.setExposureTime(20);
            surfaceCamera.setMirror(false);
            surfaceCamera.setLut(true);
            surfaceCamera.setSensitivity(0);
            json = JsonUtil.getJson(surfaceCamera);
            SharedPreferencesManager.setParam(this, "CAMERA_SURFACE", json);
        } else {
            surfaceCamera = JsonUtil.getObject(json, CameraBean.class);
        }

        currentWB = surfaceCamera.getWhiteBalance();
        currentTrigger = surfaceCamera.getTriggerMode();
        currentBayer = surfaceCamera.getBayerMode();

        isMirror = surfaceCamera.isMirror();
        isLut = surfaceCamera.isLut();

        for (int i = 0; i < ksjcam.m_devicecount; i++) {
            Log.e("TAG", "ksjcam.QueryFunction(i,0)==" + ksjcam.QueryFunction(i,0));
            if(ksjcam.QueryFunction(i,0) == 1) { //QueryFunction(i,0)  0代表查询是否是黑白相机   返回值是1说明是黑白相机 0是彩色相机
                captureThreadGo = false;
                sb_switch.setStatus(false);
                tv_fps.setText("fps:0.0");
                Toast.makeText(MainActivity.this, "黑白相机", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, ImageActivity.class);
                startActivity(intent);
                finish();
                return;
            }
            Toast.makeText(MainActivity.this, "彩色相机", Toast.LENGTH_SHORT).show();
            ksjcam.CaptureSetFieldOfView(i, surfaceCamera.getStartX(), surfaceCamera.getStartY(), surfaceCamera.getWidth(), surfaceCamera.getHeight());

            ksjcam.SetBayerMode(i, surfaceCamera.getBayerMode());//10,11(倒像)，12，13,14,15

            ksjcam.SetParam(i, KSJ_RED.ordinal(), surfaceCamera.getRedGain());
            ksjcam.SetParam(i, KSJ_GREEN.ordinal(), surfaceCamera.getGreenGain());
            ksjcam.SetParam(i, KSJ_BLUE.ordinal(), surfaceCamera.getBlueGain());

//            ksjcam.SetParam(i,KSJ_GAMMA.ordinal(),1);
//            ksjcam.SetParam(i,KSJ_BIN.ordinal(),1);
//            ksjcam.SetParam(i,KSJ_CONTRAST.ordinal(),1);
//            ksjcam.SetParam(i,KSJ_BRIGHTNESS.ordinal(),1);
//            ksjcam.SetParam(i,KSJ_EXPOSURE_LINES.ordinal(),200);
//            ksjcam.SetParam(i,KSJ_BLACKLEVEL.ordinal(),0);

            ksjcam.DeviceGetInformation(i, deviceTypeArray, serialsArray, firmwareVersionArray);
            int nSerial = serialsArray[0];
//            Log.e("zhanwei", "nSerial = " + String.valueOf(nSerial) + "   index = " + String.valueOf(i));

            ksjcam.CaptureGetFieldOfView(i, xarray, yarray, widtharray, heightarray);
//            ksjcam.CaptureGetSize(i,widtharray,heightarray);

            //触发模式
            ksjcam.SetTriggerMode(i, surfaceCamera.getTriggerMode());

            ksjcam.WhiteBalanceSet(i, surfaceCamera.getWhiteBalance());
//            ksjcam.WhiteBalanceSet(i, KSJ_WB_MODE.KSJ_HWB_MANUAL.ordinal());

            ksjcam.ExposureTimeSet(i, surfaceCamera.getExposureTime());

            ksjcam.SensitivitySetMode(i, surfaceCamera.getSensitivity());

            //开启自动曝光
            ksjcam.AEStart(i,1,-1,128);
        }

        m_nwidth = widtharray[0];
        m_nheight = heightarray[0];
        m_x = xarray[0];
        m_y = yarray[0];

//        Log.e("zhanwei", "m_nwidth = " + String.valueOf(m_nwidth));
//        Log.e("zhanwei", "m_nheight = " + String.valueOf(m_nheight));
//        Log.e("zhanwei", "m_x = " + String.valueOf(m_x));
//        Log.e("zhanwei", "m_y = " + String.valueOf(m_y));

    }

    private void setActionBar() {
        drawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                drawerLayout,         /* DrawerLayout object */
                null,  /* nav drawer icon to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
        ) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                linearLayout.removeAllViews();
                linearLayout.invalidate();
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                if (slideOffset > 0.6 && linearLayout.getChildCount() == 0)
                    viewAnimator.showMenuContent();
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);
    }

    private void createMenuList() {
        SlideMenuItem menuItem0 = new SlideMenuItem("WhiteBalance");
        list.add(menuItem0);
        SlideMenuItem menuItem = new SlideMenuItem("CatchBitmap");
        list.add(menuItem);
        SlideMenuItem menuItem2 = new SlideMenuItem("Gain");
        list.add(menuItem2);
        SlideMenuItem menuItem3 = new SlideMenuItem("BayerMode");
        list.add(menuItem3);
        SlideMenuItem menuItem4 = new SlideMenuItem("Exposure");
        list.add(menuItem4);
        SlideMenuItem menuItem5 = new SlideMenuItem("TriggerMode");
        list.add(menuItem5);
        SlideMenuItem menuItem6 = new SlideMenuItem("Mirror");
        list.add(menuItem6);
        SlideMenuItem menuItem7 = new SlideMenuItem("Field");
        list.add(menuItem7);
        SlideMenuItem menuItem8 = new SlideMenuItem("Image/Surface");
        list.add(menuItem8);
        SlideMenuItem menuItem9 = new SlideMenuItem("LUT");
        list.add(menuItem9);
        SlideMenuItem menuItem10 = new SlideMenuItem("Sensitivity");
        list.add(menuItem10);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sb_switch:
                if (captureThreadGo == true && sb_switch.getStatus()) {
                    sb_switch.setStatus(false);
                    captureThreadGo = false;
                    tv_fps.setText("fps:0.0");
                } else {
                    sb_switch.setStatus(true);
                    captureThreadGo = true;
                    startTime = System.currentTimeMillis();
                    count = 0;
                    startCaptureThread(ksjcam.m_devicecount, m_nwidth, m_nheight); //130W
                }
                break;
            case R.id.btn_menu:
                drawerLayout.openDrawer(GravityCompat.START);
                break;
        }
    }

    long startTime = System.currentTimeMillis();
    int count = 0;
    double fbs = 0.0;
    BigDecimal b;

    public void startCaptureThread(int camnum, final int width, final int height) {
        startTime = System.currentTimeMillis();
        surfaceView.setVisibility(View.VISIBLE);


//        Toast debugToast = Toast.makeText(getApplicationContext(), "startCaptureThread", Toast.LENGTH_LONG);
//        debugToast.show();


        Thread captureThread = new Thread(new Runnable() {
            @Override
            public void run() {


                while (captureThreadGo) {
//                    Log.e("TAG", "currentTime==" + currentTime);
//                    Log.e("TAG", "startTime==" + startTime);

//                    Log.e("TAG", System.currentTimeMillis() + "-"  + startTime + ":" + (System.currentTimeMillis() - startTime));
                    if ((System.currentTimeMillis()  - startTime) >= 3000) {
//                            Log.e("TAG", "count--" + count);
                        fbs = count / 3.0;
                        b = new BigDecimal(fbs);
                        runOnUiThread( new Runnable() {
                            @Override
                            public void run() {
                                tv_fps.setText("fps:" + b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
                            }
                        });
                        count = 0;
                        startTime = System.currentTimeMillis();
                    }
//                    Log.d("zhanwei", "before captureAndshow_Capture");

                    m_Lock.lock();

                    boolean b = m_PreviewHolder.getSurface().isValid();
                    if (b) {
                        int surface = ksjcam.CaptureBySurface(0, m_PreviewHolder.getSurface(), 0);// 0 no save 1 save bmp at /sdcard/
//                            ksjcam.CaptureBySurfaceSave(0,m_PreviewHolder.getSurface(),1,"/sdcard/bb.bmp");

                        count++;
                    }

                    m_Lock.unlock();


                }

            }

        });
        Log.d("zhanwei", "startCaptureThread 0");
        captureThread.start();


//        if (camnum == 2) {
//            Thread captureThread1 = new Thread(new Runnable() {
//                @Override
//                public void run() {
//
//
//                    while (captureThreadGo) {
//                        //                      captureAndshow_TwoStep(1, width, height);
//                        captureAndshow_Capture(1, width, height);
//
//
//                    }
//
//                }
//
//            });
//            captureThread1.start();
//        }
    }

    @Override
    public void redChange(int red) {
        m_Lock.lock();
        for (int i = 0; i < ksjcam.m_devicecount; i++) {
            ksjcam.SetParam(i, KSJ_RED.ordinal(), red);
        }
        m_Lock.unlock();
        surfaceCamera.setRedGain(red);
        json = JsonUtil.getJson(surfaceCamera);
        SharedPreferencesManager.setParam(this, "CAMERA_SURFACE", json);
    }

    @Override
    public void greenChange(int green) {
        m_Lock.lock();
        for (int i = 0; i < ksjcam.m_devicecount; i++) {
            ksjcam.SetParam(i, KSJ_GREEN.ordinal(), green);
        }
        m_Lock.unlock();
        surfaceCamera.setGreenGain(green);
        json = JsonUtil.getJson(surfaceCamera);
        SharedPreferencesManager.setParam(this, "CAMERA_SURFACE", json);
    }

    @Override
    public void blueChange(int blue) {
        m_Lock.lock();
        for (int i = 0; i < ksjcam.m_devicecount; i++) {
            ksjcam.SetParam(i, KSJ_BLUE.ordinal(), blue);
        }
        m_Lock.unlock();
        surfaceCamera.setBlueGain(blue);
        json = JsonUtil.getJson(surfaceCamera);
        SharedPreferencesManager.setParam(this, "CAMERA_SURFACE", json);
    }

    @Override
    public void exposureChange(int progress) {
        m_Lock.lock();
        for (int i = 0; i < ksjcam.m_devicecount; i++) {
            ksjcam.ExposureTimeSet(i, progress);
        }
        m_Lock.unlock();
        surfaceCamera.setExposureTime(progress);
        json = JsonUtil.getJson(surfaceCamera);
        SharedPreferencesManager.setParam(this, "CAMERA_SURFACE", json);
    }

    @Override
    public void selectWB(int value) {
//        Log.e("GMM", "白平衡value---" + value);
        currentWB = value;
        m_Lock.lock();
        for (int i = 0; i < ksjcam.m_devicecount; i++) {
            ksjcam.WhiteBalanceSet(i, value);
        }
        m_Lock.unlock();
        surfaceCamera.setWhiteBalance(value);
        json = JsonUtil.getJson(surfaceCamera);
        SharedPreferencesManager.setParam(this, "CAMERA_SURFACE", json);
    }

    @Override
    public void selectTrigger(int value) {
        currentTrigger = value;
        m_Lock.lock();
        for (int i = 0; i < ksjcam.m_devicecount; i++) {
            ksjcam.SetTriggerMode(i, value);
        }
        m_Lock.unlock();
        surfaceCamera.setTriggerMode(value);
        json = JsonUtil.getJson(surfaceCamera);
        SharedPreferencesManager.setParam(this, "CAMERA_SURFACE", json);
    }


    @Override
    public void selectBayer(int value) {
        currentBayer = value;
        m_Lock.lock();
        for (int i = 0; i < ksjcam.m_devicecount; i++) {
            ksjcam.SetBayerMode(i, value);
        }
        m_Lock.unlock();
        surfaceCamera.setBayerMode(value);
        json = JsonUtil.getJson(surfaceCamera);
        SharedPreferencesManager.setParam(this, "CAMERA_SURFACE", json);
    }

//    public int captureAndshow_TwoStep(final int index, int width, int height) {
//        int ret = ksjcam.SoftStartCapture(index);//trigger
//
//        int[] dataarray1 = ksjcam.CaptureRGBdataIntArrayAfterStart(index, width, height);//read
//
//        if (dataarray1 != null && dataarray1.length > 0) {
//            final Bitmap bmp = CreateBitmap_from_int_rgba_data(width, height, dataarray1);
//
//            hd.post(new Runnable() {
//                @Override
//                public void run() {
//                    m_imgvw[index].setImageBitmap(bmp);
//
//                }
//            });
//        } else {
//            Log.e("zhanwei", "read date failed");
//        }
//        return 0;
//
//    }
//
//    public int captureAndshow_Capture(final int index, int width, int height) {
//        int[] dataarray = ksjcam.CaptureRGBdataIntArray(index, width, height);//trigger
//
//        if (dataarray != null && dataarray.length > 0) {
//
//            final Bitmap bmp = CreateBitmap_from_int_rgba_data(width, height, dataarray);
//            Message message = new Message();
//            message.what = 0;
//            message.arg1 = index;
//            message.obj = bmp;
//            hd.sendMessage(message);

//        } else {
//
//            Log.e("zhanwei", "read date failed");
//        }
//
//
//        return 0;
//
//    }


    public String saveBitmapToSDCard(Bitmap bitmap, String imagename) {
        String path = "/sdcard/" + "img-" + imagename + ".PNG";
        //      String path = "img-" + imagename + ".jpg";
        Log.d("zhanwei", "saveBitmapToSDCard1      " + bitmap.toString());
        Log.d("zhanwei", "saveBitmapToSDCard1 getByteCount     " + String.valueOf(bitmap.getByteCount()));

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(path);
            if (fos != null) {

                Log.d("zhanwei", "saveBitmapToSDCard1");

                boolean result;
                result = bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
                Log.d("zhanwei", "saveBitmapToSDCard1 result    " + String.valueOf(result));
                fos.flush();
                fos.close();
                Log.d("zhanwei", "saveBitmapToSDCard2");
            }

            return path;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    //not varifyed
    public void mySaveFile(String name, byte[] buf) {
        String filename = "myfile";
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(buf);
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Bitmap myCreateBitmap(int width, int height, byte[] buf) {
        int w = width;//1280;
        int h = height;//1024;
        int[] pixels = new int[w * h];
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        for (int i = 0; i < w; ++i) {
            for (int j = 0; j < h; ++j) {
//                            int c=getPixel(buf, 1, i, j, w, h);
                int c = buf[i + j * w];
                pixels[i + j * w] = (255 << 24) + (c << 16) + (c << 8) + c;
            }
        }

        bmp.setPixels(pixels, 0, w, 0, 0, w, h);

        return bmp;
    }

    //a r g b high to low
    public static int graybyteArrayToInt(byte[] b, int pos) {
        return (b[pos] & 0xFF) |
                (b[pos] & 0xFF) << 8 |
                (b[pos] & 0xFF) << 16 |
                (255 & 0xFF) << 24;
    }

//    //a r g b high to low
//    public static int byteArrayToInt(byte[] b, int pos) {
//        return b[pos + 2] & 0xFF |
//                (b[pos + 1] & 0xFF) << 8 |
//                (b[pos] & 0xFF) << 16 |
//                (255 & 0xFF) << 24;
//    }

    public static int byteArrayToInt(byte[] b, int pos) {

        int value = 0;

        value += (int) (b[pos + 0] << 0);
        value += (int) (b[pos + 1] << 8);
        value += (int) (b[pos + 2] << 16);
        value += (int) (0xff << 24);

        return value;


    }


    //a r g b high to low
//    public static int byteArrayToInt(byte[] b, int pos) {
//        return 255 & 0xFF |
//                (b[pos + 1] & 0xFF) << 8 |
//                (0 & 0xFF) << 16 |
//                (255 & 0xFF) << 24;
//    }


    public static int byteArrayToIntfake(byte[] b, int pos) {
        return 0 & 0xFF |
                (255 & 0xFF) << 8 |
                (255 & 0xFF) << 16 |
                (255 & 0xFF) << 24;
    }

    public Bitmap CreateBitmap_from_raw_data(int width, int height, byte[] buf) {
        int w = width;//1280;
        int h = height;//1024;

        //     Log.d("zhanwei", "buf.length    " + String.valueOf(buf.length));

        int[] pixels = new int[w * h];
        int count = 0;
        for (; count < w * h; count++) {
            pixels[count] = graybyteArrayToInt(buf, count);

        }

        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bmp.setHasAlpha(false);
        bmp.setPixels(pixels, 0, w, 0, 0, w, h);

        return bmp;
    }


    public Bitmap CreateBitmap_from_byte_rgba_data(int width, int height, byte[] buf) {


        int w = width;//1280;
        int h = height;//1024;

        Log.d("zhanwei", "buf.length    " + String.valueOf(buf.length));

        int[] pixels = new int[w * h];
        int count = 0;
        for (; count < w * h; count++) {

            pixels[count] = byteArrayToInt(buf, count);

        }

        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bmp.setHasAlpha(false);
        bmp.setPixels(pixels, 0, w, 0, 0, w, h);

        return bmp;
    }

    public Bitmap CreateBitmap_from_int_rgba_data(int width, int height, int[] buf) {

        int w = width;//1280;
        int h = height;//1024;

//        Log.d("zhanwei", "buf.length    " + String.valueOf(buf.length));

        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        bmp.setHasAlpha(false);

        bmp.setPixels(buf, 0, w, 0, 0, w, h);

        return bmp;
    }


//    public Bitmap CreateBitmap_from_rgba_data(int width, int height, byte[] buf) { //very slow
//        int w = width;//1280;
//        int h = height;//1024;
//
//        Log.d("zhanwei", "buf.length    " + String.valueOf(buf.length));
//
//        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
//
//        for (int i = 0; i < height; i++) {
//            for (int j = 0; j < width; j++) {
//                int color = buf[i * width + j] + buf[i * width + j + 1] << 8 + buf[i * width + j + 2] << 16 + buf[i * width + j + 3] << 24;
//
//                bmp.setPixel(j, i, color);
//
//            }
//        }
//
//        return bmp;
//    }


    void set_evbususb_rights() {

        Process p;
        try {
            // Preform su to get root privledges
            p = Runtime.getRuntime().exec(new String[]{"su", "chmod -R 777 /dev/bus/usb/"});


            // Attempt to write a file to a root-only
            DataOutputStream os = new DataOutputStream(p.getOutputStream());


            os.writeBytes("echo \"Do I have root?\" >/system/sd/temporary.txt\n");
            os.writeBytes("chmod -R 777 /dev/bus/usb/   \n");
            //   os.writeBytes("setenforce 0\n");

            // Close the terminal
            os.writeBytes("exit\n");
            os.flush();
            try {
                p.waitFor();
                if (p.exitValue() != 255) {
                    // TODO Code to run on success
                    Log.d("zhanwei", "root");
                } else {
                    // TODO Code to run on unsuccessful
                    Log.d("zhanwei", "not root");
                }
            } catch (InterruptedException e) {
                // TODO Code to run in interrupted exception
                Log.d("zhanwei", "not root");
            }
        } catch (IOException e) {
            // TODO Code to run in input/output exception
            Log.d("zhanwei", "not root");
        }


    }


    // Example of a call to a native method
//    TextView tv = (TextView) findViewById(R.id.sample_text);
//    tv.setText(stringFromJNI());

    @Override
    protected void onPause() {
//        Log.e("TAG", "MainActivity_onPause");
//        captureThreadGo = false;
//        sb_switch.setStatus(false);
//        tv_fps.setText("fps:0.0");
//        ksjcam.UnInit();
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {

            int ShutterWidth = data.getExtras().getInt("ShutterWidth", 0);

//        Toast debugToast = Toast.makeText(getApplicationContext(),"ShutterWidth  == "+ShutterWidth, Toast.LENGTH_LONG);
//        debugToast.show();

            int Green2Gain = data.getExtras().getInt("Green2Gain", 0);

            int RedGain = data.getExtras().getInt("RedGain", 0);

            int BlueGain = data.getExtras().getInt("BlueGain", 0);


            Toast debugToast3 = Toast.makeText(getApplicationContext(), "BlueGain  == " + ShutterWidth, Toast.LENGTH_LONG);
            debugToast3.show();

//            ca.setBlueGain(BlueGain);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
//        if (ksjcam != null) {
//            ksjcam.UnInit();
//        }
//        hd.removeCallbacksAndMessages(null);
//        //非root模式
//        if (!checkSuFile() && checkAccess()) {
//            unregisterReceiver(mUsbReceiver);
//        }
        super.onDestroy();
    }


    public static void saveImage(Bitmap bmp) {
        File appDir = new File(Environment.getExternalStorageDirectory(), "ksj");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = System.currentTimeMillis() + ".jpg";
//        Log.e("TAG", "filename---" + fileName);
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static final String DEVICE_NAME = "/dev/graphics/fb0";

    @SuppressWarnings("deprecation")
    public static Bitmap acquireScreenshot(Context context) {
        WindowManager mWinManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        Display display = mWinManager.getDefaultDisplay();
        display.getMetrics(metrics);
        // 屏幕高
        int height = metrics.heightPixels;
        // 屏幕的宽
        int width = metrics.widthPixels;

        int pixelformat = display.getPixelFormat();
        PixelFormat localPixelFormat1 = new PixelFormat();
        PixelFormat.getPixelFormatInfo(pixelformat, localPixelFormat1);
        // 位深
        int deepth = localPixelFormat1.bytesPerPixel;

        byte[] arrayOfByte = new byte[height * width * deepth];
        try {
            // 读取设备缓存，获取屏幕图像流
            InputStream localInputStream = readAsRoot();
            DataInputStream localDataInputStream = new DataInputStream(
                    localInputStream);
            localDataInputStream.readFully(arrayOfByte);
            localInputStream.close();

            int[] tmpColor = new int[width * height];
            int r, g, b;
            for (int j = 0; j < width * height * deepth; j += deepth) {
                b = arrayOfByte[j] & 0xff;
                g = arrayOfByte[j + 1] & 0xff;
                r = arrayOfByte[j + 2] & 0xff;
                tmpColor[j / deepth] = (r << 16) | (g << 8) | b | (0xff000000);
            }
            // 构建bitmap
            Bitmap scrBitmap = Bitmap.createBitmap(tmpColor, width, height,
                    Bitmap.Config.ARGB_8888);
            return scrBitmap;

        } catch (Exception e) {
            Log.d("TAG", "#### 读取屏幕截图失败");
            e.printStackTrace();
        }
        return null;

    }

    /**
     * @throws Exception
     * @throws
     * @Title: readAsRoot
     * @Description: 以root权限读取屏幕截图
     */
    public static InputStream readAsRoot() throws Exception {
        File deviceFile = new File(DEVICE_NAME);
        Process localProcess = Runtime.getRuntime().exec("su");
        String str = "cat " + deviceFile.getAbsolutePath() + "\n";
        localProcess.getOutputStream().write(str.getBytes());
        return localProcess.getInputStream();
    }

    public void checkPermission() {
        requestRuntimePermission(PermissionUsage.READ_EXTRASORE, new QuestPermissionListener() {
            @Override
            public void doAllPermissionGrant() {//都都授权了，则做些 其他事情
//                Toast.makeText(MainActivity.this, "打开相机1", Toast.LENGTH_SHORT).show();
                captureThreadGo = true;
                startTime = System.currentTimeMillis();
                count = 0;
                startCaptureThread(ksjcam.m_devicecount, m_nwidth, m_nheight); //130W
                sb_switch.setStatus(true);
//                showInfo();
            }

            @Override
            public void denySomePermission() {//没有拒绝，进行其他干活
                // GetToast.useString(RunTimeStatusActivity2.this, "需要开启一些权限才能进行此操作");
                onResumeCheckPermission();
            }
        });
        if (getNeedRequestPermissionList().isEmpty()) {
//            Toast.makeText(MainActivity.this, "打开相机2", Toast.LENGTH_SHORT).show();
            captureThreadGo = true;
            startTime = System.currentTimeMillis();
            count = 0;
            startCaptureThread(ksjcam.m_devicecount, m_nwidth, m_nheight); //130W
            sb_switch.setStatus(true);
//            showInfo();
        }
    }

    //用于格式化日期,作为日志文件名的一部分
    private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

    private void showInfo() {
        int[] deviceTypeArray = new int[ksjcam.m_devicecount];
        int[] serialsArray = new int[ksjcam.m_devicecount];
        int[] firmwareVersionArray = new int[ksjcam.m_devicecount];

        for (int i = 0; i < ksjcam.m_devicecount; i++) {


            ksjcam.DeviceGetInformation(i, deviceTypeArray, serialsArray, firmwareVersionArray);

            final String content = "相机个数：" + ksjcam.m_devicecount + "\n相机型号：" + deviceTypeArray[i] + "\n序列号：" + serialsArray[i] + "\n版本：" + firmwareVersionArray[i];
            new Thread() {
                public void run() {
                    try {
                        long timestamp = System.currentTimeMillis();
                        String time = formatter.format(new Date());
                        String fileName = "deviceinfo-" + time + "-" + timestamp + ".txt";
                        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                            String path = "/sdcard/crash/";
                            File dir = new File(path);
                            if (!dir.exists()) {
                                dir.mkdirs();
                            }
                            FileOutputStream fos = new FileOutputStream(path + fileName);
                            fos.write(content.toString().getBytes());
                            fos.close();
                        }
                    } catch (Exception e) {
                        Log.e("TAG", "an error occured while writing file...", e);
                        Toast.makeText(MainActivity.this, "an error occured while writing file...", Toast.LENGTH_SHORT).show();
                    }
                }
            }.start();


        }
    }

    int min[] = new int[1];
    int max[] = new int[1];
    long timestamp;
    String time;
    String path = "/sdcard/CatchBest";
    File outfile = new File(path);

    @Override
    public void onSwitch(Resourceble slideMenuItem, int position) {
        switch (slideMenuItem.getName()) {
            case "WhiteBalance":
                whiteBalancePopupwindow.showPopupWindow(drawerLayout);
                whiteBalancePopupwindow.setCurrentValue(currentWB);
                break;
            case "CatchBitmap":
                m_Lock.lock();
                //                ksjcam.SetBayerMode(0,19);
                timestamp = System.currentTimeMillis();
                time = formatter.format(new Date());
                // 如果文件不存在，则创建一个新文件
                if (!outfile.isDirectory()) {
                    try {
                        outfile.mkdir();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                ksjcam.CaptureBitmap(0, path + "/catchbest" + time + ".bmp");

                Intent intent1 = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                File file = new File(path + "/catchbest" + time + ".bmp");
                Uri uri = Uri.fromFile(file);
                intent1.setData(uri);
                sendBroadcast(intent1);//这个广播的目的就是更新图库，发了这个广播进入相册就可以找到你保存的图片了！，记得要传你更新的file哦

                //                ksjcam.SetBayerMode(0,15);
                m_Lock.unlock();
                Toast debugToast = Toast.makeText(getApplicationContext(), "captured /sdcard/bitmapcaptured.bmp", Toast.LENGTH_LONG);
                debugToast.show();

                /**final Bitmap bitmap = acquireScreenshot(this);
                 //                Log.e("TAG", "width--" + bitmap.getWidth() + ",height--" + bitmap.getHeight());
                 if (bitmap != null) {
                 new Thread() {
                 public void run() {
                 saveImage(bitmap);
                 }
                 }.start();
                 }*/
                break;

            case "Gain":
                gainPopup.setData(surfaceCamera.getRedGain(), surfaceCamera.getGreenGain(), surfaceCamera.getBlueGain());
                for (int i = 0; i < ksjcam.m_devicecount; i++) {
                    m_Lock.lock();
                    ksjcam.GetParamRange(i, KSJ_RED.ordinal(), min, max);
                    gainPopup.setRedRange(min[0], max[0]);
                    m_Lock.unlock();
                    m_Lock.lock();
                    ksjcam.GetParamRange(i, KSJ_GREEN.ordinal(), min, max);
                    gainPopup.setGreenRange(min[0], max[0]);
                    m_Lock.unlock();
                    m_Lock.lock();
                    ksjcam.GetParamRange(i, KSJ_BLUE.ordinal(), min, max);
                    gainPopup.setBlueRange(min[0], max[0]);
                    m_Lock.unlock();

                }

                gainPopup.showPupwindow(drawerLayout);
                break;

            case "BayerMode":
                bayerModePopupwindow.showPopupWindow(drawerLayout);
                bayerModePopupwindow.setCurrentValue(currentBayer);
                break;

            case "Exposure":
                exposurePopup.setData(surfaceCamera.getExposureTime());
                m_Lock.lock();
                for (int i = 0; i < ksjcam.m_devicecount; i++) {
                    ksjcam.GetParamRange(i, KSJ_EXPOSURE.ordinal(), min, max);
                    exposurePopup.setRange(min[0], max[0]);
                }
                m_Lock.unlock();
                exposurePopup.showPopupwindow(drawerLayout);
                break;

            case "TriggerMode":
                triggerPopupwindow.showPopupWindow(drawerLayout);
                triggerPopupwindow.setCurrentValue(currentTrigger);
                break;

            case "Mirror":
                m_Lock.lock();
                if (!isMirror) {
                    for (int i = 0; i < ksjcam.m_devicecount; i++) {
                        ksjcam.SetParam(i, KSJ_MIRROR.ordinal(), 1);//镜像
                    }
                    isMirror = true;
                } else {
                    for (int i = 0; i < ksjcam.m_devicecount; i++) {
                        ksjcam.SetParam(i, KSJ_MIRROR.ordinal(), 0);//镜像  默认0
                    }
                    isMirror = false;
                }
                m_Lock.unlock();
                surfaceCamera.setMirror(isMirror);
                json = JsonUtil.getJson(surfaceCamera);
                SharedPreferencesManager.setParam(this, "CAMERA_SURFACE", json);
                break;

            case "Field":
                sb_switch.setStatus(false);
                captureThreadGo = false;
                tv_fps.setText("fps:0.0");

                int[] widtharray = new int[1];
                int[] heightarray = new int[1];
                int[] xarray = new int[1];
                int[] yarray = new int[1];
                m_Lock.lock();
                for (int i = 0; i < ksjcam.m_devicecount; i++) {
                    ksjcam.CaptureGetFieldOfView(i, xarray, yarray, widtharray, heightarray);
                }
                m_Lock.unlock();
                m_nwidth = widtharray[0];
                m_nheight = heightarray[0];
                m_x = xarray[0];
                m_y = yarray[0];
                fieldDialog = new FieldDialog(this);
                fieldDialog.setData(surfaceCamera.getStartX(), surfaceCamera.getStartY(), surfaceCamera.getWidth(), surfaceCamera.getHeight());
                fieldDialog.show();

                fieldDialog.setFieldViewListener(new FieldDialog.FieldViewListener() {
                    @Override
                    public void setValue(int x, int y, int width, int height) {
                        m_Lock.lock();
                        for (int i = 0; i < ksjcam.m_devicecount; i++) {
                            ksjcam.CaptureSetFieldOfView(i, x, y, width, height);
                        }
                        m_Lock.unlock();
                        surfaceCamera.setStartX(x);
                        surfaceCamera.setStartY(y);
                        surfaceCamera.setWidth(width);
                        surfaceCamera.setHeight(height);
                        json = JsonUtil.getJson(surfaceCamera);
                        SharedPreferencesManager.setParam(MainActivity.this, "CAMERA_SURFACE", json);
                        fieldDialog.dismiss();
                        sb_switch.setStatus(true);
                        captureThreadGo = true;
                        startTime = System.currentTimeMillis();
                        count = 0;
                        startCaptureThread(ksjcam.m_devicecount, m_nwidth, m_nheight); //130W
                    }
                });


                break;

            case "Image/Surface":
                captureThreadGo = false;
                sb_switch.setStatus(false);
                tv_fps.setText("fps:0.0");
                Intent intent = new Intent(this, ImageActivity.class);
                startActivity(intent);
                break;

            case "LUT":
                m_Lock.lock();
//                Toast.makeText(MainActivity.this, "改变前LUT==" + isLut + (isLut?"开":"关"), Toast.LENGTH_SHORT).show();
//                for (int i = 0; i < ksjcam.m_devicecount; i++) {
//                    ksjcam.LutSetEnable(i, isLut ? 0 : 1);//LUT 默认 1
//                }
                if (isLut) {
                    for (int i = 0; i < ksjcam.m_devicecount; i++) {
                        ksjcam.LutSetEnable(i, 0);//LUT 默认 1
                    }
                    isLut = false;
                } else {
                    for (int i = 0; i < ksjcam.m_devicecount; i++) {
                        ksjcam.LutSetEnable(i, 1);//LUT 默认 1
                    }
                    isLut = true;
                }

                m_Lock.unlock();
                surfaceCamera.setLut(isLut);
                json = JsonUtil.getJson(surfaceCamera);
                SharedPreferencesManager.setParam(this, "CAMERA_SURFACE", json);
//                Toast.makeText(MainActivity.this, "改变后LUT==" + isLut + (isLut?"开":"关"), Toast.LENGTH_SHORT).show();
                break;

            case "Sensitivity":
                m_Lock.lock();
                int sensitivity = surfaceCamera.getSensitivity();
                if (sensitivity == 4) {
                    sensitivity = 0;
                } else {
                    sensitivity++;
                }
                for (int i = 0; i < ksjcam.m_devicecount; i++) {
                    Toast.makeText(MainActivity.this, "当前sensitivity：" + sensitivity, Toast.LENGTH_SHORT).show();
                    ksjcam.SensitivitySetMode(i, sensitivity);
                }
                m_Lock.unlock();
                surfaceCamera.setSensitivity(sensitivity);
                json = JsonUtil.getJson(surfaceCamera);
                SharedPreferencesManager.setParam(this, "CAMERA_SURFACE", json);
                break;
        }
    }

    @Override
    public void disableHomeButton() {
//        getSupportActionBar().setHomeButtonEnabled(false);
    }

    @Override
    public void enableHomeButton() {
//        getSupportActionBar().setHomeButtonEnabled(true);
        drawerLayout.closeDrawers();
    }

    @Override
    public void addViewToContainer(View view) {
        linearLayout.addView(view);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    public  boolean upgradeRootPermission(String pkgCodePath) {
        Process process = null;
        DataOutputStream os = null;
        try {
            String cmd = "chmod -R 777 " + pkgCodePath;
            process = Runtime.getRuntime().exec("su"); //切换到root帐号
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("setenforce 0" + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (Exception e) {

            return false;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                process.destroy();
            } catch (Exception e) {
            }
        }
        return true;
    }

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("com.android.example.USB_PERMISSION")) {
//                Toast.makeText(MainActivity.this, "允许usb权限", Toast.LENGTH_SHORT).show();
                UsbDeviceConnection connection = manager.openDevice(device);
                if (connection != null) {
                    fd = connection.getFileDescriptor();
//                    Toast.makeText(MainActivity.this, "usbfd==" + fd, Toast.LENGTH_SHORT).show();
                    if (fd <= 0) {
                        return;
                    }
                    ksjcam = new cam();

                    ksjcam.PreInit(fd);
                    ksjcam.Init();
                    initData();
                    checkPermission();
                } else
                    Log.e("zhanwei", "UsbManager openDevice failed");
            }
        }
    };
    String content;
    UsbManager manager;
    UsbDevice device;
    int fd = -1;

    public void usbPermission() {
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);


        int i = 0;
//        Toast.makeText(MainActivity.this, "deviceList---" + deviceList.size() + "," + deviceList.toString(), Toast.LENGTH_LONG).show();
        while (deviceIterator.hasNext()) {
            device = deviceIterator.next();
//            Log.e("TAG", "venderId==" + device.getVendorId());
//            Toast.makeText(MainActivity.this, "venderId==" + device.getVendorId(), Toast.LENGTH_SHORT).show();
            if (device.getVendorId() == 0x0816) {
                Log.i("zhanwei", device.getDeviceName() + " " + Integer.toHexString(device.getVendorId()) +
                        " " + Integer.toHexString(device.getProductId()));
                String info = device.getDeviceName() + " " + Integer.toHexString(device.getVendorId()) +
                        " " + Integer.toHexString(device.getProductId());
//                Toast.makeText(MainActivity.this, i + "---" + info, Toast.LENGTH_LONG).show();
                i++;
                manager.requestPermission(device, mPermissionIntent);

                break;
            }
        }

//        return fd;
    }

    private long exitTime = 0;

    @Override
    public void onBackPressed() {

        if ((System.currentTimeMillis() - exitTime) > 2000) {
            Display display = getWindowManager().getDefaultDisplay();
            int height = display.getHeight();
            Toast toast = Toast.makeText(getApplicationContext(), "再按一次退出",
                    Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM, 0, height / 6);
            toast.show();
            exitTime = System.currentTimeMillis();
            return;
        }
        super.onBackPressed();


    }

    /**
     * 判断手机是否ROOT
     */
    private static boolean checkSuFile() {
        Process process = null;
        try {
            //   /system/xbin/which 或者  /system/bin/which
            process = Runtime.getRuntime().exec(new String[]{"which", "su"});
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            if (in.readLine() != null) return true;
            return false;
        } catch (Throwable t) {
            return false;
        } finally {
            if (process != null) process.destroy();
        }
    }


    /**
     * 判断手机手否可以访问adb ls dev/bus/usb
     *
     * @return
     */
    private boolean checkAccess() {
        //这个类是一个很好用的工具，java中可以执行java命令，android中可以执行shell命令
        Runtime mRuntime = Runtime.getRuntime();
        try {
//Process中封装了返回的结果和执行错误的结果
            Process mProcess = mRuntime.exec("ls /dev/bus/usb/ -al ");
            BufferedReader mReader = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
            StringBuffer mRespBuff = new StringBuffer();
            char[] buff = new char[1024];
            int ch = 0;
            while ((ch = mReader.read(buff)) != -1) {
                mRespBuff.append(buff, 0, ch);
            }
            mReader.close();
//            Log.e("TAG", "haha:" + mRespBuff.toString());
//            Toast.makeText(MainActivity.this, "haha:" + mRespBuff.toString(), Toast.LENGTH_SHORT).show();
//            mMsgText.setText(mRespBuff.toString());
//            if(mRespBuff != null) {
//                if(mRespBuff.toString().contains("daemon not running")) {
//                    Log.e("TAG", "su失败");
//                    return false;
//                } else {
//                    Log.e("TAG", "su成功");
//                    return true;
//                }
//            } else {
//                Log.e("TAG", "失败");
                return false;
//            }
        } catch (IOException e) {
// TODO Auto-generated catch block
            e.printStackTrace();
//            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }


    private static File checkRootFile() {
        File file = null;
        String[] paths = {"/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
                "/system/bin/failsafe/su", "/data/local/su"};
        for (String path : paths) {
            file = new File(path);
            if (file.exists()) return file;
        }
        return file;
    }
}
