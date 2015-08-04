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
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.ScanResult;
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
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;



public class MainActivity extends Activity {

    private BluetoothLeAdvertiser bleAd;
    private BluetoothAdapter BAdapter;
    private static final UUID MY_UUID = UUID.randomUUID();
    private static final UUID C1_UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");
    private static final UUID C2_UUID = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb");
    private static final UUID C3_UUID = UUID.fromString("00001243-0000-1000-8000-00805f9b34fb");
    private static final UUID D1_UUID = UUID.fromString("00001111-0000-1000-8000-00805f9b34fb");
    private static final UUID D2_UUID = UUID.fromString("00001112-0000-1000-8000-00805f9b34fb");
    private static final UUID S1_UUID = UUID.fromString("00001811-0000-1000-8000-00805f9b34fb");
    private static final ParcelUuid PUUID = new ParcelUuid(MY_UUID);
    private TextView tvAddr;
    private BluetoothManager Bm;
    private Button btScan;
    private ListView lvScan;
    private boolean scanning;
    private boolean advtising;
    private BluetoothGatt mgatt;

    private ScanResultAdapter mAdapter;
    private DeviceAdapter dAdapter;

    private List<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();
    private BluetoothGattServer mGattServer;
    private BluetoothGattService mGattService;


    private List<ScanResult> results;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

    private BluetoothGattCharacteristic mCharacteristic;


    private final String LIST_UUID = "UUID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvAddr = (TextView)findViewById(R.id.tvAddr);
        btScan = (Button)findViewById(R.id.btScan);
        lvScan = (ListView)findViewById(R.id.lvScan);
        mAdapter = new ScanResultAdapter(this);
        dAdapter = new DeviceAdapter(this);
        lvScan.setAdapter(mAdapter);
        btScan.setOnClickListener(onScanListener);
        lvScan.setOnItemClickListener(onDeviceSelectListener);


        Bm = (BluetoothManager) getSystemService(this.BLUETOOTH_SERVICE);
        BAdapter = Bm.getAdapter();

        initGattServer();



