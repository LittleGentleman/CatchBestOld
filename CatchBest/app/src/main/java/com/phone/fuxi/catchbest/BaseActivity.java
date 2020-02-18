package com.phone.fuxi.catchbest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;

import com.callback.QuestPermissionListener;
import com.catchbest.ActivityManager;
import com.catchbest.PermisionControl;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gmm on 2018/1/5.
 */

public class BaseActivity extends AppCompatActivity {

    private static final int REQUESTPERMISSION_CODE = 100;
    private static List<String> needRequestPermissionList;
    private static QuestPermissionListener questPermissionListener;

    private AlertDialog.Builder builder;
    private AlertDialog alertDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityManager.addPermissionActivty(this);
    }

    public static  void requestRuntimePermission(String [] permissions,QuestPermissionListener questPermissionListener){
        Activity activity = ActivityManager.permissionActivilyList.get(ActivityManager.permissionActivilyList.size() -1);
        if(activity == null){
            return;
        }
        needRequestPermissionList = new ArrayList<String>();
        for (String permission : permissions) {
            if(ActivityCompat.checkSelfPermission(activity,permission) == PackageManager.PERMISSION_DENIED){
                needRequestPermissionList.add(permission);
            }
        }
        BaseActivity.questPermissionListener  = questPermissionListener;
        if(!needRequestPermissionList.isEmpty()){
            ActivityCompat.requestPermissions(activity, needRequestPermissionList.toArray(new String[needRequestPermissionList.size()]),REQUESTPERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUESTPERMISSION_CODE){
            for (int i = 0; i < grantResults.length; i++) {
                int granrRequest = grantResults[i];
                if(granrRequest == PackageManager.PERMISSION_DENIED){//权限被拒绝啦
                    if(questPermissionListener != null){
                        questPermissionListener.denySomePermission();
                    }
                    return;
                }
            }
            questPermissionListener.doAllPermissionGrant();
        }
    }

    public static List<String> getNeedRequestPermissionList() {
        return needRequestPermissionList;
    }


    public void onResumeCheckPermission(String ...permission){
        if(PermisionControl.lackMissPermission(this,permission)){
            showMissPermissionDialog();
        }
    }

    /**
     * 丢失权限，进行弹框设置
     */
    public void showMissPermissionDialog() {
        if (builder == null) {
            builder = new AlertDialog.Builder(BaseActivity.this);
            builder.setTitle("帮助");
            String msg = "当前应用缺少必要权限。<br>\r请点击\"设置\"-\"权限\"-打开所需权限。<br>\r\r最后点击两次后退按钮，即可返回。";
            builder.setMessage(Html.fromHtml(msg));
            //materialDialog.setMovementMethod(LinkMovementMethod.getInstance());
            builder.setPositiveButton("设置", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    alertDialog.dismiss();
                }
            });
            builder.setNegativeButton("退出", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    alertDialog.dismiss();
                    startAppSettings();
                }
            });
            alertDialog = builder.create();
            alertDialog.show();
        } else {
            alertDialog.show();
        }
    }
    // 启动应用的设置
    public void startAppSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:"+ getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
