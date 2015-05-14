package com.example.xed.kidneymonitor;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;

/**
 * Background service, can run in "foreground" with notification is status bar.
 * Service automatically restarts after being stopped by system.
 * It starts BluetoothChatService and handles messages from it.
 * Received messages are sent to parcer, which performs check for correct checksum,
 * and sets received values that then are send to MainActivity via broadcast message.
 */

public class ConnectionService extends Service {

    public static boolean isServiceRunning=false;

    /**
     * Values for handler of BluetoothChatService messages
     */
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    /**
     * Default values for choosed device
     */
    public static final String DEVICE_NAME = "device_name";
    public static final String DEVICE_ADDRESS = "00:00:00:00:00:00";
    public static final String TOAST = "toast";

    /**
     * Values for broadcast receiver to handle messages from MainActivity for
     * choosing current procedure and starting/stopping it
     */
    public final static String PARAM_TASK = "task";
    public final static String PARAM_ARG = "arg";
    public final static String BROADCAST_ACTION = "SetStatus";
    public final static int TASK_SET_STATUS = 0;
    public final static int TASK_SET_PAUSE = 1;
    public final static int TASK_DO_PAIRING = 3;

    public final static int TASK_ARG_FILLING = 0;
    public final static int TASK_ARG_DIALYSIS = 1;
    public final static int TASK_ARG_SHUTDOWN = 2;
    public final static int TASK_ARG_DISINFECTION = 3;
    public final static int TASK_ARG_FLUSH = 5;

    //Initialisation of LogWriter
    final String logTag = "ConnectionService";
    LogWriter lw = new LogWriter();

    /**
     * Default values for values
     */
    public String STATE = "-1";
    public String STATUS = "-1";
    public String PARAMS = "-1";
    public String FUNCT = "-1";
    public String PAUSE = "-1";
    public String SORBTIME = "-1";
    public String BATT = "-1";
    public String LASTCONNECTED = "-1";
    public String SETTINGSOK = "-1";

    public String DPUMPFLOW1  = "0";
    public String DPUMPFLOW2  = "0";
    public String DPUMPFLOW3  = "0";
    public String DUFVOLUME1 = "0.0";
    public String DPRESS1     = "0.0";
    public String DPRESS2     = "0.0";
    public String DPRESS3     = "0.0";
    public String DTEMP1      = "0.0";
    public String DCOND1      = "0.0";
    public String DCUR1       = "0.0";
    public String DCUR2       = "0.0";
    public String DCUR3       = "0.0";
    public String DCUR4       = "0.0";

    //Random value for notifications IDs
    private static  int NOTIFY_ID = 238;

    // Handler that sends messages to MainActivity every one second
    Handler RefreshHandler = new Handler();

    private StringBuffer mOutStringBuffer;
    private BluetoothChatService mChatService = null;
    private String mConnectedDeviceName = null;
    private String mConnectedDeviceAddress = null;

    /**
     * Bytes for output commands
     */
        final byte CM_SYNC_S = (byte) 0x55;//Start of package
        final byte CM_SYNC_E = (byte) 0xAA;//End of package

        final byte bSETTINGSOK = (byte) 0x99;//Send if received settings OK

        final byte bSENDDPRESS     = (byte) 0x10;//Send to set dialysis pressures
        final byte bSENDDCOND      = (byte) 0x12;//Send to set dialysis conductivity
        final byte bSENDDTEMP      = (byte) 0x14;//Send to set dialysis temperaturecurrent
        final byte bSENDDPUMPS     = (byte) 0x16;//Send to set pumps flows

        final byte bPAUSE        = (byte) 0x5A;//Send to pause current procedure
        final byte bFILLING      = (byte) 0x5B;//Send to set procedure to FILLING
        final byte bDIALYSIS     = (byte) 0x5C;//Send to set procedure to DIALYSIS
        final byte bFLUSH        = (byte) 0x5D;//Send to set procedure to FLUSH
        final byte bDISINFECTION = (byte) 0x5E;//Send to set procedure to DISINFECTION
        final byte bSHUTDOWN     = (byte) 0x5F;//Send to set procedure to SHUTDOWN

        final byte bBATT = (byte) 0xE9;//Receiving battery stats

        final byte bSTATE = (byte) 0x82;//Receiving state
            final byte bSTATE_ON =(byte) 0x10;
            final byte bSTATE_OFF =(byte) 0x11;

        final byte bSTATUS = (byte) 0xEF;//Receiving current procedure
            final byte bSTATUS_FILLING =      (byte) 0x5B;
            final byte bSTATUS_DIALYSIS =     (byte) 0x5C;
            final byte bSTATUS_DISINFECTION = (byte) 0x5E;
            final byte bSTATUS_SHUTDOWN =     (byte) 0x5F;
            final byte bSTATUS_READY =        (byte) 0x5A;
            final byte bSTATUS_FLUSH =        (byte) 0x5D;

        final byte bPARAMS = (byte) 0x84;//Receiving procedure params
            final byte bPARAMS_NORM   = (byte) 0x10;
            final byte bPARAMS_DANGER = (byte) 0x11;

        final byte bSORBTIME =(byte) 0x85;//Receiving sorbtime

        final byte bFUNCT = (byte) 0x86;//Receiving device functioning
            final byte bFUNCT_CORRECT = (byte) 0x10;
            final byte bFUNCT_FAULT   = (byte) 0x11;

        final byte bDPRESS1 = (byte) 0xE0;//Receiving dialysis pressure1
        final byte bDPRESS2 = (byte) 0xE1;//Receiving dialysis pressure2
        final byte bDPRESS3 = (byte) 0xE2;//Receiving dialysis pressure3

