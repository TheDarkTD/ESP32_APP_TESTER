package com.example.esp32_app_tester.TestPages;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.esp32_app_tester.Devices.Insole;
import com.example.esp32_app_tester.R;

import java.util.Calendar;
public class InsoleBTPage extends AppCompatActivity{
    Button mNext7Btn, mTest1, mTest2, mTest1stop, mTest2stop;
    Calendar calendar;
    short S1, S2, S3, S4, S5, S6, S7, S8, S9;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.insolebtpage);
    }
    @Override
    public void onStart() {
        super.onStart();

        mNext7Btn = findViewById(R.id.btnNext7);

        mTest1 = findViewById(R.id.buttontestinsole1);
        mTest2 = findViewById(R.id.buttontestinsole2);
        mTest1stop = findViewById(R.id.buttontestinsole1stop);
        mTest2stop = findViewById(R.id.buttontestinsole2stop);
        S1 = S2 = S3 = S4 = S5 = S6 = S7 = S8 = S9 = 0X1FFF;
        byte freq = 1;

        mTest1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCommand((byte) 0x3A, freq);
            }
        });

        mTest1stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleStopCommand((byte) 0x3B, freq, true);
            }
        });

        mTest2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCommand((byte) 0x3A, freq);
            }
        });

        mTest2stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleStopCommand((byte) 0x3B, freq, false);
            }
        });


    }
    private void sendCommand(byte cmd, byte freq) {
        calendar = Calendar.getInstance();
        byte hour = (byte) calendar.get(Calendar.HOUR_OF_DAY);
        byte minutes = (byte) calendar.get(Calendar.MINUTE);
        byte seconds = (byte) calendar.get(Calendar.SECOND);
        byte milliseconds = (byte) calendar.get(Calendar.MILLISECOND);
        S1 = S2 = S3 = S4 = S5 = S6 = S7 = S8 = S9 = 0X1FFF;

        Insole conectInsole = new Insole();
        conectInsole.createAndSendConfigData(cmd, hour, minutes, seconds, milliseconds, freq, S1, S2, S3, S4, S5, S6, S7, S8, S9);
    }
    private void handleStopCommand(byte cmd, byte freq, boolean isWalking) {
        sendCommand(cmd, freq);

        Insole conectInsole = new Insole();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            conectInsole.receiveData();
        }, 1000);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            calendar = Calendar.getInstance();
            byte hour = (byte) calendar.get(Calendar.HOUR_OF_DAY);
            byte minutes = (byte) calendar.get(Calendar.MINUTE);
            byte seconds = (byte) calendar.get(Calendar.SECOND);
            byte milliseconds = (byte) calendar.get(Calendar.MILLISECOND);



            byte cmd2 = conectInsole.getCmd();

            int battery = conectInsole.getBattery();
            short[] SR1 = conectInsole.getSR1();
            short[] SR2 = conectInsole.getSR2();
            short[] SR3 = conectInsole.getSR3();
            short[] SR4 = conectInsole.getSR4();
            short[] SR5 = conectInsole.getSR5();
            short[] SR6 = conectInsole.getSR6();
            short[] SR7 = conectInsole.getSR7();
            short[] SR8 = conectInsole.getSR8();
            short[] SR9 = conectInsole.getSR9();
            short length = conectInsole.getLength();
            short nsensors = 9;

            System.out.println("register7: " + SR9[5]);


            byte cmd1 = 0X2A;

            conectInsole.createAndSendConfigData(cmd1, hour, minutes, seconds, milliseconds, freq, S1, S2, S3, S4, S5, S6, S7, S8, S9);
        }, 2000); // Delay to allow data reception


        mNext7Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), EletroBTPage.class));
            }
        });
    }



}
