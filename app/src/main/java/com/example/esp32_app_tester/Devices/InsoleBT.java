package com.example.esp32_app_tester.Devices;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class InsoleBT extends AppCompatActivity {
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private Handler handler;

    public static class ConfigData {
        public byte cmd;
        public byte hora, min, seg, mSeg;
        public byte freq;
        public short S1, S2, S3, S4, S5, S6, S7, S8, S9;
    }

    public static class SendData {
        public byte cmd;
        public byte hourP;
        public byte minute;
        public byte second;
        public byte millisecond;
        public int battery;
        public short length;
        public short[] SR1 = new short[60];
        public short[] SR2 = new short[60];
        public short[] SR3 = new short[60];
        public short[] SR4 = new short[60];
        public short[] SR5 = new short[60];
        public short[] SR6 = new short[60];
        public short[] SR7 = new short[60];
        public short[] SR8 = new short[60];
        public short[] SR9 = new short[60];
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    String data = (String) msg.obj;
                    try {
                        JSONObject jsonObject = new JSONObject(data);
                        // Process the received data
                        //...
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    public void createAndSendConfigData(byte kcmd, byte khora, byte kmin, byte kseg, byte kmSeg, byte kfreq, short kS1, short kS2, short kS3, short kS4, short kS5, short kS6, short kS7, short kS8, short kS9) {
        ConfigData configData = new ConfigData();
        configData.cmd = kcmd;
        configData.hora = khora;
        configData.min = kmin;
        configData.seg = kseg;
        configData.mSeg = kmSeg;
        configData.freq = kfreq;
        configData.S1 = kS1;
        configData.S2 = kS2;
        configData.S3 = kS3;
        configData.S4 = kS4;
        configData.S5 = kS5;
        configData.S6 = kS6;
        configData.S7 = kS7;
        configData.S8 = kS8;
        configData.S9 = kS9;

        sendConfigData(configData);
    }

    public void sendConfigData(@NonNull ConfigData configData) {
        StringBuilder data = new StringBuilder();
        data.append(configData.cmd).append(",")
                .append(configData.hora).append(",")
                .append(configData.min).append(",")
                .append(configData.seg).append(",")
                .append(configData.mSeg).append(",")
                .append(configData.freq).append(",")
                .append(configData.S1).append(",")
                .append(configData.S2).append(",")
                .append(configData.S3).append(",")
                .append(configData.S4).append(",")
                .append(configData.S5).append(",")
                .append(configData.S6).append(",")
                .append(configData.S7).append(",")
                .append(configData.S8).append(",")
                .append(configData.S9);

        try {
            outputStream.write(data.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void receiveData() {
        try {
            byte[] buffer = new byte[1024];
            int bytes = inputStream.read(buffer);
            String data = new String(buffer, 0, bytes);
            Message message = handler.obtainMessage(1, data);
            handler.sendMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connectToDevice() {
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice("ESP32_Palmilha1");
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        try {

            bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(uuid);

            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}