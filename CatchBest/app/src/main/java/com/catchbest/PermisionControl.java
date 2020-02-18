package com.catchbest;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

import com.callback.QuestPermissionListener;
import com.phone.fuxi.catchbest.BaseActivity;


/**
 * 类功能描述：6.0运行时权限 </br>
 * permission权限控制器</br>
 * 博客地址：http://blog.csdn.net/androidstarjack
 * @author 老于
 * Created  on 2017/1/3/002
 * @version 1.0 </p> 修改时间：</br> 修改备注：</br>
 */
public class PermisionControl {
    /**
     * 检查所有的权限是否被禁止
     */
    public static boolean lackMissPermission(Context cnt , String... permission){
        boolean relust = true ;
        for (String per : permission) {
            if(ActivityCompat.checkSelfPermission(cnt,per) == PackageManager.PERMISSION_DENIED){
                relust = false ;
                break;
            }
        }
        return relust;
    }

    /**
     * 请求运行时权限
     * eg:
     */
    public void requestRuntimePermission(QuestPermissionListener questPermissionListener, String... permissions){
        BaseActivity.requestRuntimePermission(permissions,questPermissionListener);
    }
}
