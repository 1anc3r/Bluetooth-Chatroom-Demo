package me.lancer.bluetoothchatroom.receiver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class BluetoothDiscoverReceiver extends BroadcastReceiver {

    private BlueToothDiscoverListener mListener;

    public interface BlueToothDiscoverListener{
        void discoveredDevice(BluetoothClass bluetoothClass, BluetoothDevice bluetoothDevice);
        void discoveryStarted();
        void discoveryFinished();
    }

    public void setListener(BlueToothDiscoverListener listener){
        mListener = listener;
    }

    public static IntentFilter[] getIntentFilters(){
        IntentFilter[] filters = new IntentFilter[3];
        filters[0] = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filters[1] = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filters[2] = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        return filters;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if(mListener == null)
            return;

        String action = intent.getAction();

        if(BluetoothDevice.ACTION_FOUND.equals(action)){
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            BluetoothClass bluetoothClass = intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS);
            mListener.discoveredDevice(bluetoothClass, device);
        }

        if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
            mListener.discoveryStarted();
        }

        if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
            mListener.discoveryFinished();
        }
    }
}
