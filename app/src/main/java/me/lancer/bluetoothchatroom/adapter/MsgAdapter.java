package me.lancer.bluetoothchatroom.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.github.library.bubbleview.BubbleTextVew;

import java.util.List;

import me.lancer.bluetoothchatroom.R;

public class MsgAdapter extends RecyclerView.Adapter<MsgAdapter.ViewHolder> {

    private Context context;
    private String name;
    private List<String> list;

    public MsgAdapter(Context context, String name, List<String> list) {
        this.context = context;
        this.list = list;
        this.name = name;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.msg_item, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {
        if (list.get(position) != null) {
            if (list.get(position).contains(name)) {
                viewHolder.btvRight.setVisibility(View.VISIBLE);
                viewHolder.btvRight.setText(list.get(position).split("-----")[1]);
                viewHolder.tvRightId.setVisibility(View.VISIBLE);
                viewHolder.tvRightId.setText(list.get(position).split("-----")[0]);
                viewHolder.tvRight.setVisibility(View.VISIBLE);
                viewHolder.btvLeft.setVisibility(View.GONE);
                viewHolder.tvLeftId.setVisibility(View.GONE);
                viewHolder.tvLeft.setVisibility(View.GONE);
            } else if (!list.get(position).contains(name)) {
                viewHolder.btvLeft.setVisibility(View.VISIBLE);
                viewHolder.btvLeft.setText(list.get(position).split("-----")[1]);
                viewHolder.tvLeftId.setVisibility(View.VISIBLE);
                viewHolder.tvLeftId.setText(list.get(position).split("-----")[0]);
                viewHolder.tvLeft.setVisibility(View.VISIBLE);
                viewHolder.btvRight.setVisibility(View.GONE);
                viewHolder.tvRightId.setVisibility(View.GONE);
                viewHolder.tvRight.setVisibility(View.GONE);
            }
        }
    }

    private int dp2px(float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        public TextView tvLeftId;
        public TextView tvRightId;
        public BubbleTextVew btvLeft;
        public BubbleTextVew btvRight;
        public TextView tvLeft;
        public TextView tvRight;

        public ViewHolder(View rootView) {
            super(rootView);
            tvLeftId = (TextView) rootView.findViewById(R.id.tv_left_id);
            tvRightId = (TextView) rootView.findViewById(R.id.tv_right_id);
            btvLeft = (BubbleTextVew) rootView.findViewById(R.id.btv_left);
            btvRight = (BubbleTextVew) rootView.findViewById(R.id.btv_right);
            tvLeft = (TextView) rootView.findViewById(R.id.tv_left);
            tvRight = (TextView) rootView.findViewById(R.id.tv_right);
        }
    }
}
