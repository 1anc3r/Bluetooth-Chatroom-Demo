package me.lancer.bluetoothchatroom.adapter;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import me.lancer.bluetoothchatroom.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

public class ServerAdapter extends RecyclerView.Adapter<ServerAdapter.MyViewHolder> {

    private static final String TAG = "ServerRecyclerAdapter";

    @IntDef({ACCEPTED, UNACCEPTED, ALL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ITEM_TYPE {
    }

    public static final int ACCEPTED = 0;
    public static final int UNACCEPTED = 1;
    public static final int ALL = 2;

    private ArrayList<BluetoothSocket> mAccepted;
    private ArrayList<BluetoothSocket> mUnaccepted;
    private ServerItemClickListener mListener;
    private Context mContext;

    private String mSelectedItemAddress;

    public ServerAdapter(Context context, @NonNull ServerItemClickListener listener) {
        mListener = listener;
        mContext = context;
        mAccepted = new ArrayList<>(10);
        mUnaccepted = new ArrayList<>(10);
        mSelectedItemAddress = null;
    }


    @Override
    public ServerAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.server_item, parent, false);
        return new MyViewHolder(v);
    }


    @Override
    public void onBindViewHolder(ServerAdapter.MyViewHolder holder, int position) {
        if (position == mAccepted.size() + mUnaccepted.size()) {
            holder.setVisible(false);
            return;
        }
        holder.setVisible(true);

        boolean connected = position < mAccepted.size();
        if (connected) {
            holder.mNameTextView.setText(mAccepted.get(position).getRemoteDevice().getName());
            holder.mAddressTextView.setText(mAccepted.get(position).getRemoteDevice().getAddress());
        } else {
            position -= mAccepted.size();
            holder.mNameTextView.setText(mUnaccepted.get(position).getRemoteDevice().getName());
            holder.mAddressTextView.setText(mUnaccepted.get(position).getRemoteDevice().getAddress());
        }

        if (mSelectedItemAddress == null || !mSelectedItemAddress.equals(holder.mAddressTextView.getText()))
            holder.mExtrasImageView.setSelected(false);
        else
            holder.mExtrasImageView.setSelected(true);

    }

    @Override
    public int getItemCount() {
        return mAccepted.size() + mUnaccepted.size() + 1;
    }

    public int getNumSockets(@ITEM_TYPE int itemType) {
        switch (itemType) {
            case ACCEPTED:
                return mAccepted.size();
            case UNACCEPTED:
                return mUnaccepted.size();
            case ALL:
                return mAccepted.size() + mUnaccepted.size();
            default:
                return -1;
        }
    }

    public ArrayList<BluetoothSocket> getSockets(@ITEM_TYPE int itemType) {
        switch (itemType) {
            case ACCEPTED:
                return mAccepted;
            case UNACCEPTED:
                return mUnaccepted;
            case ALL:
                ArrayList<BluetoothSocket> list = new ArrayList<>(mAccepted);
                list.addAll(mUnaccepted);
                return list;
            default:
                return new ArrayList<>();
        }
    }

    public void setSelectedItemAddress(@NonNull String address) {
        if (!contains(ALL, address)) {
            Log.w(TAG, address + ": address does not exist for setSelectedItemAddress ");
            return;
        }
        mSelectedItemAddress = address;
        notifyDataSetChanged();
    }

    public
    @Nullable
    String getSelectedItemAddress() {
        return mSelectedItemAddress;
    }

    public void removeSelectedItem() {
        mSelectedItemAddress = null;
        notifyDataSetChanged();
    }

    public void addItem(@ITEM_TYPE int itemType, @NonNull BluetoothSocket socket) {
        switch (itemType) {
            case ACCEPTED:
                if (!contains(ACCEPTED, socket.getRemoteDevice().getAddress())) {
                    mAccepted.add(socket);
                    notifyDataSetChanged();
                }
                break;
            case UNACCEPTED:
                if (!contains(UNACCEPTED, socket.getRemoteDevice().getAddress())) {
                    mUnaccepted.add(socket);
                    notifyDataSetChanged();
                }
                break;
            case ALL:
                break;
        }
    }

