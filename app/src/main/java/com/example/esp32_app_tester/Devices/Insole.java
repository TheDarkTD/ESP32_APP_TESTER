package com.example.esp32_app_tester.Devices;

import androidx.annotation.NonNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Call;
import okhttp3.Callback;
import java.io.IOException;

public class Insole {
    private OkHttpClient client;
    private SendData receivedData;

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

    public Insole() {
        client = new OkHttpClient();
        receivedData = new SendData();
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

        RequestBody body = new FormBody.Builder()
                .add("config_data", data.toString())
                .build();

        Request request = new Request.Builder()
                .url("http://ESP32_Palmilha1.local:80/config")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                // Melhorar tratamento de falha
                System.err.println("Falha ao enviar dados de configuração: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    System.out.println("Dados de configuração enviados com sucesso.");
                } else {
                    System.err.println("Falha na resposta: " + response.message());
                }
            }
        });
    }

    public void receiveData() {
        Request request = new Request.Builder()
                .url("http://ESP32_Palmilha1.local/data")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                // Melhorar tratamento de falha
                System.err.println("Falha ao receber dados: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        receivedData.cmd = (byte) jsonObject.getInt("cmd");
                        receivedData.hourP = (byte) jsonObject.getInt("hour");
                        receivedData.minute = (byte) jsonObject.getInt("minute");
                        receivedData.second = (byte) jsonObject.getInt("second");
                        receivedData.millisecond = (byte) jsonObject.getInt("millisecond");
                        receivedData.battery = jsonObject.getInt("battery");
                        receivedData.length = (short) jsonObject.getInt("length");
                        JSONArray sensorsReads = jsonObject.getJSONArray("sensors_reads");
                        for (int i = 0; i < sensorsReads.length(); i++) {
                            JSONObject sensorRead = sensorsReads.getJSONObject(i);
                            receivedData.SR1[i] = (short) sensorRead.getInt("S1");
                            receivedData.SR2[i] = (short) sensorRead.getInt("S2");
                            receivedData.SR3[i] = (short) sensorRead.getInt("S3");
                            receivedData.SR4[i] = (short) sensorRead.getInt("S4");
                            receivedData.SR5[i] = (short) sensorRead.getInt("S5");
                            receivedData.SR6[i] = (short) sensorRead.getInt("S6");
                            receivedData.SR7[i] = (short) sensorRead.getInt("S7");
                            receivedData.SR8[i] = (short) sensorRead.getInt("S8");
                            receivedData.SR9[i] = (short) sensorRead.getInt("S9");
                            System.out.println(receivedData.SR9[i]);
                        }
                        // Atualize a interface do usuário usando runOnUiThread ou um Handler
                        System.out.println("Dados recebidos e processados com sucesso.");
                        System.out.println(responseData);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        System.err.println("Erro ao processar resposta JSON: " + receivedData.SR1);
                    }
                } else {
                    System.err.println("Falha na resposta: " + response.message());
                }
            }
        });
    }

    // Getters para acessar os dados recebidos
    public byte getCmd() { return receivedData.cmd; }
    public byte getHour() { return receivedData.hourP; }
    public byte getMinute() { return receivedData.minute; }
    public byte getSecond() { return receivedData.second; }
    public byte getMillisecond() { return receivedData.millisecond; }
    public int getBattery() { return receivedData.battery; }
    public short getLength() { return receivedData.length; }
    public short[] getSR1() { return receivedData.SR1; }
    public short[] getSR2() { return receivedData.SR2; }
    public short[] getSR3() { return receivedData.SR3; }
    public short[] getSR4() { return receivedData.SR4; }
    public short[] getSR5() { return receivedData.SR5; }
    public short[] getSR6() { return receivedData.SR6; }
    public short[] getSR7() { return receivedData.SR7; }
    public short[] getSR8() { return receivedData.SR8; }
    public short[] getSR9() { return receivedData.SR9; }
}
