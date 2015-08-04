package com.example.zack.bleslave;

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
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
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
import java.util.List;
import java.util.UUID;



public class MainActivity extends Activity {

    private static final String TAG = "Zack";

    private BluetoothLeAdvertiser bleAd;
    private BluetoothAdapter BAdapter;
//    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
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
    private boolean advtising;

    private BluetoothGattServer mGattServer;
    private BluetoothGattService mGattService;

    private boolean scanning;
    private ListView lvScan;
    private ScanResultAdapter mAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private ArrayAdapter<String> btArrayAdapter;
    private ArrayList<String> foundDevices;
    private BluetoothGatt mgatt;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvAddr = (TextView)findViewById(R.id.tvAddr);
        btScan = (Button)findViewById(R.id.btScan);
        btScan.setOnClickListener(onAdvertiseListener);
        Bm = (BluetoothManager) getSystemService(this.BLUETOOTH_SERVICE);
        BAdapter = Bm.getAdapter();

        //scanner
        lvScan = (ListView)findViewById(R.id.lvScan);
        lvScan.setOnItemClickListener(onDeviceSelectListener);
        mAdapter = new ScanResultAdapter(this);
        btArrayAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1);
        foundDevices = new ArrayList<String>();

        if(BAdapter ==null||!BAdapter.isEnabled()){
            enableBluetooth();
        }else{
            BAdapter.setName("ZACK");
            initGattServer();
        }
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

    public boolean getScanning() {
        return scanning;
    }
    /**
     * BLE Scanning
     */
    public void startBleScan() {
        bluetoothLeScanner = BAdapter.getBluetoothLeScanner();
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
    }

    public void stopBleScan() {
        if(!getScanning()) return;
        scanning = false;
        bluetoothLeScanner.stopScan(scanCallback);
        Log.d("Zack", "Scanning has been stopped");
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



    BluetoothGattServerCallback mBTGattServerCallBack = new BluetoothGattServerCallback(){


        @Override
        public void onConnectionStateChange (BluetoothDevice device, int status, int newState){
            super.onConnectionStateChange(device, status, newState);
            Log.d(TAG,"onConnectionStateChange "+status+"->"+newState);

        }



        @Override
        public void onServiceAdded (int status, BluetoothGattService service){
            super.onServiceAdded(status,service);
            UUID srvcUuid = service.getUuid();
            Log.d(TAG, "onServiceAdded() - service=" + srvcUuid
                    + "status=" + status);

        }

        @Override
        public void onDescriptorReadRequest (BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            Log.d(TAG, "onDescriptorReadRequest() - "
                    + "service=" + descriptor.getCharacteristic().getService().getUuid() + ", " +
                    "characteristic=" +
                    descriptor
                            .getCharacteristic
                                    ().getUuid());
            Log.d(TAG,"descriptor UUID = "+descriptor.getUuid());
//            if(descriptor.getUuid().equals(D1_UUID)){
                descriptor.setValue("ABCD".getBytes());
                mGattServer.sendResponse(device,requestId,offset,BluetoothGatt.GATT_SUCCESS,
                        hexStringToByteArray("000AD23"));
//            }


        }


        @Override
        public void onDescriptorWriteRequest (BluetoothDevice device, int requestId,
                                              BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value){
            super.onDescriptorWriteRequest(device,requestId,descriptor,preparedWrite,
                    responseNeeded,offset,value);
            UUID descrUuid = descriptor.getUuid();
            Log.d(TAG, "onDescriptorWriteRequest() - "
                    +"descriptor=" + descrUuid.toString());
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int transId,
                                   boolean execWrite) {
            super.onExecuteWrite(device,transId,execWrite);
            Log.d(TAG, "onExecuteWrite() - "
                    + "device=" + device.toString() + ", transId=" + transId
                    + "execWrite=" + execWrite);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device,status);
            Log.d(TAG, "onNotificationSent() - "
                    + "device=" + device.toString() + ", status=" + status);
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
            Log.d(TAG, "onMtuChanged() - "
                    + "device=" + device.toString() + ", mtu=" + mtu);
        }
        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.d("Zack", "onCharacteristicReadRequest device = "+ device.toString()+"requestId=" +
                    requestId + " " +
                    "offset=" +
                    offset);
            Log.d(TAG,"characteristic UUID= "+characteristic.getUuid().toString());

            if (characteristic.getUuid().equals(UUID.fromString(C2_UUID.toString()))) {
                Log.d("Zack", "REQUEST CONFIRM");
                characteristic.setValue("Text:This is a test");
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
                    + Boolean.toString(responseNeeded) + " offset=" + offset+"value = " +
                    ""+byteArrayToHex(value));
        }

    };


    private View.OnClickListener onAdvertiseListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(getAdvertising()){
                stop_Advertise();
                btScan.setText("Advertise");
            }else{
                setup_slave_mode();
                btScan.setText("Stop");
            }

            if(getScanning()){
                stopBleScan();
            }else {
                startBleScan();
            }
        }
    };

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len/2];

        for(int i = 0; i < len; i+=2){
            data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }

        return data;
    }


    private AdapterView.OnItemClickListener onDeviceSelectListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            final BluetoothDevice device = BAdapter.getRemoteDevice("58:DC:A3:3B:47:83");

            if(device==null){
                Log.d(TAG,"NULL DEVICE");
            }
            mgatt = device.connectGatt(MainActivity.this,false,gattCallback);
            if (mgatt == null) {
                Log.e(TAG, "lost connection");
                return;
            }

            BluetoothGattService Service = mgatt.getService(S1_UUID);
            if (Service == null) {
                Log.e(TAG, "service not found!");
                return;
            }
            BluetoothGattCharacteristic charac = Service
                    .getCharacteristic(S1_UUID);
            if (charac == null) {
                Log.e(TAG, "char not found!");
                return;
            }
            charac.setValue(hexStringToByteArray("SENT MESSAGE TEST"));
            boolean status = mgatt.writeCharacteristic(charac);
            Log.d(TAG,"STA"+status);
        }
    };

    BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onServicesDiscovered (BluetoothGatt gatt, int status){
            Log.d("Zack", "onServicesDiscovered STATUS = " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mgatt.discoverServices();
            } else {
                Log.w("Zack", "onServicesDiscovered received: " + status);
            }
        }


        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG,"GATTonConnectionStateChange = "+status+"=>"+newState);


            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("Zack", "Connected to GATT server.");
                Log.d("Zack",gatt.getDevice().toString());
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
                            Log.d(TAG,uuids.get(j).toString());
                            deviceInfo += "\n" + uuids.get(j).toString();
                        }
                    }
                }



            }else{
//                Log.d("Zack", "Still Scaning");
                btScan.setText("Scanning.....");
            }
        }
    };

}

