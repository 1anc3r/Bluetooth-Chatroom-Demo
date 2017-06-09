package me.lancer.bluetoothchatroom.message;

import android.support.annotation.NonNull;

import java.util.Arrays;

/**
 * Created by HuangFangzhi on 2017/6/8.
 */

public class BluetoothMessage {

    @BluetoothMessageUtility.MESSAGE_TYPE
    private final int mMessageType;
    private final String mMacAddress;

    private static void checkAddressLength(String address){
        if(address.length() != BluetoothMessageUtility.LENGTH_ADDRESS)
            throw new IllegalArgumentException(address +" is not length " + BluetoothMessageUtility.LENGTH_ADDRESS);
    }

    private static void checkMessageType(@BluetoothMessageUtility.MESSAGE_TYPE int messageType){
        if(!BluetoothMessageUtility.isMessageType(messageType))
            throw new IllegalArgumentException(messageType + " is not a valid MESSAGE_TYPE");
    }

    public static BluetoothMessage reconstruct(byte[] byteMessage){
        if(byteMessage.length != BluetoothMessageUtility.LENGTH_HEADER)
            throw new IllegalArgumentException(byteMessage + " must be length " + BluetoothMessageUtility.LENGTH_HEADER);

        String stringMessage = (new String(byteMessage, 0, byteMessage.length));

        @BluetoothMessageUtility.MESSAGE_TYPE int messageType =  Integer.parseInt(
                stringMessage.substring(0, BluetoothMessageUtility.LENGTH_ID));

        checkMessageType(messageType);
        String address = stringMessage.substring( BluetoothMessageUtility.LENGTH_ID, BluetoothMessageUtility.LENGTH_HEADER );
        return new BluetoothMessage(messageType, address);
    }

    public static @BluetoothMessageUtility.MESSAGE_TYPE int getType(@NonNull byte[] b){
        if(b.length < BluetoothMessageUtility.LENGTH_ID)
            throw new IllegalArgumentException(b + " must be larger than " + BluetoothMessageUtility.LENGTH_ID);

        String id = new String( Arrays.copyOfRange(b,0,4) );
        @BluetoothMessageUtility.MESSAGE_TYPE int type = Integer.parseInt(id);
        if( !BluetoothMessageUtility.isMessageType(type) )
            return -1;
        return type;
    }

    public BluetoothMessage(@BluetoothMessageUtility.MESSAGE_TYPE int messageType, @NonNull String macAddress){
        checkMessageType(messageType);
        checkAddressLength(macAddress);

        mMessageType = messageType;
        mMacAddress = macAddress;
    }

    public String getMacAddress(){ return mMacAddress; }

    public @BluetoothMessageUtility.MESSAGE_TYPE int getMessageType(){return mMessageType;}

    public byte[] makeBytes(){
        return ( mMessageType + mMacAddress).getBytes();
    }
}
