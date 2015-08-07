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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Zack on 15/8/5.
 */
public class P2PActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "Zacks";
    private static final int RESULT_LOAD_MEDIA = 100;
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;

    private int mState;
    private static final UUID MY_UUID =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    private BluetoothManager mBTMagager;
    private BluetoothGatt mGatt;
    private BluetoothAdapter mBTAdapter;
    private TextView tvDevice, tvFile;
    private Button btSelect, btSent,btConnect;
    private boolean readyTosend;
    private String imagePath, MacAddr;

    private ImageView ivTest;

    private BluetoothDevice mDevice;
    private Bitmap ImageToSent;
    private OutputStream outStream = null;


    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;


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
        mState = STATE_NONE;

    }

    private void initView() {
        mBTMagager = (BluetoothManager) getSystemService(this.BLUETOOTH_SERVICE);
        mBTAdapter = mBTMagager.getAdapter();
        tvDevice = (TextView) findViewById(R.id.tvDevice);
        tvFile = (TextView) findViewById(R.id.tvfile);
        btSelect = (Button) findViewById(R.id.btSelect);
        btSent = (Button) findViewById(R.id.btSent);
        ivTest = (ImageView) findViewById(R.id.ivTest);
        btConnect = (Button)findViewById(R.id.btConnect);
        btSent.setOnClickListener(this);
        btSelect.setOnClickListener(this);
        btConnect.setOnClickListener(this);
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

                // Perform the write unsynchronized
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                ImageToSent.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                Log.d(TAG, "Size = "+byteArray.length+"Tempt to write " + new String(byteArray));

                this.write(byteArray);
                break;
            case R.id.btConnect:
                this.connect(mDevice);
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


    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        mState = STATE_CONNECTING;
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                    tmp = device.createRfcommSocketToServiceRecord(
                            Const.UNIQ_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mBTAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                return;
            }

            Log.d(TAG,"@@@@@@@@@@@@@@@@@@@@@@@@");
            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            // Keep listening to the InputStream while connected
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {

            try {
                mmOutStream.write(buffer);
//                mmOutStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }


    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        Log.d(TAG, "connected, Socket Type:" + socketType);

        // Cancel the thread that completed the connection
//        if (mConnectThread != null) {
//            Log.d(TAG,"XXXXXXXXXXXXXXXXXXXXX   313     XXXXXXXXXXXXXXXXXXXXXXX");
//            mConnectThread.cancel();
//            mConnectThread = null;
//        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            Log.d(TAG,"XXXXXXXXXXXXXXXXXXXXX   320     XXXXXXXXXXXXXXXXXXXXXXX");
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();
        Log.d(TAG,"STATE_CONNECTED");
        mState = STATE_CONNECTED;
    }

    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

}
