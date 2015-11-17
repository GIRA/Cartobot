package com.gira.firmatatest;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = (TextView) findViewById(R.id.textView);
        b = (Button) findViewById(R.id.LedButton);
        c = (Button) findViewById(R.id.connectButton);
        mov = (Button) findViewById(R.id.movingButton);
        Smov = (Button) findViewById(R.id.StopButton);

        final Activity me = this;
        Smov.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                move = false;
                Stop();

            }
        });

        mov.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Thread th = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                        while (move) {
                            if (fwd) {
                                Left();
                            } else {
                                Right();
                            }
                            fwd = !fwd;
                          Thread.sleep(1000);
                        }
                        Stop();
                        }
                      catch (Exception e) {
                            text.append("\n" + e.getMessage());
                        }}
                });
                move = true;
                th.start();
            }
        });


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
                    arduino.pinMode(11, ArduinoFirmata.OUTPUT);
                    arduino.pinMode(13, ArduinoFirmata.OUTPUT);

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
                    arduino.digitalWrite(13, led);
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

    public void Left() {


        arduino.digitalWrite(9, true);
        arduino.digitalWrite(7, true);

        arduino.digitalWrite(10, false);
        arduino.digitalWrite(11, true);

        arduino.digitalWrite(5, true);
        arduino.digitalWrite(6, false);

    }

    public void Right() {


        arduino.digitalWrite(9, true);
        arduino.digitalWrite(7, true);

        arduino.digitalWrite(11, false);
        arduino.digitalWrite(10, true);

        arduino.digitalWrite(6, true);
        arduino.digitalWrite(5, false);
    }

    public void Stop() {

        arduino.digitalWrite(9, false);
        arduino.digitalWrite(7, false);

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
