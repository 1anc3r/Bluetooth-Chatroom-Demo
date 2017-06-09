package me.lancer.bluetoothchatroom.receiver;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class BluetoothStatusReceiver extends BroadcastReceiver {

    private BlueToothStatusListener mListener;

    @IntDef({BLUETOOTH_ON, BLUETOOTH_TURNING_ON, BLUETOOTH_OFF, BLUETOOTH_TURNING_OFF})
    @Retention(RetentionPolicy.SOURCE)
    public @interface BLUETOOTH_STATE {}

    public static final int BLUETOOTH_ON            = 1;
    public static final int BLUETOOTH_TURNING_ON    = 2;
    public static final int BLUETOOTH_OFF           = 3;
    public static final int BLUETOOTH_TURNING_OFF   = 4;


    public interface BlueToothStatusListener{
        void bluetoothStateChanged(@BLUETOOTH_STATE int state);
    }

    public void setListener(BlueToothStatusListener listener){
        mListener = listener;
    }

    public static IntentFilter getIntentFilter(){
        return new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if(mListener == null)
            return;

        final String action = intent.getAction();

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    mListener.bluetoothStateChanged(BLUETOOTH_OFF);
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    mListener.bluetoothStateChanged(BLUETOOTH_TURNING_OFF);
                    break;
                case BluetoothAdapter.STATE_ON:
                    mListener.bluetoothStateChanged(BLUETOOTH_ON);
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    mListener.bluetoothStateChanged(BLUETOOTH_TURNING_ON);
                    break;
            }
        }
    }
}
