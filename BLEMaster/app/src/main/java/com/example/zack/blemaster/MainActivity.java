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



public class MainActivity extends Activity implements View.OnClickListener{

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
    private TextView tvAddr,tvStatus1,tvStatus2;
    private BluetoothManager Bm;
    private Button btScan,btHome;
    private ListView lvScan;
    private boolean scanning;
    private boolean advtising;
    private BluetoothGatt mgatt;

    private DeviceAdapter dAdapter;
    private ServiceAdapter sAdapter;
    private CharacteristicAdapter cAdapter;

    private List<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();
    private BluetoothGattServer mGattServer;
    private BluetoothGattService mGattService;
    private AdvertiseData.Builder dataBuilder;
    private AdvertiseSettings.Builder settingsBuilder;
    private int mode;
    private static final int MODE_DEVICE = 0;
    private static final int MODE_SERVICE = 1;
    private static final int MODE_CHARACTERISTIC = 2;




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
        tvStatus1 = (TextView)findViewById(R.id.tvStatus1);
        tvStatus2 = (TextView)findViewById(R.id.tvStatus2);
        btScan = (Button)findViewById(R.id.btScan);
        btHome = (Button)findViewById(R.id.btHome);
        lvScan = (ListView)findViewById(R.id.lvScan);
        dAdapter = new DeviceAdapter(this);
        sAdapter = new ServiceAdapter(this);
        cAdapter = new CharacteristicAdapter(this);
        btScan.setOnClickListener(this);
        btHome.setOnClickListener(this);
        lvScan.setOnItemClickListener(onDeviceSelectListener);
        mode = MODE_DEVICE;

        Bm = (BluetoothManager) getSystemService(this.BLUETOOTH_SERVICE);
        BAdapter = Bm.getAdapter();

        if(BAdapter ==null||!BAdapter.isEnabled()){
            enableBluetooth();
        }else{
            BAdapter.setName("Zack");
            initGattServer();
            setup_slave_mode();
        }

    }


    @Override
    public void onResume(){
        super.onResume();
        mode = MODE_DEVICE;
        btScan.setText("Scan");
        tvAddr.setText("");
        tvStatus1.setText("");
        tvStatus2.setText("");
        lvScan.setAdapter(dAdapter);
    }

    @Override
    public void onPause(){
        super.onPause();
        if(getScanning()) {
            scanning = false;
            unregisterReceiver(mReceiver);
            BAdapter.cancelDiscovery();
            mDeviceList.clear();
            dAdapter.clear();
        }else if(getAdvertising()){
            advtising = false;
            bleAd.stopAdvertising(mAdvertiseCallback);
        }
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
        des1.setValue(Util.hexStringToByteArray("00abde"));
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

    private void start_Advertise(){
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
            tvStatus1.setText("Advertising......");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.w("Zack", "Peripheral Advertise Failed: "+errorCode);
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

        if(BAdapter == null) {
            tvAddr.setText("Bluetooth NOT supported");
        } else if(!BAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
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
        public void onServicesDiscovered (BluetoothGatt gatt, int status){
            Log.d("Zack", "onServicesDiscovered STATUS = " + status);

            final List<BluetoothGattService> mSer = gatt.getServices();

            for(int i = 0;i<mSer.size();i++){
                Log.d("Zack",mSer.get(i).getUuid().toString());
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mode = MODE_SERVICE;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sAdapter.setServiceList(mSer);
                        lvScan.setAdapter(sAdapter);
                    }
                });

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
            String Output = Util.byteArrayToHex(cc.getValue());
            if(characteristic.getService().getUuid().toString().contains("180f")){
                Log.d("Zack","Read = "+Util.byteArrayToHex(cc.getValue()));
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
                Log.d("Zack","FOUND DEVICE"+device.getName());
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




    @Override
    public void onClick(View v) {
        int view = v.getId();
        switch (view){
            case R.id.btHome:
                if(mode == MODE_DEVICE){
                    //do nothing
                }else if(mode == MODE_SERVICE){
                   //back to init
                    lvScan.setAdapter(dAdapter);
                    tvStatus1.setText("");
                    tvAddr.setText("");
                    btScan.setText("Scan");
                    if(getAdvertising()){
                        stop_Advertise();
                    }
                    if(getScanning()){
                        stopBTScan();
                    }
                    mode = MODE_DEVICE;
                }else if(mode == MODE_CHARACTERISTIC){
                    //back to init
                    lvScan.setAdapter(dAdapter);
                    tvStatus1.setText("");
                    tvAddr.setText("");
                    btScan.setText("Scan");
                    if(getAdvertising()){
                        stop_Advertise();
                    }
                    if(getScanning()){
                        stopBTScan();
                    }
                    mode = MODE_DEVICE;
                }
                break;
            case R.id.btScan:
                if(getAdvertising()){
                    Log.d("Zack", "Stop Scan");
                    stop_Advertise();
                }else{
                    Log.d("Zack", "Start Scan");
                    start_Advertise();
                }

                if(getScanning()){
                    stopBTScan();
                    btScan.setText("Scan");
                }else {
                    startBTScan();
                    btScan.setText("Stop");
                }
                break;
        }
    }

    private AdapterView.OnItemClickListener onDeviceSelectListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


            switch (mode){
                case MODE_DEVICE:
                    final BluetoothDevice device = BAdapter.getRemoteDevice(dAdapter.getItem(position).getAddress());
                    Log.d("Zack", "CLICK ITEM = " + device.getName());
                    mgatt = device.connectGatt(MainActivity.this, false, gattCallback);
                    stopBTScan();
                    break;
                case MODE_SERVICE:
                    final BluetoothGattService service = sAdapter.getItem(position);
                    Log.d("Zack","CLICK SERVICE = "+service.toString());
                    List<BluetoothGattCharacteristic> mCha = service.getCharacteristics();
                    cAdapter.setCharacteristicList(mCha);
                    lvScan.setAdapter(cAdapter);
                    mode = MODE_CHARACTERISTIC;
                    break;
                case MODE_CHARACTERISTIC:
                    Log.d("Zack","CLICK CHARACTERISTIC = "+cAdapter.getItem(position).getUuid().toString());
                    BluetoothGattCharacteristic mCharater = cAdapter.getItem(position);
                    if(cAdapter.isCanRead()){
                        Log.d("Zack","Read");
                        mgatt.readCharacteristic(mCharater);
                    }else if(cAdapter.isCanWrite()){
                        Log.d("Zack","Write");
                        mgatt.writeCharacteristic(mCharater);
                    }
                    break;
            }
        }
    };


}

