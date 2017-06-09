package me.lancer.bluetoothchatroom.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import me.lancer.bluetoothchatroom.R;

import java.util.ArrayList;

public class ClientAdapter extends RecyclerView.Adapter<ClientAdapter.MyViewHolder> {

    private PairingItemClickListener mListener;
    private ArrayList<BluetoothDevice> mDevices;
    private Context mContext;

    public ClientAdapter(Context context){
        mContext = context;
        mDevices = new ArrayList<>(10);
    }


    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.client_item, parent, false);

        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        if(position == mDevices.size()){
            holder.setVisible(false);
        }else {
            holder.setVisible(true);
            holder.mNameTextView.setText(mDevices.get(position).getName());
            holder.mAddressTextView.setText(mDevices.get(position).getAddress());
        }
    }

    @Override
    public int getItemCount() {
        return mDevices.size() + 1;
    }

    public void addItem(BluetoothDevice device){
        if(device == null)
            throw new IllegalArgumentException("addItem parameter cannot be null");
        if( !contains(device.getAddress()) ) {
            mDevices.add(device);
            notifyDataSetChanged();
        }
    }

    public void addItems(ArrayList<BluetoothDevice> devices){
        if(devices == null)
            throw new IllegalArgumentException("parameter is null");
        for(BluetoothDevice device: devices){
            addItem(device);
        }
    }

    public boolean contains(String address){
        for (BluetoothDevice i: mDevices) {
            if(i.getAddress().equals(address))
                return true;
        }
        return false;
    }

    public void clearData(){
        mDevices.clear();
        notifyDataSetChanged();
    }

    public void setPairingItemClickListener(PairingItemClickListener listener){
        mListener = listener;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public TextView mNameTextView;
        public TextView mAddressTextView;
        public ImageView mConnectionImageView;

        public MyViewHolder(View itemView) {
            super(itemView);
            mNameTextView = (TextView) itemView.findViewById(R.id.tv_bluetooth_name);
            mAddressTextView = (TextView) itemView.findViewById(R.id.tv_bluetooth_address);
            mConnectionImageView = (ImageView) itemView.findViewById(R.id.iv_join);
            mConnectionImageView.setOnClickListener(this);
        }

        public void setVisible(boolean visible){
            if (visible) {
                mNameTextView.setVisibility(View.VISIBLE);
                mAddressTextView.setVisibility(View.VISIBLE);
                mConnectionImageView.setVisibility(View.VISIBLE);
            } else {
                mNameTextView.setVisibility(View.INVISIBLE);
                mAddressTextView.setVisibility(View.INVISIBLE);
                mConnectionImageView.setVisibility(View.INVISIBLE);
            }
        }

        @Override
        public void onClick(View v) {
            if(mListener != null){
                mListener.itemClick(v,mDevices.get(getAdapterPosition()));
            }
        }
    }

    public interface PairingItemClickListener{
        void itemClick(View clickedView, BluetoothDevice device);
    }
}
