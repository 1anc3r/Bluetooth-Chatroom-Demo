package me.lancer.bluetoothchatroom.message;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Arrays;

/**
 * Created by HuangFangzhi on 2017/6/8.
 */

public class BluetoothMessageOpen extends BluetoothMessage {

    private byte[] mMessage;

    public static BluetoothMessageOpen reconstruct(@NonNull byte[] byteMessage){
        if(byteMessage.length < BluetoothMessageUtility.LENGTH_HEADER)
            throw new IllegalArgumentException(byteMessage + " must be longer than the header size " + BluetoothMessageUtility.LENGTH_HEADER);

        BluetoothMessage m = BluetoothMessage.reconstruct(Arrays.copyOfRange(byteMessage, 0, BluetoothMessageUtility.LENGTH_HEADER));

        if(m.getMessageType() != BluetoothMessageUtility.TYPE_APP_MESSAGE)
            throw new IllegalArgumentException(BluetoothMessageUtility.TYPE_APP_MESSAGE + " must be the " +
                    "message type, found " + m.getMessageType());

        return new BluetoothMessageOpen(m.getMacAddress(), Arrays.copyOfRange(byteMessage, BluetoothMessageUtility.LENGTH_HEADER, byteMessage.length));
    }

    public BluetoothMessageOpen(@NonNull String macAddress, @Nullable byte[] data) {
        super(BluetoothMessageUtility.TYPE_APP_MESSAGE, macAddress);
        if(data == null)
            data = "".getBytes();

        mMessage = new byte[BluetoothMessageUtility.LENGTH_HEADER + data.length];

        System.arraycopy( super.makeBytes(), 0, mMessage, 0, BluetoothMessageUtility.LENGTH_HEADER);
        System.arraycopy(data, 0, mMessage, BluetoothMessageUtility.LENGTH_HEADER, data.length);

    }

    public byte[] getData(){
        return Arrays.copyOfRange(mMessage, BluetoothMessageUtility.LENGTH_HEADER, mMessage.length);
    }

    @Override
    public byte[] makeBytes() {
        return mMessage;
    }
}
