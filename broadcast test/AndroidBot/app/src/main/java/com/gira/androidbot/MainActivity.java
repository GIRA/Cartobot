package com.gira.androidbot;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TextureView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.Console;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;


public class MainActivity extends Activity {

    private TextView logText;
    private Thread broadcastListeningThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        logText = (TextView) findViewById(R.id.LogText);
        logText.setMovementMethod(ScrollingMovementMethod.getInstance());
        startBroadcast();
        connected = false;
    }

    private void PrintText(final String text) {
        runOnUiThread(new Runnable() {
            public void run() {
                logText.append("\n" + text);
            }
        });

        System.out.println(text);
        SendTcpMessage(text.getBytes());
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

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    DatagramSocket socket;
    Socket tcpSocket;
    Thread tcpReceiveThread;

    private void EstablishTCPConnection(InetAddress remoteHost) {
//this means i got a broadcast from some server and now i will connect to it
        if (!connected) {
            try {
                tcpSocket = new Socket(remoteHost, 5201);
                connected = true;
                if (tcpReceiveThread != null) {
                    tcpReceiveThread.interrupt();
                    tcpReceiveThread = null;
                }
                tcpReceiveThread = new Thread(new Runnable() {
                    public void run() {
                        try {
                            while (tcpSocket.isConnected()) {

                                InputStream is = tcpSocket.getInputStream();
                                if (is.available() > 0) {
                                    byte[] data = new byte[is.available()];
                                    is.read(data);
                                    HandleTcpMessage(data);
                                } else {
                                    Thread.sleep(200);

                                }

                            }
                            connected=false;
                            startBroadcast();
                        } catch (Exception ex) {
                            PrintText(ex.getMessage());
                        }
                    }
                });

                tcpReceiveThread.start();
            } catch (Exception e) {
                PrintText(e.getMessage());
            }
        }
    }

    private void SendTcpMessage(byte[] data) {
        try {
            if (tcpSocket != null && tcpSocket.isConnected()) {
                tcpSocket.getOutputStream().write(data);

            }
        } catch (Exception ex) {
            PrintText(ex.getMessage());

        }
    }

    private void HandleTcpMessage(byte[] data) {

        PrintText("TCP Received "+ new String(data));
    }

    private Boolean connected;

    @Override
    protected void onDestroy() {
        try   {
            if(tcpSocket!= null)
                tcpSocket.close();
        }catch (Exception ex){}
    }

    private void startBroadcast() {
        if (broadcastListeningThread == null || !broadcastListeningThread.isAlive()) {
            broadcastListeningThread = new Thread(new Runnable() {

                public void run() {
                    try {
                        //Keep a socket open to listen to all the UDP trafic that is destined for this port
                        if (socket == null || socket.isClosed()) {
                            socket = new DatagramSocket(5200, InetAddress.getByName("0.0.0.0"));
                            socket.setBroadcast(true);
                        }
                        while (!connected) {
                            PrintText(">>>Ready to receive broadcast packets!");
                            //Receive a packet
                            byte[] recvBuf = new byte[15000];
                            DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                            socket.receive(packet);
                            //Packet received
                            PrintText(">>>Discovery packet received from: " + packet.getAddress().getHostAddress());
                            PrintText(">>>Packet received; data: " + new String(packet.getData()));
                            EstablishTCPConnection(packet.getAddress());
                        }

                    } catch (Exception ex) {

                        PrintText("Error " + ex.getMessage());

                    }
                }
            });
            broadcastListeningThread.start();
        }
    }

    private void stopBroadcast() {
        if (broadcastListeningThread != null) {
            broadcastListeningThread.interrupt();
            broadcastListeningThread = null;

        }

    }
}
