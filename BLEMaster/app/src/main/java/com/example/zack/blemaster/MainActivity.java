package com.example.zack.blemaster;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;



public class MainActivity extends Activity {

    private BluetoothLeAdvertiser bleAd;
    private BluetoothAdapter BAdapter;
    private AdvertiseData mAdvertiseData;
    private AdvertiseSettings mAdvertiseSettings;
//    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final UUID MY_UUID = UUID.randomUUID();
    private static final ParcelUuid PUUID = new ParcelUuid(MY_UUID);
    private TextView tvAddr;
    private BluetoothGattServer gattserver;
    private BluetoothManager Bm;
    private Button btScan;
    private ListView lvScan;
    private boolean scanning;
    private boolean advtising;
    private BluetoothGatt mgatt;

    private BluetoothLeScanner bluetoothLeScanner;
    private ArrayAdapter<String> btArrayAdapter;
    private ArrayList<String> foundDevices;
    private ScanResultAdapter mAdapter;
    private DeviceAdapter dAdapter;
    private List<ScanResult> results;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private List<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();
    private BluetoothGattServer mGattServer;
    private BluetoothGattService mGattService;
    private BluetoothGattCharacteristic mCharacteristic;


    private final String LIST_UUID = "UUID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvAddr = (TextView)findViewById(R.id.tvAddr);
        btScan = (Button)findViewById(R.id.btScan);
        lvScan = (ListView)findViewById(R.id.lvScan);
        btArrayAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1);
        mAdapter = new ScanResultAdapter(this);
        dAdapter = new DeviceAdapter(this);
        lvScan.setAdapter(mAdapter);
        foundDevices = new ArrayList<String>();
        btScan.setOnClickListener(onScanListener);
        lvScan.setOnItemClickListener(onDeviceSelectListener);


        Bm = (BluetoothManager) getSystemService(this.BLUETOOTH_SERVICE);
        BAdapter = Bm.getAdapter();

        initGattServer();



        if(BAdapter ==null||!BAdapter.isEnabled()){
            enableBluetooth();
        }else{
            BAdapter.setName("AAAA");
//            setup_slave_mode();
//            setup_master();
        }
        scanning = false;
        advtising = false;

    }

    private void initGattServer() {
        mGattServer = Bm.openGattServer(this, mBTGattServerCallBack);
        mGattService = new BluetoothGattService(MY_UUID,BluetoothGattService.SERVICE_TYPE_PRIMARY);
        mCharacteristic = new BluetoothGattCharacteristic(MY_UUID,BluetoothGattCharacteristic
                .PERMISSION_WRITE,BluetoothGattCharacteristic.PROPERTY_WRITE);
        mGattService.addCharacteristic(mCharacteristic);
        mGattServer.addService(mGattService);
    }

    private void setup_master() {
        bluetoothLeScanner = BAdapter.getBluetoothLeScanner();

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

    /**
     * BLE Scanning
     */
    public void startBleScan() {
        setup_master();
        if(getScanning()) return;
        enableBluetooth();
        scanning = true;
        ScanFilter.Builder filterBuilder = new ScanFilter.Builder();
        ScanSettings.Builder settingsBuilder = new ScanSettings.Builder();
        settingsBuilder.setReportDelay(1000);
        settingsBuilder.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
        settingsBuilder.build();
        List<ScanFilter> filters = new ArrayList<ScanFilter>();
        filters.add(filterBuilder.build());
        bluetoothLeScanner.startScan(filters, settingsBuilder.build(), scanCallback);
        Log.d("Zack", "Bluetooth is currently scanning...");
        btScan.setOnClickListener(onScanListener);
    }

    public void stopBleScan() {
        if(!getScanning()) return;
        scanning = false;
        bluetoothLeScanner.stopScan(scanCallback);
        Log.d("Zack", "Scanning has been stopped");
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


    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType,ScanResult scanResult) {
            BluetoothDevice device = scanResult.getDevice();
            if(foundDevices.contains(device.getAddress())) return;
            foundDevices.add(device.getAddress());
            String deviceInfo = device.getName() + " - " + device.getAddress();
            Log.d("Zack", "Device: " + deviceInfo + " Scanned!");
            ScanRecord scanRecord = scanResult.getScanRecord();
            List<ParcelUuid> uuids = scanRecord.getServiceUuids();

            if(uuids != null) {
                Log.d("Zack", "UUIDS FOUND FROM DEVICE");
                for(int i = 0; i < uuids.size(); i++) {
                    deviceInfo += "\n" + uuids.get(i).toString();
                }
            }

            final String text = deviceInfo;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btArrayAdapter.add(text);
                    btArrayAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onScanFailed(int i) {
            Log.e("Zack", "Scan attempt failed ERROR Code = "+i);
            btScan.setText("Scan Fail");
        }

        @Override
        public void onBatchScanResults(final List<ScanResult> results) {
            if(results.size()>0) {
                runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            mAdapter.setDeviceList(results);
                            mAdapter.notifyDataSetChanged();
                            lvScan.setAdapter(mAdapter);
                        }
                    });
                List<String> mName = new ArrayList<String>();
                for(int i =0;i<results.size();i++){
                    BluetoothDevice device = results.get(i).getDevice();
                    if(foundDevices.contains(device.getAddress())) return;
                    foundDevices.add(device.getAddress());
                    String deviceInfo = device.getName() + " - " + device.getAddress();
                    Log.d("Zack", "Device: " + deviceInfo + " Scanned!");
                    ScanRecord scanRecord = results.get(i).getScanRecord();
                    List<ParcelUuid> uuids = scanRecord.getServiceUuids();

                    if(uuids != null) {
                        Log.d("Zack", "UUIDS FOUND FROM DEVICE");
                        for(int j = 0; j < uuids.size(); j++) {
                            deviceInfo += "\n" + uuids.get(j).toString();
                        }
                    }

                    final String text = deviceInfo;

                }



            }else{
                Log.d("Zack", "Still Scaning");
                btScan.setText("Scanning.....");
            }
        }
    };

    private View.OnClickListener onScanListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
