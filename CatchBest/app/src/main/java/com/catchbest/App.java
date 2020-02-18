package com.catchbest;

import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.view.CrashHandler;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by gmm on 2018/1/4.
 */

public class App extends Application {
    //用于格式化日期,作为日志文件名的一部分
    private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    @Override
    public void onCreate() {
        super.onCreate();


        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(getApplicationContext());
//
//        String apkRoot="chmod -R 777 "+getPackageCodePath();
//        RootCommand(apkRoot);
//
//        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
//        final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
//        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
//        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
//
//        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0,
//                new Intent(ACTION_USB_PERMISSION), 0);
//        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
//        registerReceiver(mUsbReceiver, filter);
//
//        int fd = -1;
//        while(deviceIterator.hasNext()){
//            UsbDevice device = deviceIterator.next();
//            Log.i("TAG", device.getDeviceName() + " " + Integer.toHexString(device.getVendorId()) +
//                    " " + Integer.toHexString(device.getProductId()));
//
//            manager.requestPermission(device, mPermissionIntent);
//            UsbDeviceConnection connection = manager.openDevice(device);
//            if(connection != null){
//                fd = connection.getFileDescriptor();
//            } else
//                Log.e("TAG", "UsbManager openDevice failed");
//            break;
//        }
    }


    /**
     * 11
     * 应用程序运行命令获取 Root权限，设备必须已破解(获得ROOT权限)
     * 12
     *
     * @param command 命令：String apkRoot="chmod 777 "+getPackageCodePath(); RootCommand(apkRoot);
     *                13
     * @return 应用程序是/否获取Root权限
     * 14
     */

    public static boolean RootCommand(String command)

    {

        Process process = null;

        DataOutputStream os = null;

        try

        {

            process = Runtime.getRuntime().exec("su");

            os = new DataOutputStream(process.getOutputStream());

            os.writeBytes(command + "\n");

            os.writeBytes("exit\n");

            os.flush();

            process.waitFor();

        } catch (Exception e)

        {

            Log.d("*** DEBUG ***", "ROOT REE" + e.getMessage());

            return false;

        } finally

        {

            try

            {

                if (os != null)

                {

                    os.close();

                }

                process.destroy();

            } catch (Exception e)

            {

            }

        }

        Log.d("*** DEBUG ***", "Root SUC ");

        return true;

    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.android.example.USB_PERMISSION".equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {

                        }
                    } else {
//                        showTmsg("用户不允许USB访问设备，程序退出！");
                        Toast.makeText(App.this, "用户不允许USB访问设备，程序退出！", Toast.LENGTH_SHORT).show();
//                        finish();
                    }
                }
            }
        }
    };


}
