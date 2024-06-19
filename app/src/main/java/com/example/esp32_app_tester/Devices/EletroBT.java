package com.example.esp32_app_tester.Devices;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.esp32_app_tester.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class EletroBT extends AppCompatActivity {
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;
    private static final String TAG = "EletroBT";
    private static final String DEVICE_NAME = "ESP32_Eletro";
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private SendData receivedData;

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public static class ConfigData {
        public byte cmd;
        public byte PEST, INT;
        public short TMEST, INEST;
    }

    public static class SendData {
        public byte cmd;
        public int battery;
    }

    private final ActivityResultLauncher<Intent> enableBluetoothLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    findESP32Device();
                } else {
                    Toast.makeText(this, "Bluetooth não habilitado", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.eletrobtpage);

        if (checkAndRequestPermissions()) {
            initializeBluetooth();
        }
    }

    private boolean checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, REQUEST_BLUETOOTH_PERMISSIONS);
            return false;
        }
        return true;
    }

    private void initializeBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth não suportado neste dispositivo", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBluetoothLauncher.launch(enableBtIntent);
        } else {
            findESP32Device();
        }
    }

    private void findESP32Device() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (DEVICE_NAME.equals(device.getName())) {
                connectToDevice(device);
                return;
            }
        }

        // Se o dispositivo não estiver emparelhado, inicie a varredura
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        bluetoothAdapter.startDiscovery();
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(EletroBT.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                if (DEVICE_NAME.equals(device.getName())) {
                    if (ActivityCompat.checkSelfPermission(EletroBT.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    bluetoothAdapter.cancelDiscovery();
                    unregisterReceiver(receiver);
                    connectToDevice(device);
                }
            }
        }
    };

    private void connectToDevice(BluetoothDevice device) {
        new Thread(() -> {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                bluetoothSocket.connect();
                outputStream = bluetoothSocket.getOutputStream();
                inputStream = bluetoothSocket.getInputStream();
                receivedData = new SendData();

                Log.d(TAG, "Conectado ao dispositivo: " + device.getName());
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Erro ao conectar ao dispositivo Bluetooth: " + e.getMessage());
            }
        }).start();
    }

    public void SendConfig(byte kcmd, byte kPEST, byte kINT, short kTMEST, short kINEST) {
        ConfigData configData = new ConfigData();
        configData.cmd = kcmd;
        configData.PEST = kPEST;
        configData.INT = kINT;
        configData.TMEST = kTMEST;
        configData.INEST = kINEST;

        new Thread(() -> {
            try {
                StringBuilder data = new StringBuilder();
                data.append(configData.cmd).append(",")
                        .append(configData.PEST).append(",")
                        .append(configData.INT).append(",")
                        .append(configData.TMEST).append(",")
                        .append(configData.INEST).append(",");

                outputStream.write(data.toString().getBytes());
                outputStream.flush();
                Log.d(TAG, "Dados de configuração enviados com sucesso.");
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Erro ao enviar dados de configuração: " + e.getMessage());
            }
        }).start();
    }

    public void receiveData() {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                int bytesRead = inputStream.read(buffer);

                if (bytesRead > 0) {
                    String responseData = new String(buffer, 0, bytesRead);
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        receivedData.cmd = (byte) jsonObject.getInt("cmd");
                        receivedData.battery = jsonObject.getInt("battery");

                        Log.d(TAG, "Dados recebidos e processados com sucesso.");
                        Log.d(TAG, responseData);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Erro ao processar resposta JSON: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Erro ao receber dados: " + e.getMessage());
            }
        }).start();
    }

    public byte getCmd() {
        return receivedData.cmd;
    }

    public int getBattery() {
        return receivedData.battery;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeBluetooth();
            } else {
                Log.e(TAG, "Permissões de Bluetooth não concedidas");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
