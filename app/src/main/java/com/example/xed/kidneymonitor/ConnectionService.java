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
import java.io.FileInputStream;
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

    public static boolean isServiceRunning = false;
    SharedPreferences sPref;

    public final static String STATUS_FILLING = "0";
    public final static String STATUS_DIALYSIS = "1";
    public final static String STATUS_SHUTDOWN = "2";
    public final static String STATUS_DISINFECTION = "3";
    public final static String STATUS_READY = "4";
    public final static String STATUS_FLUSH = "5";
    public final static String STATUS_UNKNOWN = "-1";

    public final static String STATE_ON = "0";
    public final static String STATE_OFF = "1";
    public final static String STATE_UNKNOWN = "-1";

    public final static String PARAMS_NORMAL = "0";
    public final static String PARAMS_DANGER = "1";
    public final static String PARAMS_UNKNOWN = "-1";

    public static String FUNCT_CORRECT = "0";
    public static String FUNCT_FAULT = "1";
    public static String FUNCT_UNKNOWN = "-1";

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
    final static String logTag = "CS";
    LogWriter lw = new LogWriter();

    /**
     * Default values for values
     */
    public static String STATE = STATE_UNKNOWN;
    public static String STATUS = STATUS_UNKNOWN;
    public static String PREV_STATUS = "-2";
    public static String PARAMS = PARAMS_UNKNOWN;
    public static String FUNCT = FUNCT_UNKNOWN;
    public static String PAUSE = "-1";
    public static String SORBTIME = "-1";
    public static String BATT = "-1";
    public static String LASTCONNECTED = "-1";
    public static String SETTINGSOK = "-1";

    public String DPUMPFLOW1 = "0";
    public float fDPUMPFLOW1 = 0.0f;
    public String DPUMPFLOW2 = "0";
    public float fDPUMPFLOW2 = 0.0f;
    public String DPUMPFLOW3 = "0";
    public float fDPUMPFLOW3 = 0.0f;
    public String DUFVOLUME1 = "0.0";
    public float fDUFVOLUME1 = 0.0f;
    public String DPRESS1 = "0.0";
    public float fDPRESS1 = 0.0f;
    public String DPRESS2 = "0.0";
    public float fDPRESS2 = 0.0f;
    public String DPRESS3 = "0.0";
    public float fDPRESS3 = 0.0f;
    public String DTEMP1 = "0.0";
    public float fDTEMP1 = 0.0f;
    public String DCOND1 = "0";
    public int iDCOND1 = 0;
    public String DCUR1 = "0.0";
    public float fDCUR1 = 0.0f;
    public String DCUR2 = "0.0";
    public float fDCUR2 = 0.0f;
    public String DCUR3 = "0.0";
    public float fDCUR3 = 0.0f;
    public String DCUR4 = "0.0";
    public float fDCUR4 = 0.0f;

    public long LASTCONNECTED_MILLIS = -1;//time of last received command

    //Random value for notifications IDs
    private static int NOTIFY_ID = 238;

    // Handler that sends messages to MainActivity every one second
    Handler RefreshHandler = new Handler();

    //Handler tries to connect to device every 5s 10 times
    Handler AutoconnectHandler = new Handler();
    static int ConnectTryCount = 0;

    private StringBuffer mOutStringBuffer;
    private BluetoothChatService mChatService = null;
    private String mConnectedDeviceName = null;
    private String mConnectedDeviceAddress = null;

    /**
     * Bytes for output commands
     */
    final static byte CM_SYNC_S = (byte) 0x55;//Start of package
    final static byte CM_SYNC_E = (byte) 0xAA;//End of package

    final static byte bSENDDPRESS = (byte) 0x10;//Receiving command to set dialysis pressures
    final static byte bSENDDCOND = (byte) 0x12;//Receiving command to set dialysis conductivity
    final static byte bSENDDTEMP = (byte) 0x14;//Receiving command to set dialysis temperature
    final static byte bSENDDPUMPS = (byte) 0x16;//Receiving command to set pumps flows
    final static byte bHEARTBEAT = (byte) 0x18;//Receiving command to set pumps flows

    final static byte bPAUSE = (byte) 0x5A;//Send to pause current procedure
    final static byte bFILLING = (byte) 0x5B;//Send to set procedure to FILLING
    final static byte bDIALYSIS = (byte) 0x5C;//Send to set procedure to DIALYSIS
    final static byte bFLUSH = (byte) 0x5D;//Send to set procedure to FLUSH
    final static byte bDISINFECTION = (byte) 0x5E;//Send to set procedure to DISINFECTION
    final static byte bSHUTDOWN = (byte) 0x5F;//Send to set procedure to SHUTDOWN

    final static byte bBATT = (byte) 0xE9;//Receiving battery stats

    final static byte bSTATUS = (byte) 0xEF;//Receiving current procedure
    final static byte bSTATUS_FILLING = (byte) 0x5B;
    final static byte bSTATUS_DIALYSIS = (byte) 0x5C;
    final static byte bSTATUS_DISINFECTION = (byte) 0x5E;
    final static byte bSTATUS_SHUTDOWN = (byte) 0x5F;
    final static byte bSTATUS_READY = (byte) 0x5A;
    final static byte bSTATUS_FLUSH = (byte) 0x5D;

    final static byte bPARAMS = (byte) 0x84;//Receiving procedure params
    final static byte bPARAMS_NORM = (byte) 0x10;
    final static byte bPARAMS_DANGER = (byte) 0x11;

    final static byte bSORBTIME = (byte) 0x85;//Receiving sorbtime

    final static byte bFUNCT = (byte) 0x86;//Receiving device functioning
    final static byte bFUNCT_CORRECT = (byte) 0x10;
    final static byte bFUNCT_FAULT = (byte) 0x11;

    final static byte bDPRESS1 = (byte) 0xE0;//Receiving dialysis pressure1
    final static byte bDPRESS2 = (byte) 0xE1;//Receiving dialysis pressure2
    final static byte bDPRESS3 = (byte) 0xE2;//Receiving dialysis pressure3

    final static byte bDTEMP1 = (byte) 0xE3;//Receiving dialysis temperature1
    final static byte bDCOND1 = (byte) 0xE4;//Receiving dialysis conductivity1

    final static byte bDCUR1 = (byte) 0xE5;//Receiving dialysis current1
    final static byte bDCUR2 = (byte) 0xE6;//Receiving dialysis current2
    final static byte bDCUR3 = (byte) 0xE7;//Receiving dialysis current3
    final static byte bDCUR4 = (byte) 0xE8;//Receiving dialysis current4

    /**
     * *ERROR codes
     */
    final static byte PE_PRESS1 = (byte) 0xF0;    // Error on pressure sensor 1
    final static byte PE_PRESS2 = (byte) 0xF1;    // Error on pressure sensor 2
    final static byte PE_PRESS3 = (byte) 0xF2;    // Error on pressure sensor 3
    final static byte PE_TEMP = (byte) 0xF3;    // Error on temperature sensor
    final static byte PE_ELECTRO = (byte) 0xF4;    // Error on conductivity sensor
    final static byte PE_EDS1 = (byte) 0xF5;    // Error on electric cell 1
    final static byte PE_EDS2 = (byte) 0xF6;    // Error on electric cell 2
    final static byte PE_EDS3 = (byte) 0xF7;    // Error on electric cell 3
    final static byte PE_EDS4 = (byte) 0xF8;    // Error on electric cell 4
    final static byte PE_BATT = (byte) 0xF9;    // Error on low battery
    final static byte PE_PUMP1 = (byte) 0xFA;    // Pump 1 error, rpm low
    final static byte PE_PUMP2 = (byte) 0xFB;    // Pump 2 error, rpm low
    final static byte PE_PUMP3 = (byte) 0xFC;    // Pump 3 error, rpm low
    final static byte PE_ERROR = (byte) 0xFF;    // Pump 3 error, rpm low

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

                        case BluetoothChatService.STATE_CONNECTED: {
                            lw.appendLog(logTag, getResources().getText(R.string.connected_to).toString() + mConnectedDeviceName, true);
                            sendStringMessage("CONNECT \r\n");
                            ConnectTryCount = 0;
                            break;
                        }

                        case BluetoothChatService.STATE_CONNECTING:
                            lw.appendLog(logTag, "Connecting to: " + mConnectedDeviceName + ", try "+ConnectTryCount);
                            break;

                        case BluetoothChatService.STATE_LISTEN:
                            break;


                        case BluetoothChatService.STATE_NONE:
                            lw.appendLog(logTag, "Not connected");
                            break;
                    }
                    break;

                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    lw.appendLog(logTag, "WRITING:  " + BytestoHexString(writeBuf));
                    break;

                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    //Log.i(logTag, "READING:  " + BytestoHexString(readBuf));
                    if (msg.arg1 > 0){
                        lw.appendLog(logTag, "READING:  " + BytestoHexString(readBuf));
                        parseInBytes(readBuf);
                    }
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

    /**
     * Returns index of first found byte in array, -1 if none found
     */
    int arrayIndexOf(byte[] inp, byte what) {
        for (int i = 0; i < inp.length; i++)
            if (inp[i] == what)
                return i;
        return -1;
    }

    /**
     *Does parsing and executing of received commands
     * @param inp packet as byte array
     */
    void parseInBytes(byte[] inp) {
        int end = arrayIndexOf(inp, CM_SYNC_E);//index of last byte
        int start = end - 7;//index of first byte

        int com1Index = start + 1;//index of first command
        int com2Index = start + 2;//index of second command

        if (inp.length > 7)
            if (arrayIndexOf(inp, CM_SYNC_S) != -1 && arrayIndexOf(inp, CM_SYNC_E) != -1) {
                byte com1 = inp[com1Index];//first command
                byte com2 = inp[com2Index];//second command

                byte currentArg = inp[start + 3];//first byte of data
                byte[] databytes = new byte[]{inp[start + 6], inp[start + 5], inp[start + 4], inp[start + 3]};//data array in reverse order

                int full_data_int = ByteBuffer.wrap(databytes).getInt();//data converted to int
                float full_data_float = ByteBuffer.wrap(databytes).getFloat();//data converted to float

                LASTCONNECTED = "0";//setting flag for time of last received command
                LASTCONNECTED_MILLIS = System.currentTimeMillis();//setting time of last received command

                switch (com1) {//executing first command

                    case bBATT: {//setting battery percentage
                        lw.appendLog(logTag, getResources().getText(R.string.batt_set).toString() + full_data_int + "%", true);
                        lw.appendLog(logTag, "setting battery to " + full_data_int + "%");
                        BATT = String.valueOf(full_data_int);
                        break;
                    }

                    case bSTATUS: {//setting current procedure
                        lw.appendLog(logTag, "got command STATUS and " + currentArg);
                        sendMessageBytes(bHEARTBEAT);
                        switch (currentArg) {
                            case bSTATUS_FILLING: {
                                lw.appendLog(logTag, "setting STATUS to FILLING, previous is " + PREV_STATUS);
                                lw.appendLog(logTag, getResources().getText(R.string.starus_set).toString() +
                                                     getResources().getText(R.string.value_status_filling).toString(), true);
                                if (!STATUS.equals(STATUS_FILLING) && !STATUS.equals(STATUS_UNKNOWN))//if previous status is not FILLING and not UNKNOWN
                                    PREV_STATUS = STATUS;

                                STATUS = STATUS_FILLING;
                                break;
                            }

                            case bSTATUS_DIALYSIS: {
                                lw.appendLog(logTag, "setting STATUS to DIALYSIS, previous is " + PREV_STATUS);

                                lw.appendLog(logTag, getResources().getText(R.string.starus_set).toString() +
                                        getResources().getText(R.string.value_status_dialysis).toString(), true);
                                if (!STATUS.equals(STATUS_DIALYSIS) && !STATUS.equals(STATUS_UNKNOWN))//if previous status is not DIALYSIS and not UNKNOWN
                                    PREV_STATUS = STATUS;

                                STATUS = STATUS_DIALYSIS;
                                break;
                            }

                            case bSTATUS_SHUTDOWN: {
                                lw.appendLog(logTag, "setting STATUS to SHUTDOWN, previous is " + PREV_STATUS);
                                lw.appendLog(logTag, getResources().getText(R.string.starus_set).toString() +
                                        getResources().getText(R.string.value_status_shutdown).toString(), true);
                                if (!STATUS.equals(STATUS_SHUTDOWN) && !STATUS.equals(STATUS_UNKNOWN))//if previous status is not SHUTDOWN and not UNKNOWN
                                    PREV_STATUS = STATUS;

                                STATUS = STATUS_SHUTDOWN;
                                break;
                            }

                            case bSTATUS_DISINFECTION: {
                                lw.appendLog(logTag, "setting STATUS to DISINFECTION, previous is " + PREV_STATUS);
                                lw.appendLog(logTag, getResources().getText(R.string.starus_set).toString() +
                                        getResources().getText(R.string.value_status_disinfection).toString(), true);
                                if (!STATUS.equals(STATUS_DISINFECTION) && !STATUS.equals(STATUS_UNKNOWN))//if previous status is not DISINFECTION and not UNKNOWN
                                    PREV_STATUS = STATUS;

                                STATUS = STATUS_DISINFECTION;
                                break;
                            }

                            case bSTATUS_READY: {
                                lw.appendLog(logTag, "setting STATUS to READY, previous is " + PREV_STATUS);
                                lw.appendLog(logTag, getResources().getText(R.string.starus_set).toString() +
                                        getResources().getText(R.string.value_status_ready).toString(), true);
                                if (!STATUS.equals(STATUS_READY) && !STATUS.equals(STATUS_UNKNOWN))//if previous status is not READY and not UNKNOWN
                                    PREV_STATUS = STATUS;

                                STATUS = STATUS_READY;
                                break;
                            }

                            case bSTATUS_FLUSH: {
                                lw.appendLog(logTag, "setting STATUS to FLUSH, previous is " + PREV_STATUS);
                                lw.appendLog(logTag, getResources().getText(R.string.starus_set).toString() +
                                        getResources().getText(R.string.value_status_flush).toString(), true);
                                if (!STATUS.equals(STATUS_FLUSH) && !STATUS.equals(STATUS_UNKNOWN))//if previous status is not FLUSH and not UNKNOWN
                                    PREV_STATUS = STATUS;

                                STATUS = STATUS_FLUSH;
                                break;
                            }

                            default: {
                                lw.appendLog(logTag, "setting STATUS to UNKNOWN, previous is " + PREV_STATUS, true);
                                lw.appendLog(logTag, getResources().getText(R.string.starus_set).toString() +
                                        getResources().getText(R.string.value_status_unknown).toString(), true);
                                STATUS = STATUS_UNKNOWN;
                                break;
                            }
                        }
                        break;
                    }

                    case bPARAMS: {//NOTE: not received from device
                        lw.appendLog(logTag, "got command PARAMS and " + currentArg);
                        switch (currentArg) {
                            case bPARAMS_NORM: {
                                lw.appendLog(logTag, getResources().getText(R.string.params_set).toString() +
                                        getResources().getText(R.string.value_procedure_params_normal).toString(), true);
                                PARAMS = PARAMS_NORMAL;
                                break;
                            }

                            case bPARAMS_DANGER: {
                                lw.appendLog(logTag, getResources().getText(R.string.params_set).toString() +
                                        getResources().getText(R.string.value_procedure_params_danger).toString(), true);
                                PARAMS = PARAMS_DANGER;
                                break;
                            }

                            default: {
                                lw.appendLog(logTag, getResources().getText(R.string.params_set).toString() +
                                        getResources().getText(R.string.value_procedure_params_unknown).toString(), true);
                                PARAMS = PARAMS_UNKNOWN;
                                break;
                            }
                        }
                        break;
                    }

                    case bSORBTIME: {//NOTE: not received from device
                        lw.appendLog(logTag, "setting SORBTIME to " + currentArg, true);
                        SORBTIME = String.valueOf(full_data_int);
                        break;
                    }

                    case bFUNCT: {//NOTE: not received from device
                        lw.appendLog(logTag, "got command FUNCT and " + currentArg);
                        switch (currentArg) {
                            case bFUNCT_CORRECT: {
                                lw.appendLog(logTag, getResources().getText(R.string.funct_set).toString() +
                                        getResources().getText(R.string.value_device_functioning_correct).toString(), true);
                                FUNCT = FUNCT_CORRECT;
                                break;
                            }

                            case bFUNCT_FAULT: {
                                lw.appendLog(logTag, getResources().getText(R.string.funct_set).toString() +
                                        getResources().getText(R.string.value_device_functioning_fault).toString(), true);
                                FUNCT = FUNCT_FAULT;
                                break;
                            }

                            default: {
                                lw.appendLog(logTag, getResources().getText(R.string.funct_set).toString() +
                                        getResources().getText(R.string.value_device_functioning_unknown).toString(), true);
                                FUNCT = FUNCT_UNKNOWN;
                                break;
                            }
                        }
                        break;
                    }

                    case bDPRESS1: {//setting first pressure value
                        fDPRESS1 = full_data_float * 51.715f;
                        DPRESS1 = String.valueOf(fDPRESS1);//converting to mmHg and string
                        lw.appendLog(logTag, "setting DPRESS1 to " + DPRESS1);
                        break;
                    }

                    case bDPRESS2: {//setting second pressure value
                        fDPRESS2 = full_data_float * 51.715f;
                        DPRESS2 = String.valueOf(fDPRESS2);//converting to mmHg and string
                        lw.appendLog(logTag, "setting DPRESS2 to " + DPRESS2);
                        break;
                    }

                    case bDPRESS3: {//setting third pressure value
                        fDPRESS3 = full_data_float * 51.715f;
                        DPRESS3 = String.valueOf(fDPRESS3);//converting to mmHg and string
                        lw.appendLog(logTag, "setting DPRESS3 to " + DPRESS3);
                        break;
                    }

                    case bDTEMP1: {//setting temperature value
                        fDTEMP1 = full_data_int / 10.0f;//converting to Celsius degrees and string
                        DTEMP1 = String.valueOf(fDTEMP1);
                        lw.appendLog(logTag, "setting DTEMP1 to " + DTEMP1);
                        break;
                    }

                    case bDCOND1: {////setting conductivity value
                        iDCOND1 = full_data_int;
                        DCOND1 = String.valueOf(iDCOND1);
                        lw.appendLog(logTag, "setting DCOND1 to " + DCOND1);
                        break;
                    }

                    case bDCUR1: {//setting first electric current value
                        fDCUR1 = full_data_float * 1000;
                        DCUR1 = String.valueOf(fDCUR1);
                        lw.appendLog(logTag, "setting DCUR1 to " + DCUR1);
                        break;
                    }

                    case bDCUR2: {//setting second electric current value
                        fDCUR2 = full_data_float * 1000;
                        DCUR2 = String.valueOf(fDCUR2);
                        lw.appendLog(logTag, "setting DCUR2 to " + DCUR2);
                        break;
                    }

                    case bDCUR3: {//setting third electric current value
                        fDCUR3 = full_data_float * 1000;
                        DCUR3 = String.valueOf(fDCUR3);
                        lw.appendLog(logTag, "setting DCUR3 to " + DCUR3);
                        break;
                    }

                    case bDCUR4: {//setting fourth electric current value
                        fDCUR4 = full_data_float * 1000;
                        DCUR4 = String.valueOf(fDCUR4);
                        lw.appendLog(logTag, "setting DCUR4 to " + DCUR4);
                        break;
                    }

                    case bSENDDPUMPS: {//sending pumps flows
                        switch (com2){
                            case (byte)0x01:{
                                lw.appendLog(logTag, "send FPUMP1FLOW");
                                sendMessageBytes((byte) (bSENDDPUMPS + (byte) 0x01), (byte) 0x01, intTo4byte(FPUMP1FLOW));//first filling pump
                                break;
                            }

                            case (byte)0x02:{
                                lw.appendLog(logTag, "send DPUMP1FLOW");
                                sendMessageBytes((byte) (bSENDDPUMPS + (byte) 0x01), (byte) 0x02, intTo4byte(DPUMP1FLOW));//first dialysis pump
                                break;
                            }

                            case (byte)0x03:{
                                lw.appendLog(logTag, "send UFPUMP1FLOW", true);
                                sendMessageBytes((byte) (bSENDDPUMPS + (byte) 0x01), (byte) 0x03, intTo4byte(UFPUMP1FLOW));//first unfilling pump
                                break;
                            }

                            case (byte)0x11:{
                                lw.appendLog(logTag, "send FPUMP2FLOW", true);
                                sendMessageBytes((byte) (bSENDDPUMPS + (byte) 0x01), (byte) 0x11, intTo4byte(FPUMP2FLOW));//first filling pump
                                break;
                            }

                            case (byte)0x12:{
                                lw.appendLog(logTag, "send DPUMP2FLOW", true);
                                sendMessageBytes((byte) (bSENDDPUMPS + (byte) 0x01), (byte) 0x12, intTo4byte(DPUMP2FLOW));//first dialysis pump
                                break;
                            }

                            case (byte)0x13:{
                                lw.appendLog(logTag, "send UFPUMP2FLOW", true);
                                sendMessageBytes((byte) (bSENDDPUMPS + (byte) 0x01), (byte) 0x13, intTo4byte(UFPUMP2FLOW));//first unfilling pump
                                break;
                            }

                            case (byte)0x21:{
                                lw.appendLog(logTag, "send FPUMP3FLOW", true);
                                sendMessageBytes((byte) (bSENDDPUMPS + (byte) 0x01), (byte) 0x21, intTo4byte(FPUMP3FLOW));//first filling pump
                                break;
                            }

                            case (byte)0x22:{
                                lw.appendLog(logTag, "send DPUMP3FLOW", true);
                                sendMessageBytes((byte) (bSENDDPUMPS + (byte) 0x01), (byte) 0x22, intTo4byte(DPUMP3FLOW));//first dialysis pump
                                break;
                            }

                            case (byte)0x23:{
                                lw.appendLog(logTag, "send UFPUMP3FLOW", true);
                                sendMessageBytes((byte) (bSENDDPUMPS + (byte) 0x01), (byte) 0x23, intTo4byte(UFPUMP3FLOW));//first unfilling pump
                                break;
                            }

                            default:
                                break;
                        }
                        break;
                    }

                    case bSENDDPRESS: {//sending values for pressures ranges
                        switch (com2){
                            case (byte)0x01:{
                                lw.appendLog(logTag, "send DPRESS1MIN");
                                sendMessageBytes((byte) (bSENDDPRESS + (byte) 0x01), (byte) 0x01, floatTo4byte(DPRESS1MIN));//first min value
                                break;
                            }

                            case (byte)0x02:{
                                lw.appendLog(logTag, "send DPRESS1MAX");
                                sendMessageBytes((byte) (bSENDDPRESS + (byte) 0x01), (byte) 0x02, floatTo4byte(DPRESS1MAX));//first max value
                                break;
                            }

                            case (byte)0x11:{
                                lw.appendLog(logTag, "send DPRESS2MIN");
                                sendMessageBytes((byte) (bSENDDPRESS + (byte) 0x01), (byte) 0x11, floatTo4byte(DPRESS2MIN));//second min value
                                break;
                            }

                            case (byte)0x12:{
                                lw.appendLog(logTag, "send DPRESS2MAX");
                                sendMessageBytes((byte) (bSENDDPRESS + (byte) 0x01), (byte) 0x12, floatTo4byte(DPRESS2MAX));//second max value
                                break;
                            }

                            case (byte)0x21:{
                                lw.appendLog(logTag, "send DPRESS3MIN");
                                sendMessageBytes((byte) (bSENDDPRESS + (byte) 0x01), (byte) 0x21, floatTo4byte(DPRESS3MIN));//third min value
                                break;
                            }

                            case (byte)0x22:{
                                lw.appendLog(logTag, "send DPRESS3MAX");
                                sendMessageBytes((byte) (bSENDDPRESS + (byte) 0x01), (byte) 0x22, floatTo4byte(DPRESS3MAX));//third max value
                                break;
                            }

                            default:
                                break;
                        }
                        break;
                    }

                    case bSENDDTEMP: {//sending values for temperature range
                        switch (com2){
                            case (byte)0x01:{
                                lw.appendLog(logTag, "send DTEMP1MIN ");
                                sendMessageBytes((byte) (bSENDDTEMP + (byte) 0x01), (byte) 0x01, floatTo4byte(DTEMP1MIN));//min temp
                                break;
                            }

                            case (byte)0x02:{
                                lw.appendLog(logTag, "send DTEMP1MAX ");
                                sendMessageBytes((byte) (bSENDDTEMP + (byte) 0x01), (byte) 0x02, floatTo4byte(DTEMP1MAX));//max temp
                                break;
                            }

                            default:
                                break;
                        }
                        break;
                    }

                    case bSENDDCOND: {//sending values for conductivity range
                        switch (com2){
                            case (byte)0x01:{
                                lw.appendLog(logTag, "send DCOND1MIN ");
                                sendMessageBytes((byte) (bSENDDCOND + (byte) 0x01), (byte) 0x01, floatTo4byte(DCOND1MIN));
                                break;
                            }

                            case (byte)0x02:{
                                lw.appendLog(logTag, "send DCOND1MAX ");
                                sendMessageBytes((byte) (bSENDDCOND + (byte) 0x01), (byte) 0x02, floatTo4byte(DCOND1MAX));
                                break;
                            }

                            default:
                                break;
                        }

                        break;
                    }

                    case PE_PRESS1: {//receiving error
                        processError(getResources().getText(R.string.error_press).toString() + "1");
                        break;
                    }

                    case PE_PRESS2: {//receiving error
                        processError(getResources().getText(R.string.error_press).toString() + "2");
                        break;
                    }

                    case PE_PRESS3: {//receiving error
                        processError(getResources().getText(R.string.error_press).toString() + "3");
                        break;
                    }

                    case PE_TEMP: {//receiving error
                        processError(getResources().getText(R.string.error_temp).toString());
                        break;
                    }

                    case PE_ELECTRO: {//receiving error
                        processError(getResources().getText(R.string.error_electro).toString());
                        break;
                    }

                    case PE_EDS1: {//receiving error
                        processError(getResources().getText(R.string.error_eds).toString() + "1");
                        break;
                    }

                    case PE_EDS2: {//receiving error
                        processError(getResources().getText(R.string.error_eds).toString() + "2");
                        break;
                    }

                    case PE_EDS3: {//receiving error
                        processError(getResources().getText(R.string.error_eds).toString() + "3");
                        break;
                    }

                    case PE_EDS4: {//receiving error
                        processError(getResources().getText(R.string.error_eds).toString() + "4");
                        break;
                    }

                    case PE_BATT: {//receiving error
                        processError(getResources().getText(R.string.error_batt).toString());
                        break;
                    }

                    case PE_PUMP1: {//receiving error
                        processError(getResources().getText(R.string.error_eds).toString() + "1");
                        break;
                    }

                    case PE_PUMP2: {//receiving error
                        processError(getResources().getText(R.string.error_eds).toString() + "2");
                        break;
                    }

                    case PE_PUMP3: {//receiving error
                        processError(getResources().getText(R.string.error_eds).toString() + "3");
                        break;
                    }

                    case PE_ERROR: {//receiving error
                        processError(getResources().getText(R.string.error_unknown).toString());
                        break;
                    }

                    default:
                        break;
                }


                switch (STATUS) {//checking for values being in given ranges for given procedure
                    case STATUS_DIALYSIS: {
                        if ((fDPRESS1 >= DPRESS1MIN) && (fDPRESS1 <= DPRESS1MAX) &&
                                (fDPRESS2 >= DPRESS2MIN) && (fDPRESS2 <= DPRESS2MAX) &&
                                (fDPRESS3 >= DPRESS3MIN) && (fDPRESS3 <= DPRESS3MAX) &&
                                (fDTEMP1 >= DTEMP1MIN) && (fDTEMP1 <= DTEMP1MAX) &&
                                (iDCOND1 >= DCOND1MIN) && (iDCOND1 <= DCOND1MAX)) {
                            FUNCT = FUNCT_CORRECT;
                            PARAMS = PARAMS_NORMAL;
                        } else {
                            FUNCT = FUNCT_FAULT;
                            PARAMS = PARAMS_DANGER;
                        }
                        break;
                    }

                    case STATUS_FILLING: {
                        if ((fDPRESS1 >= DPRESS1MIN) && (fDPRESS1 <= DPRESS1MAX) &&
                                (fDPRESS2 >= DPRESS2MIN) && (fDPRESS2 <= DPRESS2MAX) &&
                                (fDPRESS3 >= DPRESS3MIN) && (fDPRESS3 <= DPRESS3MAX)) {
                            FUNCT = FUNCT_CORRECT;
                            PARAMS = PARAMS_NORMAL;
                        } else {
                            FUNCT = FUNCT_FAULT;
                            PARAMS = PARAMS_DANGER;
                        }
                        break;
                    }

                    case STATUS_FLUSH: {
                        if ((fDPRESS1 >= DPRESS1MIN) && (fDPRESS1 <= DPRESS1MAX) &&
                                (fDPRESS2 >= DPRESS2MIN) && (fDPRESS2 <= DPRESS2MAX) &&
                                (fDPRESS3 >= DPRESS3MIN) && (fDPRESS3 <= DPRESS3MAX)) {
                            FUNCT = FUNCT_CORRECT;
                            PARAMS = PARAMS_NORMAL;
                        } else {
                            FUNCT = FUNCT_FAULT;
                            PARAMS = PARAMS_DANGER;
                        }
                        break;
                    }

                    default: {
                        FUNCT = FUNCT_CORRECT;
                        PARAMS = PARAMS_NORMAL;

                        break;
                    }
                }

                if (!STATUS.equals(STATUS_SHUTDOWN))//If status is not SHUTDOWN, then STATE is ON
                    STATE = STATE_ON;
                else {
                    STATE = STATE_OFF;//Otherwise, STATE is OFF
                    lw.appendLog(logTag, getResources().getText(R.string.value_status_shutdown).toString(), true);
                }
            }
    }

    void processError(String msg){
        sendNotification(msg);
        lw.appendLog(logTag, msg, true);
        FUNCT = FUNCT_FAULT;
        PARAMS = PARAMS_DANGER;
    }

    /**
     * convert float to 4 byte array
     */
    byte[] floatTo4byte(float fvalue) {//
        return ByteBuffer.allocate(4).putFloat(fvalue).array();
    }

    /**
     * convert int to 4 byte array
     */
    byte[] intTo4byte(int ivalue) {
        return ByteBuffer.allocate(4).putInt(ivalue).array();
    }

    /**
     * Handle messages received from main screen activity: setting status and pause/resume,
     * do pairing  with saved address
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
                            sendMessageBytes(bSTATUS, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, bDIALYSIS});
                            lw.appendLog(logTag, getResources().getText(R.string.user_switched_to).toString() +
                                                 getResources().getText(R.string.value_status_dialysis).toString(), true);
                            break;
                        }

                        case TASK_ARG_FILLING: {
                            sendMessageBytes(bSTATUS, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, bFILLING});
                            lw.appendLog(logTag, getResources().getText(R.string.user_switched_to).toString() +
                                    getResources().getText(R.string.value_status_filling).toString(), true);
                            break;
                        }

                        case TASK_ARG_SHUTDOWN: {
                            sendMessageBytes(bSTATUS, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, bSHUTDOWN});
                            lw.appendLog(logTag, getResources().getText(R.string.user_switched_to).toString() +
                                    getResources().getText(R.string.value_status_shutdown).toString(), true);
                            break;
                        }

                        case TASK_ARG_DISINFECTION: {
                            sendMessageBytes(bSTATUS, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, bDISINFECTION});
                            lw.appendLog(logTag, getResources().getText(R.string.user_switched_to).toString() +
                                    getResources().getText(R.string.value_status_disinfection).toString(), true);
                            break;
                        }

                        case TASK_ARG_FLUSH: {
                            sendMessageBytes(bSTATUS, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, bFLUSH});
                            lw.appendLog(logTag, getResources().getText(R.string.user_switched_to).toString() +
                                    getResources().getText(R.string.value_status_flush).toString(), true);
                            break;
                        }

                        default:
                            break;
                    }
                    break;
                }

                case TASK_SET_PAUSE: {
                    sendMessageBytes(bSTATUS, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, bPAUSE});
                    lw.appendLog(logTag, getResources().getText(R.string.user_switched_to).toString() +
                            getResources().getText(R.string.title_pause_procedure).toString(), true);
                    break;
                }

                case TASK_DO_PAIRING: {
                    String address = sPref.getString(PrefActivity.SAVED_ADDRESS, "00:00:00:00:00:00");
                    if (!"00:00:00:00:00:00".equals(address)) {
                        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                        mChatService.connect(device, true);//securely connect to chosen device
                        lw.appendLog(logTag, "Pairing with " + PrefActivity.CHOSEN_NAME + '@' + PrefActivity.CHOSEN_ADDRESS);
                    }
                    break;
                }

                default:
                    break;
            }
        }
    };


    /**
     * Send packet with only first command
     *
     * @param com1 first command
     */
    void sendMessageBytes(byte com1) {
        byte[] outp = new byte[]{CM_SYNC_S, com1, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, CM_SYNC_E};
        mChatService.write(outp);
    }

    /**
     * Send packet with first command and data
     *
     * @param com1 first command
     * @param data data array
     */
    void sendMessageBytes(byte com1, byte[] data) {
        byte[] outp = new byte[]{CM_SYNC_S, com1, (byte) 0x00, data[3], data[2], data[1], data[0], CM_SYNC_E};
        mChatService.write(outp);
    }

    /**
     * Send packet with only two commands
     *
     * @param com1 first command
     * @param com2 second command
     */
    void sendMessageBytes(byte com1, byte com2) {
        byte[] outp = new byte[]{CM_SYNC_S, com1, com2, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, CM_SYNC_E};
        mChatService.write(outp);
    }

    /**
     * Send packet with two commands and data
     *
     * @param com1 first command
     * @param com2 second command
     * @param data data array
     */
    void sendMessageBytes(byte com1, byte com2, byte[] data) {
        byte[] outp = new byte[]{CM_SYNC_S, com1, com2, data[3], data[2], data[1], data[0], CM_SYNC_E};
        mChatService.write(outp);
    }

    /**
     * Send values to MainActivity and ParamsActivity every second
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

            intentValues.putExtra(MainActivity.PARAM_TASK, MainActivity.TASK_SET_LASTCONNECTED);
            intentValues.putExtra(MainActivity.PARAM_ARG, LASTCONNECTED);
            sendBroadcast(intentValues);

            LASTCONNECTED = "-1";//Set default state to fix value on main screen
            if ((System.currentTimeMillis() - LASTCONNECTED_MILLIS) > 10000)//if last command was received more than 10 seconds ago - reset all values
            {
                STATE = STATE_UNKNOWN;
                STATUS = STATUS_UNKNOWN;
                PARAMS = PARAMS_UNKNOWN;
                FUNCT = FUNCT_UNKNOWN;
                BATT = "-1";

            }

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

            if (!STATE.equals(STATE_UNKNOWN) && STATUS.equals(STATUS_DIALYSIS)) {
                SharedPreferences.Editor ed = sPref.edit(); //Setting for preference editing
                long remaining_time = sPref.getLong(PrefActivity.TIME_REMAINING, 43200000);//12hours in ms
                long tick = sPref.getLong(PrefActivity.LAST_TICK, System.currentTimeMillis());
                if (STATE.equals(STATE_OFF))
                    tick = System.currentTimeMillis();

                ed.putLong(PrefActivity.TIME_REMAINING, remaining_time - (System.currentTimeMillis() - tick));
                ed.putLong(PrefActivity.LAST_TICK, System.currentTimeMillis());
                ed.commit();
            }

            RefreshHandler.postDelayed(timedTask, 1000);//refresh after one second
        }
    };

    Runnable AutoconnectTask = new Runnable() {
        @Override
        public void run() {
            if(mChatService.getState()==BluetoothChatService.STATE_LISTEN &&
                    STATUS.equals(STATUS_UNKNOWN) &&
                    sPref.getBoolean(PrefActivity.AUTOCONNECT, false) &&
                    ConnectTryCount<=10){//if waiting for connection - try to connect to saved device
                       String address = sPref.getString(PrefActivity.SAVED_ADDRESS, "00:00:00:00:00:00");
                       if(!"00:00:00:00:00:00".equals(address)){
                            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                            mChatService.connect(device, true);//securely connect to chosen device
                            //lw.appendLog(logTag, "Connecting to " + PrefActivity.CHOSEN_NAME+'@'+PrefActivity.CHOSEN_ADDRESS, true);
                           ConnectTryCount++;
                    }
            }
            if(ConnectTryCount>10 && ConnectTryCount<20)
            {
                SharedPreferences.Editor ed = sPref.edit(); //Setting for preference editing
                ed.putBoolean(PrefActivity.AUTOCONNECT, false);
                ed.apply();
                sendNotification("FAILED TO AUTOCONNECT");
                ConnectTryCount=20;
            }
            AutoconnectHandler.postDelayed(AutoconnectTask, 5000);//refresh after one second
        }
    };


    public void onCreate() {
        super.onCreate();
        Log.d(logTag, "onCreate");
        lw.appendLog(logTag, "onCreate");
        sPref = getSharedPreferences(PrefActivity.APP_PREFERENCES, MODE_PRIVATE); //Load preferences;
        if (mChatService == null) setupChat();
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }

        readSettingsFromFile();

        //Register receiver with filter to receive messages from MainActivity
        IntentFilter intFilt = new IntentFilter(BROADCAST_ACTION);
        registerReceiver(StatusReceiver, intFilt);

        //Register receiver with filter to handle situation when choosen device disconnected
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(DisconnectReceiver, filter);

        RefreshHandler.post(timedTask);
        AutoconnectHandler.post(AutoconnectTask);
    }

    /**
     * * Receiver watches for bluetooth disconnect, and restarts bluetoothchatservice
     */
    private final BroadcastReceiver DisconnectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
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
        isServiceRunning = true;

        if (sPref.getBoolean(PrefActivity.IS_FOREGROUND, false))
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
        isServiceRunning = false;
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


    /**
     * Start service in foreground with notification in bar
     */
    public void startInForeground() {

        Bitmap icon = BitmapFactory.decodeResource(ConnectionService.this.getResources(),
                R.mipmap.ic_launcher);

        //start MainActivity on notification click
        Intent notificationIntent = new Intent(ConnectionService.this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(ConnectionService.this,
                0, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        Notification notif = new Notification.Builder(ConnectionService.this)
                .setContentIntent(contentIntent)
                .setContentTitle(getResources().getText(R.string.app_name))
                .setContentText(getResources().getText(R.string.title_click_to_open))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(icon)
                .build();

        Intent i = new Intent(this, ConnectionService.class);

        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pi = PendingIntent.getActivity(this, 0,
                i, 0);

        startForeground(237, notif);
    }

    /**
     * Send message to connected device
     */
    public void sendStringMessage(String input) {
        lw.appendLog(logTag, "Sending: " + input);
        mChatService.write(input.getBytes());
    }

    /**
     * Check if string in settings file is in desired format
     *
     * @param strLine string to check
     * @return true if string is correct
     */
    Boolean isStringCorrect(String strLine) {
        strLine = strLine.toLowerCase();
        return (!strLine.startsWith(";") && strLine.endsWith(";") &&
                strLine.contains("=") && strLine.contains(":") &&
                (strLine.contains("dpump") || strLine.contains("dpres") || strLine.contains("dcond") || strLine.contains("dtemp") ||
                        strLine.contains("dcur") || strLine.contains("fpump") || strLine.contains("ufpump")));
    }

    /**
     * Read setting from file sdcard/kidneymonitor/settings.txt
     */
    void readSettingsFromFile() {
        try {
            FileInputStream fstream = new FileInputStream(Environment.getExternalStorageDirectory().getPath() + "/kidneymonitor/settings.txt");//Read from file
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            //Read File Line By Lined
            while ((strLine = br.readLine()) != null) {//Reading file line by line
                if (isStringCorrect(strLine)) {//If string is not a comment

                    String snumber = strLine.substring(strLine.indexOf("=") + 1, strLine.indexOf(":"));//Get channel #
                    int number = Integer.valueOf(snumber);

                    String svalue = strLine.substring(strLine.indexOf(":") + 1, strLine.indexOf(";"));//Get value

                    int ivalue = 0;
                    float fvalue = 0f;
                    if (svalue.contains(".")) //if float - convert to float
                        fvalue = Float.parseFloat(svalue);
                    else//otherwise convert to int
                        ivalue = Integer.parseInt(svalue);

                    String setting = strLine.substring(0, strLine.indexOf("="));//Get command itself
                    RequestTypeSettings reqSetting = RequestTypeSettings.getType(setting);//Get enum type

                    switch (reqSetting) {
                        case dPump: {//Send dialysis pump #number value
                            if (number == 1) DPUMP1FLOW = ivalue;
                            if (number == 2) DPUMP2FLOW = ivalue;
                            if (number == 3) DPUMP3FLOW = ivalue;
                            break;
                        }

                        case dPres: {//Send dialysis pressure #number value
                            if (number == 1) DPRESS1MIN = fvalue;
                            if (number == 2) DPRESS1MAX = fvalue;
                            if (number == 3) DPRESS2MIN = fvalue;
                            if (number == 4) DPRESS2MAX = fvalue;
                            if (number == 5) DPRESS3MIN = fvalue;
                            if (number == 6) DPRESS3MAX = fvalue;
                            break;
                        }

                        case dCond: {//Send dialysis conductivity #number value
                            if (number == 1) DCOND1MIN = fvalue;
                            if (number == 2) DCOND1MAX = fvalue;
                            break;
                        }

                        case dTemp: {//Send dialysis temperature #number value
                            if (number == 1) DTEMP1MIN = fvalue;
                            if (number == 2) DTEMP1MAX = fvalue;
                            break;
                        }
                        case dCur: {//Send dialysis current #number value
                            //sendMessageBytes(bSETDCUR, bnumber, bvalue);
                            //lw.appendLog(logTag, "set dCur#" + number + " to " + svalue);
                            break;
                        }
                        case fPump: {//Send filling pump #number value
                            if (number == 1) FPUMP1FLOW = ivalue;
                            if (number == 2) FPUMP2FLOW = ivalue;
                            if (number == 3) FPUMP3FLOW = ivalue;
                            break;
                        }
                        case ufPump: {//Send unfilling pump #number value
                            if (number == 1) UFPUMP1FLOW = ivalue;
                            if (number == 2) UFPUMP2FLOW = ivalue;
                            if (number == 3) UFPUMP3FLOW = ivalue;
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
        } catch (Exception e) {
            lw.appendLog(logTag, e.toString() + " while reading settings file"); // handle exception
        }
    }

    /**
     * Send notification to user through notification bar
     *
     * @param currentArg String to show
     */
    void sendNotification(String currentArg) {
        lw.appendLog(logTag, "NOTIF:" + currentArg);

        Context context = ConnectionService.this;
        Bitmap icon = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.ic_cross);

        //start MainActivity on click
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context,
                0, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        Notification notification = new Notification.Builder(context)
                .setContentIntent(contentIntent)
                .setContentTitle(getResources().getText(R.string.app_name))
                .setContentText(currentArg)
                .setSmallIcon(R.drawable.ic_cross)
                .setLargeIcon(icon)
                .setAutoCancel(true)
                .setLights(Color.WHITE, 0, 1)
                .build();

        notification.flags = notification.flags | Notification.FLAG_SHOW_LIGHTS;

        if (sPref.getBoolean(PrefActivity.VIBRATION, false))
            notification.vibrate = new long[]{1000, 1000, 1000};


        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (sPref.getBoolean(PrefActivity.SOUND, false))
            notification.sound = soundUri;

        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFY_ID, notification);
        NOTIFY_ID++;
        notificationManager.cancel(NOTIFY_ID--);
    }

    /**
     * Converts array of bytes to hex string
     *
     * @param bytes array to convert
     * @return hex string
     */
    public static String BytestoHexString(byte[] bytes) {
        char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v / 16];
            hexChars[j * 2 + 1] = hexArray[v % 16];
        }
        return new String(hexChars);
    }

    /**
     * Enumerator for reading settings file
     */
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