package com.phone.fuxi.catchbest;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.view.ExposurePopupwindow;
import com.view.FieldDialog;
import com.view.GainPopupwindow;
import com.view.MenuButton;
import com.view.SwitchButton;
import com.view.TriggerPopupwindow;
import com.view.WhiteBalancePopupwindow;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.catchbest.KSJ_PARAM.KSJ_BLUE;
import static com.catchbest.KSJ_PARAM.KSJ_EXPOSURE;
import static com.catchbest.KSJ_PARAM.KSJ_GREEN;
import static com.catchbest.KSJ_PARAM.KSJ_MIRROR;
import static com.catchbest.KSJ_PARAM.KSJ_RED;

public class ImageActivity extends BaseActivity implements View.OnClickListener
        , GainPopupwindow.RedGainChangeListener
        , GainPopupwindow.GreenGainChangeListener
        , GainPopupwindow.BlueGainChangeListener
        , ExposurePopupwindow.ExposureChangeListener
        , WhiteBalancePopupwindow.WhiteBalanceSelectListener
        , TriggerPopupwindow.TriggerModeListener
        , ViewAnimator.ViewAnimatorListener
        , BayerModePopupwindow.BayerModeListener {

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

    private int bayerMode = 12;//10,11(倒像)，12，13,14,15
    private int currentWB = KSJ_WB_MODE.KSJ_SWB_AUTO_ONCE.ordinal();
    private int currentTrigger = KSJ_TRIGGRMODE.KSJ_TRIGGER_INTERNAL.ordinal();
    private int currentBayer = KSJ_BAYERMODE.KSJ_BGGR_BGR32_FLIP.ordinal();

    private boolean isMirror = false;
    private boolean isLut = true;

    public int m_nwidth;
    public int m_nheight;
    public int m_x;
    public int m_y;


    public ImageView[] m_imgvw;

    cam ksjcam;
    Lock m_Lock;

    boolean captureThreadGo = false;//相机开启状态  true：开启  false：停止
    Handler hd = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - startTime >= 3000) {
                        startTime = currentTime;
                        fbs = count / 3.0;
                        b = new BigDecimal(fbs);
                        tv_fps.setText("fps:" + b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
                        count = 0;
                    } else {
                        currentTime = System.currentTimeMillis();
                    }
                    m_imgvw[msg.arg1].setImageBitmap((Bitmap) msg.obj);
                    count++;
                    break;
            }
        }
    };
    private String json;
    private CameraBean imageCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        initView();
        initListener();
        initData();
        if (ksjcam.m_devicecount == 0) {
            return;
        }
        checkPermission();
    }

    private void initView() {
        gainPopup = new GainPopupwindow(this);
        exposurePopup = new ExposurePopupwindow(this);
        whiteBalancePopupwindow = new WhiteBalancePopupwindow(this);
        triggerPopupwindow = new TriggerPopupwindow(this);
        bayerModePopupwindow = new BayerModePopupwindow(this);
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

        m_imgvw = new ImageView[2];
        m_imgvw[0] = (ImageView) findViewById(R.id.imageView);
        m_imgvw[1] = (ImageView) findViewById(R.id.imageView1);

    }

    private void initListener() {
        sb_switch.setOnClickListener(this);

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

        ksjcam = new cam();


        int c = ksjcam.Init();
        Log.e("GMM", " ksjcam.Init()==" + c);
        Log.e("GMM", " Init ksjcam--" + ksjcam);

        m_Lock = new ReentrantLock();


        ksjcam.m_devicecount = ksjcam.DeviceGetCount();

        Log.e("GMM", "ksjcam.m_devicecount = " + String.valueOf(ksjcam.m_devicecount));
        json = (String) SharedPreferencesManager.getParam(this, "CAMERA_IMAGE", "");
        if (json.equals("")) {
            imageCamera = new CameraBean();
            imageCamera.setStartX(0);
            imageCamera.setStartY(0);
            imageCamera.setWidth(1280);
            imageCamera.setHeight(960);
            imageCamera.setBayerMode(KSJ_BAYERMODE.KSJ_BGGR_BGR32_FLIP.ordinal());
            imageCamera.setRedGain(48);
            imageCamera.setGreenGain(48);
            imageCamera.setBlueGain(48);
            imageCamera.setTriggerMode(KSJ_TRIGGRMODE.KSJ_TRIGGER_INTERNAL.ordinal());
            imageCamera.setWhiteBalance(KSJ_WB_MODE.KSJ_SWB_AUTO_ONCE.ordinal());
            imageCamera.setExposureTime(20);
            imageCamera.setMirror(false);
            imageCamera.setLut(true);
            imageCamera.setSensitivity(0);
            json = JsonUtil.getJson(imageCamera);
            SharedPreferencesManager.setParam(this, "CAMERA_IMAGE", json);
        } else {
            imageCamera = JsonUtil.getObject(json, CameraBean.class);
        }

        currentWB = imageCamera.getWhiteBalance();
        currentTrigger = imageCamera.getTriggerMode();
        currentBayer = imageCamera.getBayerMode();

        isMirror = imageCamera.isMirror();
        isLut = imageCamera.isLut();

        for (int i = 0; i < ksjcam.m_devicecount; i++) {

            int nRet = ksjcam.CaptureSetFieldOfView(i, imageCamera.getStartX(), imageCamera.getStartY(), imageCamera.getWidth(), imageCamera.getHeight());
            Log.e("GMM", "CaptureSetFieldOfView nRet----" + nRet);
            ksjcam.SetBayerMode(i, imageCamera.getBayerMode());//10,11(倒像)，12，13,14,15

            ksjcam.SetParam(i, KSJ_RED.ordinal(), imageCamera.getRedGain());
            ksjcam.SetParam(i, KSJ_GREEN.ordinal(), imageCamera.getGreenGain());
            ksjcam.SetParam(i, KSJ_BLUE.ordinal(), imageCamera.getBlueGain());

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
            ksjcam.SetTriggerMode(i, imageCamera.getTriggerMode());

            ksjcam.WhiteBalanceSet(i, imageCamera.getWhiteBalance());
//            ksjcam.WhiteBalanceSet(i, KSJ_WB_MODE.KSJ_HWB_MANUAL.ordinal());

            ksjcam.ExposureTimeSet(i, imageCamera.getExposureTime());
            ksjcam.SensitivitySetMode(i, imageCamera.getSensitivity());
        }

        m_nwidth = widtharray[0];
        m_nheight = heightarray[0];
        m_x = xarray[0];
        m_y = yarray[0];

        Log.e("GMM", "m_nwidth = " + String.valueOf(m_nwidth));
        Log.e("GMM", "m_nheight = " + String.valueOf(m_nheight));
        Log.e("GMM", "m_x = " + String.valueOf(m_x));
        Log.e("GMM", "m_y = " + String.valueOf(m_y));

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

    int min[] = new int[1];
    int max[] = new int[1];

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

        m_imgvw[0].setVisibility(View.VISIBLE);

        Toast debugToast = Toast.makeText(getApplicationContext(), "startCaptureThread", Toast.LENGTH_LONG);
        debugToast.show();


        Thread captureThread = new Thread(new Runnable() {
            @Override
            public void run() {
//                Log.e("GMM", "ksjcam.QueryFunction(0,0)==" + ksjcam.QueryFunction(0,0));
                if(ksjcam.QueryFunction(0,0) == 1) {
                    while (captureThreadGo) {
                        int ret = captureAndShow_BlackWhite(0, width, height);
                        if (ret == -1) {
                            captureThreadGo = false;
                            sb_switch.setStatus(false);
                            tv_fps.setText("fps:0.0");
                        }
                    }
                } else if(ksjcam.QueryFunction(0,0) == 0) {
                    while (captureThreadGo) {
                        int ret = captureAndShow_Color(0, width, height);
                        if (ret == -1) {
                            captureThreadGo = false;
                            sb_switch.setStatus(false);
                            tv_fps.setText("fps:0.0");
                        }
                    }
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ImageActivity.this, "显示失败", Toast.LENGTH_SHORT).show();

                        }
                    });
                }
