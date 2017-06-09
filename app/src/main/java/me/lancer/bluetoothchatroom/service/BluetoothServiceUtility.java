package me.lancer.bluetoothchatroom.service;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;

public class BluetoothServiceUtility {

    private static final String TAG = "BluetoothServiceUtility";
    public static final String SDP_NAME = "BluetoothChat";
    public static final int LENGTH_CLOSE_CODE = 3;

    public static final int MAX_NUM_BLUETOOTH_DEVICES = 4;

    @IntDef({CLOSE_SERVER_NOT_RESPONDING, CLOSE_READ_CLOSE, CLOSE_WRITE_CLOSE,
            CLOSE_SERVICE_DESTROYED, CLOSE_KICKED_FROM_SERVER, CLOSE_SAY_GOODBYE, CLOSE_GET_GOODBYE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CLOSE_CODE {}
    public static final int CLOSE_SERVER_NOT_RESPONDING    = 101;
    public static final int CLOSE_READ_CLOSE               = 102;
    public static final int CLOSE_WRITE_CLOSE              = 103;
    public static final int CLOSE_SERVICE_DESTROYED        = 104;
    public static final int CLOSE_KICKED_FROM_SERVER       = 105;
    public static final int CLOSE_SAY_GOODBYE              = 106;
    public static final int CLOSE_GET_GOODBYE              = 107;

    public static boolean isCloseCode(int code){
        switch (code) {
            case CLOSE_GET_GOODBYE:
            case CLOSE_KICKED_FROM_SERVER:
            case CLOSE_READ_CLOSE:
            case CLOSE_SAY_GOODBYE:
            case CLOSE_SERVER_NOT_RESPONDING:
            case CLOSE_SERVICE_DESTROYED:
            case CLOSE_WRITE_CLOSE:
                return true;
            default:
                return false;
        }
    }

    public static String getCloseCodeInfo(@CLOSE_CODE int closeCode){
        switch (closeCode){
            case BluetoothServiceUtility.CLOSE_SERVER_NOT_RESPONDING:
                return "ID_SERVER_NOT_RESPONDING";
            case BluetoothServiceUtility.CLOSE_READ_CLOSE:
                return "ID_READ_CLOSE";
            case BluetoothServiceUtility.CLOSE_WRITE_CLOSE:
                return "ID_WRITE_CLOSE";
            case BluetoothServiceUtility.CLOSE_SERVICE_DESTROYED:
                return "ID_SERVICE_DESTROYED";
            case BluetoothServiceUtility.CLOSE_KICKED_FROM_SERVER:
                return "ID_KICKED_FROM_SERVER";
            case BluetoothServiceUtility.CLOSE_SAY_GOODBYE:
                return "CLOSE_SAY_GOODBYE";
            case BluetoothServiceUtility.CLOSE_GET_GOODBYE:
                return "CLOSE_GET_GOODBYE";
            default:
                return "UNKNOWN ERROR CODE " + closeCode;
        }
    }

    public static UUID getBTChatUUID(){
        byte[] b = "ajsvcrgcdfg".getBytes();
        return UUID.nameUUIDFromBytes(b);
    }
}
