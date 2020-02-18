package com.catchbest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;


/**
 * 类功能描述：6.0运行时权限 </br>
 * activity管理器</br>
 * 博客地址：http://blog.csdn.net/androidstarjack
 * @author 老于
 * Created  on 2017/1/3/002
 * @version 1.0 </p> 修改时间：</br> 修改备注：</br>
 */
public class ActivityManager {
    /**
     * skip to @param(cls)，and call @param(aty's) finish() method
     */
    public static void skipActivity(Activity aty, Class<?> cls) {
        showActivity(aty, cls);
        aty.finish();
    }

    /**
     * skip to @param(cls)，and call @param(aty's) finish() method
     */
    public static void skipActivity(Activity aty, Intent it) {
        showActivity(aty, it);
        aty.finish();
    }

    /**
     * skip to @param(cls)，and call @param(aty's) finish() method
     */
    public static void skipActivity(Activity aty, Class<?> cls, Bundle extras) {
        showActivity(aty, cls, extras);
        aty.finish();
    }

    /**
     * show to @param(cls)，but can't finish activity
     */
    public static void showActivity(Context aty, Class<?> cls) {
        Intent intent = new Intent();
        intent.setClass(aty, cls);
        aty.startActivity(intent);
//        AnimationUtil.showNext(aty);
    }
    /**
     * show to @param(cls)，but can't finish activity
     */
    public static void showActivity(Activity aty, Intent it) {
        aty.startActivity(it);
    }

    /**
     * show to @param(cls)，but can't finish activity
     */
    public static void showActivity(Context aty, Class<?> cls, Bundle extras) {
        Intent intent = new Intent();
        intent.putExtras(extras);
        intent.setClass(aty, cls);
        aty.startActivity(intent);
//        AnimationUtil.showNext(aty);
    }

    /**
     * extends 权限获取的 activity
     */
    public static List<Activity> permissionActivilyList = new ArrayList<>();
    public static void addPermissionActivty(Activity activty){
        if(permissionActivilyList.contains(activty)){
            permissionActivilyList.remove(activty);
            permissionActivilyList.add(activty);
        }else{
            permissionActivilyList.add(activty);
        }
    }
    public static void removePermissionActiivty(Activity activty){
        if(permissionActivilyList.contains(activty)){
            permissionActivilyList.remove(activty);
        }
    }
}
