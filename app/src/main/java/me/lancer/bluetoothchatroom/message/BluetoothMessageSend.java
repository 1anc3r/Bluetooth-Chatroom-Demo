package me.lancer.bluetoothchatroom.message;

import android.support.annotation.NonNull;

/**
 * Created by HuangFangzhi on 2017/6/8.
 */

public class BluetoothMessageSend extends BluetoothMessage {

    public static BluetoothMessageSend reconstruct(byte[] byteMessage){

        if(byteMessage.length != BluetoothMessageUtility.LENGTH_HEADER)
            throw new IllegalArgumentException(byteMessage + " is invalid param, must be length "
                    + BluetoothMessageUtility.LENGTH_HEADER);

        BluetoothMessage m = BluetoothMessage.reconstruct(byteMessage);

        if(m.getMessageType() != BluetoothMessageUtility.TYPE_HELLO)
            throw new IllegalArgumentException(BluetoothMessageUtility.TYPE_HELLO + " must be the " +
                    "message type, found " + m.getMessageType());

        return new BluetoothMessageSend(m.getMacAddress());
    }

    public BluetoothMessageSend(@NonNull String macAddress) {
        super(BluetoothMessageUtility.TYPE_HELLO, macAddress);
    }
}
