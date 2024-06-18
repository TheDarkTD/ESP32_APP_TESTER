package com.example.esp32_app_tester.TestPages;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.esp32_app_tester.Devices.Eletro;
import com.example.esp32_app_tester.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class EletroPage extends AppCompatActivity {
    Button mNext8Btn;
    Button mStartS;
    NumberPicker mrepetition, mintensity, mlpativa, mlpdesativa;
    Integer repetition, intensity, ton, toff;
    Socket socket;
    PrintWriter output;
    BufferedReader input;
    byte PEST,INT,cmd;
    short TMEST,INEST;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.eletropage);
    }

    @Override
    public void onStart() {
        super.onStart();

        mNext8Btn = findViewById(R.id.btnNext8);

        mrepetition = findViewById(R.id.repeticao1);
        mintensity= findViewById(R.id.intensidade1);
        mlpativa = findViewById(R.id.lpativa1);
        mlpdesativa = findViewById(R.id.lpativa);

        mStartS = findViewById(R.id.buttontestvibra);


        mrepetition.setMinValue(0);
        mrepetition.setMaxValue(256);
        mrepetition.setOnValueChangedListener(new NumberPicker.OnValueChangeListener(){
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal){
                repetition = newVal;
            }
        });

        mintensity.setMinValue(0);
        mintensity.setMaxValue(256);
        mintensity.setOnValueChangedListener(new NumberPicker.OnValueChangeListener(){
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal){
                intensity = newVal;
            }
        });

        mlpativa.setMinValue(0);
        mlpativa.setMaxValue(65535);
        mlpativa.setOnValueChangedListener(new NumberPicker.OnValueChangeListener(){
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal){
                ton = newVal;
            }
        });

        mlpdesativa.setMinValue(0);
        mlpdesativa.setMaxValue(65535);
        mlpdesativa.setOnValueChangedListener(new NumberPicker.OnValueChangeListener(){
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal){
                toff = newVal;
            }
        });



        //Botão para iniciar estímulo teste
        mStartS.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onClick(View v) {

                cmd =0x1B;
                INT = Byte.parseByte(String.valueOf(intensity));
                TMEST = Short.parseShort(String.valueOf(ton));
                INEST = Short.parseShort(String.valueOf(toff));
                PEST = Byte.parseByte(String.valueOf(repetition));
                Eletro conectar = new Eletro(EletroPage.this);
                conectar.SendConfig(cmd, PEST, INT, TMEST, INEST);

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    conectar.receiveData();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        conectar.receiveData();
                        conectar.restoreOriginalConnection();
                    }, 7000);
                }, 2000);

                int battery = conectar.getBattery();

            }
        });

    }}