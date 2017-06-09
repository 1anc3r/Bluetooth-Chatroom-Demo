package me.lancer.bluetoothchatroom.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import me.lancer.bluetoothchatroom.R;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private BluetoothAdapter mBlueToothAdapter;
    private Button btnCreate, btnJoin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBlueToothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBlueToothAdapter == null) {
            Toast.makeText(this, "蓝牙不可用", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnCreate = (Button) findViewById(R.id.btn_create_chatroom);
        btnCreate.setOnClickListener(this);
        btnJoin = (Button) findViewById(R.id.btn_join_chatroom);
        btnJoin.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view == btnCreate){
            if(mBlueToothAdapter.isEnabled()) {
                startActivity(new Intent(this, ServerActivity.class));
            }else{
                Toast.makeText(MainActivity.this, "蓝牙不可用", Toast.LENGTH_SHORT).show();
            }
        }else if (view == btnJoin){
            startActivity(new Intent(this, ClientActivity.class));
        }
    }
}