        final byte bDTEMP1 = (byte) 0xE3;//Receiving dialysis temperature1
        final byte bDCOND1 = (byte) 0xE4;//Receiving dialysis conductivity1

        final byte bDCUR1 = (byte) 0xE5;//Receiving dialysis current1
        final byte bDCUR2 = (byte) 0xE6;//Receiving dialysis current2
        final byte bDCUR3 = (byte) 0xE7;//Receiving dialysis current3
        final byte bDCUR4 = (byte) 0xE8;//Receiving dialysis current4


    /**
    **ERROR codes
     */
    final byte  PE_PRESS1 = (byte)  0xF0;    // Error on pressure sensor 1
    final byte  PE_PRESS2 = (byte)  0xF1;    // Error on pressure sensor 2
    final byte  PE_PRESS3 = (byte)  0xF2;    // Error on pressure sensor 3
    final byte  PE_TEMP   = (byte)  0xF3;    // Error on temperature sensor
    final byte  PE_ELECTRO= (byte)  0xF4;    // Error on conductivity sensor
    final byte  PE_EDS1   = (byte)  0xF5;    // Error on electric cell 1
    final byte  PE_EDS2   = (byte)  0xF6;    // Error on electric cell 2
    final byte  PE_EDS3   = (byte)  0xF7;    // Error on electric cell 3
    final byte  PE_EDS4   = (byte)  0xF8;    // Error on electric cell 4
    final byte  PE_BATT   = (byte)  0xF9;    // Error on low battery
    final byte  PE_PUMP1  = (byte)  0xFA;    // Pump 1 error, rpm low
    final byte  PE_PUMP2  = (byte)  0xFB;    // Pump 2 error, rpm low
    final byte  PE_PUMP3  = (byte)  0xFC;    // Pump 3 error, rpm low


    /**
     * Values read from settings file
     */
    public static int DPUMP1FLOW = 0;
    public static int DPUMP2FLOW = 0;
    public static int DPUMP3FLOW = 0;

    public static float DPRESS1MIN = 0.0f;
    public static float DPRESS1MAX = 0.0f;
    public static float DPRESS2MIN = 0.0f;
    public static float DPRESS2MAX = 0.0f;
    public static float DPRESS3MIN = 0.0f;
    public static float DPRESS3MAX = 0.0f;

    public static float DTEMP1MIN = 0.0f;
    public static float DTEMP1MAX = 0.0f;

    public static float DCOND1MIN = 0.0f;
    public static float DCOND1MAX = 0.0f;

    public static int FPUMP1FLOW = 0;
    public static int FPUMP2FLOW = 0;
    public static int FPUMP3FLOW = 0;

    public static int UFPUMP1FLOW = 0;
    public static int UFPUMP2FLOW = 0;
    public static int UFPUMP3FLOW = 0;


