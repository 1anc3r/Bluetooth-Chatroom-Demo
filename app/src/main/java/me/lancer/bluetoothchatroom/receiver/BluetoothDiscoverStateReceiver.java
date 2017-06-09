package me.lancer.bluetoothchatroom.receiver;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class BluetoothDiscoverStateReceiver extends BroadcastReceiver {

    private BlueToothDiscoverStateListener mListener;

    public interface BlueToothDiscoverStateListener{
        void discoverable();
        void undiscoverable();
    }

    public static IntentFilter getIntentFilter(){
        return new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
    }


    public void setListener(BlueToothDiscoverStateListener listener){
        mListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(mListener == null)
            return;

        String action = intent.getAction();

        if(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {

            int scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1);

            if (scanMode == -1){
                Log.d("BTDiscoverStateReceiver", "EXTRA_SCAN_MODE was not found");
                return;
            }

            if(scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
                mListener.discoverable();
            else
                mListener.undiscoverable();

        }
    }
}
