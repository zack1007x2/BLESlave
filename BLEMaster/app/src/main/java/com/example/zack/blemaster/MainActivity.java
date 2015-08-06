package com.example.zack.blemaster;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class MainActivity extends Activity implements View.OnClickListener {


    private static final String TAG = "Zack";
    private static final String DEVICE_NEXUS = "NEX6";
    private static final String DEVICE_OTHER = "Zack";
    private static final boolean NEXUS6 = false;

    private static final int REQUEST_BLUETOOTH_ENABLE = 1;

    private BluetoothLeAdvertiser bleAd;
    private BluetoothAdapter BAdapter;
    private static final UUID CUSTOM_SERVICE_UUID = UUID.randomUUID();
    private static final UUID C1_UUID = UUID.fromString("2222180F-0000-1000-8000-00805f9b34fb");
    private static final UUID C2_UUID = UUID.fromString("22221805-0000-1000-8000-00805f9b34fb");
    private static final UUID C3_UUID = UUID.fromString("22221243-0000-1000-8000-00805f9b34fb");
    private static final UUID D1_UUID = UUID.fromString("00001111-0000-1000-8000-00805f9b34fb");
    private static final UUID D2_UUID = UUID.fromString("00001112-0000-1000-8000-00805f9b34fb");
    private static final UUID S1_UUID = UUID.fromString("00001811-0000-1000-8000-00805f9b34fb");
    private static final ParcelUuid PUUID = new ParcelUuid(CUSTOM_SERVICE_UUID);
    private TextView tvAddr, tvStatus1, tvStatus2;
    private BluetoothManager Bm;
    private Button btScan, btHome;
    private ListView lvScan;
    private boolean scanning;
    private boolean advtising;
    private BluetoothGatt mgatt;
    private BluetoothDevice curdevice;

    private DeviceAdapter dAdapter;
    private ServiceAdapter sAdapter;
    private CharacteristicAdapter cAdapter;

    private List<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();
    private BluetoothGattServer mGattServer;

    private AdvertiseData.Builder dataBuilder;
    private AdvertiseSettings.Builder settingsBuilder;
    private int mode;
    private static final int MODE_DEVICE = 0;
    private static final int MODE_SERVICE = 1;
    private static final int MODE_CHARACTERISTIC = 2;

    private byte[] buffer = new byte[8192];
    private ImageView ivReceive;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvAddr = (TextView) findViewById(R.id.tvAddr);
        tvStatus1 = (TextView) findViewById(R.id.tvStatus1);
        tvStatus2 = (TextView) findViewById(R.id.tvStatus2);
        btScan = (Button) findViewById(R.id.btScan);
        btHome = (Button) findViewById(R.id.btHome);
        lvScan = (ListView) findViewById(R.id.lvScan);
        ivReceive = (ImageView) findViewById(R.id.ivReceive);
        dAdapter = new DeviceAdapter(this);
        sAdapter = new ServiceAdapter(this);
        cAdapter = new CharacteristicAdapter(this);
        btScan.setOnClickListener(this);
        btHome.setOnClickListener(this);
        lvScan.setOnItemClickListener(onDeviceSelectListener);
        mode = MODE_DEVICE;

        Bm = (BluetoothManager) getSystemService(this.BLUETOOTH_SERVICE);
        BAdapter = Bm.getAdapter();

        if (BAdapter == null || !BAdapter.isEnabled()) {
            btScan.setEnabled(false);
            enableBluetooth();
        } else {
            if (NEXUS6) {
                BAdapter.setName(DEVICE_NEXUS);
            } else {
                BAdapter.setName(DEVICE_OTHER);
            }
            initGattServer();
            setup_slave_mode();

        }

    }


    @Override
    public void onResume() {
        super.onResume();
        mode = MODE_DEVICE;
        btScan.setText("Scan");
        tvAddr.setText("");
        tvStatus1.setText("");
        tvStatus2.setText("");
        lvScan.setAdapter(dAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getScanning()) {
            scanning = false;
            unregisterReceiver(mReceiver);
            BAdapter.cancelDiscovery();
            mDeviceList.clear();
            dAdapter.clear();
            btScan.setText("Scan");
        } else if (getAdvertising()) {
            advtising = false;
            bleAd.stopAdvertising(mAdvertiseCallback);
        }
    }


    private void initGattServer() {

        //Service for test read write notify indicate master & slave.......etc.
        BluetoothGattService mGattService;
        BluetoothGattCharacteristic mCharacteristic1, mCharacteristic2, mCharacteristic3;
        BluetoothGattDescriptor des1, des2;
        mGattServer = Bm.openGattServer(this, mBTGattServerCallBack);
        mGattService = new BluetoothGattService(CUSTOM_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        mCharacteristic1 = new BluetoothGattCharacteristic(C1_UUID, BluetoothGattCharacteristic
                .PROPERTY_WRITE, BluetoothGattCharacteristic
                .PERMISSION_WRITE | BluetoothGattDescriptor.PERMISSION_READ);
        mCharacteristic2 = new BluetoothGattCharacteristic(C2_UUID, BluetoothGattCharacteristic
                .PROPERTY_READ, BluetoothGattCharacteristic
                .PERMISSION_READ);
        mCharacteristic3 = new BluetoothGattCharacteristic(C3_UUID, BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        des1 = new BluetoothGattDescriptor(D1_UUID, BluetoothGattDescriptor.PERMISSION_READ);
        des1.setValue(Util.hexStringToByteArray("00abde"));
        des2 = new BluetoothGattDescriptor(D2_UUID, BluetoothGattDescriptor.PERMISSION_WRITE);

        mCharacteristic2.addDescriptor(des1);
        mCharacteristic2.addDescriptor(des2);
        mGattService.addCharacteristic(mCharacteristic1);
        mGattService.addCharacteristic(mCharacteristic2);
        mGattService.addCharacteristic(mCharacteristic3);


        //Service for P2P media transfer
        BluetoothGattService mGattP2PService;
        mGattP2PService = new BluetoothGattService(Const.UNIQ_UUID, BluetoothGattService
                .SERVICE_TYPE_PRIMARY);

        mGattServer.addService(mGattService);
        mGattServer.addService(mGattP2PService);
    }


    private void setup_slave_mode() {
        bleAd = BAdapter.getBluetoothLeAdvertiser();
        dataBuilder = new AdvertiseData.Builder();
        settingsBuilder = new AdvertiseSettings.Builder();
        dataBuilder.setIncludeTxPowerLevel(true);
        dataBuilder.setIncludeDeviceName(true);
        dataBuilder.addServiceUuid(PUUID);
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        settingsBuilder.setTimeout(100000);
        settingsBuilder.setConnectable(true);
    }

    private void start_Advertise() {
        if (NEXUS6) {
            bleAd.startAdvertising(settingsBuilder.build(), dataBuilder.build(), mAdvertiseCallback);
            if (bleAd != null && BAdapter != null) {
                tvAddr.setText("NAME = " + BAdapter.getName() + "\nAddress = " + BAdapter.getAddress
                        () + "\nUUID = " + CUSTOM_SERVICE_UUID.toString());

            }
        }
    }


    private void stop_Advertise() {
        if (NEXUS6) {
            bleAd.stopAdvertising(mAdvertiseCallback);
            btScan.setText("Advertise");
            advtising = false;
        }
    }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "Peripheral Advertise Started.");
            advtising = true;
            tvStatus1.setText("Advertising......");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.w(TAG, "Peripheral Advertise Failed: " + errorCode);
            advtising = false;
            tvStatus1.setText("Advertise Fail");
        }
    };

    public boolean getScanning() {
        return scanning;
    }

    public boolean getAdvertising() {
        return advtising;
    }


    //Check if bluetooth is enabled, if not, then request enable
    private void enableBluetooth() {

        if (BAdapter == null) {
            tvAddr.setText("Bluetooth NOT supported");
        } else if (!BAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_BLUETOOTH_ENABLE);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == REQUEST_BLUETOOTH_ENABLE) {
            btScan.setEnabled(true);
        }

    }


    private void stopBTScan() {
        scanning = false;
        unregisterReceiver(mReceiver);
        BAdapter.cancelDiscovery();
        mDeviceList.clear();
        dAdapter.clear();
    }

    private void startBTScan() {
        scanning = true;
        lvScan.setAdapter(dAdapter);
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
        BAdapter.startDiscovery();
    }


    BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered STATUS = " + status);

            final List<BluetoothGattService> mSer = gatt.getServices();

            for (int i = 0; i < mSer.size(); i++) {
                Log.d(TAG, mSer.get(i).getUuid().toString());
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mode = MODE_SERVICE;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btHome.setText("Home");
                        sAdapter.setServiceList(mSer);
                        lvScan.setAdapter(sAdapter);
                    }
                });

            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }


        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.");
                Log.d(TAG, gatt.getDevice().toString());
                mgatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");
                curdevice = null;
            }
        }


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.d(TAG, "GATT onCharacteristicRead");
            final BluetoothGattCharacteristic cc = characteristic;
            String Output = Util.byteArrayToHex(cc.getValue());
            if (characteristic.getService().getUuid().toString().contains("180f")) {
                Log.d(TAG, "Read = " + Util.byteArrayToHex(cc.getValue()));
                int a = Util.hex2decimal(Util.byteArrayToHex(cc.getValue()));
                Output = String.valueOf(a);
            }
            final String finalOutput = Output;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvAddr.setText(finalOutput);
                }
            });

            if (status == BluetoothGatt.GATT_SUCCESS) {

            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "GATT onCharacteristicChanged");
        }


    };


    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "FOUND DEVICE" + device.getName());
                // Add the name and address to an array adapter to show in a ListView
                if (!mDeviceList.contains(device)) {
                    mDeviceList.add(device);
                    dAdapter.setDeviceList(mDeviceList);
                    Log.d(TAG, "mDeviceList Size = " + dAdapter.getCount());
                    dAdapter.notifyDataSetChanged();
                }
            }

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                Log.d(TAG, "onRecieve HERE");
            }
        }
    };


    BluetoothGattServerCallback mBTGattServerCallBack = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            Log.d(TAG, "onConnectionStateChange status=" + status + "->" + newState);
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.d(TAG, "onCharacteristicReadRequest requestId=" + requestId + " offset=" +
                    offset + "Charatersitic UUID = " + characteristic.getUuid().toString());

            if (characteristic.getUuid().equals(UUID.fromString(C2_UUID.toString()))) {
                characteristic.setValue("Text:This is a test characteristic");
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                        offset,
                        characteristic.getValue());
            }

        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            Log.d(TAG, "onCharacteristicWriteRequest requestId=" + requestId + " preparedWrite="
                    + Boolean.toString(preparedWrite) + " responseNeeded="
                    + Boolean.toString(responseNeeded) + " offset=" + offset);
        }

    };


    @Override
    public void onClick(View v) {
        int view = v.getId();
        switch (view) {
            case R.id.btHome:
                if (mode == MODE_DEVICE) {
                    reset();
                    AcceptData acceptData = new AcceptData();
                    acceptData.start();
//                    Bitmap image = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
//                    if (image != null) {
//                        ivReceive.setImageBitmap(image);
//                        ivReceive.setVisibility(View.VISIBLE);
//                    }

                } else if (mode == MODE_SERVICE) {
                    //back to init
                    reset();
                } else if (mode == MODE_CHARACTERISTIC) {
                    //back to init
                    reset();
                }
                break;
            case R.id.btScan:
                if (getAdvertising()) {
                    Log.d(TAG, "Stop Scan");
                    stop_Advertise();
                } else {
                    Log.d(TAG, "Start Scan");
                    start_Advertise();
                }

                if (getScanning()) {
                    stopBTScan();
                    btScan.setText("Scan");
                } else {
                    startBTScan();
                    btScan.setText("Stop");
                }
                break;
        }
    }

    private void reset() {
        btHome.setText("Receive");
        lvScan.setAdapter(dAdapter);
        tvStatus1.setText("");
        tvAddr.setText("");
        btScan.setText("Scan");
        if (getAdvertising()) {
            stop_Advertise();
        }
        if (getScanning()) {
            stopBTScan();
        }
        mode = MODE_DEVICE;
    }

    private AdapterView.OnItemClickListener onDeviceSelectListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            final int posi = position;

            switch (mode) {
                case MODE_DEVICE:
                    curdevice = BAdapter.getRemoteDevice(dAdapter.getItem(position).getAddress());
                    Log.d(TAG, "CLICK ITEM = " + curdevice.getName());
                    mgatt = curdevice.connectGatt(MainActivity.this, false, gattCallback);
                    stopBTScan();
                    break;
                case MODE_SERVICE:
                    if (curdevice == null) {
                        reset();
                    }
                    final BluetoothDevice device = curdevice;
                    final BluetoothGattService service = sAdapter.getItem(posi);
                    if (service.getUuid().equals(Const.UNIQ_UUID)) {
                        Intent intent = new Intent();
                        intent.setClass(MainActivity.this, P2PActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putString("MAC", device.getAddress());
                        intent.putExtras(bundle);
                        startActivity(intent);
                        finish();
                    } else {
                        Log.d(TAG, "CLICK SERVICE = " + service.toString());
                        List<BluetoothGattCharacteristic> mCha = service.getCharacteristics();
                        cAdapter.setCharacteristicList(mCha);
                        lvScan.setAdapter(cAdapter);
                        mode = MODE_CHARACTERISTIC;
                    }
                    break;
                case MODE_CHARACTERISTIC:
                    Log.d(TAG, "CLICK CHARACTERISTIC = " + cAdapter.getItem(position).getUuid().toString());
                    BluetoothGattCharacteristic mCharater = cAdapter.getItem(position);
                    if (cAdapter.getmAbilityList().get(position).isReadable()) {
                        mgatt.readCharacteristic(mCharater);
                    } else if (cAdapter.getmAbilityList().get(position).isWritable()) {
                        if ((mCharater.getProperties() & BluetoothGattCharacteristic
                                .PROPERTY_WRITE) != 0) {
                            Log.d(TAG, "is Writable");
                            mCharater.setValue("Hello");
                            boolean success = mgatt.writeCharacteristic(mCharater);
                            if (success) {
                                Log.d(TAG, "Write Success");
                            }
                        }
                    }
                    break;
            }
        }
    };


    class AcceptData extends Thread{
        private final BluetoothServerSocket mmServerSocket;
        private BluetoothSocket socket = null;
        private InputStream mmInStream;
        private String device;
        public AcceptData() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = BAdapter.listenUsingRfcommWithServiceRecord("Bluetooth", Const.UNIQ_UUID);
            } catch (IOException e) {
            }
            Log.d(TAG,"listenUsingRfcommWithServiceRecord");
            mmServerSocket = tmp;
            try {
                socket = mmServerSocket.accept();
            } catch (IOException e) {
            }
            Log.d(TAG,"accept");
            device = socket.getRemoteDevice().getName();
            Toast.makeText(getBaseContext(), "Connected to " + device, Toast.LENGTH_SHORT).show();
            InputStream tmpIn = null;
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
            }
            mmInStream = tmpIn;
            int byteNo;
            try {
                byteNo = mmInStream.read(buffer);
                if (byteNo != -1) {
                    //ensure DATAMAXSIZE Byte is read.
                    int byteNo2 = byteNo;
                    int bufferSize = 7340;
                    while(byteNo2 != bufferSize){
                        bufferSize = bufferSize - byteNo2;
                        byteNo2 = mmInStream.read(buffer,byteNo,bufferSize);
                        if(byteNo2 == -1){
                            break;
                        }
                        byteNo = byteNo+byteNo2;
                    }
                }
                if (socket != null) {
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                    }
                }
            }
            catch (Exception e) {
            }
        }
    }


}