//                while (captureThreadGo) {
//                    int ret = captureAndshow_Capture(0, width, height);
//                    if(ret == -1) {
//                        captureThreadGo = false;
//                        sb_switch.setStatus(false);
//                        tv_fps.setText("fps:0.0");
//                    }
//                }

            }

        });
        Log.d("zhanwei", "startCaptureThread 0");
        captureThread.start();


        if (camnum == 2) {
            Thread captureThread1 = new Thread(new Runnable() {
                @Override
                public void run() {


                    while (captureThreadGo) {
                        //                      captureAndshow_TwoStep(1, width, height);
                        int ret = captureAndshow_Capture(1, width, height);

                        if(ret == -1) {
                            captureThreadGo = false;
                            sb_switch.setStatus(false);
                            tv_fps.setText("fps:0.0");
                        }

                    }

                }

            });
            captureThread1.start();
        }
    }

    @Override
    public void redChange(int red) {
        m_Lock.lock();
        for (int i = 0; i < ksjcam.m_devicecount; i++) {
            ksjcam.SetParam(i, KSJ_RED.ordinal(), red);

        }
        m_Lock.unlock();
        imageCamera.setRedGain(red);
        json = JsonUtil.getJson(imageCamera);
        SharedPreferencesManager.setParam(this, "CAMERA_IMAGE", json);
    }

    @Override
    public void greenChange(int green) {
        m_Lock.lock();
        for (int i = 0; i < ksjcam.m_devicecount; i++) {
            ksjcam.SetParam(i, KSJ_GREEN.ordinal(), green);

        }
        m_Lock.unlock();
        imageCamera.setGreenGain(green);
        json = JsonUtil.getJson(imageCamera);
        SharedPreferencesManager.setParam(this, "CAMERA_IMAGE", json);
    }

    @Override
    public void blueChange(int blue) {
        m_Lock.lock();
        for (int i = 0; i < ksjcam.m_devicecount; i++) {
            ksjcam.SetParam(i, KSJ_BLUE.ordinal(), blue);

        }
        m_Lock.unlock();
        imageCamera.setBlueGain(blue);
        json = JsonUtil.getJson(imageCamera);
        SharedPreferencesManager.setParam(this, "CAMERA_IMAGE", json);
    }

    @Override
    public void exposureChange(int progress) {
        m_Lock.lock();
        for (int i = 0; i < ksjcam.m_devicecount; i++) {
            ksjcam.ExposureTimeSet(i, progress);
        }
        m_Lock.unlock();
        imageCamera.setExposureTime(progress);
        json = JsonUtil.getJson(imageCamera);
        SharedPreferencesManager.setParam(this, "CAMERA_IMAGE", json);
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
        imageCamera.setWhiteBalance(value);
        json = JsonUtil.getJson(imageCamera);
        SharedPreferencesManager.setParam(this, "CAMERA_IMAGE", json);
    }

    @Override
    public void selectTrigger(int value) {
        currentTrigger = value;
        m_Lock.lock();
        for (int i = 0; i < ksjcam.m_devicecount; i++) {
            ksjcam.SetTriggerMode(i, value);
        }
        m_Lock.unlock();
        imageCamera.setTriggerMode(value);
        json = JsonUtil.getJson(imageCamera);
        SharedPreferencesManager.setParam(this, "CAMERA_IMAGE", json);
    }

    @Override
    public void selectBayer(int value) {
        currentBayer = value;
        m_Lock.lock();
        for (int i = 0; i < ksjcam.m_devicecount; i++) {
            ksjcam.SetBayerMode(i, value);
        }
        m_Lock.unlock();
        imageCamera.setBayerMode(value);
        json = JsonUtil.getJson(imageCamera);
        SharedPreferencesManager.setParam(this, "CAMERA_IMAGE", json);
    }

    public int captureAndShow_BlackWhite(final int index, int width, int height){
        byte[] byteArray = ksjcam.CaptureRAWdataArray(index, width, height);
        if (byteArray != null && byteArray.length > 0) {
            Bitmap bitmap = createBitmap_from_byte_alpha_data(width, height, byteArray);
            Message message = new Message();
            message.what = 0;
            message.arg1 = index;
            message.obj = bitmap;
            hd.sendMessage(message);
        } else {
            return -1;
        }

//            int[] dataarray = ksjcam.CaptureRGBdataIntArray(index, width, height);//trigger
//
//        if (dataarray != null && dataarray.length > 0) {
//
//            Bitmap bmp = CreateBitmap_from_int_rgba_data(width, height, dataarray);
//            Message message = new Message();
//            message.what = 0;
//            message.arg1 = index;
//            message.obj = bmp;
//            hd.sendMessage(message);
//        } else {
//
//            return -1;
//        }

        return 0;
    }

    public int captureAndShow_Color(final int index, int width, int height){
        int[] dataarray = ksjcam.CaptureRGBdataIntArray(index, width, height);//trigger

        if (dataarray != null && dataarray.length > 0) {

            Bitmap bmp = CreateBitmap_from_int_rgba_data(width, height, dataarray);
            Message message = new Message();
            message.what = 0;
            message.arg1 = index;
            message.obj = bmp;
            hd.sendMessage(message);
        } else {

            return -1;
        }

        return 0;
    }

    public int captureAndshow_Capture(final int index, int width, int height) {
        if (ksjcam.QueryFunction(index, 0) == 1) {

        } else if (ksjcam.QueryFunction(index, 0) == 0) {

        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ImageActivity.this, "显示失败", Toast.LENGTH_SHORT).show();

                }
            });
            return -1;
        }


        return 0;

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

        value += (int) (b[pos] << 0);
        value += (int) (b[pos] << 8);
        value += (int) (b[pos] << 16);
        value += (int) (255 << 24);

        return value;

    }

    public static int[] getIntArray(byte[] b) {

        int[] intArray = new int[b.length];

        for (int i = 0; i < intArray.length; i++) {
            intArray[i] = byteArrayToInt(b, i);
//            if(i>intArray.length-10) {
//                Log.e("TAG", "iiiiii" + i+"   "+(b.length / 4));
//            }
        }
//        Log.e("TAG", "iiiiii" + "end "+"   "+(b.length / 4));
        return intArray;

//        int[] intArray = new int[b.length / 4];
//        for(int i = 0; i < intArray.length; i++) {
//            byte[] byteArray = new byte[4];
//            for(int j = 0; j < byteArray.length; j++) {
//                byteArray[j] = b[i*4 + j];
//            }
//            intArray[i] = byteArrayToInt()
//        }
    }

    public Bitmap createBitmap_from_byte_alpha_data(int width, int height, byte[] buf) {
//        Bitmap bitmap2 = BitmapFactory.decodeByteArray()
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setHasAlpha(false);
//        Log.d("zhanwei","createBitmap_from_byte_alpha_data");
        bitmap.setPixels(getIntArray(buf), 0, width, 0, 0, width, height);
//        getIntArray(buf);
//        bitmap.set

        return bitmap;
    }

    public Bitmap CreateBitmap_from_int_rgba_data(int width, int height, int[] buf) {

        int w = width;//1280;
        int h = height;//1024;

        Log.d("zhanwei", "buf.length    " + String.valueOf(buf.length));

        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        bmp.setHasAlpha(false);

        bmp.setPixels(buf, 0, w, 0, 0, w, h);

//        saveBitmapToSDCard(bmp,"ceshi");
        return bmp;
    }

    @Override
    protected void onPause() {
//        Log.e("TAG", "ImageActivity_onPause");
        captureThreadGo = false;
        sb_switch.setStatus(false);
        tv_fps.setText("fps:0.0");
 //      ksjcam.UnInit();
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
//        Log.e("TAG", "ImageActivity_onDestroy");
//        ksjcam.UnInit();
        hd.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    public void checkPermission() {
        requestRuntimePermission(PermissionUsage.READ_EXTRASORE, new QuestPermissionListener() {
            @Override
            public void doAllPermissionGrant() {//都都授权了，则做些 其他事情
//                Toast.makeText(MainActivity.this, "打开相机1", Toast.LENGTH_SHORT).show();
//                Log.e("TAG", "doAllPermissionGrant");
                captureThreadGo = true;
                startTime = System.currentTimeMillis();
                count = 0;
                startCaptureThread(ksjcam.m_devicecount, m_nwidth, m_nheight); //130W
                showInfo();
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
            showInfo();
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
                        Toast.makeText(ImageActivity.this, "an error occured while writing file...", Toast.LENGTH_SHORT).show();
                    }
                }
            }.start();


        }
    }

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

                ksjcam.CaptureBitmap(0, "/sdcard/bitmapcaptured.bmp");

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
                gainPopup.setData(imageCamera.getRedGain(), imageCamera.getGreenGain(), imageCamera.getBlueGain());
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
                exposurePopup.setData(imageCamera.getExposureTime());
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
                        ksjcam.SetParam(i, KSJ_MIRROR.ordinal(), 1);//镜像  默认0
                    }
                    isMirror = true;
                } else {
                    for (int i = 0; i < ksjcam.m_devicecount; i++) {
                        ksjcam.SetParam(i, KSJ_MIRROR.ordinal(), 0);//镜像  默认0
                    }
                    isMirror = false;
                }
                m_Lock.unlock();
                imageCamera.setMirror(isMirror);
                json = JsonUtil.getJson(imageCamera);
                SharedPreferencesManager.setParam(this, "CAMERA_IMAGE", json);
                break;

            case "Field":
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
                fieldDialog.setData(m_x, m_y, m_nwidth, m_nheight);
                fieldDialog.show();
                m_Lock.lock();
                fieldDialog.setFieldViewListener(new FieldDialog.FieldViewListener() {
                    @Override
                    public void setValue(int x, int y, int width, int height) {
                        for (int i = 0; i < ksjcam.m_devicecount; i++) {
                            ksjcam.CaptureSetFieldOfView(i, x, y, width, height);
                        }
                        imageCamera.setStartX(x);
                        imageCamera.setStartY(y);
                        imageCamera.setWidth(width);
                        imageCamera.setHeight(height);
                        json = JsonUtil.getJson(imageCamera);
                        SharedPreferencesManager.setParam(ImageActivity.this, "CAMERA_IMAGE", json);
                        fieldDialog.dismiss();
                    }
                });
                m_Lock.unlock();
                break;

            case "Image/Surface":
                captureThreadGo = false;
                sb_switch.setStatus(false);
                tv_fps.setText("fps:0.0");
                finish();
                break;

            case "LUT":
                m_Lock.lock();

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
                imageCamera.setLut(isLut);
                json = JsonUtil.getJson(imageCamera);
                SharedPreferencesManager.setParam(this, "CAMERA_IMAGE", json);
                break;

            case "Sensitivity":
                m_Lock.lock();
                int sensitivity = imageCamera.getSensitivity();
                if (sensitivity == 4) {
                    sensitivity = 0;
                } else {
                    sensitivity++;
                }
                for (int i = 0; i < ksjcam.m_devicecount; i++) {
                    Toast.makeText(ImageActivity.this, "当前sensitivity：" + sensitivity, Toast.LENGTH_SHORT).show();
                    ksjcam.SensitivitySetMode(i, sensitivity);
                }
                m_Lock.unlock();
                imageCamera.setSensitivity(sensitivity);
                json = JsonUtil.getJson(imageCamera);
                SharedPreferencesManager.setParam(this, "CAMERA_SURFACE", json);
                break;
        }
    }

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
}
