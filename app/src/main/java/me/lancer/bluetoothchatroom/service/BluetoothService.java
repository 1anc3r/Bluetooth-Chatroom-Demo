package me.lancer.bluetoothchatroom.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import me.lancer.bluetoothchatroom.message.BluetoothMessage;
import me.lancer.bluetoothchatroom.message.BluetoothMessageOpen;
import me.lancer.bluetoothchatroom.message.BluetoothMessageClose;
import me.lancer.bluetoothchatroom.message.BluetoothMessageSend;
import me.lancer.bluetoothchatroom.message.BluetoothMessageAccept;
import me.lancer.bluetoothchatroom.message.BluetoothMessageSetup;
import me.lancer.bluetoothchatroom.message.BluetoothMessageUtility;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;


public class BluetoothService extends Service {
    
    public static final int TIMEOUT_DURATION = 6000;
    public static final int MAX_CONNECTION_ATTEMPTS = 3;

    private static final String TAG = "BluetoothService";
    private HashMap<String, BluetoothConnectionInfo> mClients;
    private BluetoothBinder mBinder;
    private BluetoothServiceHandler mClientHandler;
    private TimeoutHandler mTimeoutHandler;

    private String mMyBluetoothName;
    private String mMyAddress;

    private static class TimeoutHandler extends Handler {
        public int mTimeoutWhat = 1993;
        WeakReference<BluetoothService> mService;

        public TimeoutHandler(BluetoothService service){
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            if(msg.what != mTimeoutWhat) {
                Log.d(TAG, "unknown what " + msg.what + ", received for TimeoutHandler");
                return;
            }

            for(BluetoothConnectionInfo info : mService.get().mClients.values()){
                if(info.connectionAttempts >= MAX_CONNECTION_ATTEMPTS){
                    Log.v(TAG, "timeout has occurred: " + info.device.getAddress());
                    mService.get().mBinder.removeSocket(info.device.getAddress(), BluetoothServiceUtility.CLOSE_SERVER_NOT_RESPONDING);
                }else{
                    info.connectionAttempts++;
                    if(info.connectionAttempts >1)
                        Log.v(TAG, "connection attempt " + info.connectionAttempts + ": " + info.device.getAddress());
                    mService.get().sendMessage(new BluetoothMessageSend(mService.get().mMyAddress),info.device.getAddress());
                }
            }
            Message m = obtainMessage(mTimeoutWhat);
            sendMessageDelayed(m,TIMEOUT_DURATION);
        }
    }

    /**
     * Enable reading and writing if it hasn't already for the bluetooth device with the
     * specified info.
     *
     * @param info info and data about the bluetooth connection
     */
    private void enableRW(@NonNull BluetoothConnectionInfo info){
        Log.v(TAG, "enableRW for " + info.device.getAddress());

        if(info.readThread == null || !info.readThread.isAlive()){
            info.readThread = new ReadThread(info.device.getAddress(), 1024, info.inputStream);
            info.readThread.start();
        }else{
            Log.v(TAG, "already reading");
        }

        if(info.writeThread == null || !info.writeThread.isAlive()){
            info.writeThread = new WriteThread(info.device.getAddress(), info.outputStream, info.blockingQueue);
            info.writeThread.start();
        }else{
            Log.v(TAG, "already writing");
        }
    }

    /**
     * Disable reading and writing if it hasn't already for the bluetooth device with the
     * specified info.
     *
     * @param info info and data about the bluetooth connection
     */
    private void disableRW(BluetoothConnectionInfo info){
        Log.v(TAG, "disableRW for " + info.device.getAddress());

        if(info.writeThread == null || !info.writeThread.isAlive()){
            Log.v(TAG,"not currently writing");
        }else{
            Log.v(TAG,"disable writing");
            info.blockingQueue.offer(new BluetoothMessageOpen(WriteThread.SHUTDOWN_KEY, null));
        }

        if(info.readThread == null || !info.readThread.isAlive()){
            Log.v(TAG,"not currently reading");
        }else{
            Log.v(TAG,"disable reading");
            info.readThread.interrupt();
        }
        info.readThread = null;
        info.writeThread = null;
    }

