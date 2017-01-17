/*
 * Copyright (C) 2013 Keisuke SUZUKI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * Distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.physicaloid.lib.usb.driver.uart;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.Toast;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;
import com.physicaloid.BuildConfig;
import com.physicaloid.lib.framework.SerialCommunicator;
import com.physicaloid.misc.RingBuffer;
import com.wch.wchusbdriver.CH34xAndroidDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Exchanger;

public class UartCH340 extends SerialCommunicator {
    private static final boolean DEBUG_SHOW = false && BuildConfig.DEBUG;
    private static final String TAG = UartCH340.class.getSimpleName();
    private static final String ACTION_USB_PERMISSION = "com.physicaloid.lib.usb.driver.uart.UartCH340.USB_PERMISSION";

    private Context mContext;

    private CH34xAndroidDriver ch340Driver;
    //private D2xxManager ftD2xx = null;
    //private FT_Device ftDev = null;

    private UartConfig mUartConfig;
    private static final int RING_BUFFER_SIZE = 1024;
    private static final int USB_READ_BUFFER_SIZE = 256;
    private static final int USB_WRITE_BUFFER_SIZE = 256;
    private RingBuffer mBuffer;

    private static final int USB_OPEN_INDEX = 0;
    private static final int MAX_READBUF_SIZE = 256;
    private static final int READ_WAIT_MS = 10;

    private boolean mReadThreadStop;

    public UartCH340(Context context) {
        super(context);
        mContext = context;
        mReadThreadStop = true;
        mUartConfig = new UartConfig();
        mBuffer = new RingBuffer(RING_BUFFER_SIZE);
        try {
            ch340Driver = new CH34xAndroidDriver((UsbManager) context.getSystemService(Context.USB_SERVICE), context, ACTION_USB_PERMISSION);
        } catch (Exception ex) {
            Log.e(TAG, ex.toString());
        }
        if(!ch340Driver.UsbFeatureSupported())
        {
            Toast.makeText(context, "No Support USB host API", Toast.LENGTH_SHORT).show();
            ch340Driver = null;
        }
    }


    @Override
    public boolean isOpened() {
        return ch340Driver!=null && ch340Driver.isConnected();
    }

    @Override
    public boolean open() {
        try {
            int result = ch340Driver.ResumeUsbList();
            if (result == 2)
                return false;
            else {
                boolean flags = ch340Driver.UartInit();
                if (!flags) {
                    Log.d(TAG, "Init Uart Error");
                    return false;
                } else {
                    int baudRate = 9660;
                    byte dataBit = 8;
                    byte stopBit = 1;
                    byte parity = 0;
                    byte flowControl = 0;
                    boolean bla = ch340Driver.SetConfig(baudRate, dataBit, stopBit, parity, flowControl);
                    startRead();
                    return bla;
                }
            }
        } catch (Exception ex){
            ex.printStackTrace();
            return false;
        }
    }


    @Override
    public boolean close() {
        if (ch340Driver==null) return true;
        stopRead();
        ch340Driver.CloseDevice();
        return !ch340Driver.isConnected();
    }


    @Override
    public int read(byte[] buf, int size) {
        return mBuffer.get(buf, size);
    }


    @Override
    public int write(byte[] buf, int size) {
        if (buf == null) {
            return 0;
        }
        int offset = 0;
        int write_size;
        int written_size;
        byte[] wbuf = new byte[USB_WRITE_BUFFER_SIZE];

        while (offset < size) {
            write_size = USB_WRITE_BUFFER_SIZE;

            if (offset + write_size > size) {
                write_size = size - offset;
            }
            System.arraycopy(buf, offset, wbuf, 0, write_size);

            synchronized (ch340Driver) {
                try {
                    written_size = ch340Driver.WriteData(wbuf, write_size);
                } catch (Exception ex){
                    written_size = 0;
                }
            }

            if (written_size < 0) {
                return -1;
            }
            offset += written_size;
        }

        return offset;
    }


    private void stopRead() {
        mReadThreadStop = true;
    }


    private void startRead() {
        if (mReadThreadStop) {
            mReadThreadStop = false;
            new Thread(mLoop).start();
        }
    }

    private Runnable mLoop = new Runnable() {
        @Override
        public void run() {
            int len;
            byte[] rbuf = new byte[USB_READ_BUFFER_SIZE];
            for (; ; ) {// this is the main loop for transferring
                try {
                    synchronized (ch340Driver) {
                        len = ch340Driver.ReadData(rbuf, 64, 50);
                    }
                    if (len > MAX_READBUF_SIZE) len = MAX_READBUF_SIZE;
                    if (len > 0) {
                        mBuffer.add(rbuf, len);
                        onRead(len);
                    }
                }catch (Exception ex){
                    len = 0;
                }
                if (mReadThreadStop) {
                    return;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }
            }
        } // end of run()
    }; // end of runnable


    @Override
    public boolean setUartConfig(UartConfig config) {
        /*String test = String.format("setUartConfig(UartConfig)\nBaudrate:%d\nDataBits:%d\nStopBits:%d\nParity:%d\nDtr:%s Rts:%s",
                mUartConfig.baudrate, mUartConfig.dataBits, mUartConfig.stopBits, mUartConfig.parity, mUartConfig.dtrOn?"true":"false", mUartConfig.rtsOn?"true":"false");
        Toast.makeText(mContext, test, Toast.LENGTH_LONG).show();*/
        mUartConfig = config;
        boolean ret = false;
        synchronized (ch340Driver){
            ret = ch340Driver.SetConfig(mUartConfig.baudrate,
                    convertCH340DataBits(mUartConfig.dataBits),
                    convertCH340StopBits(mUartConfig.stopBits),
                    convertCH340Parity(mUartConfig.parity),
                    convertCH340FlowControl(mUartConfig.dtrOn, mUartConfig.rtsOn));
        }
        return ret;
    }


    @Override
    public boolean setBaudrate(int baudrate) {
        if(ch340Driver==null)
            return false;
        mUartConfig.baudrate = baudrate;
        return setUartConfig(mUartConfig);
    }


    @Override
    public boolean setDataBits(int dataBits) {
        if(ch340Driver==null)
            return false;
        mUartConfig.dataBits = dataBits;
        return setUartConfig(mUartConfig);
    }


    @Override
    public boolean setParity(int parity) {
        if(ch340Driver==null)
            return false;
        mUartConfig.parity = parity;
        return setUartConfig(mUartConfig);
    }


    @Override
    public boolean setStopBits(int stopBits) {
        if(ch340Driver==null)
            return false;
        mUartConfig.stopBits = stopBits;
        return setUartConfig(mUartConfig);
    }


    @Override
    public boolean setDtrRts(boolean dtrOn, boolean rtsOn) {
        if(ch340Driver==null)
            return false;
        /*String test = String.format("setDtrRts(dtrOn, rtsOn)\nDtr:%s Rts:%s",
                mUartConfig.dtrOn?"true":"false", mUartConfig.rtsOn?"true":"false");
        Toast.makeText(mContext, test, Toast.LENGTH_SHORT).show();*/
        mUartConfig.dtrOn = dtrOn;
        mUartConfig.rtsOn = rtsOn;
        return setUartConfig(mUartConfig);
    }


    @Override
    public UartConfig getUartConfig() {
        return mUartConfig;
    }


    @Override
    public int getBaudrate() {
        return mUartConfig.baudrate;
    }


    @Override
    public int getDataBits() {
        return mUartConfig.dataBits;
    }


    @Override
    public int getParity() {
        return mUartConfig.parity;
    }


    @Override
    public int getStopBits() {
        return mUartConfig.stopBits;
    }


    @Override
    public boolean getDtr() {
        return mUartConfig.dtrOn;
    }


    @Override
    public boolean getRts() {
        return mUartConfig.rtsOn;
    }


    @Override
    public void clearBuffer() {
        // clear ftdi chip buffer
        synchronized (ch340Driver) {
            //ftDev.purge((byte) (D2xxManager.FT_PURGE_TX | D2xxManager.FT_PURGE_RX));
        }
        mBuffer.clear();
    }


    //////////////////////////////////////////////////////////
    // Listener for reading uart
    //////////////////////////////////////////////////////////
    private List<ReadLisener> uartReadListenerList
            = new ArrayList<ReadLisener>();
    private boolean mStopReadListener = false;

    @Override
    public void addReadListener(ReadLisener listener) {
        uartReadListenerList.add(listener);
    }

    @Override
    public void clearReadListener() {
        uartReadListenerList.clear();
    }

    @Override
    public void startReadListener() {
        mStopReadListener = false;
    }

    @Override
    public void stopReadListener() {
        mStopReadListener = true;
    }

    private void onRead(int size) {
        if (mStopReadListener) return;
        for (ReadLisener listener : uartReadListenerList) {
            listener.onRead(size);
        }
    }
    //////////////////////////////////////////////////////////


    private byte convertCH340DataBits(int dataBits) {
        switch (dataBits) {
            case UartConfig.DATA_BITS7:
                return D2xxManager.FT_DATA_BITS_7;
            case UartConfig.DATA_BITS8:
                return D2xxManager.FT_DATA_BITS_8;
            default:
                return D2xxManager.FT_DATA_BITS_8;
        }
    }


    private byte convertCH340StopBits(int stopBits) {
        switch (stopBits) {
            case UartConfig.STOP_BITS1:
                return D2xxManager.FT_STOP_BITS_1;
            case UartConfig.STOP_BITS2:
                return D2xxManager.FT_STOP_BITS_2;
            default:
                return D2xxManager.FT_STOP_BITS_1;
        }
    }


    private byte convertCH340Parity(int parity) {
        switch (parity) {
            case UartConfig.PARITY_NONE:
                return D2xxManager.FT_PARITY_NONE;
            case UartConfig.PARITY_ODD:
                return D2xxManager.FT_PARITY_ODD;
            case UartConfig.PARITY_EVEN:
                return D2xxManager.FT_PARITY_EVEN;
            case UartConfig.PARITY_MARK:
                return D2xxManager.FT_PARITY_MARK;
            case UartConfig.PARITY_SPACE:
                return D2xxManager.FT_PARITY_SPACE;
            default:
                return D2xxManager.FT_PARITY_NONE;
        }
    }

    private byte convertCH340FlowControl(boolean dtr, boolean rts) {
        if(rts)
            return 1;
        else
            return 0;
    }

    private String toHexStr(byte[] b, int length) {
        String str = "";
        for (int i = 0; i < length; i++) {
            str += String.format("%02x ", b[i]);
        }
        return str;
    }

}
