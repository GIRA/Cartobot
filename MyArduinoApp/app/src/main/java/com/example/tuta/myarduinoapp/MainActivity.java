package com.example.tuta.myarduinoapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.physicaloid.lib.Boards;
import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.programmer.avr.UploadErrors;
import com.physicaloid.lib.usb.driver.uart.ReadLisener;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    Physicaloid mPhysicaloid;

    Button btnOpen;
    Button btnClose;
    Spinner spBoardType;
    EditText etSend;
    Button btnSend;
    TextView tvSerialRead;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnOpen = (Button) findViewById(R.id.btnOpen);
        btnClose = (Button) findViewById(R.id.btnClose);
        spBoardType = (Spinner) findViewById(R.id.spBoardType);
        etSend = (EditText) findViewById(R.id.etSend);
        btnSend = (Button) findViewById(R.id.btnSend);
        tvSerialRead = (TextView) findViewById(R.id.etSerialRead);

        mPhysicaloid = new Physicaloid(MainActivity.this);
    }

    private void setUi(boolean connected){
        btnOpen.setEnabled(!connected);
        btnOpen.setEnabled(!connected);
        btnClose.setEnabled(connected);
        etSend.setEnabled(connected);
        btnSend.setEnabled(connected);
    }

    public void btnSend_click(View view){
        String str = etSend.getText().toString()+"\r\n";	//get text from EditText
        if(str.length()>0) {
            byte[] buf = str.getBytes();	//convert string to byte array
            mPhysicaloid.write(buf, buf.length);	//write data to arduino
        }
    }
    public void btnClose_click(View view){
        if(mPhysicaloid.close()) { 	//close the connection to arduino
            mPhysicaloid.clearReadListener();	//clear read listener
            setUi(false);	// set UI accordingly
        }
    }

    public void btnOpen_click(View view){
        if(mPhysicaloid.open()) { 	// tries to connect to device and if device was connected
            setUi(true);
            tvSerialRead.setMovementMethod(new ScrollingMovementMethod());
            Toast.makeText(this, "OPEN OK", Toast.LENGTH_LONG).show();
            // read listener, When new data is received from Arduino add it to Text view
            mPhysicaloid.addReadListener(new ReadLisener() {
                @Override
                public void onRead(int size) {
                    byte[] buf = new byte[size];
                    mPhysicaloid.read(buf, size);
                    tvAppend(tvSerialRead, Html.fromHtml("<font color=blue>" + new String(buf) + "</font>")); 		// add data to text viiew
                }
            });
        } else {
            //Error while connecting
            Toast.makeText(this, "Cannot open", Toast.LENGTH_LONG).show();
        }
    }

    Handler mHandler = new Handler();
    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext); 	// add text to Text view
            }
        });
    }


    public void btnSketchA_click(View view){
        try {
            uploadSketch(getAssets().open("Blink.ino.hex"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void btnSketchB_click(View view){
        try {
            uploadSketch(getAssets().open("Blink2.ino.hex"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void btnSketchC_click(View view){
        try {
            uploadSketch(getAssets().open("serialSketch.ino.hex"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void btnCH340_click(View view){
        startActivity(new Intent(MainActivity.this, UartLoopBackActivity.class));
    }


    private void uploadSketch(InputStream is){
        if(spBoardType.getSelectedItem().toString().contains("UNO"))
            mPhysicaloid.upload(Boards.ARDUINO_UNO, is, uploadCallBack);
        else
            mPhysicaloid.upload(Boards.ARDUINO_NANO_328, is, uploadCallBack);

    }

    Physicaloid.UploadCallBack uploadCallBack = new Physicaloid.UploadCallBack() {
        @Override
        public void onPreUpload() {
            //Toast.makeText(MainActivity.this, "PRE UPLOAD SKETCH", Toast.LENGTH_SHORT).show();
            tvAppend(tvSerialRead, "\nPRE UPLOAD SKETCH");
        }

        @Override
        public void onUploading(int i) {
        }

        @Override
        public void onPostUpload(boolean b) {
            //Toast.makeText(MainActivity.this, "POST UPLOAD SKETCH Boolean: " + (b?"TRUE":"FALSE"), Toast.LENGTH_SHORT).show();
            tvAppend(tvSerialRead, "\nPOST UPLOAD SKETCH Boolean: " + (b?"TRUE":"FALSE"));
        }

        @Override
        public void onCancel() {
            //Toast.makeText(MainActivity.this, "CANCEL UPLOAD SKETCH", Toast.LENGTH_SHORT).show();
            tvAppend(tvSerialRead, "\nCANCEL UPLOAD SKETCH");
        }

        @Override
        public void onError(UploadErrors uploadErrors) {
            //Toast.makeText(MainActivity.this, "ERROR UPLOAD SKETCH", Toast.LENGTH_SHORT).show();
            tvAppend(tvSerialRead, "\nERROR UPLOAD SKETCH");
        }
    };
}
