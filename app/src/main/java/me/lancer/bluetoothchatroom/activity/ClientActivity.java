package me.lancer.bluetoothchatroom.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.pnikosis.materialishprogress.ProgressWheel;

import me.lancer.bluetoothchatroom.adapter.ClientAdapter;
import me.lancer.bluetoothchatroom.aynctask.ClientConnectTask;
import me.lancer.bluetoothchatroom.R;
import me.lancer.bluetoothchatroom.receiver.BluetoothDiscoverReceiver;
import me.lancer.bluetoothchatroom.receiver.BluetoothStatusReceiver;
import me.lancer.bluetoothchatroom.service.BluetoothService;
import me.lancer.bluetoothchatroom.service.BluetoothServiceHandler;
import me.lancer.bluetoothchatroom.service.BluetoothServiceUtility;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Set;

public class ClientActivity extends AppCompatActivity implements BluetoothStatusReceiver.BlueToothStatusListener,
        BluetoothDiscoverReceiver.BlueToothDiscoverListener, ClientConnectTask.ClientEventListener,
        ClientAdapter.PairingItemClickListener {

    private static final String TAG = "ClientActivity";

    @IntDef({DEFAULT, FINDING_EMPTY, FINDING_NOT_EMPTY, FOUND_DEVICES, CONNECTING, CONNECTED})
    @Retention(RetentionPolicy.SOURCE)

    public @interface PAIRING_STATE {
    }

    public static final int DEFAULT = 0;
    public static final int FINDING_EMPTY = 1;
    public static final int FINDING_NOT_EMPTY = 2;
    public static final int FOUND_DEVICES = 3;
    public static final int CONNECTING = 4;
    public static final int CONNECTED = 5;

    private TextView mMessageText;
    private Button mFab;
    private RecyclerView mRecyclerView;
    private ClientAdapter mAdapter;
    private ViewGroup mConnectionViewGroup;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothStatusReceiver mBTStatusReceiver = null;
    private BluetoothDiscoverReceiver mBTDReceiver = null;

    private ClientConnectTask mClientTask;
    private MyBluetoothHandler mHandler;
    private BluetoothServiceConnection mBluetoothConnection;
    private BluetoothService.BluetoothBinder mBinder;
    private boolean mServiceConnected;
    public
    @PAIRING_STATE
    int mState = DEFAULT;

    private void changeState(@PAIRING_STATE int state) {
        if (mState == state) {
            Log.v(TAG, "going to same state " + state);
            return;
        }
        switch (state) {
            case DEFAULT:
                if (mState != FINDING_EMPTY)
                    throw new IllegalArgumentException("cannot go from state" + mState + " to state " + state);
                mMessageText.append(getResources().getString(R.string.pairing_message_search_failed) + "\n" + getResources().getString(R.string.pairing_action_click_fab)+"\n");
                break;
            case FINDING_EMPTY:
                if (mState != DEFAULT && mState != FOUND_DEVICES)
                    throw new IllegalArgumentException("cannot go from state" + mState + " to state " + state);
                mMessageText.append(getResources().getString(R.string.pairing_message_searching)+"\n");
                break;
            case FINDING_NOT_EMPTY:
                if (mState != FINDING_EMPTY && mState != DEFAULT && mState != FOUND_DEVICES)
                    throw new IllegalArgumentException("cannot go from state" + mState + " to state " + state);
                mMessageText.append(getResources().getString(R.string.pairing_message_searching) + "\n" + getResources().getString(R.string.pairing_action_click_btd)+"\n");
                break;
            case FOUND_DEVICES:
                if (mState != CONNECTING && mState != CONNECTED && mState != FINDING_NOT_EMPTY)
                    throw new IllegalArgumentException("cannot go from state" + mState + " to state " + state);
                if (mState == FINDING_NOT_EMPTY)
                    mMessageText.append(getResources().getString(R.string.pairing_message_search_finished)+"\n");
                else if (mState == CONNECTING)
                    mMessageText.append(getResources().getString(R.string.pairing_message_connect_failed)+"\n");
                else
                    mMessageText.append(getResources().getString(R.string.pairing_message_connection_closed) + "\n" + getResources().getString(R.string.pairing_action_click_btd) + "\n" + getResources().getString(R.string.pairing_action_click_fab)+"\n");
                break;
            case CONNECTING:
                if (mState != FOUND_DEVICES && mState != FINDING_NOT_EMPTY)
                    throw new IllegalArgumentException("cannot go from state" + mState + " to state " + state);
                mMessageText.append(getResources().getString(R.string.pairing_message_connecting)+"\n");
                break;
            case CONNECTED:
                if (mState != CONNECTING)
                    throw new IllegalArgumentException("cannot go from state" + mState + " to state " + state);
                mMessageText.append(getResources().getString(R.string.pairing_message_connected) + "\n" + getResources().getString(R.string.pairing_action_waiting)+"\n");
                break;
            default:
                throw new IllegalArgumentException("invalid state " + state + " sent to changeState");
        }

        Log.v(TAG, "going from state " + mState + " to state " + state);
        mState = state;
    }

    private void resetState() {
        mState = DEFAULT;
        mMessageText.append(getResources().getString(R.string.pairing_action_click_fab)+"\n");
    }

    private ArrayList<BluetoothDevice> getPairs() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        ArrayList<BluetoothDevice> devices = new ArrayList<>(pairedDevices.size());
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                devices.add(device);
            }
        }
        return devices;
    }

    private void transitionConnectionVisibility(boolean showConnection) {
        if (showConnection) {
            mConnectionViewGroup.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
//            mFab.hide();
            mFab.setVisibility(View.GONE);
        } else {
            mConnectionViewGroup.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
            if (mBluetoothAdapter.isDiscovering())
//                mFab.hide();
                mFab.setVisibility(View.GONE);
            else
//                mFab.show();
                mFab.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "蓝牙不可用", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            bluetoothStateChanged(BluetoothStatusReceiver.BLUETOOTH_OFF);
        }

        mBluetoothConnection = new BluetoothServiceConnection();

        TextView t = ((TextView) findViewById(R.id.tv_bluetooth_name));
        if (t != null)
            t.setText(mBluetoothAdapter.getName());

        t = (TextView) findViewById(R.id.tv_bluetooth_address);
        if (t != null)
            t.setText(mBluetoothAdapter.getAddress());

        mFab = (Button) findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                mFab.hide();
                mFab.setVisibility(View.GONE);
                if (mBluetoothAdapter.isDiscovering())
                    return;
                mAdapter.clearData();
                mBluetoothAdapter.startDiscovery();

            }
        });

        mMessageText = (TextView) findViewById(R.id.tv_msg);

        mConnectionViewGroup = (ViewGroup) findViewById(R.id.connection_view_group);

        mRecyclerView = (RecyclerView) findViewById(R.id.rv_server);
        if (mRecyclerView != null)
            mRecyclerView.setHasFixedSize(true);
        mAdapter = new ClientAdapter(this);
        mAdapter.setPairingItemClickListener(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mHandler = new MyBluetoothHandler(this);
        resetState();

        Intent intent = new Intent(this, BluetoothService.class);
        bindService(intent, mBluetoothConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mBluetoothAdapter.isDiscovering()) {

        }

        if (mBTStatusReceiver == null) {
            mBTStatusReceiver = new BluetoothStatusReceiver();
            mBTStatusReceiver.setListener(this);
            registerReceiver(mBTStatusReceiver, BluetoothStatusReceiver.getIntentFilter());

            mBTDReceiver = new BluetoothDiscoverReceiver();
            mBTDReceiver.setListener(this);

            IntentFilter[] filters = BluetoothDiscoverReceiver.getIntentFilters();
            for (IntentFilter filter : filters) {
                registerReceiver(mBTDReceiver, filter);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBTStatusReceiver != null) {
            unregisterReceiver(mBTStatusReceiver);
            mBTStatusReceiver = null;
            unregisterReceiver(mBTDReceiver);
            mBTDReceiver = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mServiceConnected)
            unbindService(mBluetoothConnection);

        mServiceConnected = false;
        mBinder = null;
    }


    public void cancelConnection(View v) {
        if (mClientTask != null) {
            mClientTask.cancel(true);

            if (mClientTask.getStatus() == AsyncTask.Status.RUNNING) {
                changeState(FOUND_DEVICES);
                transitionConnectionVisibility(false);
            } else
                mBinder.removeSockets(BluetoothServiceUtility.CLOSE_SAY_GOODBYE);

            mClientTask = null;
        }
    }

    @Override
    public void bluetoothStateChanged(@BluetoothStatusReceiver.BLUETOOTH_STATE int state) {
        switch (state) {
            case BluetoothStatusReceiver.BLUETOOTH_ON:
                Toast.makeText(this, "蓝牙已开启", Toast.LENGTH_SHORT).show();
                break;
            case BluetoothStatusReceiver.BLUETOOTH_OFF:
                Toast.makeText(this, "蓝牙已关闭", Toast.LENGTH_SHORT).show();
                finish();
                break;
            case BluetoothStatusReceiver.BLUETOOTH_TURNING_ON:
            case BluetoothStatusReceiver.BLUETOOTH_TURNING_OFF:
                finish();
                break;
        }
    }

    @Override
    public void discoveryStarted() {
        mAdapter.addItems(getPairs());
        if (mAdapter.getItemCount() > 1)
            changeState(FINDING_NOT_EMPTY);
        else
            changeState(FINDING_EMPTY);
    }

    @Override
    public void discoveredDevice(BluetoothClass bluetoothClass, BluetoothDevice bluetoothDevice) {
        mAdapter.addItem(bluetoothDevice);
        changeState(FINDING_NOT_EMPTY);
    }

    @Override
    public void discoveryFinished() {
        if (mState == CONNECTING || mState == CONNECTED)
            return;
        if (mAdapter.getItemCount() > 1)
            changeState(FOUND_DEVICES);
        else
            changeState(DEFAULT);
//        mFab.show();
        mFab.setVisibility(View.VISIBLE);
    }


    @Override
    public void itemClick(View clickedView, BluetoothDevice device) {
        changeState(CONNECTING);
        ((TextView)mConnectionViewGroup.findViewById(R.id.tv_bluetooth_name)).setText(device.getName());
        ((TextView)mConnectionViewGroup.findViewById(R.id.tv_bluetooth_address)).setText(device.getAddress());
        transitionConnectionVisibility(true);

        if (mBluetoothAdapter.isDiscovering())
            mBluetoothAdapter.cancelDiscovery();
        mClientTask = new ClientConnectTask(device, BluetoothServiceUtility.getBTChatUUID());
        mClientTask.setListener(this);
        mClientTask.execute();
    }

    @Override
    public void serverSearchFinished(boolean found, BluetoothSocket socket) {
        if (found) {
            if (mServiceConnected) {
                Toast.makeText(ClientActivity.this, "已连接到蓝牙主机", Toast.LENGTH_SHORT).show();
                changeState(CONNECTED);
                mBinder.addSocket(socket);
                return;
            }
            Log.e(TAG, "not connected to read or write service when a server is found");
        }
        Toast.makeText(ClientActivity.this, "无法连接到蓝牙主机，请重新尝试", Toast.LENGTH_SHORT).show();
        changeState(FOUND_DEVICES);
        transitionConnectionVisibility(false);
    }


    private static class MyBluetoothHandler extends BluetoothServiceHandler {

        private final WeakReference<ClientActivity> mActivity;

        public MyBluetoothHandler(ClientActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void serverSetupFinished() {
            mActivity.get().mBinder.setHandler(null);
            mActivity.get().startActivity(new Intent(mActivity.get(), ChatroomActivity.class));
        }

        @Override
        public void connectionClosed(String macAddress, @BluetoothServiceUtility.CLOSE_CODE int closeCode) {
            Toast.makeText(mActivity.get(), BluetoothServiceUtility.getCloseCodeInfo(closeCode) +
                    " : 与 " + macAddress +" 断开连接", Toast.LENGTH_SHORT).show();
            mActivity.get().changeState(FOUND_DEVICES);
            mActivity.get().transitionConnectionVisibility(false);
        }

        @Override
        public void appMessage(String address, byte[] data) {
            Log.v(TAG, "got app message " + new String(data));
        }
    }


    private class BluetoothServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v(TAG, "bluetoothConnection connected");
            mServiceConnected = true;
            mBinder = (BluetoothService.BluetoothBinder) service;
            mBinder.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v(TAG, "bluetoothConnection disconnected");
            mServiceConnected = false;
            mBinder = null;
        }
    }
}