    /**
     * Sends a message to the remote bluetooth device.
     *
     * @param message the message to be sent
     * @param macAddress the mac address to send to
     * @return true if the message was sent, false if it could not.
     */
    private boolean sendMessage(BluetoothMessage message, String macAddress){
        String msg = new String(message.makeBytes());
        Log.v(TAG, "WRITE APP MESSAGE -"+ msg +"- to " + macAddress);

        if( !mClients.containsKey(macAddress) )
            return false;

        BluetoothConnectionInfo tmpInfo = mClients.get(macAddress);

        if(tmpInfo == null || tmpInfo.socket == null)
            return false;

        if(!tmpInfo.blockingQueue.offer(message))
            Log.v(TAG, "blocking queue is full, cannot put message " + msg);
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "onCreate");
        mClients = new HashMap<>(BluetoothServiceUtility.MAX_NUM_BLUETOOTH_DEVICES);
        mBinder = new BluetoothBinder();
        mTimeoutHandler = new TimeoutHandler(this);
        mMyBluetoothName = "Default Name";
        mMyAddress = "zz:zz:zz:zz:zz:zz";
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        mMyBluetoothName = adapter.getName();
        mMyAddress = adapter.getAddress();
        mTimeoutHandler.sendMessageDelayed(mTimeoutHandler.obtainMessage(mTimeoutHandler.mTimeoutWhat),TIMEOUT_DURATION);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(TAG, "onUnbind");
        return false;
    }

    @Override
    public void onDestroy() {
        mBinder.removeSockets(BluetoothServiceUtility.CLOSE_SERVICE_DESTROYED);
    }

    /**
     * Thread that reads data from a remote bluetooth device using an inputStream retrieved from a
     * bluetooth socket. The data that is read will be sent to handler that was set from the Binder.
     * In the Message, the number of bytes will be sent as arg1 and the message will be sent in
     * obj parameter as a string.
     */
    private class ReadThread extends Thread {

        private InputStream mInputStream;
        private final int mBufferSize;
        private final String mAddress;

        /**
         * Creates a new thread that will read input from a stream.
         *
         * @param address mac address for the remote bluetooth device
         * @param bufferSize max size of input buffer
         * @param inputStream input stream of the bluetooth connection
         */
        public ReadThread(String address, int bufferSize, InputStream inputStream){
            mInputStream = inputStream;
            mBufferSize = bufferSize;
            mAddress = address;
        }

        /**
         * Attempts to create a BluetoothMessage object form buffer. The service either handles the message or
         * passes it down to the mClientHandler depending on the type of message.
         *
         * @param numBytes size of message
         * @param buffer the message that was read
         */
        private void parseMessage(int numBytes, byte[] buffer){
            //check if buffer is as long as the id
            //attempt to extract the type from the buffer,
            //based on the type, perform appropriate action

            String message = new String(buffer, 0, numBytes);
            Log.v(TAG, "Read message " + message + " from " + mAddress);
            if(numBytes < BluetoothMessageUtility.LENGTH_ID) {
                Log.w(TAG, "Got a message with size less than the length of a service id: " + message);
                return;
            }

            if(mClientHandler == null) {
                Log.e(TAG, "NO HANDLER, LOSING MESSAGE: " + message);
                return;
            }

            Message msg;
            int type = BluetoothMessage.getType(buffer);

            switch (type) {
                case BluetoothMessageUtility.TYPE_HELLO:
                    //always reply to hellos
                    sendMessage(new BluetoothMessageAccept(mMyAddress), mAddress);
                    break;

                case BluetoothMessageUtility.TYPE_HELLO_REPLY:
                    //reset the timeout counter since we got a reply
                    BluetoothConnectionInfo info = mClients.get(mAddress);
                    if(info != null)
                        info.connectionAttempts = 0;
                    break;

                case BluetoothMessageUtility.TYPE_CONNECTION_CLOSED:
                    //only kicked and goodbye close messages are sent over bluetooth, the
                    //other close messages are errors that force the connection to close immediately.
                    BluetoothMessageClose m_c = BluetoothMessageClose.reconstruct(message.getBytes());

                    if(m_c.getCloseCode() == BluetoothServiceUtility.CLOSE_KICKED_FROM_SERVER
                            || m_c.getCloseCode() == BluetoothServiceUtility.CLOSE_SAY_GOODBYE )
                        mBinder.removeSocket(m_c.getMacAddress(), m_c.getCloseCode());
                    else
                        Log.w(TAG, "Read close code that should not have been sent " + m_c.getCloseCode());
                    break;

                case BluetoothMessageUtility.TYPE_SERVER_SETUP_FINISHED:
                    BluetoothMessageSetup m_s = BluetoothMessageSetup.reconstruct(message.getBytes());

                    msg = mClientHandler.obtainMessage(0, 0, 0, m_s);
                    mClientHandler.sendMessage(msg);
                    break;

                case BluetoothMessageUtility.TYPE_APP_MESSAGE:

                    BluetoothMessageOpen m_a = BluetoothMessageOpen.reconstruct(message.getBytes());

                    msg = mClientHandler.obtainMessage(0, 0, 0, m_a);
                    mClientHandler.sendMessage(msg);
                    break;
                default:
                    Log.v(TAG, "Unknown message id: " + message);
            }
        }

        @Override
        public void run() {
            Log.v(TAG, "START READING: " + mAddress);

            byte[] buffer = new byte[mBufferSize];
            int numBytes;

            while( !isInterrupted() ){
                try {
                    numBytes = mInputStream.read(buffer);
                    parseMessage(numBytes, buffer);
                } catch (IOException e) {
                    Log.v(TAG, "READ EXCEPTION: " + mAddress);
                    e.printStackTrace();
                    mBinder.removeSocket(mAddress, BluetoothServiceUtility.CLOSE_READ_CLOSE);
                    return;
                }
            }
            Log.v(TAG,"READING INTERRUPTED: " + mAddress);
            mBinder.removeSocket(mAddress, BluetoothServiceUtility.CLOSE_READ_CLOSE);
        }
    }

    /**
     * Thread that writes data to a remote bluetooth device using an outputStream retrieved from a
     * bluetooth socket.
     */
    private class WriteThread extends Thread {
        private final String mAddress;
        private OutputStream mOutputStream;
        private ArrayBlockingQueue<BluetoothMessage> mQueue;
        /**
         * when the thread receives this message from the queue, the thread will close.
         */
        private static final String SHUTDOWN_KEY = "xx:xx:xx:xx:xx:xx";

        /**
         * Creates a thread that writes input using a stream. Communicate to this thread by giving it
         * messages to send using the blocking queue that is given as a parameter.
         *
         * @param address mac address of the thread used for debugging
         * @param outputStream output stream of a bluetooth socket
         * @param queue a queue that will be given messages to be sent using output stream..
         */
        public WriteThread(String address, OutputStream outputStream, ArrayBlockingQueue<BluetoothMessage> queue){
            mAddress = address;
            mOutputStream = outputStream;
            mQueue = queue;
        }

        public boolean shutdown(BluetoothMessage message){
            return message.getMessageType() == BluetoothMessageUtility.TYPE_APP_MESSAGE &&
                    new String(((BluetoothMessageOpen) message).getData()).equals(SHUTDOWN_KEY);
        }

        @Override
        public void run() {
            Log.v(TAG, "START WRITING: " + mAddress);
            BluetoothMessage message ;
            byte[] bytes;
            while ( !isInterrupted() ){
                try{
                    message = mQueue.take();
                    if( shutdown(message) ){
                        Log.v(TAG, "SHUTDOWN KEY RECEIVED: " + mAddress);
                        mBinder.removeSocket(mAddress, BluetoothServiceUtility.CLOSE_WRITE_CLOSE);
                        return;
                    }
                    bytes = message.makeBytes();
                    mOutputStream.write(bytes);
                }catch (InterruptedException e){
                    Log.v(TAG, "WRITE QUEUE EXCEPTION  " + mAddress);
                    e.printStackTrace();
                    mBinder.removeSocket(mAddress, BluetoothServiceUtility.CLOSE_WRITE_CLOSE);
                    return;
                } catch (IOException e) {
                    Log.v(TAG, "WRITE EXCEPTION, " + mAddress);
                    e.printStackTrace();
                    mBinder.removeSocket(mAddress, BluetoothServiceUtility.CLOSE_WRITE_CLOSE);
                    return;
                }
            }
            Log.v(TAG,"CLOSING INTERRUPTED: " + mAddress);
            mBinder.removeSocket(mAddress, BluetoothServiceUtility.CLOSE_WRITE_CLOSE);
        }
    }

    /**
     * Stores data associated with a bluetooth connection
     */
    private class BluetoothConnectionInfo {
        /**
         * true if the connection is being deleted.
         */
        volatile boolean deleting;
        /**
         * number of unanswered hello messages
         */
        int connectionAttempts;
        OutputStream outputStream;
        InputStream inputStream;
        Thread writeThread;
        Thread readThread;
        BluetoothSocket socket;
        BluetoothDevice device;
        /**
         * used to give writing thread data.
         */
        ArrayBlockingQueue<BluetoothMessage> blockingQueue;
    }

    /**
     * Class that allows for communication with the service. Has Three primary steps to setup.
     * <p>
     * 1. Give the service bluetooth sockets.
     * <p>
     * 2. Give the service the handler from the ui thread.
     * <p>
     * 3. Remove and close sockets.
     * <p>
     * Sockets are automatically closed when the services onDestroy is called.
     */
    public class BluetoothBinder extends Binder {

        /**
         * Add a bluetooth socket that has been connected with a remote bluetooth device. A socket
         * can be connected using BluetoothServerSocket.accept() for a server or
         * createRfcommSocketToServiceRecord().connect() for a client.
         *
         * @param socket bluetooth socket that will be added
         * @return false if output streams could not be created from socekets
         */
        public boolean addSocket(@NonNull BluetoothSocket socket){
            Log.v(TAG, "adding socket with address " +socket.getRemoteDevice().getAddress());
            BluetoothConnectionInfo info = new BluetoothConnectionInfo();

            OutputStream tmpOut;
            InputStream tmpIn;

            //get outputStream, return if cannot
            try {
                tmpOut = socket.getOutputStream();

            } catch (IOException e) {
                Log.e(TAG, "could not get an outputStream from socket " + socket.getRemoteDevice().getAddress());
                e.printStackTrace();
                return false;
            }

            //get inputStream, close outputStream and return if cannot
            try {
                tmpIn = socket.getInputStream();

            } catch (IOException e) {
                Log.e(TAG, "could not get an inputStream from socket " + socket.getRemoteDevice().getAddress());
                e.printStackTrace();
                try {
                    Log.e(TAG, "closing outputStream because could not get inputStream from socket "
                            + socket.getRemoteDevice().getAddress());
                    tmpOut.close();
                } catch (IOException e1) {
                    Log.e(TAG, "closing outputStream because could not get inputStream from socket "
                            + socket.getRemoteDevice().getAddress() + ": has FAILED");
                    e1.printStackTrace();
                }
                return false;
            }
            info.connectionAttempts = 0;
            info.deleting = false;
            info.inputStream = tmpIn;
            info.outputStream = tmpOut;
            info.socket = socket;
            info.device = socket.getRemoteDevice();
            //TODO determine ideal queue size for requests
            info.blockingQueue = new ArrayBlockingQueue<>(10, true);

            mClients.put(info.device.getAddress(), info);
            enableRW(info);
            return true;
        }


        /**
         * Removes and closes all sockets that have been added. The reading and writing will
         * also stop for that socket if it hasn't already.
         *
         * @param closeCode id that identifies why the socket is being closed
         */
        public void removeSockets(@BluetoothServiceUtility.CLOSE_CODE int closeCode){

            ArrayList<BluetoothConnectionInfo> list = new ArrayList<>(mClients.values());
            for(int i = 0; i < list.size(); i++){
                removeSocket(list.get(i).device.getAddress(), closeCode);
            }
        }


        /**
         * Removes and closes the socket with the given mac address if it exists. The reading and
         * writing will also stop for that socket.
         *
         * @param macAddress mac address of the bluetooth device that will have the message sent to
         * @param closeCode id that identifies why the socket is being closed
         */
        public void removeSocket(String macAddress, @BluetoothServiceUtility.CLOSE_CODE int closeCode){

            BluetoothMessageClose closeMessage = new BluetoothMessageClose(mMyAddress, closeCode);

            BluetoothConnectionInfo tmpInfo = mClients.get(macAddress);

            //if cant find connection or already deleting, return
            if(tmpInfo == null || tmpInfo.deleting)
                return;
            tmpInfo.deleting = true;

            Log.v(TAG, closeCode +": removeSocket for " + macAddress);

            if(closeCode == BluetoothServiceUtility.CLOSE_KICKED_FROM_SERVER ||
                    closeCode == BluetoothServiceUtility.CLOSE_SAY_GOODBYE){
                sendMessage(closeMessage, macAddress);
            }

            disableRW(tmpInfo);

            //TODO delay rest of code so that socket isn't closed before close message is read
            try {
                Log.v(TAG, "closing the input stream: " + macAddress);
                tmpInfo.inputStream.close();
            } catch (IOException e) {
                Log.w(TAG, "could not close the input stream: " + macAddress);
            }

            try {
                Log.v(TAG, "closing the output stream: " + macAddress);
                tmpInfo.outputStream.close();
            } catch (IOException e) {
                Log.w(TAG, "could not close the output stream: " + macAddress);
            }

            try {
                Log.v(TAG, "closing the input socket: " + macAddress);
                tmpInfo.socket.close();
            } catch (IOException e) {
                Log.w(TAG, "could not close socket: " + macAddress);
            }

            mClients.remove(tmpInfo.device.getAddress());
            Log.v(TAG, "removed from clients: " + tmpInfo.device.getAddress());

            //Send message to handler
            if(mClientHandler != null){
                Message m = mClientHandler.obtainMessage(0, 0, 0, closeMessage);
                mClientHandler.sendMessage(m);
            }
        }


        /**
         * Write a message to all bluetooth sockets that are enabled.
         *
         * @param data message to be sent
         */
        public void writeMessage(byte[] data){
            for(BluetoothConnectionInfo tmpInfo: mClients.values()){
                writeMessage(data, tmpInfo.device.getAddress());
            }
        }


        /**
         * write a message to the specified bluetooth device with the specified mac address.The mac
         * address ia available using BluetoothSocket.getRemoteDevice().getAddress().
         *
         * @param data data to be sent
         * @param macAddress mac address of the bluetooth device that will have the message sent to
         * @return false if mac address doesn't exist for an added socket.
         */
        public boolean writeMessage(byte[] data, String macAddress){
            BluetoothMessageOpen m = new BluetoothMessageOpen(mMyAddress, data);
            return sendMessage(m,macAddress);
        }


        /**
         * The server calls this method to have clients know that the server is finished
         * setting up and that they have been accepted.
         */
        public void serverReady(){
            for(BluetoothConnectionInfo tmpInfo: mClients.values()){
                sendMessage(new BluetoothMessageSetup(mMyAddress), tmpInfo.device.getAddress());
            }
        }


        /**
         * Get the bluetooth device with the given mac address.
         *
         * @param macAddress mac address of the bluetooth device that will have the message sent to
         * @return the bluetooth device with the given macAddress, null if it does not exist.
         */
        public BluetoothDevice getDevice(String macAddress){
            if( !mClients.containsKey(macAddress) )
                return null;
            return (mClients.get(macAddress)).device;
        }


        /**
         * Handler that will receive messages when something is read. String message will be in obj
         * parameter and arg1 will contain size of message.
         *
         * @param handler handler to receive messages.
         */
        public void setHandler(@Nullable BluetoothServiceHandler handler){
            mClientHandler = handler;
        }


        /**
         * Get an instance of the handler that has been set.
         * @return handler that has been set
         */
        public @Nullable
        BluetoothServiceHandler getHandler(){
            return mClientHandler;
        }
    }
}
