package com.example.esp32_app_tester.Devices;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Call;
import okhttp3.Callback;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public class Eletro {
    private OkHttpClient client;
    private OkHttpClient originalClient; // Referência para o OkHttpClient original
    private SendData receivedData;
    private ConnectivityManager connectivityManager;
    private Network connectedNetwork;
    private Queue<ConfigData> configQueue;
    private ConnectivityManager.NetworkCallback originalNetworkCallback; // Referência para o NetworkCallback original

    // Definindo SSID e senha da rede Wi-Fi
    private static final String WIFI_SSID = "ESP32_Eletro";
    private static final String WIFI_PASSPHRASE = "ESP32ELETRO";

    public static class ConfigData {
        public byte cmd;
        public byte PEST, INT;
        public short TMEST, INEST;
    }

    public static class SendData {
        public byte cmd;
        public int battery;
    }

    public interface WifiConnectionCallback {
        void onConnected();
        void onFailed();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public Eletro(@NonNull Context context) {
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        client = new OkHttpClient();
        originalClient = client; // Salva a referência para o OkHttpClient original
        receivedData = new SendData();
        configQueue = new LinkedList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            connectToWifi(new WifiConnectionCallback() {
                @Override
                public void onConnected() {
                    System.out.println("Conexão Wi-Fi estabelecida.");
                    // Processa todas as configurações na fila
                    while (!configQueue.isEmpty()) {
                        sendConfigData(configQueue.poll());
                    }
                }

                @Override
                public void onFailed() {
                    System.err.println("Falha na conexão Wi-Fi.");
                }
            });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void connectToWifi(WifiConnectionCallback callback) {
        WifiNetworkSpecifier wifiNetworkSpecifier = new WifiNetworkSpecifier.Builder()
                .setSsid(WIFI_SSID)
                .setWpa2Passphrase(WIFI_PASSPHRASE)
                .build();

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(wifiNetworkSpecifier)
                .build();

        // Salva o NetworkCallback original
        originalNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                // Salva a conexão original
                originalClient = client;
                // Cria um novo OkHttpClient que usa apenas essa rede.
                client = new OkHttpClient.Builder()
                        .socketFactory(network.getSocketFactory())
                        .build();

                // Salva a rede conectada atualmente
                connectedNetwork = network;
                callback.onConnected();
                System.out.println("Conectado à rede Wi-Fi com sucesso.");
            }

            // ... (implementação de outros métodos de callback conforme necessário)
        };

        // Solicita a rede usando o NetworkRequest e o NetworkCallback personalizado
        connectivityManager.requestNetwork(networkRequest, originalNetworkCallback);
    }

    public void SendConfig(byte kcmd, byte kPEST, byte kINT, short kTMEST, short kINEST) {
        ConfigData configData = new ConfigData();
        configData.cmd = kcmd;
        configData.PEST = kPEST;
        configData.INT = kINT;
        configData.TMEST = kTMEST;
        configData.INEST = kINEST;

        if (connectedNetwork != null) {
            sendConfigData(configData);
        } else {
            System.out.println("Rede não conectada. Adicionando a configuração na fila.");
            configQueue.add(configData);
        }
    }

    private void sendConfigData(@NonNull ConfigData configData) {
        StringBuilder data = new StringBuilder();
        data.append(configData.cmd).append(",")
                .append(configData.PEST).append(",")
                .append(configData.INT).append(",")
                .append(configData.TMEST).append(",")
                .append(configData.INEST).append(",");

        RequestBody body = new FormBody.Builder()
                .add("config_data", data.toString())
                .build();

        Request request = new Request.Builder()
                .url("http://192.168.4.8:80/config")
                .post(body)
                .build();

        if (connectedNetwork != null) {
            client = client.newBuilder().socketFactory(connectedNetwork.getSocketFactory()).build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    System.err.println("Falha ao enviar dados de configuração: " + e.getMessage());
                    // Restaura o cliente original após a falha
                    client = originalClient;
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        System.out.println("Dados de configuração enviados com sucesso.");
                    } else {
                        // Capturando a resposta completa para depuração
                        System.err.println("Falha na resposta: " + response.message());
                        System.err.println("Código de status: " + response.code());
                        System.err.println("Corpo da resposta: " + response.body().string());
                    }
                    // Restaura o cliente original após a resposta
                    client = originalClient;
                }
            });
        } else {
            System.err.println("Rede não está disponível.");
        }
    }

    public void receiveData() {
        Request request = new Request.Builder()
                .url("http://192.168.4.8:80/data")
                .build();

        if (connectedNetwork != null) {
            client = client.newBuilder().socketFactory(connectedNetwork.getSocketFactory()).build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    System.err.println("Falha ao receber dados: " + e.getMessage());
                    // Restaura o cliente original após a falha
                    client = originalClient;
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseData = response.body().string();
                        try {
                            JSONObject jsonObject = new JSONObject(responseData);
                            receivedData.cmd = (byte) jsonObject.getInt("cmd");
                            receivedData.battery = jsonObject.getInt("battery");

                            System.out.println("Dados recebidos e processados com sucesso.");
                            System.out.println(responseData);

                        } catch (JSONException e) {
                            e.printStackTrace();
                            System.err.println("Erro ao processar resposta JSON: " + e.getMessage());
                        }
                    } else {
                        // Capturando a resposta completa para depuração
                        System.err.println("Falha na resposta: " + response.message());
                        System.err.println("Código de status: " + response.code());
                        System.err.println("Corpo da resposta: " + response.body().string());
                    }
                    // Restaura o cliente original após a resposta
                    client = originalClient;
                }
            });
        } else {
            System.err.println("Rede não está disponível.");
        }
    }

    // Getters para acessar os dados recebidos
    public byte getCmd() {
        return receivedData.cmd;
    }

    public int getBattery() {
        return receivedData.battery;
    }

    // Método para restaurar a conexão original
    public void restoreOriginalConnection() {
        // Cancela a solicitação de rede e restaura o cliente original
        connectivityManager.unregisterNetworkCallback(originalNetworkCallback);
        client = originalClient;
        System.out.println("Conexão original restaurada.");
    }
}

