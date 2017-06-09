package me.lancer.bluetoothchatroom.message;

import android.support.annotation.NonNull;

import java.util.Arrays;

import me.lancer.bluetoothchatroom.service.BluetoothServiceUtility;

/**
 * Created by HuangFangzhi on 2017/6/8.
 */

public class BluetoothMessageClose extends BluetoothMessage {

    private @BluetoothServiceUtility.CLOSE_CODE int mCloseCode;
    private byte[] mMessage;

    public static BluetoothMessageClose reconstruct(byte[] byteMessage){
        if(byteMessage.length != BluetoothMessageUtility.LENGTH_HEADER + BluetoothServiceUtility.LENGTH_CLOSE_CODE)
            throw new IllegalArgumentException(byteMessage + " is invalid param, must be length "
                    + BluetoothMessageUtility.LENGTH_HEADER + BluetoothServiceUtility.LENGTH_CLOSE_CODE);

        BluetoothMessage m = BluetoothMessage.reconstruct(Arrays.copyOfRange(byteMessage, 0, BluetoothMessageUtility.LENGTH_HEADER));

        if(m.getMessageType() != BluetoothMessageUtility.TYPE_CONNECTION_CLOSED)
            throw new IllegalArgumentException(BluetoothMessageUtility.TYPE_CONNECTION_CLOSED + " must be the " +
                    "message type, found " + m.getMessageType());

        String code =  new String(Arrays.copyOfRange(byteMessage, BluetoothMessageUtility.LENGTH_HEADER, byteMessage.length));
        @BluetoothServiceUtility.CLOSE_CODE int closeCode = Integer.parseInt(code);

        if( !BluetoothServiceUtility.isCloseCode(closeCode) )
            throw new IllegalArgumentException(closeCode + " is not a valid close close");

        return new BluetoothMessageClose(m.getMacAddress(), closeCode );

    }

    public BluetoothMessageClose(@NonNull String macAddress, @BluetoothServiceUtility.CLOSE_CODE int closeCode) {
        super(BluetoothMessageUtility.TYPE_CONNECTION_CLOSED, macAddress);

        if( !BluetoothServiceUtility.isCloseCode(closeCode) )
            throw new  IllegalArgumentException(closeCode + " is not a valid close close");

        mMessage = new byte[BluetoothMessageUtility.LENGTH_HEADER + BluetoothServiceUtility.LENGTH_CLOSE_CODE];

        System.arraycopy( super.makeBytes(), 0, mMessage, 0, BluetoothMessageUtility.LENGTH_HEADER);
        System.arraycopy((closeCode + "").getBytes(), 0, mMessage, BluetoothMessageUtility.LENGTH_HEADER, BluetoothServiceUtility.LENGTH_CLOSE_CODE);
        mCloseCode = closeCode;
    }

    public @BluetoothServiceUtility.CLOSE_CODE int getCloseCode(){
        return mCloseCode;
    }

    @Override
    public byte[] makeBytes() {
        return mMessage;
    }
}
