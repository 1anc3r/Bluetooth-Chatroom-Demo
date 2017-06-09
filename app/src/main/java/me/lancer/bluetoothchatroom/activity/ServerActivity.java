package me.lancer.bluetoothchatroom.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.pnikosis.materialishprogress.ProgressWheel;

import me.lancer.bluetoothchatroom.R;
import me.lancer.bluetoothchatroom.adapter.ServerAdapter;
import me.lancer.bluetoothchatroom.aynctask.ServerAcceptTask;
import me.lancer.bluetoothchatroom.receiver.BluetoothDiscoverStateReceiver;
import me.lancer.bluetoothchatroom.receiver.BluetoothStatusReceiver;
import me.lancer.bluetoothchatroom.receiver.BluetoothStatusReceiver.BlueToothStatusListener;
import me.lancer.bluetoothchatroom.service.BluetoothService;
import me.lancer.bluetoothchatroom.service.BluetoothServiceHandler;
import me.lancer.bluetoothchatroom.service.BluetoothServiceUtility;

import java.lang.ref.WeakReference;

public class ServerActivity extends AppCompatActivity implements BlueToothStatusListener,
        BluetoothDiscoverStateReceiver.BlueToothDiscoverStateListener,
        ServerAcceptTask.ServerEventListener, ServerAdapter.ServerItemClickListener {

    private static final String TAG = "SetupServer";
    private static final int DISCOVERY_DURATION = 180;

    private boolean mRequestingDiscoverable = false;

    private BluetoothAdapter mBluetoothAdapter;

    private ActionMode mActionMode;
    private MyActionModeCallback mActionCallback;

    private TextView mBlueToothName;
    private TextView mBlueAddress;
    private TextView mStatusText;
    private Button mStartFab;
    private RecyclerView mRecyclerView;
    private ServerAdapter mRecyclerAdapter;

    private BluetoothStatusReceiver mBTStatusReceiver = null;
    private BluetoothDiscoverStateReceiver mBTDStateReceiver = null;

    private ServerAcceptTask mServerTask = null;
    private BluetoothService.BluetoothBinder mBinder;
    private ServiceConnection mBluetoothConnection;
    private MyBluetoothHandler mHandler;
    private boolean mConnected;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate");

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "蓝牙不可用", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            bluetoothStateChanged(BluetoothStatusReceiver.BLUETOOTH_OFF);
        }

        setContentView(R.layout.activity_server);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mStartFab = (Button) findViewById(R.id.fab);
