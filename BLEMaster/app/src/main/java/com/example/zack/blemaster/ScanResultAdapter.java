package com.example.zack.blemaster;

import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.ParcelUuid;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Zack on 15/7/31.
 */

public class ScanResultAdapter extends BaseAdapter {

    private List<ScanResult> mDeviceList = new ArrayList<ScanResult>();
    private Context context;
    private ScanResult mDevice;

    public ScanResultAdapter(Context context) {
        this.context = context;
    }

    public void setDeviceList(List<ScanResult> DeviceList){
        mDeviceList = DeviceList;
    }


    @Override
    public int getCount() {
        return mDeviceList.size();
    }

    @Override
    public ScanResult getItem(int position) {
        return mDeviceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
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

        mDevice = mDeviceList.get(position);

        viewHolder.tvTitle.setText(mDevice.getDevice().getName());
        viewHolder.tvMac.setText(mDevice.getDevice().getAddress());
        ScanRecord scanRecord = mDevice.getScanRecord();
        List<ParcelUuid> uuids = scanRecord.getServiceUuids();
        if(uuids != null) {
            for(int j = 0; j < uuids.size(); j++) {
                ServiceUUID +=  uuids.get(j).toString() + "\n";
            }
        }
        viewHolder.tvContent_title.setText(ServiceUUID);

        return convertView;
    }

    private class ViewHolder {
        TextView tvContent_title,tvTitle,tvMac;

    }
}
