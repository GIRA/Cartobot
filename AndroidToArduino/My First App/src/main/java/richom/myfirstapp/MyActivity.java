package richom.myfirstapp;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MyActivity extends AppCompatActivity {

    private TextView log;
    private UsbSerialDriver usb;
    private Socket client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        log = (TextView)findViewById(R.id.log);
        log.setMovementMethod(new ScrollingMovementMethod());

        Timer clientTimer = new Timer("Cartobot - Client", true);
        clientTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                client();
            }
        }, 0, 1000);

        Timer usbTimer = new Timer("Cartobot - USB", true);
        usbTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                usb();
            }
        }, 0, 1000);

        Timer downTimer = new Timer("Cartobot - Down", true);
        downTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                down();
            }
        }, 0, 100);

        Timer upTimer = new Timer("Cartobot - Up", true);
        upTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                up();
            }
        }, 0, 100);

        /*
        Timer testTimer = new Timer("Test", true);
        testTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (usb == null) return;
                try {
                    log("Writing...");
                    int w = usb.write(new byte[]{1, 2, 3}, 1000);
                    log("%d", w);

                    log("Reading");
                    byte[] buf = new byte[16];
                    int r = usb.read(buf, 1000);
                    log("%d", r);
                    for (byte b : buf) {
                        log("%d", b);
                    }
                } catch (Throwable e) {
                    log(e);
                }
            }
        }, 0, 1000);
        */
    }

    private void usb() {
        if (usb != null) return;

        try {
            log("Looking for devices");
            UsbManager manager = (UsbManager) getApplicationContext().getSystemService(Context.USB_SERVICE);
            UsbSerialDriver port = UsbSerialProber.acquire(manager);
            if (port == null) {
                log("No device found");
            } else {
                port.open();
                port.setBaudRate(57600);
                usb = port;
                log("Device connected!");
            }
        } catch (Throwable e) {
            log(e);
            usb = null;
        }
    }

    private void client() {
        if (client != null) return;
        ServerSocket server = null;
        Socket socket = null;
        try {
            int port = 5050;
            server = new ServerSocket(port);
            log("Waiting for connections on %s:%d...", getWifiIpAddress(), port);
            socket = server.accept();
            client = socket;
            log("Client connected!");
        } catch (IOException e) {
            log(e);
            safeClose(socket);
            client = null;
        } finally {
            safeClose(server);
        }
    }

    private void down() {
        if (client == null || usb == null) return;

        log("Reading from client...");
        byte[] client_in = new byte[4096];
        int bytesRead = 0;
        try {
            bytesRead = client.getInputStream().read(client_in);
        } catch (Throwable ex) {
            log(ex);
            bytesRead = -1; // ERROR!
        }
        if (bytesRead == -1) {
            log("Client disconnected");
            safeClose(client);
            client = null;
            return;
        } else if (bytesRead > 0) {
            log("%d bytes read from client:", bytesRead);
            //log(client_in);
            newLine();

            byte[] usb_out = new byte[bytesRead];
            for (int i = 0; i < bytesRead; i++) {
                usb_out[i] = client_in[i];
            }
            log("Writing to usb...");
            int bytesWritten = 0;
            try {
                bytesWritten = usb.write(usb_out, 200);
            } catch (Throwable ex) {
                log(ex);
                bytesWritten = -1;
            }
            if (bytesWritten == -1) {
                log("USB disconnected");
                /*safeClose(usb);
                usb = null;*/
                return;
            }

            log("%d bytes written to usb:", bytesWritten);
            newLine();
        }
    }

    private void up() {
        if (client == null || usb == null) return;

        log("Reading from usb...");
        byte[] in = new byte[4096];
        int bytesRead = 0;
        try {
            bytesRead = usb.read(in, 200);
        } catch (Throwable ex) {
            log(ex);
            bytesRead = -1; // ERROR!
        }
        if (bytesRead == -1) {
            log("USB disconnected");
            safeClose(usb);
            usb = null;
            return;
        } else if (bytesRead > 0) {
            log("%d bytes read from usb:", bytesRead);
            //log(in);
            newLine();
            log("Writing to client...");
            try {
                client.getOutputStream().write(in, 0, bytesRead);
            } catch (Throwable ex) {
                log(ex);
                log("Client disconnected");
                safeClose(usb);
                usb = null;
                return;
            }
            log("%d bytes written to client:", bytesRead);
            newLine();
        }
    }

    private void safeClose(Closeable object) {
        if (object == null) return;
        try { object.close(); }
        catch (Exception e) { log(e); }
    }

    private void safeClose(UsbSerialDriver object) {
        if (object == null) return;
        try { object.close(); }
        catch (Exception e) { log(e); }
    }

    private void newLine() {
        log("");
    }

    private void log(Throwable ex) {
        log(ex.toString());
        ex.printStackTrace();
    }

    private void log(final byte[] bytes) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (log.getText().length() > 200) {
                    log.setText("");
                }
                for (byte b : bytes) {
                    log.append(String.format("%02X ", b));
                }
                log.append("\n");
            }
        });
    }

    private void log(final String format, final Object... args) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                log.append(String.format(format, args));
                log.append("\n");
            }
        });
    }

    private String getWifiIpAddress() {
        WifiManager wifiMan = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        int ipAddress = wifiInf.getIpAddress();
        return String.format("%d.%d.%d.%d", (ipAddress & 0xff),(ipAddress >> 8 & 0xff),(ipAddress >> 16 & 0xff),(ipAddress >> 24 & 0xff));
    }
}