    /**
     * Handling BluetoothChatService messages
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case MESSAGE_STATE_CHANGE:
                    Log.d(logTag, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {

                        case BluetoothChatService.STATE_CONNECTED:
                            lw.appendLog(logTag, "\nConnected to: " + mConnectedDeviceName);
                            break;

                        case BluetoothChatService.STATE_CONNECTING:
                            lw.appendLog(logTag, "\nConnecting to: " + mConnectedDeviceName);
                            break;

                        case BluetoothChatService.STATE_LISTEN:
                            break;


                        case BluetoothChatService.STATE_NONE:
                            lw.appendLog(logTag, "\nNot connected");
                            break;
                    }
                    break;

                case MESSAGE_WRITE:
                    lw.appendLog(logTag, "WRITING!");
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    lw.appendLog(logTag, "\nMe:  " + writeMessage);
                    break;

                case MESSAGE_READ:
                    lw.appendLog(logTag, "READING");
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    lw.appendLog(logTag, "\n" + mConnectedDeviceName + ":  " + readMessage);
                    //parseandexecute(readMessage);//start parser with received string
                    parseInBytes(readBuf);
                    break;

                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    mConnectedDeviceAddress = msg.getData().getString(DEVICE_ADDRESS);
                    break;

                case MESSAGE_TOAST:
                    lw.appendLog(logTag, msg.getData().getString(TOAST));
                    break;
            }
        }
    };

    void parseInBytes(byte[] inp){
        if(inp.length>7)
        if(inp[0]==CM_SYNC_S && inp[7]==CM_SYNC_E)
        {
            byte com1 = inp[1];
            byte com2 = inp[2];

            byte currentArg = inp[6];
            byte[] databytes = new byte[] {inp[3],inp[4],inp[5],inp[6]};
            //int full_data_int = byteArrayToInt(databytes);

            int full_data_int = ByteBuffer.wrap(databytes).getInt();
            float full_data_float = ByteBuffer.wrap(databytes).getFloat();

            //int full_data_int = java.nio.ByteBuffer.wrap(databytes).getInt();
            LASTCONNECTED = "0";



            switch (com1){

                case bBATT:{
                    lw.appendLog(logTag, "setting BATT to " + full_data_int, true);
                    lw.appendLog(logTag, "setting battery to " + full_data_int + "%");
                    BATT = String.valueOf(full_data_int);
                    break;
                }

                case bSTATUS: {
                    lw.appendLog(logTag, "got command STATUS and " + currentArg);
                    switch (currentArg) {
                        case bSTATUS_FILLING: {
                            lw.appendLog(logTag, "setting STATUS to FILLING", true);
                            STATUS = "0";
                            break;
                        }
                        case bSTATUS_DIALYSIS: {

                            lw.appendLog(logTag, "setting STATUS to DIALYSIS", true);
                            STATUS = "1";
                            break;
                        }
                        case bSTATUS_SHUTDOWN: {
                            lw.appendLog(logTag, "setting STATUS to SHUTDOWN", true);
                            STATUS = "2";
                            break;
                        }
                        case bSTATUS_DISINFECTION: {
                            lw.appendLog(logTag, "setting STATUS to DISINFECTION", true);
                            STATUS = "3";
                            break;
                        }
                        case bSTATUS_READY: {
                            lw.appendLog(logTag, "setting STATUS to READY", true);
                            STATUS = "4";
                            break;
                        }
                        case bSTATUS_FLUSH: {
                            lw.appendLog(logTag, "setting STATUS to FLUSH", true);
                            STATUS = "5";
                            break;
                        }
                        default: {
                            lw.appendLog(logTag, "setting STATUS to UNKNOWN", true);
                            STATUS = "-1";
                            break;
                        }
                    }
                    break;
                }

                //TODO:when PARAMS is NORMAL?
                case bPARAMS: {
                    lw.appendLog(logTag, "got command PARAMS and " + currentArg);
                    switch (currentArg) {
                        case bPARAMS_NORM: {
                            lw.appendLog(logTag, "setting PARAMS to NORMAL", true);
                            PARAMS = "0";
                            break;
                        }
                        case bPARAMS_DANGER: {
                            lw.appendLog(logTag, "setting PARAMS to DANGER", true);
                            PARAMS = "1";
                            break;
                        }
                        default: {
                            lw.appendLog(logTag, "setting PARAMS to UNKNOWN", true);
                            PARAMS = "-1";
                            break;
                        }
                    }
                    break;
                }

                //TODO: sorbtime calculation
                case bSORBTIME: {
                    lw.appendLog(logTag, "setting SORBTIME to " + currentArg, true);
                    SORBTIME = String.valueOf(full_data_int);
                    break;
                }

                //TODO:when FUNCT is NORMAL?
                case bFUNCT: {
                    lw.appendLog(logTag, "got command FUNCT and " + currentArg);
                    switch (currentArg) {
                        case bFUNCT_CORRECT: {
                            lw.appendLog(logTag, "setting FUNCT to CORRECT", true);
                            FUNCT = "0";
                            break;
                        }
                        case bFUNCT_FAULT: {
                            lw.appendLog(logTag, "setting FUNCT to FAULT", true);
                            FUNCT = "1";
                            break;
                        }
                        default: {
                            lw.appendLog(logTag, "setting FUNCT to UNKNOWN", true);
                            FUNCT = "-1";
                            break;
                        }
                    }
                    break;
                }

                //TODO: is needed?
                case bSETTINGSOK:{
                    SETTINGSOK = "0";
                    lw.appendLog(logTag, "Settings OK", true);
                    break;
                }

                case bDPRESS1:{
                    DPRESS1 = String.valueOf(full_data_float);
                    lw.appendLog(logTag, "setting DPRESS1 to " + DPRESS1, true);

                    break;
                }

                case bDPRESS2:{
                    DPRESS2 = String.valueOf(full_data_float);
                    lw.appendLog(logTag, "setting DPRESS2 to " + DPRESS2, true);

                    break;
                }

                case bDPRESS3:{
                    DPRESS3 = String.valueOf(full_data_float);
                    lw.appendLog(logTag, "setting DPRESS3 to " + DPRESS3, true);

                    break;
                }

                case bDTEMP1:{
                    DTEMP1 = String.valueOf(full_data_float);
                    lw.appendLog(logTag, "setting DTEMP1 to " + DTEMP1, true);

                    break;
                }

                case bDCOND1:{
                    DCOND1 = String.valueOf(full_data_float);
                    lw.appendLog(logTag, "setting DCOND1 to " + DCOND1, true);

                    break;
                }

                case bDCUR1:{
                    DCUR1 = String.valueOf(full_data_float);
                    lw.appendLog(logTag, "setting DCUR1 to " + DCUR1, true);
                    break;
                }

                case bDCUR2:{
                    DCUR2 = String.valueOf(full_data_float);
                    lw.appendLog(logTag, "setting DCUR2 to " + DCUR2, true);
                    break;
                }

                case bDCUR3:{
                    DCUR3 = String.valueOf(full_data_float);
                    lw.appendLog(logTag, "setting DCUR3 to " + DCUR3, true);
                    break;
                }

                case bDCUR4:{
                    DCUR4 = String.valueOf(full_data_float);
                    lw.appendLog(logTag, "setting DCUR4 to " + DCUR4, true);
                    break;
                }

                case bSENDDPUMPS:{
                    if(com2==(byte)0x01) {
                        sendMessageBytes((byte) (bSENDDPUMPS + (byte) 0x01), com2, intTo4byte(FPUMP1FLOW));
                        lw.appendLog(logTag, "send FPUMP1 " + FPUMP1FLOW, true);
                    }
                    if(com2==(byte)0x02) {
                        sendMessageBytes((byte) (bSENDDPUMPS + (byte) 0x01), com2, intTo4byte(DPUMP1FLOW));
                        lw.appendLog(logTag, "send DPUMP1 " + DPUMP1FLOW, true);
                    }
                    if(com2==(byte)0x03) {
                        sendMessageBytes((byte) (bSENDDPUMPS + (byte) 0x01), com2, intTo4byte(UFPUMP1FLOW));
                        lw.appendLog(logTag, "send UFPUMP1 " + UFPUMP1FLOW, true);
                    }

                    if(com2==(byte)0x11) {
                        sendMessageBytes((byte) (bSENDDPUMPS + (byte) 0x01), com2, intTo4byte(FPUMP2FLOW));
                        lw.appendLog(logTag, "send FPUMP2 " + FPUMP2FLOW, true);
                    }
                    if(com2==(byte)0x12) {
                        sendMessageBytes((byte) (bSENDDPUMPS + (byte) 0x01), com2, intTo4byte(DPUMP2FLOW));
                        lw.appendLog(logTag, "send DPUMP2 " + DPUMP2FLOW, true);
                    }
                    if(com2==(byte)0x13) {
                        sendMessageBytes((byte) (bSENDDPUMPS + (byte) 0x01), com2, intTo4byte(UFPUMP2FLOW));
                        lw.appendLog(logTag, "send UFPUMP2 " + UFPUMP2FLOW, true);
                    }

                    if(com2==(byte)0x21) {
                        sendMessageBytes((byte) (bSENDDPUMPS + (byte) 0x01), com2, intTo4byte(FPUMP3FLOW));
                        lw.appendLog(logTag, "send FPUMP3 " + FPUMP3FLOW, true);
                    }
                    if(com2==(byte)0x22) {
                        sendMessageBytes((byte) (bSENDDPUMPS + (byte) 0x01), com2, intTo4byte(DPUMP3FLOW));
                        lw.appendLog(logTag, "send DPUMP3 " + DPUMP3FLOW, true);
                    }
                    if(com2==(byte)0x23) {
                        sendMessageBytes((byte) (bSENDDPUMPS + (byte) 0x01), com2, intTo4byte(UFPUMP3FLOW));
                        lw.appendLog(logTag, "send UFPUMP3 " + UFPUMP3FLOW, true);
                    }
                    break;
                }

                case bSENDDPRESS:{
                    if(com2==(byte)0x01) {
                        sendMessageBytes((byte) (bSENDDPRESS + (byte) 0x01), com2, floatTo4byte(DPRESS1MIN));
                        lw.appendLog(logTag, "send DPRESS1MIN " + DPRESS1MIN, true);
                    }
                    if(com2==(byte)0x02) {
                        sendMessageBytes((byte) (bSENDDPRESS + (byte) 0x01), com2, floatTo4byte(DPRESS1MAX));
                        lw.appendLog(logTag, "send DPRESS1MAX " + DPRESS1MAX, true);
                    }
                    if(com2==(byte)0x11) {
                        sendMessageBytes((byte) (bSENDDPRESS + (byte) 0x01), com2, floatTo4byte(DPRESS2MIN));
                        lw.appendLog(logTag, "send DPRESS2MIN " + DPRESS2MIN, true);
                    }
                    if(com2==(byte)0x12) {
                        sendMessageBytes((byte) (bSENDDPRESS + (byte) 0x01), com2, floatTo4byte(DPRESS2MAX));
                        lw.appendLog(logTag, "send DPRESS2MAX " + DPRESS2MAX, true);
                    }if(com2==(byte)0x21) {
                        sendMessageBytes((byte) (bSENDDPRESS + (byte) 0x01), com2, floatTo4byte(DPRESS3MIN));
                        lw.appendLog(logTag, "send DPRESS3MIN " + DPRESS3MIN, true);
                    }
                    if(com2==(byte)0x22) {
                        sendMessageBytes((byte) (bSENDDPRESS + (byte) 0x01), com2, floatTo4byte(DPRESS3MAX));
                        lw.appendLog(logTag, "send DPRESS3MAX " + DPRESS3MAX, true);
                    }
                    break;
                }

                case bSENDDTEMP:{
                    if(com2==(byte)0x01) {
                        sendMessageBytes((byte)(bSENDDTEMP+(byte)0x01), com2, floatTo4byte(DTEMP1MIN));
                        lw.appendLog(logTag, "send DTEMPMIN " + DTEMP1MIN, true);
                    }
                    if(com2==(byte)0x02) {
                        sendMessageBytes((byte) (bSENDDTEMP + (byte) 0x01), com2, floatTo4byte(DTEMP1MAX));
                        lw.appendLog(logTag, "send DTEMPMAX " + DTEMP1MAX, true);
                    }
                    break;
                }

                case bSENDDCOND:{
                    if(com2==(byte)0x01) {
                        sendMessageBytes((byte)(bSENDDCOND+(byte)0x01), com2, floatTo4byte(DCOND1MIN));
                        lw.appendLog(logTag, "send DCONDMIN " + DCOND1MIN, true);
                    }
                    if(com2==(byte)0x02) {
                        sendMessageBytes((byte) (bSENDDCOND + (byte) 0x01), com2, floatTo4byte(DCOND1MAX));
                        lw.appendLog(logTag, "send DCONDMAX " + DCOND1MAX, true);
                    }
                    break;
                }



                case PE_PRESS1:{
                    sendNotification("ERROR PE_PRESS1");
                    lw.appendLog(logTag, "ERROR PE_PRESS1", true);
                    FUNCT = "1";
                    PARAMS = "1";
                    break;
                }

                case PE_PRESS2:{
                    sendNotification("ERROR PE_PRESS2");
                    lw.appendLog(logTag, "ERROR PE_PRESS2", true);
                    FUNCT = "1";
                    PARAMS = "1";
                    break;
                }

                case PE_PRESS3:{
                    sendNotification("ERROR PE_PRESS3");
                    lw.appendLog(logTag, "ERROR PE_PRESS3", true);
                    FUNCT = "1";
                    PARAMS = "1";
                    break;
                }

                case PE_TEMP:{
                    sendNotification("ERROR PE_TEMP");
                    lw.appendLog(logTag, "ERROR PE_TEMP", true);
                    FUNCT = "1";
                    PARAMS = "1";
                    break;
                }

                case PE_ELECTRO:{
                    sendNotification("ERROR PE_ELECTRO");
                    lw.appendLog(logTag, "ERROR PE_ELECTRO", true);
                    FUNCT = "1";
                    PARAMS = "1";
                    break;
                }

                case PE_EDS1:{
                    sendNotification("ERROR PE_EDS1");
                    lw.appendLog(logTag, "ERROR PE_EDS1", true);
                    FUNCT = "1";
                    PARAMS = "1";
                    break;
                }

                case PE_EDS2:{
                    sendNotification("ERROR PE_EDS2");
                    lw.appendLog(logTag, "ERROR PE_EDS2", true);
                    FUNCT = "1";
                    PARAMS = "1";
                    break;
                }

                case PE_EDS3:{
                    sendNotification("ERROR PE_EDS3");
                    lw.appendLog(logTag, "ERROR PE_EDS3", true);
                    FUNCT = "1";
                    PARAMS = "1";
                    break;
                }

                case PE_EDS4:{
                    sendNotification("ERROR PE_EDS4");
                    lw.appendLog(logTag, "ERROR PE_EDS4", true);
                    FUNCT = "1";
                    PARAMS = "1";
                    break;
                }

                case PE_BATT:{
                    sendNotification("ERROR PE_BATT");
                    lw.appendLog(logTag, "ERROR PE_BATT", true);
                    FUNCT = "1";
                    PARAMS = "1";
                    break;
                }

                case PE_PUMP1:{
                    sendNotification("ERROR PE_PUMP1");
                    lw.appendLog(logTag, "ERROR PE_PUMP1", true);
                    FUNCT = "1";
                    PARAMS = "1";
                    break;
                }

                case PE_PUMP2:{
                    sendNotification("ERROR PE_PUMP2");
                    lw.appendLog(logTag, "ERROR PE_PUMP2", true);
                    FUNCT = "1";
                    PARAMS = "1";
                    break;
                }

                case PE_PUMP3:{
                    sendNotification("ERROR PE_PUMP3");
                    lw.appendLog(logTag, "ERROR PE_PUMP3", true);
                    FUNCT = "1";
                    PARAMS = "1";
                    break;
                }

                default:
                    break;
            }

            if((Float.valueOf(DPRESS1)>DPRESS1MIN) && (Float.valueOf(DPRESS1)<DPRESS1MAX) &&
               (Float.valueOf(DPRESS2)>DPRESS2MIN) && (Float.valueOf(DPRESS2)<DPRESS2MAX) &&
               (Float.valueOf(DPRESS3)>DPRESS3MIN) && (Float.valueOf(DPRESS3)<DPRESS3MAX) &&
               (Float.valueOf(DTEMP1)>DTEMP1MIN)   && (Float.valueOf(DTEMP1)<DTEMP1MAX)   &&
               (Float.valueOf(DCOND1)>DCOND1MIN)   && (Float.valueOf(DCOND1)<DCOND1MAX))
            {
                FUNCT = "0";
                PARAMS = "0";
            }
            else
            {
                FUNCT = "1";
                PARAMS = "1";
            }

            if(!STATUS.equals("2"))//If status is not SHUTDOWN, then STATE is ON
                STATE = "0";
            else{
                STATE = "1";//Otherwise, STATE is OFF
                lw.appendLog(logTag, "setting STATE to OFF", true);
            }
        }
    }

    byte[] floatTo4byte(float fvalue){
        return ByteBuffer.allocate(4).putFloat(fvalue).array();
    }

    byte[] intTo4byte(int ivalue){
        return ByteBuffer.allocate(4).putInt(ivalue).array();
    }

    /**
     * Handle messages received from main screen activity: setting status and pause/resume
     */
    BroadcastReceiver StatusReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            int task = intent.getIntExtra(PARAM_TASK, -1);
            int arg = intent.getIntExtra(PARAM_ARG, -1);