//        mStartFab.hide();
        mStartFab.setVisibility(View.GONE);
        mStartFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecyclerAdapter.getNumSockets(ServerAdapter.ACCEPTED) == 0) {
                    Toast.makeText(ServerActivity.this, "不接受用户", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (BluetoothSocket socket : mRecyclerAdapter.getSockets(ServerAdapter.UNACCEPTED))
                    mBinder.removeSocket(socket.getRemoteDevice().getAddress(), BluetoothServiceUtility.CLOSE_KICKED_FROM_SERVER);

                mBinder.setHandler(null);
                Intent intent = new Intent(ServerActivity.this, ChatroomActivity.class);
                intent.putExtra(ChatroomActivity.EXTRA_SERVER, true);
                startActivity(intent);
            }
        });
        mStatusText = (TextView) findViewById(R.id.tv_msg);
        mStatusText.append("正在等待用户连接主机\n");
        mBlueToothName = (TextView) findViewById(R.id.tv_bluetooth_name);
        mBlueToothName.setText(mBluetoothAdapter.getName());
        mBlueAddress = (TextView) findViewById(R.id.tv_bluetooth_address);
        mBlueAddress.setText(mBluetoothAdapter.getAddress());

        mRecyclerView = (RecyclerView) findViewById(R.id.rv_client);
        mRecyclerAdapter = new ServerAdapter(this, this);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mRecyclerAdapter);

        mHandler = new MyBluetoothHandler(this);
        mBluetoothConnection = new MyBluetoothConnection();
        bindService(new Intent(this, BluetoothService.class), mBluetoothConnection, BIND_AUTO_CREATE);
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "onStart");

        if (mBTStatusReceiver == null) {
            mBTStatusReceiver = new BluetoothStatusReceiver();
            mBTStatusReceiver.setListener(this);
            registerReceiver(mBTStatusReceiver, BluetoothStatusReceiver.getIntentFilter());

            mBTDStateReceiver = new BluetoothDiscoverStateReceiver();
            mBTDStateReceiver.setListener(this);
            registerReceiver(mBTDStateReceiver, BluetoothDiscoverStateReceiver.getIntentFilter());
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume");

        if (!mBluetoothAdapter.isEnabled()) {
            bluetoothStateChanged(BluetoothStatusReceiver.BLUETOOTH_OFF);
        }

        if (mBluetoothAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            startServer();
        }

        makeDiscoverable();
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause");
    }


    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, "onStop");
        stopServer();

        if (mBTStatusReceiver != null) {
            unregisterReceiver(mBTStatusReceiver);
            mBTStatusReceiver = null;
            unregisterReceiver(mBTDStateReceiver);
            mBTDStateReceiver = null;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy");
        if (mConnected)
            unbindService(mBluetoothConnection);

        mConnected = false;
        mBinder = null;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DISCOVERY_DURATION) {
            mRequestingDiscoverable = false;
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "蓝牙主机必须开放检测", Toast.LENGTH_LONG).show();
                finish();
            } else if (resultCode == DISCOVERY_DURATION) {
                startServer();
            } else {
                Log.e(TAG, "unknown result code: " + resultCode);
            }
        } else {
            Log.e(TAG, "unknown request code: " + requestCode);
        }
    }

    public void makeDiscoverable() {
        if (!mBluetoothAdapter.isEnabled())
            return;

        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE
                && !mRequestingDiscoverable) {

            Log.v(TAG, "makeDiscoverable");

            mRequestingDiscoverable = true;
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERY_DURATION);
            startActivityForResult(discoverableIntent, DISCOVERY_DURATION);
        }
    }

    public void startServer() {
        if (mServerTask == null) {
            Log.v(TAG, "startServer");
            mServerTask = new ServerAcceptTask(mBluetoothAdapter, BluetoothServiceUtility.getBTChatUUID(), BluetoothServiceUtility.SDP_NAME);
            mServerTask.setListener(this);
            mServerTask.execute();
        }
    }

    public void stopServer() {
        if (mServerTask != null) {
            Log.v(TAG, "stopServer");
            mServerTask.cancel(true);
            mServerTask.cancelServer();
            mServerTask = null;
        }
    }

    @Override
    public void bluetoothStateChanged(@BluetoothStatusReceiver.BLUETOOTH_STATE int state) {
        switch (state) {
            case BluetoothStatusReceiver.BLUETOOTH_OFF:
                Toast.makeText(this, "蓝牙关闭，蓝牙主机关机", Toast.LENGTH_LONG).show();
                stopServer();
                finish();
                break;
            case BluetoothStatusReceiver.BLUETOOTH_ON:
            case BluetoothStatusReceiver.BLUETOOTH_TURNING_ON:
            case BluetoothStatusReceiver.BLUETOOTH_TURNING_OFF:
                break;
        }
    }

    @Override
    public void discoverable() {
    }

    @Override
    public void undiscoverable() {
        stopServer();
        makeDiscoverable();
    }

    @Override
    public void foundClient(BluetoothSocket clientSocket) {
        if (!mConnected) {
            Log.e(TAG, "not connected to BluetoothService when a server is found");
            return;
        }
        mRecyclerAdapter.addItem(ServerAdapter.UNACCEPTED, clientSocket);
        mStatusText.append(getResources().getString(R.string.status_no_accepted_clients)+"\n");
        mBinder.addSocket(clientSocket);
    }

    @Override
    public void itemClick(BluetoothSocket socket) {
        String address = socket.getRemoteDevice().getAddress();
        boolean connected = mRecyclerAdapter.contains(ServerAdapter.ACCEPTED, address);
        int type = connected ? ServerAdapter.ACCEPTED : ServerAdapter.UNACCEPTED;

        if (mActionCallback == null)
            mActionCallback = new MyActionModeCallback(type, socket);
        else
            mActionCallback.setData(type, socket);


        if (mActionMode == null)
            mActionMode = startSupportActionMode(mActionCallback);
        else
            mActionMode.invalidate();
    }


    private class MyBluetoothConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v(TAG, "BluetoothConnection connected");
            mConnected = true;
            mBinder = (BluetoothService.BluetoothBinder) service;
            mBinder.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v(TAG, "BluetoothConnection disconnected");
            mConnected = false;
            mBinder = null;
        }
    }


    private static class MyBluetoothHandler extends BluetoothServiceHandler {

        private final WeakReference<ServerActivity> mActivity;

        public MyBluetoothHandler(ServerActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void serverSetupFinished() {
            Log.w(TAG, "Should not have received a call to serverSetupFinished");
        }

        @Override
        public void connectionClosed(String macAddress, @BluetoothServiceUtility.CLOSE_CODE int closeCode) {
            mActivity.get().mRecyclerAdapter.removeItem(macAddress);
            if (mActivity.get().mRecyclerAdapter.getNumSockets(ServerAdapter.ACCEPTED) < 1) {
                mActivity.get().mStartFab.setVisibility(View.GONE);
                mActivity.get().mStatusText.setText(R.string.status_no_accepted_clients);
            }

            if (mActivity.get().mActionMode != null
                    && mActivity.get().mActionCallback.getAddress().equals(macAddress)) {
                mActivity.get().mActionMode.finish();
            }

            Toast.makeText(mActivity.get(), BluetoothServiceUtility.getCloseCodeInfo(closeCode) +
                    " : 与 " + macAddress +" 断开连接", Toast.LENGTH_SHORT).show();

        }

        @Override
        public void appMessage(String address, byte[] data) {
            Log.v(TAG, "got app message " + new String(data));
        }
    }

    private class MyActionModeCallback implements ActionMode.Callback {
        private BluetoothSocket mSocket;
        private String mAddress;
        private
        @ServerAdapter.ITEM_TYPE
        int mItemType;

        public MyActionModeCallback(@ServerAdapter.ITEM_TYPE int itemType, BluetoothSocket socket) {
            mSocket = socket;
            mAddress = socket.getRemoteDevice().getAddress();
            mItemType = itemType;
        }

        public String getAddress() {
            return mAddress;
        }

        public void setData(@ServerAdapter.ITEM_TYPE int itemType, BluetoothSocket socket) {
            mSocket = socket;
            mAddress = socket.getRemoteDevice().getAddress();
            mItemType = itemType;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            Log.v(TAG, "onCreateActionMode: " + mAddress);
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_server, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            Log.v(TAG, "onPrepareActionMode: " + mAddress);
            mRecyclerAdapter.setSelectedItemAddress(mAddress);

            mode.setTitle(mSocket.getRemoteDevice().getName());

            MenuItem item = menu.findItem(R.id.menu_swap);

            if (item == null)
                return true;

            if (mItemType == ServerAdapter.ACCEPTED)
                item.setIcon(R.mipmap.ic_person_remove_white_24dp);
            else
                item.setIcon(R.mipmap.ic_person_add_white_24dp);

            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            Log.v(TAG, "onActionItemClicked: " + mAddress);
            String address = mSocket.getRemoteDevice().getAddress();

            switch (item.getItemId()) {
                case R.id.menu_remove:
                    mBinder.removeSocket(address, BluetoothServiceUtility.CLOSE_KICKED_FROM_SERVER);
                    mode.finish();
                    return true;
                case R.id.menu_swap:
                    mRecyclerAdapter.changeItemType(ServerAdapter.ALL, mSocket);

                    if (mRecyclerAdapter.getNumSockets(ServerAdapter.ACCEPTED) > 0) {
//                        mStartFab.show();
                        mStartFab.setVisibility(View.VISIBLE);
                        mStatusText.append(getResources().getString(R.string.status_accepted_clients)+"\n");
                    } else {
//                        mStartFab.hide();
                        mStartFab.setVisibility(View.GONE);
                        mStatusText.append(getResources().getString(R.string.status_no_accepted_clients)+"\n");

                    }
                    mode.finish();
                    return true;
            }
            return false;

        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            Log.v(TAG, "onDestroyActionMode: " + mAddress);
            mRecyclerAdapter.removeSelectedItem();
            mActionMode = null;
        }
    }
}
