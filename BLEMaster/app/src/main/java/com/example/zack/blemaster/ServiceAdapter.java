package com.example.zack.blemaster;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
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
public class ServiceAdapter extends BaseAdapter {
    private List<BluetoothGattService> mServiceList = new ArrayList<BluetoothGattService>();
    private Context context;
    private BluetoothGattService mService;

    public ServiceAdapter(Context context) {
        this.context = context;
    }

    public void setServiceList(List<BluetoothGattService> DeviceList){
        mServiceList = DeviceList;
    }


    @Override
    public int getCount() {
        return mServiceList.size();
    }

    @Override
    public BluetoothGattService getItem(int position) {
        return mServiceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }
    public void clear(){
        mServiceList.clear();
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

        mService = mServiceList.get(position);
        List<BluetoothGattCharacteristic> mCha = mService.getCharacteristics();
        StringBuffer SB = new StringBuffer();

        for(int i = 0;i<mCha.size();i++){
            SB.append(mCha.get(i).getUuid().toString()+"\n");
        }

        viewHolder.tvTitle.setText(mService.getUuid().toString());
        viewHolder.tvMac.setText(SB.toString());

        return convertView;
    }

    private class ViewHolder {
        TextView tvContent_title,tvTitle,tvMac;

    }
}
