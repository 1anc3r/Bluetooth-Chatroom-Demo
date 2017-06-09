package me.lancer.bluetoothchatroom.aynctask;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class ClientConnectTask extends AsyncTask<Void, Void, BluetoothSocket> {
    private static final String TAG = "ClientConnectTask";

    private BluetoothSocket mClientSocket;
    private ClientEventListener mListener = null;

    public ClientConnectTask(BluetoothDevice device, UUID uuid){
        BluetoothSocket tmp = null;
        try {
            tmp = device.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
            Log.v(TAG, "could not get the socket");
            e.printStackTrace();
        }

        mClientSocket = tmp;
    }

    public void setListener(ClientEventListener listener){
        mListener = listener;
    }

    @Override
    protected BluetoothSocket doInBackground(Void... params) {
        Log.v(TAG, "starting client");
        try {
            mClientSocket.connect();
            return mClientSocket;
        } catch (IOException e) {
            Log.v(TAG, "could not connect to server");
            try {
                mClientSocket.close();
            } catch (IOException e1) {
                Log.v(TAG, "could not close socket");
                e1.printStackTrace();
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(BluetoothSocket socket) {
        if(socket != null) {
            Log.v(TAG, "socket ready for communication");
            if(isCancelled()){
                Log.v(TAG, "connected with " + socket.getRemoteDevice().getAddress() + ", but canceled");
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }

            if(mListener!= null)
                mListener.serverSearchFinished(true, socket);
        }else {
            Log.v(TAG, "socket not available for communication");
            if(mListener!= null)
                mListener.serverSearchFinished(false, null);
        }
    }

    @Override
    protected void onCancelled() {
        Log.v(TAG, "canceling client");
        try {
            mClientSocket.close();
        } catch (IOException e1) {
            Log.v(TAG, "could not close socket");
            e1.printStackTrace();
        }
    }

    public interface ClientEventListener{
        /**
         * The Search for the server has compleated or timed out
         *
         * @param found true if the server was found, false otherwise
         * @param socket socket that connected to server, null if found is false
         */
        void serverSearchFinished(boolean found, BluetoothSocket socket);
    }
}