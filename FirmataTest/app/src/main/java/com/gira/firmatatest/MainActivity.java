package com.gira.firmatatest;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.shokai.firmata.ArduinoFirmata;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private Boolean led = false;
    Boolean move = false;
    Boolean fwd = true;
    TextView text;
    Button b;
    Button c;

    Button mov;
    Button Smov;
    private ArduinoFirmata arduino;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = (TextView) findViewById(R.id.textView);
        b = (Button) findViewById(R.id.LedButton);
        c = (Button) findViewById(R.id.connectButton);

        final Activity me = this;





        c.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    arduino = new ArduinoFirmata(me);
                    arduino.connect();
                    arduino.pinMode(5, ArduinoFirmata.OUTPUT);
                    arduino.pinMode(6, ArduinoFirmata.OUTPUT);
                    arduino.pinMode(7, ArduinoFirmata.OUTPUT);
                    arduino.pinMode(9, ArduinoFirmata.OUTPUT);
                    arduino.pinMode(10, ArduinoFirmata.OUTPUT);
                    arduino.pinMode(11, ArduinoFirmata.PWM);
                    arduino.pinMode(12, ArduinoFirmata.OUTPUT);

                    Thread th = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                while (true) {
                                   final int temp =arduino.analogRead(1);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            text.setText(String.valueOf(temp));
                                        }
                                    });
                                    arduino.analogWrite(11, temp/4);

                                    Thread.sleep(10);
                                }
                            }
                            catch (Exception e) {
                                text.append("\n" + e.getMessage());
                            }}
                    });

                    th.start();
                } catch (Exception e) {
                    text.append("\n" + e.getMessage());
                }
            }
        });
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    led = !led;
                    arduino.digitalWrite(12, led);
                } catch (Exception e) {

                    text.append("\n" + e.getMessage());
                }
            }
        });
        try {
        } catch (Exception e) {
            text.append("\n" + e.getMessage());

        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
