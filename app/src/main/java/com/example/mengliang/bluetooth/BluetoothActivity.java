package com.example.mengliang.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class BluetoothActivity extends  Activity{

    /* 一些常量，代表服务器的名称 */
    public static final String PROTOCOL_SCHEME_RFCOMM = "btspp";

    private enum eCMode{
        eCMode_Chat,
        eCMode_Debug
    };

    private enum eWMode{
        eWMode_Server,
        eWMode_Cilent
    }

    private Intent intent;

    private FindDeviceBroatcase findDevice=new FindDeviceBroatcase();

    private AlertDialog.Builder dialog;
    private AlertDialog pdialog;
    private List<String> devicelist=new ArrayList<String>();
    private ArrayAdapter<String> deviceAdapter;
    private ListView devicelistView;
    private String deviceAddress = null;
    private String deviceName=null;

    private ProgressBar progressBar;

    private ListView mListView;
    private Button sendButton;
    private Button disconnectButton;
    private EditText editMsgView;
    private ArrayAdapter<String> mAdapter;
    private List<String> msgList=new ArrayList<String>();
    Context mContext;

    private BluetoothServerSocket mserverSocket = null;
    private ServerThread startServerThread = null;
    private clientThread clientConnectThread = null;
    private BluetoothSocket socket = null;
    private BluetoothDevice device = null;
    private readThread mreadThread = null;;
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private boolean BluetoothisOpen = false;

    private eCMode Communicte_Mode;        //通讯模式标志
    private eCMode Communicte_Modet;
    private eWMode Work_Mode;      //工作模式标志
    private eWMode Work_Modet;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat);
        MyApplication.getInstance().addActivity(this);
        mContext = this;

        Work_Mode=eWMode.eWMode_Cilent;

        uiInit();
        bluetoothInit();
    }

    private void uiInit() {

        //intent =getIntent();

        mAdapter=new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, msgList);
        mListView = (ListView) findViewById(R.id.list);
        mListView.setAdapter(mAdapter);
        mListView.setFastScrollEnabled(true);
        editMsgView= (EditText)findViewById(R.id.MessageText);
        editMsgView.clearFocus();

        deviceAdapter=new ArrayAdapter<String>(this,android.R.layout.simple_expandable_list_item_1,devicelist);
        devicelistView=new ListView(BluetoothActivity.this);
        devicelistView.setOnItemClickListener(new DeviceListListener());

        disconnectButton= (Button)findViewById(R.id.btn_disconnect);
        disconnectButton.setOnClickListener(new ConnectButtonListener());

        sendButton= (Button)findViewById(R.id.btn_msg_send);
        sendButton.setOnClickListener(new SendButtonListener());

        progressBar=(ProgressBar)findViewById(R.id.progressBarId1);



        devicelistView.setAdapter(deviceAdapter);

        IntentFilter filter1=new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(findDevice, filter1);

        IntentFilter filter2=new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(findDevice, filter2);
    }

    private void bluetoothInit()
    {
        if(mBluetoothAdapter!=null)
        {
            Intent intent1=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent1,RESULT_FIRST_USER);

            Intent intent2=new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            intent2.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,200);
            startActivity(intent2);
        }
        else
        {
            Toast.makeText(mContext,"不支持蓝牙功能",Toast.LENGTH_SHORT).show();
        }
    }

    private class FindDeviceBroatcase extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action))
            {
                BluetoothDevice btd=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                System.out.println(btd.getName());
                devicelist.add(btd.getName()+'\n'+btd.getAddress());
            }

            if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                progressBar.setVisibility(View.INVISIBLE);      //掩藏进度条
                //dialog=new AlertDialog.Builder(BluetoothActivity.this);
                //dialog.setTitle("请选择蓝牙");
                //dialog.setView(devicelistView);
                //dialog.setPositiveButton("确定", null);
                //dialog.setNegativeButton("取消", null);
                pdialog=dialog.show();
                Toast.makeText(mContext, "搜索结束", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Handler LinkDetectedHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //Toast.makeText(mContext, (String)msg.obj, Toast.LENGTH_SHORT).show();
            if(msg.what==1)
            {
                msgList.add((String)msg.obj);
            }
            else
            {
                msgList.add((String)msg.obj);
            }
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(msgList.size() - 1);
        }
    };

    @Override
    protected void onResume() {

        super.onResume();
    }

    //开启客户端
    private class clientThread extends Thread {
        @Override
        public void run() {
            try {
                //创建一个Socket连接：只需要服务器在注册时的UUID号
                // socket = device.createRfcommSocketToServiceRecord(BluetoothProtocols.OBEX_OBJECT_PUSH_PROTOCOL_UUID);
                socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                //连接
                Message msg2 = new Message();
                msg2.obj = "请稍候，正在连接服务器:"+BluetoothMsg.BlueToothAddress;
                msg2.what = 0;
                LinkDetectedHandler.sendMessage(msg2);

                socket.connect();

                Message msg = new Message();
                msg.obj = "已经连接上服务端！可以发送信息。";
                msg.what = 0;
                LinkDetectedHandler.sendMessage(msg);
                //启动接受数据
                mreadThread = new readThread();
                mreadThread.start();
            }
            catch (IOException e)
            {
                Log.e("connect", "", e);
                Message msg = new Message();
                msg.obj = "连接服务端异常！断开连接重新试一试。";
                msg.what = 0;
                LinkDetectedHandler.sendMessage(msg);
            }
        }
    };

    //开启服务器
    private class ServerThread extends Thread {
        @Override
        public void run() {

            try {
                    /* 创建一个蓝牙服务器
                     * 参数分别：服务器名称、UUID   */
                mserverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(PROTOCOL_SCHEME_RFCOMM,
                        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));

                Log.d("server", "wait cilent connect...");

                Message msg = new Message();
                msg.obj = "请稍候，正在等待客户端的连接...";
                msg.what = 0;
                LinkDetectedHandler.sendMessage(msg);

                    /* 接受客户端的连接请求 */
                socket = mserverSocket.accept();
                Log.d("server", "accept success !");

                Message msg2 = new Message();
                String info = "客户端已经连接上！可以发送信息。";
                msg2.obj = info;
                msg.what = 0;
                LinkDetectedHandler.sendMessage(msg2);
                //启动接受数据
                mreadThread = new readThread();
                mreadThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
    /* 停止服务器 */
    private void shutdownServer() {
        new Thread() {
            @Override
            public void run() {
                if(startServerThread != null)
                {
                    startServerThread.interrupt();
                    startServerThread = null;
                }
                if(mreadThread != null)
                {
                    mreadThread.interrupt();
                    mreadThread = null;
                }
                try {
                    if(socket != null)
                    {
                        socket.close();
                        socket = null;
                    }
                    if (mserverSocket != null)
                    {
                        mserverSocket.close();/* 关闭服务器 */
                        mserverSocket = null;
                    }
                } catch (IOException e) {
                    Log.e("server", "mserverSocket.close()", e);
                }
            };
        }.start();
    }
    /* 停止客户端连接 */
    private void shutdownClient() {
        new Thread() {
            @Override
            public void run() {
                if(clientConnectThread!=null)
                {
                    clientConnectThread.interrupt();
                    clientConnectThread= null;
                }
                if(mreadThread != null)
                {
                    mreadThread.interrupt();
                    mreadThread = null;
                }
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    socket = null;
                }
            };
        }.start();
    }

    //发送数据
    private void sendMessageHandle(String msg)
    {
        if (socket == null)
        {
            Toast.makeText(mContext, "没有连接", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            OutputStream os = socket.getOutputStream();
            os.write(msg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        msgList.add(msg);
        mAdapter.notifyDataSetChanged();
        mListView.setSelection(msgList.size() - 1);
    }
    //读取数据
    private class readThread extends Thread {
        @Override
        public void run() {

            byte[] buffer = new byte[1024];
            int bytes;
            InputStream mmInStream = null;

            try {
                mmInStream = socket.getInputStream();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            while (true) {
                try {
                    // Read from the InputStream
                    if( (bytes = mmInStream.read(buffer)) > 0 )
                    {
                        byte[] buf_data = new byte[bytes];
                        for(int i=0; i<bytes; i++)
                        {
                            buf_data[i] = buffer[i];
                        }
                        String s = new String(buf_data);
                        Message msg = new Message();
                        msg.obj = s;
                        msg.what = 1;
                        LinkDetectedHandler.sendMessage(msg);
                    }
                } catch (IOException e) {
                    try {
                        mmInStream.close();
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    break;
                }
            }
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (Work_Mode==eWMode.eWMode_Cilent)
        {
            shutdownClient();
        }
        else if (Work_Mode==eWMode.eWMode_Server)
        {
            shutdownServer();
        }
        BluetoothisOpen = false;
        //BluetoothMsg.serviceOrCilent = BluetoothMsg.ServerOrCilent.NONE;
    }


    private class ConnectButtonListener implements OnClickListener{
        @Override
        public void onClick(View arg0) {
            // TODO Auto-generated method stub
            if(disconnectButton.getText().equals("连接"))
            {
                if(BluetoothisOpen)
                {
                    Toast.makeText(mContext, "连接已经打开，可以通信。如果要再建立连接，请先断开！", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(Work_Mode==eWMode.eWMode_Cilent)
                {
                    if(deviceAddress!=null)
                    {
                        device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
                        clientConnectThread = new clientThread();
                        clientConnectThread.start();
                        BluetoothisOpen = true;
                        disconnectButton.setText("断开");
                    }
                    else
                    {
                        if(mBluetoothAdapter.isDiscovering())
                        {
                            mBluetoothAdapter.cancelDiscovery();
                            //btserch.setText("重新搜索");
                            Toast.makeText(mContext, "停止搜索", Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            devicelist.clear();
                            //已经配对的设备
                            Set<BluetoothDevice> bddevice=mBluetoothAdapter.getBondedDevices();
                            if(bddevice.size()>0){ //存在已经配对过的蓝牙设备
                                for(Iterator<BluetoothDevice> it=bddevice.iterator();it.hasNext();){
                                    BluetoothDevice btd=it.next();
                                    devicelist.add(btd.getName() + '\n' + btd.getAddress());
                                    //mBluetoothAdapter.notifyDataSetChanged();
                                }
                            }else{  //不存在已经配对过的蓝牙设备
                                devicelist.add("No can be matched to use bluetooth");
                                //adapter.notifyDataSetChanged();
                            }

                            //Toast.makeText(mContext, "开始搜索", Toast.LENGTH_SHORT).show();
                            dialog=new AlertDialog.Builder(BluetoothActivity.this);
                            dialog.setTitle("请选择蓝牙");
                            dialog.setView(devicelistView);
                            dialog.setPositiveButton("搜索", new FindDeviceListener());
                            dialog.setNegativeButton("取消", null);
                            pdialog=dialog.show();
                        }
                    }
                }
                else if(Work_Mode==eWMode.eWMode_Server)
                {
                    startServerThread = new ServerThread();
                    startServerThread.start();
                    BluetoothisOpen = true;
                }
            }
            else if(disconnectButton.getText().equals("断开"))
            {
                disconnectButton.setText("连接");
                if (Work_Mode==eWMode.eWMode_Cilent) {
                    shutdownClient();
                }
                else if (Work_Mode==eWMode.eWMode_Server) {
                    shutdownServer();
                }
                BluetoothisOpen = false;
                //BluetoothMsg.serviceOrCilent = BluetoothMsg.ServerOrCilent.NONE;
                Toast.makeText(mContext, "已断开连接！", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class SendButtonListener implements OnClickListener{
        @Override
        public void onClick(View arg0) {

            String msgText = editMsgView.getText().toString();
            if (msgText.length() > 0) {
                sendMessageHandle(msgText);
                editMsgView.setText("");
                editMsgView.clearFocus();
                //close InputMethodManager
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editMsgView.getWindowToken(), 0);
            } else
                Toast.makeText(mContext, "发送内容不能为空！", Toast.LENGTH_SHORT).show();
        }
    }

    private class DeviceListListener implements OnItemClickListener{
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final String msg = devicelist.get(position);
            deviceAddress=msg.substring(msg.length()-17);
            deviceName=msg.substring(0,msg.length()-18);
            System.out.println("addr="+deviceAddress);
            System.out.println("name="+deviceName);
            pdialog.dismiss();      //退出对话框
        }
    }

    private class FindDeviceListener implements DialogInterface.OnClickListener{
        @Override
        public void onClick(DialogInterface dialog, int which) {
            mBluetoothAdapter.startDiscovery();
            pdialog.dismiss();
            progressBar.setVisibility(View.VISIBLE);        //显示进度条
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //menu.add("菜单项一");
        //menu.add("菜单项二");
        //menu.add("菜单项三");
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {

        switch (item.getItemId())
        {
            case R.id.CommunicteModeId:
                //do
                Com_modeSelect(Communicte_Mode);
                break;
            case R.id.WorkModeId:
                Work_modeSelect(Work_Mode);
                break;
            case R.id.exitId:
                //do
//                android.os.Process.killProcess(android.os.Process.myPid());
//                //System.exit(0);
//                Intent home = new Intent(Intent.ACTION_MAIN);
//
//                home.addCategory(Intent.CATEGORY_HOME);
//
//                startActivity(home);
                MyApplication.getInstance().exit();
                break;
            default:
                break;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private void Com_modeSelect(eCMode cur_mode)
    {
        int tempVal=0;
        if(cur_mode==eCMode.eCMode_Chat)    tempVal=0;
        else if(cur_mode==eCMode.eCMode_Debug)  tempVal=1;
        AlertDialog.Builder dialog=new AlertDialog.Builder(BluetoothActivity.this);
        dialog.setTitle("交流模式选择");

        dialog.setSingleChoiceItems(new String[]{"聊天模式", "调试模式"}, tempVal, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        Communicte_Modet = eCMode.eCMode_Chat;
                        break;
                    case 1:
                        Communicte_Modet = eCMode.eCMode_Debug;
                        break;
                }
            }
        });
        dialog.setNegativeButton("取消", null);
        dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Communicte_Mode=Communicte_Modet;
            }
        });
        dialog.show();
    }

    private void Work_modeSelect(eWMode cur_mode)
    {
        int tempVal=0;
        if(cur_mode==eWMode.eWMode_Server)    tempVal=0;
        else if(cur_mode==eWMode.eWMode_Cilent)  tempVal=1;
        AlertDialog.Builder dialog=new AlertDialog.Builder(BluetoothActivity.this);
        dialog.setTitle("工作模式选择");
        dialog.setSingleChoiceItems(new String[]{"服务器", "客户端"}, tempVal, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        Work_Modet = eWMode.eWMode_Server;
                        break;
                    case 1:
                        Work_Modet = eWMode.eWMode_Cilent;
                        break;
                }
            }
        });
        dialog.setNegativeButton("取消", null);
        dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Work_Mode=Work_Modet;
            }
        });
        dialog.show();
    }

}
