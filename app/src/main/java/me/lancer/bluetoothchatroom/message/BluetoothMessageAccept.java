package me.lancer.bluetoothchatroom.message;

import android.support.annotation.NonNull;

/**
 * Created by HuangFangzhi on 2017/6/8.
 */

public class BluetoothMessageAccept extends BluetoothMessage {

    public static BluetoothMessageAccept reconstruct(byte[] byteMessage){

        if(byteMessage.length != BluetoothMessageUtility.LENGTH_HEADER)
            throw new IllegalArgumentException(byteMessage + " is invalid param, must be length "
                    + BluetoothMessageUtility.LENGTH_HEADER);

        BluetoothMessage m = BluetoothMessage.reconstruct(byteMessage);

        if(m.getMessageType() != BluetoothMessageUtility.TYPE_HELLO_REPLY)
            throw new IllegalArgumentException(BluetoothMessageUtility.TYPE_HELLO_REPLY + " must be the " +
                    "message type, found " + m.getMessageType());

        return new BluetoothMessageAccept(m.getMacAddress());
    }

    public BluetoothMessageAccept(@NonNull String macAddress) {
        super(BluetoothMessageUtility.TYPE_HELLO_REPLY, macAddress);
    }
}