        if(BAdapter ==null||!BAdapter.isEnabled()){
            enableBluetooth();
        }else{
            BAdapter.setName("Zack");
        }
        scanning = false;
        advtising = false;

    }

    private void initGattServer() {
        BluetoothGattCharacteristic mCharacteristic1, mCharacteristic2, mCharacteristic3;
        BluetoothGattDescriptor des1,des2;
        mGattServer = Bm.openGattServer(this, mBTGattServerCallBack);
        mGattService = new BluetoothGattService(MY_UUID,BluetoothGattService.SERVICE_TYPE_PRIMARY);
        mCharacteristic1 = new BluetoothGattCharacteristic(C1_UUID,BluetoothGattCharacteristic
                .PROPERTY_WRITE,BluetoothGattCharacteristic
                .PERMISSION_WRITE);
        mCharacteristic2 = new BluetoothGattCharacteristic(C2_UUID,BluetoothGattCharacteristic
                .PROPERTY_READ,BluetoothGattCharacteristic
                .PERMISSION_READ);
        mCharacteristic3 = new BluetoothGattCharacteristic(C3_UUID,BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);

        des1 = new BluetoothGattDescriptor(D1_UUID,BluetoothGattDescriptor.PERMISSION_READ);
        des1.setValue(hexStringToByteArray("00abde"));
        des2 = new BluetoothGattDescriptor(D2_UUID,BluetoothGattDescriptor.PERMISSION_WRITE);

        mCharacteristic2.addDescriptor(des1);
        mCharacteristic2.addDescriptor(des2);
        mGattService.addCharacteristic(mCharacteristic1);
        mGattService.addCharacteristic(mCharacteristic2);
        mGattService.addCharacteristic(mCharacteristic3);
        mGattServer.addService(mGattService);
    }



    private void setup_slave_mode() {
        bleAd = BAdapter.getBluetoothLeAdvertiser();
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        dataBuilder.setIncludeTxPowerLevel(true);
        dataBuilder.setIncludeDeviceName(true);
        dataBuilder.addServiceUuid(PUUID);
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        settingsBuilder.setTimeout(100000);
        settingsBuilder.setConnectable(true);
        bleAd.startAdvertising(settingsBuilder.build(), dataBuilder.build(), mAdvertiseCallback);
        if(bleAd!=null&&BAdapter!=null) {
            tvAddr.setText("NAME = " + BAdapter.getName() + "\nAddress = " + BAdapter.getAddress
                    ()+"\nUUID = "+MY_UUID.toString());
        }
    }

    private void stop_Advertise(){
        bleAd.stopAdvertising(mAdvertiseCallback);
        btScan.setText("Advertise");
        advtising = false;
    }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i("Zack", "Peripheral Advertise Started.");
            advtising = true;
            btScan.setText("Advertising......");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.w("Zack", "Peripheral Advertise Failed: "+errorCode);
            advtising = false;
            btScan.setText("Advertise");
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

        if(BAdapter == null) {
            tvAddr.setText("Bluetooth NOT supported");
        } else if(!BAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
    }




    private View.OnClickListener onScanListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(getAdvertising()){
                Log.d("Zack","Stop Scan");
                stop_Advertise();
                btScan.setText("Advertise");
            }else{
                Log.d("Zack","Start Scan");
                setup_slave_mode();
                btScan.setText("Stop");
            }

            if(getScanning()){
//                stopBleScan();
                stopBTScan();
                btScan.setText("Scan");
            }else {
//                startBleScan();
                startBTScan();
                btScan.setText("Stop");
            }
        }
    };

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

    private AdapterView.OnItemClickListener onDeviceSelectListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            final BluetoothDevice device = BAdapter.getRemoteDevice(dAdapter.getItem(position).getAddress());
            Log.d("Zack","CLICK ITEM = "+device.getName());
            mgatt = device.connectGatt(MainActivity.this,false,gattCallback);
            stopBTScan();
        }
    };


    BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onServicesDiscovered (BluetoothGatt gatt, int status){
            Log.d("Zack", "onServicesDiscovered STATUS = " + status);
            List<BluetoothGattService> mSer = gatt.getServices();

            for(int i = 0;i<mSer.size();i++){
                Log.d("Zack",mSer.get(i).getUuid().toString());
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService Service = mgatt.getService(UUID.fromString
                        ("00001811-0000-1000-8000-00805f9b34fb"));
                if (Service == null) {
                    Log.e("Zack", "service not found!");
                    return;
                }

                List<BluetoothGattCharacteristic> mCha = Service.getCharacteristics();


                for(int i = 0;i<mCha.size();i++){
                    Log.d("Zack","CHA = "+mCha.get(i).getUuid().toString());
                }
                BluetoothGattCharacteristic characR = Service
                        .getCharacteristic(UUID.fromString("00002a44-0000-1000-8000-00805f9b34fb"));
                if (characR == null) {
                    Log.e("Zack", "characteristic not found!");
                    return;
                }
                characR.setValue(hexStringToByteArray("24"));
                boolean statsus = mgatt.writeCharacteristic(characR);
                Log.d("Zack","STA"+statsus);

                BluetoothGattCharacteristic characL = Service
                        .getCharacteristic(UUID.fromString("00002a47-0000-1000-8000-00805f9b34fb"));
                mgatt.setCharacteristicNotification(characL, true);
                if (characL == null) {
                    Log.e("Zack", "characteristic not found!");
                    return;
                }
                boolean Rs =mgatt.readCharacteristic(characL);
                Log.d("Zack","STA"+Rs);



            } else {
                Log.w("Zack", "onServicesDiscovered received: " + status);
            }
        }


        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("Zack", "Connected to GATT server.");
                Log.d("Zack", gatt.getDevice().toString());
                mgatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("Zack", "Disconnected from GATT server.");
            }
        }


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.d("Zack", "GATT onCharacteristicRead");
            final BluetoothGattCharacteristic cc = characteristic;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvAddr.setText(byteArrayToHex(cc.getValue()));
                }
            });

            if (status == BluetoothGatt.GATT_SUCCESS) {

            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.d("Zack","GATT onCharacteristicChanged");
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
                // Add the name and address to an array adapter to show in a ListView
                if(!mDeviceList.contains(device)) {
                    mDeviceList.add(device);
                    dAdapter.setDeviceList(mDeviceList);
                    Log.d("Zack", "mDeviceList Size = " + dAdapter.getCount());
                    dAdapter.notifyDataSetChanged();

                }
            }
        }
    };


    BluetoothGattServerCallback mBTGattServerCallBack = new BluetoothGattServerCallback(){
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            Log.d("Zack", "onConnectionStateChange status=" + status + "->" + newState);
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.d("Zack", "onCharacteristicReadRequest requestId=" + requestId + " offset=" + offset);

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
            Log.d("Zack", "onCharacteristicWriteRequest requestId=" + requestId + " preparedWrite="
                    + Boolean.toString(preparedWrite) + " responseNeeded="
                    + Boolean.toString(responseNeeded) + " offset=" + offset);
        }

    };


    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len/2];

        for(int i = 0; i < len; i+=2){
            data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }

        return data;
    }
    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }
}

