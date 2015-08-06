package com.example.zack.blemaster;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Zack on 15/8/5.
 */
public class P2PActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "Zack";
    private static final int RESULT_LOAD_MEDIA = 100;
    private static final int CONNECT_STATE_NONE = 1;
    private static final int CONNECT_STATE_CONNECTED = 2;
    private static final int CONNECT_STATE_FAILED = 3;
    private static final int CONNECT_STATE_SOCKET_CREATE_FAILED = 4;
    private static final int CONNECT_STATE_SENT_IMAGE_FAILED = 5;

    private int mState;
    private static final UUID MY_UUID =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    private BluetoothManager mBTMagager;
    private BluetoothGatt mGatt;
    private BluetoothAdapter mBTAdapter;
    private TextView tvDevice, tvFile;
    private Button btSelect, btSent;
    private boolean readyTosend;
    private String imagePath, MacAddr;

    private ImageView ivTest;
    private SendData mConnectThread;

    private BluetoothDevice mDevice;
    private Bitmap ImageToSent;
    private OutputStream outStream = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitty_p2p);

        initView();
        initConnection();

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    private void initConnection() {
        MacAddr = getIntent().getExtras().getString("MAC");
        tvDevice.setText(MacAddr);
        mDevice = mBTAdapter.getRemoteDevice(MacAddr);
        mState = CONNECT_STATE_NONE;

        mConnectThread = new SendData();
        if (mState != CONNECT_STATE_SOCKET_CREATE_FAILED)
            mConnectThread.start();
    }

    private void initView() {
        mBTMagager = (BluetoothManager) getSystemService(this.BLUETOOTH_SERVICE);
        mBTAdapter = mBTMagager.getAdapter();
        tvDevice = (TextView) findViewById(R.id.tvDevice);
        tvFile = (TextView) findViewById(R.id.tvfile);
        btSelect = (Button) findViewById(R.id.btSelect);
        btSent = (Button) findViewById(R.id.btSent);
        ivTest = (ImageView) findViewById(R.id.ivTest);
        btSent.setOnClickListener(this);
        btSelect.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int ID = v.getId();
        switch (ID) {
            case R.id.btSelect:
                Intent mediaChooser = new Intent(Intent.ACTION_PICK);
                mediaChooser.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(mediaChooser, RESULT_LOAD_MEDIA);
                break;
            case R.id.btSent:
                if (mState != CONNECT_STATE_FAILED)
                    mConnectThread.sendImage();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RESULT_LOAD_MEDIA:
                if (resultCode == RESULT_OK && null != data) {
                    Uri pickedImage = data.getData();
                    String[] filePath = {MediaStore.Images.Media.DATA};
                    Cursor cursor = getContentResolver().query(pickedImage, filePath, null, null, null);
                    cursor.moveToFirst();
                    imagePath = cursor.getString(cursor.getColumnIndex(filePath[0]));
                    ImageToSent = BitmapFactory.decodeFile(imagePath);
                    tvFile.setText(imagePath);
                    ivTest.setImageBitmap(ImageToSent);

                    cursor.close();
                }
                break;
        }
    }


    class SendData extends Thread {
        private BluetoothDevice device = null;
        private BluetoothSocket btSocket = null;
        private OutputStream outStream = null;

        public SendData(){
            device = mBTAdapter.getRemoteDevice(MacAddr);
            try
            {
                btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            }
            catch (Exception e) {
                // TODO: handle exception
            }
            mBTAdapter.cancelDiscovery();
            try {
                btSocket.connect();
            } catch (IOException e) {
                try {
                    btSocket.close();
                } catch (IOException e2) {
                }
            }
            Toast.makeText(getBaseContext(), "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
            try {
                outStream = btSocket.getOutputStream();
            } catch (IOException e) {
            }
        }

        public void sendImage()
        {
            try {
                mBTAdapter = BluetoothAdapter.getDefaultAdapter();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageToSent.compress(Bitmap.CompressFormat.JPEG, 100,baos); //bm is the bitmap object
                byte[] b = baos.toByteArray();
                Toast.makeText(getBaseContext(), String.valueOf(b.length), Toast.LENGTH_SHORT).show();
                outStream.write(b);
                outStream.flush();
                Log.d(TAG,"Image Sent");
            } catch (IOException e) {
                Log.d(TAG,"Image Sent ERROR          "+e);
            }
        }

    }


}
