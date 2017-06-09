package me.lancer.bluetoothchatroom.message;

import android.support.annotation.NonNull;

/**
 * Created by HuangFangzhi on 2017/6/8.
 */

public class BluetoothMessageSetup extends BluetoothMessage {

    public static BluetoothMessageSetup reconstruct(byte[] byteMessage){

        if(byteMessage.length != BluetoothMessageUtility.LENGTH_HEADER)
            throw new IllegalArgumentException(byteMessage + " is invalid param, must be length "
                    + BluetoothMessageUtility.LENGTH_HEADER);

        BluetoothMessage m = BluetoothMessage.reconstruct(byteMessage);

        if(m.getMessageType() != BluetoothMessageUtility.TYPE_SERVER_SETUP_FINISHED)
            throw new IllegalArgumentException(BluetoothMessageUtility.TYPE_SERVER_SETUP_FINISHED + " must be the " +
                    "message type, found " + m.getMessageType());

        return new BluetoothMessageSetup(m.getMacAddress());
    }

    public BluetoothMessageSetup(@NonNull String macAddress) {
        super(BluetoothMessageUtility.TYPE_SERVER_SETUP_FINISHED, macAddress);
    }
}
