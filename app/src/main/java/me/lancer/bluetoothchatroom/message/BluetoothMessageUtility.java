package me.lancer.bluetoothchatroom.message;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

/**
 * Created by HuangFangzhi on 2017/6/8.
 */

public class BluetoothMessageUtility {

    @IntDef(flag = true, value = {TYPE_HELLO, TYPE_HELLO_REPLY, TYPE_CONNECTION_CLOSED, TYPE_SERVER_SETUP_FINISHED, TYPE_APP_MESSAGE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MESSAGE_TYPE {}

    public static final int TYPE_HELLO = 1000;
    public static final int TYPE_HELLO_REPLY = 1001;
    public static final int TYPE_CONNECTION_CLOSED = 1100;
    public static final int TYPE_SERVER_SETUP_FINISHED = 1101;
    public static final int TYPE_APP_MESSAGE = 1102;
    public static final int LENGTH_ID = 4;
    public static final int LENGTH_ADDRESS = 17;
    public static final int LENGTH_HEADER = LENGTH_ID +LENGTH_ADDRESS;

    public static boolean isMessageType(int type){
        switch (type) {
            case TYPE_APP_MESSAGE:
            case TYPE_CONNECTION_CLOSED:
            case TYPE_HELLO_REPLY:
            case TYPE_HELLO:
            case TYPE_SERVER_SETUP_FINISHED:
                return true;
            default:
                return false;
        }
    }
}
