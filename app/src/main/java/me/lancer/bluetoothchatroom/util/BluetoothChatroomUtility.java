package me.lancer.bluetoothchatroom.util;


public class BluetoothChatroomUtility {
    private static final String TAG = "BluetoothChatroomUtility";

    //number of bytes int the message identifier
    public static final int ID_LENGTH = 4;

    public static final String ID_SEND_DISPLAY_TEXT  = "2000";

    /**
     * Creates a message that should be displayed to all clients in the chat.
     *
     * @param displayText the text that will be shown to the user.
     */
    public static String makeDisplayTextMessage(String displayText){
        return ID_SEND_DISPLAY_TEXT + displayText;

    }
}
