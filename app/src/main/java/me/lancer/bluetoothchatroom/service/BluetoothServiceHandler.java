package me.lancer.bluetoothchatroom.service;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import me.lancer.bluetoothchatroom.message.BluetoothMessage;
import me.lancer.bluetoothchatroom.message.BluetoothMessageOpen;
import me.lancer.bluetoothchatroom.message.BluetoothMessageClose;
import me.lancer.bluetoothchatroom.message.BluetoothMessageSend;
import me.lancer.bluetoothchatroom.message.BluetoothMessageAccept;
import me.lancer.bluetoothchatroom.message.BluetoothMessageUtility;

public abstract class BluetoothServiceHandler extends Handler {

    public static final String TAG = "BluetoothServiceHandler";

    public abstract void connectionClosed(String macAddress, @BluetoothServiceUtility.CLOSE_CODE int closeCode);

    public abstract void appMessage(String macAddress, byte[] bytes);

    public abstract void serverSetupFinished();

    public final void handleMessage(Message msg){

        int type = BluetoothMessage.getType( ((BluetoothMessage) msg.obj).makeBytes() );

        switch (type) {
            case BluetoothMessageUtility.TYPE_APP_MESSAGE :
                BluetoothMessageOpen m = (BluetoothMessageOpen)msg.obj;
                appMessage(m.getMacAddress(), (m.getData()));
                break;
            case BluetoothMessageUtility.TYPE_SERVER_SETUP_FINISHED:
                serverSetupFinished();
                break;
            case BluetoothMessageUtility.TYPE_CONNECTION_CLOSED:
                BluetoothMessageClose m1 = (BluetoothMessageClose)msg.obj ;
                connectionClosed(m1.getMacAddress(), m1.getCloseCode());
                break;
            case BluetoothMessageUtility.TYPE_HELLO:
                BluetoothMessageSend m2 = (BluetoothMessageSend)msg.obj ;
                Log.w(TAG, "should not have received message " + new String(m2.makeBytes()));
                break;
            case BluetoothMessageUtility.TYPE_HELLO_REPLY:
                BluetoothMessageAccept m3 = (BluetoothMessageAccept) msg.obj ;
                Log.w(TAG, "should not have received message " + new String(m3.makeBytes()));
                break;
            default:
                Log.w(TAG, "unknown type " + type);
        }
    }
}