//            if(getAdvertising()){
//                Log.d("Zack","Stop Scan");
//                stop_Advertise();
//                btScan.setText("Advertise");
//            }else{
//                Log.d("Zack","Start Scan");
//                setup_slave_mode();
//                btScan.setText("Stop");
//            }

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
//            stopBleScan();
//            Log.d("Zack","Click mAdapter.getItem(position).getDevice() = "+ mAdapter.getItem(position).getDevice().getAddress());
//            final BluetoothDevice device = BAdapter.getRemoteDevice(mAdapter.getItem(position).getDevice().getAddress());
            mgatt = device.connectGatt(MainActivity.this,false,gattCallback);
            stopBTScan();
        }
    };


    BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onServicesDiscovered (BluetoothGatt gatt, int status){
            Log.d("Zack", "onServicesDiscovered STATUS = " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                displayGattServices(gatt.getServices());
            } else {
                Log.w("Zack", "onServicesDiscovered received: " + status);
            }
        }


        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("Zack", "Connected to GATT server.");
                Log.d("Zack",gatt.getDevice().toString());
                displayGattServices(gatt.getServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("Zack", "Disconnected from GATT server.");
            }
        }


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
        }
    };


    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        Log.d("Zack","displayGattServices"+gattServices.size());
        String uuid = null;
        String unknownServiceString = "unknown_service";
        String unknownCharaString = "unknown_characteristic";
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            String Datainfo = null;
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);
            Log.d("Zack", "displayGattServices UUID = " + uuid);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();
            Datainfo = "UUID = "+gattService.toString();
            Log.d("Zack","displayGattServices = "+Datainfo);
            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                Datainfo+="\n"+gattCharacteristic.toString();
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            btArrayAdapter.add(Datainfo);
            Log.d("Zack", "displayGattServices = " + Datainfo);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        lvScan.setAdapter(btArrayAdapter);
                    }
                }

        );

    }


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

            if (characteristic.getUuid().equals(UUID.fromString(MY_UUID.toString()))) {
                Log.d("Zack", "SERVICE_UUID_1");
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


}

