package com.lxl.easy.mqtt.demo;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    static final String TAG = "Utils";

    /**
     * 判断service是否已经运行
     * 必须判断uid,因为可能有重名的Service,所以要找自己程序的Service
     *
     * @param className Service的全名,例如PushService.class.getName() *
     * @return true:Service已运行 false:Service未运行
     */
    public static boolean isServiceRunning(Context context, String className) {
        Log.i(TAG, "===check service state...");
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList = am.getRunningServices(Integer.MAX_VALUE);
        int myUid = android.os.Process.myUid();
        for (ActivityManager.RunningServiceInfo runningServiceInfo : serviceList) {
            if (runningServiceInfo.uid == myUid && runningServiceInfo.service.getClassName().equals(className)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Toast显示即时信息：
     * 每次把上一条取消以打达到显示最新一条消息
     */
    private static Toast mToast = null;

    public static void ToastShow(final Context context, int length, final int pos, final String mainText, final String subText) {
        //拼接主信息和次信息
        String toastText = mainText + (TextUtils.isEmpty(subText) ? "" : ("\t" + subText));

        //如果上一条还没显示完也立即取消
        if (mToast != null)
            mToast.cancel();

        mToast = Toast.makeText(context, toastText, length);
        mToast.setGravity(pos, 0, 0);
        mToast.show();
    }

    /**
     * 获得当前APP的版本号：
     */
    public static int getAppVersionCode(Context mContext) {
        PackageManager manager = mContext.getPackageManager();
        int code = 0;
        try {
            PackageInfo info = manager.getPackageInfo(mContext.getPackageName(), 0);
            code = info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } return code;
    }

    /**
     * 获得当前APP的版本名：
     */
    public static String getAppVersionName(Context mContext) {
        PackageManager manager = mContext.getPackageManager();
        String name=null;
        try {
            PackageInfo info = manager.getPackageInfo(mContext.getPackageName(), 0);
            name = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } return name;
    }

    /**
     * 获得当前APP的包名：
     */
    public static String getAppPktName(Context mContext) {
        PackageManager manager = mContext.getPackageManager();
        String pktName = null;
        try {
            PackageInfo info = manager.getPackageInfo(mContext.getPackageName(), 0);
            pktName = info.packageName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return pktName;
    }

    /**
     * 保存输入数据，以便下次自动填入
     */
    public static void savePreferences(Context ctx, String entry, String value) {
        //实例化SharedPreferences对象（第一步）
        SharedPreferences mySharedPreferences= ctx.getSharedPreferences("test",
                Activity.MODE_PRIVATE);
        //实例化SharedPreferences.Editor对象（第二步）
        SharedPreferences.Editor editor = mySharedPreferences.edit();
        //用putString的方法保存数据
        editor.putString(entry, value);
        //提交当前数据
        editor.commit();
        Log.d(TAG,"savePreferences : <" + entry + "," + value + ">");
    }

    public static String getPreferences(Context ctx, String entry) {
        //同样，在读取SharedPreferences数据前要实例化出一个SharedPreferences对象
        SharedPreferences sharedPreferences= ctx.getSharedPreferences("test",
                Activity.MODE_PRIVATE);
        // 使用getString方法获得value，注意第2个参数是value的默认值
        return sharedPreferences.getString(entry,null);
    }

    /**
     * 利用正则表达式判断字符串是否是数字
     * @param str
     * @return
     */
    public static boolean isNumeric(String str){
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        if( !isNum.matches() ){
            return false;
        }
        return true;
    }

    /**
     * 判断是否快速双击
     * @return
     */
    private static long lastClickTime=0;
    public static boolean isFastDoubleClick() {
        long time = System.currentTimeMillis();
        long timeD = time - lastClickTime;
        Log.w(TAG," Double click timeD = " + timeD);
        if ( 0 < timeD && timeD < 500) {
            return true;
        }
        lastClickTime = time;
        return false;
    }

    /**
     * 从JSON字符串获取key值
     *
     * @param data
     * @param key
     * @return
     */
    public static String getValueFromJson(String data, String key) {
        JSONObject dataJson;
        try {
            dataJson = new JSONObject(data);
            String value = dataJson.getString(key);
            return value;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}

