package com.example.zack.blemaster;

import android.os.ParcelUuid;

import java.util.UUID;

/**
 * Created by Zack on 15/8/6.
 */
public class Const {
    public static final UUID CUSTOM_SERVICE_UUID = UUID.randomUUID();
    public static final UUID C1_UUID = UUID.fromString("2222180F-0000-1000-8000-00805f9b34fb");
    public static final UUID C2_UUID = UUID.fromString("22221805-0000-1000-8000-00805f9b34fb");
    public static final UUID C3_UUID = UUID.fromString("22221243-0000-1000-8000-00805f9b34fb");
    public static final UUID D1_UUID = UUID.fromString("00001111-0000-1000-8000-00805f9b34fb");
    public static final UUID D2_UUID = UUID.fromString("00001112-0000-1000-8000-00805f9b34fb");
    public static final UUID S1_UUID = UUID.fromString("00001811-0000-1000-8000-00805f9b34fb");
    public static final UUID UNIQ_UUID = UUID.fromString("835a7e00-3bea-11e5-8ac6-0002a5d5c51b");
    public static final ParcelUuid PUUID = new ParcelUuid(CUSTOM_SERVICE_UUID);
}