    public void addItems(@ITEM_TYPE int itemType, @NonNull ArrayList<BluetoothSocket> sockets) {
        for (BluetoothSocket socket : sockets) {
            addItem(itemType, socket);
        }
    }

    public void changeItemType(@ITEM_TYPE int itemType, @NonNull BluetoothSocket socket) {
        switch (itemType) {
            case ALL:
            case ACCEPTED:
                if (mUnaccepted.contains(socket)) {
                    mUnaccepted.remove(socket);
                    mAccepted.add(socket);
                    notifyDataSetChanged();
                    break;
                }
                if (itemType == ACCEPTED)
                    break;
            case UNACCEPTED:
                if (mAccepted.contains(socket)) {
                    mAccepted.remove(socket);
                    mUnaccepted.add(socket);
                    notifyDataSetChanged();
                    break;
                }
                if (itemType == UNACCEPTED)
                    break;
        }
    }

    public void clearData() {
        mAccepted.clear();
        mUnaccepted.clear();
        notifyDataSetChanged();
    }

    public boolean contains(@ITEM_TYPE int itemType, String address) {
        switch (itemType) {
            case ALL:
            case ACCEPTED:
                for (BluetoothSocket socket : mAccepted) {
                    if (socket.getRemoteDevice().getAddress().equals(address))
                        return true;
                }
                if (itemType == ACCEPTED)
                    break;

            case UNACCEPTED:
                for (BluetoothSocket socket : mUnaccepted) {
                    if (socket.getRemoteDevice().getAddress().equals(address))
                        return true;
                }
                if (itemType == UNACCEPTED)
                    break;
        }
        return false;
    }

    public void removeItem(BluetoothSocket socket) {
        if (mAccepted.remove(socket) || mUnaccepted.remove(socket))
            notifyDataSetChanged();
    }

    public void removeItem(String address) {
        boolean changed = false;
        for (BluetoothSocket socket : mAccepted) {
            if (socket.getRemoteDevice().getAddress().equals(address)) {
                mAccepted.remove(socket);
                changed = true;
                break;
            }
        }

        for (BluetoothSocket socket : mUnaccepted) {
            if (socket.getRemoteDevice().getAddress().equals(address)) {
                mUnaccepted.remove(socket);
                changed = true;
                break;
            }
        }

        if (changed)
            notifyDataSetChanged();
    }

    public interface ServerItemClickListener {

        void itemClick(BluetoothSocket socket);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView mNameTextView;
        public TextView mAddressTextView;
        public ImageView mExtrasImageView;

        public MyViewHolder(View view) {
            super(view);
            mNameTextView = (TextView) view.findViewById(R.id.tv_bluetooth_name);
            mAddressTextView = (TextView) view.findViewById(R.id.tv_bluetooth_address);
            mExtrasImageView = (ImageView) view.findViewById(R.id.iv_join);

            mExtrasImageView.setOnClickListener(this);
        }

        public void setVisible(boolean visible) {
            if (visible) {
                mNameTextView.setVisibility(View.VISIBLE);
                mAddressTextView.setVisibility(View.VISIBLE);
                mExtrasImageView.setVisibility(View.VISIBLE);
            } else {
                mNameTextView.setVisibility(View.INVISIBLE);
                mAddressTextView.setVisibility(View.INVISIBLE);
                mExtrasImageView.setVisibility(View.INVISIBLE);
                mExtrasImageView.setSelected(false);
            }
        }

        @Override
        public void onClick(View v) {
            if (mListener != null) {
                boolean connected = getAdapterPosition() < mAccepted.size();
                if (connected) {
                    mListener.itemClick(mAccepted.get(getAdapterPosition()));
                } else {
                    mListener.itemClick(mUnaccepted.get(getAdapterPosition() - mAccepted.size()));
                }
            }
        }
    }
}
