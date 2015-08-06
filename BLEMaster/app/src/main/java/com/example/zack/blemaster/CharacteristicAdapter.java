package com.example.zack.blemaster;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Zack on 15/8/4.
 */
public class CharacteristicAdapter extends BaseAdapter {
    private List<BluetoothGattCharacteristic> mCharacteristicList = new ArrayList<>();
    private List<CharacAbility> mAbilityList = new ArrayList<>();

    private CharacAbility mAbi;

    private Context context;
    private BluetoothGattCharacteristic mCharacteristic;


    public CharacteristicAdapter(Context context) {
        this.context = context;
    }

    public void setCharacteristicList(List<BluetoothGattCharacteristic> DeviceList){
        mCharacteristicList = DeviceList;
    }


    @Override
    public int getCount() {
        return mCharacteristicList.size();
    }

    @Override
    public BluetoothGattCharacteristic getItem(int position) {
        return mCharacteristicList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }
    public void clear(){
        mCharacteristicList.clear();
        this.notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        String ServiceUUID = null;
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.device_item, null);
            viewHolder = new ViewHolder();
            viewHolder.tvMac = (TextView) convertView
                    .findViewById(R.id.tvMac);
            viewHolder.tvTitle = (TextView) convertView
                    .findViewById(R.id.tvTitle);
            viewHolder.tvContent_title = (TextView)convertView.findViewById(R.id.tvContent_title);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        mCharacteristic = (BluetoothGattCharacteristic) mCharacteristicList.get(position);
        viewHolder.tvTitle.setText(mCharacteristic.getUuid().toString());
        String Permission = String.valueOf(mCharacteristic.getPermissions());
        StringBuffer Property = new StringBuffer();
        Property.append("Property :");

        String a = Integer.toHexString(mCharacteristic
                .getProperties());

//        Log.d("Zack","Property code = "+a);


        int second_byte = 0;
        int first_byte = Integer.parseInt(String.valueOf(a.charAt(0)));
        if(a.length()>1){
            first_byte  = Integer.parseInt(String.valueOf(a.charAt(1)));
            second_byte = Integer.parseInt(String.valueOf(a.charAt(0)));
        }
        mAbi = new CharacAbility();

//        Log.d("Zack","Property 1st code = "+first_byte);
//        Log.d("Zack","Property 2nd code = "+second_byte);



        switch (first_byte){
            case 1:
                Property.append("BroadCast");
                break;
            case 2:
                Property.append("Read");
                mAbi.setReadable(true);
                mAbi.setWritable(false);
                break;
            case 4:
                Property.append("Write_No_Response");
                break;
            case 8:
                Property.append("Write");
                mAbi.setReadable(false);
                mAbi.setWritable(true);
                break;
        }

        switch (second_byte){
            case 1:
                Property.append(", PROPERTY_NOTIFY");
                break;
            case 2:
                Property.append(", PROPERTY_INDICATE");
                break;
            case 4:
                Property.append(", PROPERTY_SIGNED_WRITE");
                break;
            case 8:
                Property.append(", PROPERTY_EXTENDED_PROPS");
                break;
        }
        mAbilityList.add(position,mAbi);

        viewHolder.tvContent_title.setText(Property.toString());

        return convertView;
    }

    private class ViewHolder {
        TextView tvContent_title,tvTitle,tvMac;

    }

    public List<CharacAbility> getmAbilityList() {
        return mAbilityList;
    }


}
