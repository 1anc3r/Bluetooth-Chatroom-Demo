package me.lancer.bluetoothchatroom.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import me.lancer.bluetoothchatroom.adapter.MsgAdapter;
import me.lancer.bluetoothchatroom.util.BluetoothChatroomUtility;
import me.lancer.bluetoothchatroom.R;
import me.lancer.bluetoothchatroom.service.BluetoothService;
import me.lancer.bluetoothchatroom.service.BluetoothServiceHandler;
import me.lancer.bluetoothchatroom.service.BluetoothServiceUtility;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ChatroomActivity extends AppCompatActivity {

    public static final String TAG = "ChatroomActivity";
    public static final String EXTRA_SERVER = "EXTRA_SERVER";

    private EditText mEditText;
    private Button btnSend;
    private RecyclerView mRecyclerView;
    private MsgAdapter mAdapter;
    private List<String> mList = new ArrayList<>();

    private BluetoothService.BluetoothBinder mBinder;
    private MyBluetoothHandler mBT_Handler;

    private String mName;
    private String mAddress;
    private boolean mConnected;
    private boolean mIsServer;

    private static class MyBluetoothHandler extends BluetoothServiceHandler {

        private final WeakReference<ChatroomActivity> mActivity;

        public MyBluetoothHandler(ChatroomActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void serverSetupFinished() {
            Log.w(TAG, "Should not have received a call to serverSetupFinished");
        }

        @Override
        public void connectionClosed(String macAddress, @BluetoothServiceUtility.CLOSE_CODE int closeCode) {
            Toast.makeText(mActivity.get(), BluetoothServiceUtility.getCloseCodeInfo(closeCode) +
                    " : 与 " + macAddress +" 断开连接", Toast.LENGTH_SHORT).show();
            //TODO if server, tell all clients that somebody disconnected.
        }

        @Override
        public void appMessage(String address, byte[] data) {
            String msg = new String(data);
            if (data.length < BluetoothChatroomUtility.ID_LENGTH) {
                Log.w(TAG, "unreadable message " + msg);
                return;
            }

            String messageId = (msg.substring(0, BluetoothChatroomUtility.ID_LENGTH));
            String messageData = "";
            if (msg.length() > BluetoothChatroomUtility.ID_LENGTH)
                messageData = msg.substring(BluetoothChatroomUtility.ID_LENGTH, msg.length());

            switch (messageId) {
                case BluetoothChatroomUtility.ID_SEND_DISPLAY_TEXT:
                    mActivity.get().showMessage(messageData);
                    if (mActivity.get().mIsServer) {
                        String appMessage = BluetoothChatroomUtility.makeDisplayTextMessage(messageData);
                        mActivity.get().mBinder.writeMessage(appMessage.getBytes());
                    }
                    break;
                default:
                    Log.v(TAG, " unknown chatroom message id " + messageId + ", with message " + messageData);
                    break;
            }
        }
    }

    ServiceConnection mBluetoothConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v(TAG, "BluetoothConnection connected");
            mConnected = true;
            mBinder = (BluetoothService.BluetoothBinder) service;
            mBinder.setHandler(mBT_Handler);

            if (mIsServer) {
                mBinder.serverReady();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v(TAG, "BluetoothConnection disconnected");
            mConnected = false;
            mBinder = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatroom);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mIsServer = getIntent().getBooleanExtra(EXTRA_SERVER, false);

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        mName = adapter.getName();
        mAddress = adapter.getAddress();

        mEditText = (EditText) findViewById(R.id.et_msg);
        btnSend = (Button) findViewById(R.id.btn_send);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = mEditText.getText().toString();
                String message = mName + "-----" + text;
                mEditText.setText("");
                if (mConnected) {
                    String chatMessage = BluetoothChatroomUtility.makeDisplayTextMessage(message);
                    mBinder.writeMessage(chatMessage.getBytes());
                }
                if (mIsServer)
                    showMessage(message);
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.rv_chat);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new MsgAdapter(this, mName, mList);
        mRecyclerView.setAdapter(mAdapter);

        mBT_Handler = new MyBluetoothHandler(this);
        bindService(new Intent(this, BluetoothService.class), mBluetoothConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mConnected) {
            unbindService(mBluetoothConnection);
        }

        mConnected = false;
        mBinder = null;
    }

    public void showMessage(String message) {
        mList.add(message);
        mAdapter.notifyDataSetChanged();
    }
}