            // switch tasks for setting main screen values
            switch (task) {
                case TASK_SET_STATUS: {
                    switch (arg) {
                        case TASK_ARG_DIALYSIS: {
                            sendMessageBytes(bDIALYSIS);
                            lw.appendLog(logTag, "User switching to DIALYSIS", true);
                            break;
                        }
                        case TASK_ARG_FILLING: {
                            sendMessageBytes(bFILLING);
                            lw.appendLog(logTag, "User switching to FILLING", true);
                            break;
                        }
                        case TASK_ARG_SHUTDOWN: {
                            sendMessageBytes(bSHUTDOWN);
                            lw.appendLog(logTag, "User switching to SHUTDOWN", true);
                            break;
                        }
                        case TASK_ARG_DISINFECTION: {
                            sendMessageBytes(bDISINFECTION);
                            lw.appendLog(logTag, "User switching to DISINFECTION", true);
                            break;
                        }
                        case TASK_ARG_FLUSH: {
                            sendMessageBytes(bFLUSH);
                            lw.appendLog(logTag, "User switching to FLUSH", true);
                            break;
                        }
                        default:
                            break;
                    }
                    break;
                }

                case TASK_SET_PAUSE: {
                    sendMessageBytes(bPAUSE);
                    lw.appendLog(logTag, "User set PAUSE", true);
                    break;
                }

                case TASK_DO_PAIRING: {
                    SharedPreferences sPref = getSharedPreferences(PrefActivity.APP_PREFERENCES, MODE_PRIVATE); //Load preferences;
                    String address = sPref.getString(PrefActivity.SAVED_ADDRESS, "00:00:00:00:00:00");
                    if(!"00:00:00:00:00:00".equals(address)){
                        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                        mChatService.connect(device, true);//securely connect to chosen device
                        lw.appendLog(logTag, "Pairing with " + PrefActivity.CHOSEN_NAME+'@'+PrefActivity.CHOSEN_ADDRESS, true);
                    }
                    break;
                }

                default:
                    break;
            }

        }

    };


    void sendMessageBytes(byte com1)
    {
        byte[] outp = new byte[] {CM_SYNC_S, com1, (byte)0x00, (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00, CM_SYNC_E};
        mChatService.write(outp);
    }

    void sendMessageBytes(byte com1, byte[] data)
    {
        byte[] outp = new byte[] {CM_SYNC_S, com1, (byte)0x00, data[0], data[1], data[2], data[3], CM_SYNC_E};
        mChatService.write(outp);
    }

    void sendMessageBytes(byte com1, byte com2)
    {
        byte[] outp = new byte[] {CM_SYNC_S, com1, com2,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00, CM_SYNC_E};
        mChatService.write(outp);
    }

    void sendMessageBytes(byte com1, byte com2, byte[] data)
    {
        byte[] outp = new byte[] {CM_SYNC_S, com1, com2, data[0], data[1], data[2], data[3], CM_SYNC_E};
        mChatService.write(outp);
    }

    /**
     *Send values to MainActivity every second
     */
    Runnable timedTask = new Runnable() {
        @Override
        public void run() {
            Intent intentValues = new Intent(MainActivity.BROADCAST_ACTION);
            Intent intentParams = new Intent(ParamsActivity.BROADCAST_ACTION);

            intentValues.putExtra(MainActivity.PARAM_TASK, MainActivity.TASK_SET_STATE);
            intentValues.putExtra(MainActivity.PARAM_ARG, STATE);
            sendBroadcast(intentValues);

            intentValues.putExtra(MainActivity.PARAM_TASK, MainActivity.TASK_SET_STATUS);
            intentValues.putExtra(MainActivity.PARAM_ARG, STATUS);
            sendBroadcast(intentValues);

            intentValues.putExtra(MainActivity.PARAM_TASK, MainActivity.TASK_SET_PARAMS);
            intentValues.putExtra(MainActivity.PARAM_ARG, PARAMS);
            sendBroadcast(intentValues);

            intentValues.putExtra(MainActivity.PARAM_TASK, MainActivity.TASK_SET_FUNCT);
            intentValues.putExtra(MainActivity.PARAM_ARG, FUNCT);
            sendBroadcast(intentValues);

            intentValues.putExtra(MainActivity.PARAM_TASK, MainActivity.TASK_SET_SORBTIME);
            intentValues.putExtra(MainActivity.PARAM_ARG, SORBTIME);
            sendBroadcast(intentValues);

            intentValues.putExtra(MainActivity.PARAM_TASK, MainActivity.TASK_SET_BATT);
            intentValues.putExtra(MainActivity.PARAM_ARG, BATT);
            sendBroadcast(intentValues);

            intentValues.putExtra(MainActivity.PARAM_TASK, MainActivity.TASK_SET_PAUSE);
            intentValues.putExtra(MainActivity.PARAM_ARG, PAUSE);
            sendBroadcast(intentValues);

            intentValues.putExtra(MainActivity.PARAM_TASK, MainActivity.TASK_SET_SETTINGSOK);
            intentValues.putExtra(MainActivity.PARAM_ARG, SETTINGSOK);
            sendBroadcast(intentValues);

            intentValues.putExtra(MainActivity.PARAM_TASK, MainActivity.TASK_SET_LASTCONNECTED);
            intentValues.putExtra(MainActivity.PARAM_ARG, LASTCONNECTED);
            sendBroadcast(intentValues);
            LASTCONNECTED="-1";//Set default state to fix value on main screen

            intentParams.putExtra(ParamsActivity.PARAM_TASK, ParamsActivity.TASK_SET_DUFVOLUME1);
            intentParams.putExtra(ParamsActivity.PARAM_ARG, DUFVOLUME1);
            sendBroadcast(intentParams);

            intentParams.putExtra(ParamsActivity.PARAM_TASK, ParamsActivity.TASK_SET_DPRESS1);
            intentParams.putExtra(ParamsActivity.PARAM_ARG, DPRESS1);
            sendBroadcast(intentParams);

            intentParams.putExtra(ParamsActivity.PARAM_TASK, ParamsActivity.TASK_SET_DPRESS2);
            intentParams.putExtra(ParamsActivity.PARAM_ARG, DPRESS2);
            sendBroadcast(intentParams);

            intentParams.putExtra(ParamsActivity.PARAM_TASK, ParamsActivity.TASK_SET_DPRESS3);
            intentParams.putExtra(ParamsActivity.PARAM_ARG, DPRESS3);
            sendBroadcast(intentParams);

            intentParams.putExtra(ParamsActivity.PARAM_TASK, ParamsActivity.TASK_SET_DTEMP1);
            intentParams.putExtra(ParamsActivity.PARAM_ARG, DTEMP1);
            sendBroadcast(intentParams);

            intentParams.putExtra(ParamsActivity.PARAM_TASK, ParamsActivity.TASK_SET_DCOND1);
            intentParams.putExtra(ParamsActivity.PARAM_ARG, DCOND1);
            sendBroadcast(intentParams);

            intentParams.putExtra(ParamsActivity.PARAM_TASK, ParamsActivity.TASK_SET_DCUR1);
            intentParams.putExtra(ParamsActivity.PARAM_ARG, DCUR1);
            sendBroadcast(intentParams);

            intentParams.putExtra(ParamsActivity.PARAM_TASK, ParamsActivity.TASK_SET_DCUR2);
            intentParams.putExtra(ParamsActivity.PARAM_ARG, DCUR2);
            sendBroadcast(intentParams);

            intentParams.putExtra(ParamsActivity.PARAM_TASK, ParamsActivity.TASK_SET_DCUR3);
            intentParams.putExtra(ParamsActivity.PARAM_ARG, DCUR3);
            sendBroadcast(intentParams);

            intentParams.putExtra(ParamsActivity.PARAM_TASK, ParamsActivity.TASK_SET_DCUR4);
            intentParams.putExtra(ParamsActivity.PARAM_ARG, DCUR4);
            sendBroadcast(intentParams);

            SharedPreferences sPref =  getSharedPreferences(PrefActivity.APP_PREFERENCES, MODE_PRIVATE);
            if(sPref.getBoolean(PrefActivity.AUTOCONNECT, false))
                if(DEVICE_ADDRESS.equals("00:00:00:00:00:00")){
                    Intent intent = new Intent(ConnectionService.BROADCAST_ACTION);
                    intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_DO_PAIRING);
                    sendBroadcast(intent);
                }

            RefreshHandler.postDelayed(timedTask, 1000);//refresh after one second
        }
    };

    public void onCreate() {
        super.onCreate();
        Log.d(logTag, "onCreate");
        lw.appendLog(logTag, "onCreate");

        if (mChatService == null) setupChat();
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
        RefreshHandler.post(timedTask);

        readSettingsFromFile();

        //Register receiver with filter to receive messages from MainActivity
        IntentFilter intFilt = new IntentFilter(BROADCAST_ACTION);
        registerReceiver(StatusReceiver, intFilt);

        //Register receiver with filter to handle situation when choosen device disconnected
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(DisconnectReceiver, filter);
    }


    /**
    ** Receiver watches for bluetooth disconnect, and restarts bluetoothchatservice
    **/
    private final BroadcastReceiver DisconnectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final BluetoothDevice device = intent.getParcelableExtra( BluetoothDevice.EXTRA_DEVICE );
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                lw.appendLog(logTag, "Lost connection, restarting...");
                mChatService.stop();
                mChatService.start();
            }
        }
    };

    /**
     * Start service. If foreground setting is on then start it in foreground
     */
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(logTag, "onStartCommand");
        lw.appendLog(logTag, "onStartCommand");
        isServiceRunning=true;

        SharedPreferences sPref = getSharedPreferences(PrefActivity.APP_PREFERENCES, MODE_PRIVATE); //Load preferences
        if(sPref.getBoolean(PrefActivity.IS_FOREGROUND, false))
            startInForeground();

        return START_STICKY;//Service will be restarted if killed by Android
    }

    /**
     * On service stop deregister all broadcast receivers, stop foreground service and stop service
     */
    public void onDestroy() {
        super.onDestroy();
        Log.d(logTag, "onDestroy");
        lw.appendLog(logTag, "onDestroy");

        if (mChatService != null)
            mChatService.stop();
        isServiceRunning=false;
        unregisterReceiver(StatusReceiver);
        unregisterReceiver(DisconnectReceiver);
        stopForeground(true);
        stopSelf();
    }

    public IBinder onBind(Intent intent) {
        Log.d(logTag, "onBind");
        lw.appendLog(logTag, "onBind");
        return null;
    }

    private void setupChat() {
        Log.d(logTag, "setupChat()");
        lw.appendLog(logTag, "setupChat()");

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }


    //Start service in foreground with notification in bar
    public void startInForeground(){

        Bitmap icon = BitmapFactory.decodeResource(ConnectionService.this.getResources(),
                R.drawable.ic_refresh_grey600_24dp);

        //start MainActivity on notification click
        Intent notificationIntent = new Intent(ConnectionService.this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(ConnectionService.this,
                0, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        Notification notif = new Notification.Builder(ConnectionService.this)
                .setContentIntent(contentIntent)
                .setContentTitle(getResources().getText(R.string.app_name))
                .setContentText(getResources().getText(R.string.title_click_to_open))
                .setSmallIcon(R.drawable.ic_refresh_grey600_24dp)
                .setLargeIcon(icon)
                .build();

        Intent i=new Intent(this, ConnectionService.class);

        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pi=PendingIntent.getActivity(this, 0,
                i, 0);

        startForeground(237, notif);
    }

    //Send message to connected device
    public void sendMessage(String input){
        String temp="!$"+input+"*";
        temp+="\r\n";
        lw.appendLog(logTag, "Sending: " + temp);
        mChatService.write(temp.getBytes());
    }

    Boolean  isStringCorrect(String strLine){
        strLine = strLine.toLowerCase();
        return (!strLine.startsWith(";") && strLine.endsWith(";") &&
                strLine.contains("=") && strLine.contains(":") &&
                (strLine.contains("dpump") || strLine.contains("dpres") || strLine.contains("dcond") || strLine.contains("dtemp") ||
                        strLine.contains("dcur")  || strLine.contains("fpump")  || strLine.contains("ufpump")));
    }

    void readSettingsFromFile()
    {
        try {
            FileInputStream fstream = new FileInputStream(Environment.getExternalStorageDirectory().getPath() + "/settings.txt");//Read from file
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            //Read File Line By Lined
            while ((strLine = br.readLine()) != null) {//Reading file line by line
                if (isStringCorrect(strLine)) {//If string is not a comment

                    String snumber = strLine.substring(strLine.indexOf("=") + 1, strLine.indexOf(":"));//Get channel #
                    int number = Integer.valueOf(snumber);

                    String svalue = strLine.substring(strLine.indexOf(":") + 1, strLine.indexOf(";"));//Get value

                    int ivalue=0;
                    float fvalue=0f;
                    if(svalue.contains(".")){//if float - convert to float
                        fvalue = Float.parseFloat(svalue);
                    }
                    else//otherwise convert to int
                    {
                        ivalue = Integer.parseInt(svalue);
                    }

                    String setting = strLine.substring(0, strLine.indexOf("="));//Get command itself
                    RequestTypeSettings reqSetting = RequestTypeSettings.getType(setting);//Get enum type

                    switch (reqSetting) {
                        case dPump://Send dialysis pump #number value
                        {
                            if(number==1)DPUMP1FLOW=ivalue;
                            if(number==2)DPUMP2FLOW=ivalue;
                            if(number==3)DPUMP3FLOW=ivalue;
                            break;
                        }
                        case dPres://Send dialysis pressure #number value
                        {
                            if(number==1)DPRESS1MIN=fvalue;
                            if(number==2)DPRESS1MAX=fvalue;
                            if(number==3)DPRESS2MIN=fvalue;
                            if(number==4)DPRESS2MAX=fvalue;
                            if(number==5)DPRESS3MIN=fvalue;
                            if(number==6)DPRESS3MAX=fvalue;
                            break;
                        }
                        case dCond://Send dialysis conductivity #number value
                        {
                            if(number==1)DCOND1MIN=fvalue;
                            if(number==2)DCOND1MAX=fvalue;
                            break;
                        }
                        case dTemp://Send dialysis temperature #number value
                        {
                            if(number==1)DTEMP1MIN=fvalue;
                            if(number==2)DTEMP1MAX=fvalue;
                            break;
                        }
                        case dCur://Send dialysis current #number value
                        {
                            //sendMessageBytes(bSETDCUR, bnumber, bvalue);
                            //lw.appendLog(logTag, "set dCur#" + number + " to " + svalue);
                            break;
                        }
                        case fPump://Send filling pump #number value
                        {
                            if(number==1)FPUMP1FLOW=ivalue;
                            if(number==2)FPUMP2FLOW=ivalue;
                            if(number==3)FPUMP3FLOW=ivalue;
                            break;
                        }
                        case ufPump://Send unfilling pump #number value
                        {
                            if(number==1)UFPUMP1FLOW=ivalue;
                            if(number==2)UFPUMP2FLOW=ivalue;
                            if(number==3)UFPUMP3FLOW=ivalue;
                            break;
                        }
                        default:
                            break;
                    }
                }
            }

            //Close the input stream
            lw.appendLog(logTag, "Settings file read complete");
            br.close();
        }
        catch (Exception e)
        {
            lw.appendLog(logTag, e.toString()+" while reading settings file"); // handle exception
        }
    }

    void sendNotification(String currentArg){
        lw.appendLog(logTag, "NOTIF:"+currentArg, true);
        Context context = ConnectionService.this;

        Bitmap icon = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.ic_refresh_grey600_24dp);

        //start MainActivity on click
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context,
                0, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        Notification notification = new Notification.Builder(context)
                .setContentIntent(contentIntent)
                .setContentTitle(getResources().getText(R.string.app_name))
                .setContentText(currentArg)
                .setSmallIcon(R.drawable.ic_help_grey600_24dp)
                .setLargeIcon(icon)
                .setAutoCancel(true)
                .setLights(Color.WHITE, 0, 1)
                .build();

        notification.flags = notification.flags | Notification.FLAG_SHOW_LIGHTS;

        SharedPreferences sPref = getSharedPreferences(PrefActivity.APP_PREFERENCES, MODE_PRIVATE); //Load preferences
        if (sPref.getBoolean(PrefActivity.VIBRATION, false))
            notification.vibrate = new long[]{1000, 1000, 1000};


        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (sPref.getBoolean(PrefActivity.SOUND, false))
            notification.sound = soundUri;

        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFY_ID, notification);
        NOTIFY_ID++;
    }

    enum RequestType {

        STATE("STATE"),
        BATT("BATT"),
        STATUS("STATUS"),
        PARAMS("PARAMS"),
        SORBTIME("SORBTIME"),
        FUNCT("FUNCT"),
        NOTIF("NOTIF"),
        PAUSE("PAUSE"),
        A0("0"), A1("1"), A2("2"), A3("3"), A4("4"), A5("5"), A6("6"), A7("7"), A8("8"), A9("9");

        private String typeValue;

        RequestType(String type) {
            typeValue = type;
        }

        static public RequestType getType(String pType) {
            for (RequestType type : RequestType.values()) {
                if (type.getTypeValue().equals(pType)) {
                    return type;
                }
            }
            throw new RuntimeException("unknown type");
        }

        public String getTypeValue() {
            return typeValue;
        }
    }

    enum RequestTypeSettings {

        dPump("dPump"),
        dPres("dPres"),
        dCond("dCond"),
        dTemp("dTemp"),
        dCur("dCur"),
        fPump("fPump"),
        ufPump("ufPump"),
        A0("0"), A1("1"), A2("2"), A3("3"), A4("4"), A5("5"), A6("6"), A7("7"), A8("8"), A9("9");

        private String typeValue;

        RequestTypeSettings(String type) {
            typeValue = type;
        }

        static public RequestTypeSettings getType(String pType) {
            for (RequestTypeSettings type : RequestTypeSettings.values()) {
                if (type.getTypeValue().equals(pType)) {
                    return type;
                }
            }
            throw new RuntimeException("unknown type");
        }

        public String getTypeValue() {
            return typeValue;
        }
    }

}