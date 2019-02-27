package com.lxl.easy.mqtt.demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.provider.Settings.Secure;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;

import com.lxl.easy.mqtt.EasyMqttService;
import com.lxl.easy.mqtt.IEasyMqttCallBack;

/**
 * @author sheldon
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String TAG="EasyMQTT";
    private EasyMqttService mqttService;

    EditText addressET = null;
    EditText topicET = null;
    EditText messageET = null;
    EditText receiveET = null;
    EditText userNameET = null;
    EditText passwordET = null;

    Button connectButton = null;
    Button disconnectButton = null;
    Button sendButton = null;

    String sAddress = null;
    String sUserName = null;
    String sPassword = null;
    String sTopic = null;
    String sMessage = null;
    String sDeviceID = null;

    private final String[] strPermissions  = new String[] {
            //Manifest.permission.CAMERA,
            //Manifest.permission.RECORD_AUDIO,
            //Manifest.permission.READ_EXTERNAL_STORAGE,
            //Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE
    };

    /**
     * 回调时使用
     */
    private final int MQTT_PERMISSION_REQUEST_CODE = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * 检测权限
         */
        /* 判断SDK版本，确认是否动态申请权限 **/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            /* 第 1 步: 检查是否有相应的权限 **/
            if(!checkPermissionAllGranted(strPermissions)) {
                /* 第 2 步: 请求权限,一次请求多个权限, 如果其他有权限是已经授予的将会自动忽略掉 **/
                ActivityCompat.requestPermissions(
                        this,
                        strPermissions,
                        MQTT_PERMISSION_REQUEST_CODE
                );
            }

            /* 第 3 步: 判断权限申请结果，如用户未同意则引导至设置界面打开权限 **/
            int[] grantResults={0};
            onRequestPermissionsResult(MQTT_PERMISSION_REQUEST_CODE,strPermissions,grantResults);
        }

        /**
         * 初始化界面
         */
        setupView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        close(); //关闭MQTT连接
    }


    /**
     * 按键监听
     */
    @Override
    public void onClick(View v) {
        if(v == connectButton)
        {
            sAddress = addressET.getText().toString().trim();
            sUserName = userNameET.getText().toString().trim();
            sPassword = passwordET.getText().toString().trim();

            if(sAddress.equals(""))
            {
                Utils.ToastShow(getApplicationContext() , Toast.LENGTH_SHORT, Gravity.CENTER_HORIZONTAL,
                        "Service address must be provided", null);
            }
            else
            {
                Utils.savePreferences(getApplicationContext(),"address", sAddress);
                Utils.savePreferences(getApplicationContext(),"userName", sUserName);

                buildEasyMqttService(); // 根据输入的IP和端口建立服务
                connect(); // 进行连接，成功回调里面执行主题订阅和消息发布
            }
        }

        if(v == disconnectButton)
        {
            disconnect();
        }

        if(v == sendButton)
        {
            sMessage = messageET.getText().toString().trim();
            sTopic = topicET.getText().toString().trim();

            // allow empty messages
            if(sMessage.equals("") || sTopic.equals(""))
            {
                Utils.ToastShow(getApplicationContext() , Toast.LENGTH_SHORT, Gravity.CENTER_HORIZONTAL,
                        "Message and topic must be provided", null);
            }
            else
            {
                Utils.savePreferences(getApplicationContext(),"topic", sTopic);
                Utils.savePreferences(getApplicationContext(),"message", sMessage);
                send();
            }
        }
    }

    /**
     * 申请权限结果返回处理
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MQTT_PERMISSION_REQUEST_CODE) {
            boolean isAllGranted = true;

            // 判断是否所有的权限都已经授予了
            for (int grant : grantResults) {
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    isAllGranted = false;
                    break;
                }
            }

            if (!isAllGranted) {
                // 弹出对话框告诉用户需要权限的原因, 并引导用户去应用权限管理中手动打开权限按钮
                openAppDetails();
            }
        }
    }

    /**
     * 检查是否拥有指定的所有权限
     */
    private boolean checkPermissionAllGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                // 只要有一个权限没有被授予, 则直接返回 false
                return false;
            }
        }
        return true;
    }

    /**
     * 打开 APP 的详情设置
     */
    private void openAppDetails() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getResources().getString(R.string.permission_guide));
        builder.setPositiveButton(getResources().getString(R.string.permission_manual), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setData(Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(intent);
            }
        });
        builder.setNegativeButton(getResources().getString(R.string.permission_cancel), null);
        builder.show();
    }

    /**
     * 初始化UI变量
     */
    @SuppressLint("HardwareIds")
    public void setupView()
    {
        // lock the screen in portrait mode
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        sDeviceID = Secure.getString(this.getContentResolver(), Secure.ANDROID_ID);
        addressET = (EditText)findViewById(R.id.addressEditText);
        String serverAddr = Utils.getPreferences(getApplicationContext(),"address");
        addressET.setText(serverAddr==null?"tcp://47.96.206.64:8085":serverAddr);
        userNameET = (EditText)findViewById(R.id.userNameEditText);
        userNameET.setText(Utils.getPreferences(getApplicationContext(),"userName"));
        passwordET = (EditText)findViewById(R.id.passwordEditText);
        topicET = (EditText)findViewById(R.id.topicEditText);
        topicET.setText(Utils.getPreferences(getApplicationContext(),"topic"));
        messageET = (EditText)findViewById(R.id.messageEditText);
        messageET.setText(Utils.getPreferences(getApplicationContext(),"message"));
        receiveET = (EditText)findViewById(R.id.receiveEditText);

        connectButton = (Button)findViewById(R.id.connectButton);
        connectButton.setOnClickListener(this);

        disconnectButton = (Button)findViewById(R.id.disconnectButton);
        disconnectButton.setOnClickListener(this);

        sendButton = (Button)findViewById(R.id.sendButton);
        sendButton.setOnClickListener(this);
    }

    /**
     * 判断服务是否连接
     */
    private boolean isConnected() {
        return mqttService.isConnected();
    }

    /**
     * 发布消息
     */
    private void publish(String msg, String topic, int qos, boolean retained) {
        if(isConnected())
            mqttService.publish(msg, topic, qos, retained);
        else {
            Utils.ToastShow(getApplicationContext(), Toast.LENGTH_SHORT, Gravity.CENTER_HORIZONTAL,
                    "Error: Already disconnected!", null);
            sendButton.setEnabled(false);
        }
    }

    /**
     * 断开连接
     */
    private void disconnect() {
        if(mqttService!=null)
            mqttService.disconnect();

        sendButton.setEnabled(false);
        Log.i(TAG,"disconnect");
    }

    /**
     * 关闭连接
     */
    private void close() {
        if(mqttService!=null){
            if(isConnected()){
                disconnect();
            }
            mqttService.close();
        }
        Log.i(TAG,"close");
    }

    /**
     * 订阅主题 这里订阅三个主题分别是"bulb", "door", "window"
     */
    private void subscribe() {
        String[] topics = new String[]{"bulb", "door", "window"};
        //主题对应的推送策略 分别是0, 1, 2 建议服务端和客户端配置的主题一致
        // 0 表示只会发送一次推送消息 收到不收到都不关心
        // 1 保证能收到消息，但不一定只收到一条
        // 2 保证收到切只能收到一条消息
        int[] qoss = new int[]{0, 1, 2};

        for(String s:topics)
            Log.i(TAG,"subscribe topic : " + s);

        mqttService.subscribe(topics, qoss);
    }

    /**
     * 连接Mqtt服务器
     */
    private void connect() {
        Log.i(TAG,"connect...");
        mqttService.connect(new IEasyMqttCallBack() {
            @Override
            public void messageArrived(String topic, String message, int qos) {
                //推送消息到达
                Log.i(TAG,"messageArrived : "+message+", qos="+qos);
                receiveET.setText(message);
            }

            @Override
            public void connectionLost(Throwable arg0) {
                //连接断开
                sendButton.setEnabled(false);
                Log.i(TAG,"connectionLost");
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken arg0) {
                Log.i(TAG,"deliveryComplete");
            }

            @Override
            public void connectSuccess(IMqttToken arg0) {
                //连接成功
                Log.i(TAG,"connectSuccess");

                //订阅主题
                subscribe();

                sendButton.setEnabled(true);
            }

            @Override
            public void connectFailed(IMqttToken arg0, Throwable arg1) {
                //连接失败
                Utils.ToastShow(getApplicationContext() , Toast.LENGTH_SHORT, Gravity.CENTER_HORIZONTAL,
                        "Please check network!", null);

                sendButton.setEnabled(false);
                Log.i(TAG,"connectFailed");
            }
        });
    }

    /**
     * 发布消息
     */
    private void send() {
        //消息策略
        // 0 表示只会发送一次推送消息 收到不收到都不关心
        // 1 保证能收到消息，但不一定只收到一条
        // 2 保证收到切只能收到一条消息
        int qos = 0;

        //发布消息
        publish(sMessage, sTopic, qos, false);

        Log.i(TAG,"publish msg : " + sMessage + ", topic : " + sTopic);
    }

    /**
     * 构建EasyMqttService对象
     */
    private void buildEasyMqttService() {
        mqttService = new EasyMqttService.Builder()
                //设置自动重连
                .autoReconnect(true)
                //设置不清除回话session 可收到服务器之前发出的推送消息
                .cleanSession(false)
                //唯一标示 保证每个设备都唯一就可以
                .clientId(sDeviceID)
                //mqtt服务器地址 格式例如：tcp://10.0.261.159:1883
                .serverUrl(sAddress)
                //心跳包默认的发送间隔
                .keepAliveInterval(20)
                //构建出EasyMqttService 建议用application的context
                .bulid(this.getApplicationContext());

        Log.i(TAG,"sDeviceID:"+sDeviceID+", sAddress:"+sAddress);
    }

}
