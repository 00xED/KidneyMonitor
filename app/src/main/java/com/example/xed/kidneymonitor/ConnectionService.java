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
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

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
    public final static int TASK_SET_RESUME = 2;
    public final static int TASK_DO_PAIRING = 3;

    public final static int TASK_ARG_FILLING = 0;
    public final static int TASK_ARG_DIALYSIS = 1;
    public final static int TASK_ARG_SHUTDOWN = 2;
    public final static int TASK_ARG_DISINFECTION = 3;

    /**
     * Initialisation of LowWriter
     */
    final String logTag = "ConnectionService";

    /**
     * Default values for values
     */
    public int STATE = 9;
    public int STATUS = 9;
    public int PARAMS = 9;
    public int FUNCT = 9;
    public int SORBTIME = -1;
    public int BATT = -1;
    public int LASTCONNECTED = -1;

    //Initialising log writer
    LogWriter lw = new LogWriter();

    //Random value for notifications IDs
    private static  int NOTIFY_ID = 238;

    // Handler that sends messages to MainActivity every one second
    Handler RefreshHandler = new Handler();

    private StringBuffer mOutStringBuffer;
    private BluetoothChatService mChatService = null;
    private String mConnectedDeviceName = null;
    private String mConnectedDeviceAddress = null;

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
                    parseandexecute(readMessage);//start parser with received string
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
     * Handle messages received from main screen activity: setting status and pause/resume
     */
    BroadcastReceiver StatusReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            int task = intent.getIntExtra(PARAM_TASK, 9);
            int arg = intent.getIntExtra(PARAM_ARG, 9);

            // switch tasks for setting main screen values
            switch (task) {
                case TASK_SET_STATUS: {
                    switch (arg) {
                        case TASK_ARG_DIALYSIS: {
                            sendMessage("SET_STATE_DIALYSIS");
                            break;
                        }
                        case TASK_ARG_FILLING: {
                            sendMessage("SET_STATE_FILLING");
                            break;
                        }
                        case TASK_ARG_SHUTDOWN: {
                            sendMessage("SET_STATE_SHUTDOWN");
                            break;
                        }
                        case TASK_ARG_DISINFECTION: {
                            sendMessage("SET_STATE_DISINFECTION");
                            break;
                        }
                        default:
                            break;
                    }
                    break;
                }


                case TASK_SET_PAUSE: {
                    sendMessage("SET_PAUSE");
                    break;
                }

                case TASK_SET_RESUME: {
                    sendMessage("SET_RESUME");
                    break;
                }

                case TASK_DO_PAIRING: {
                    lw.appendLog(logTag, "Pairing with " + PrefActivity.CHOSEN_ADDRESS+"@"+PrefActivity.CHOSEN_ADDRESS);
                    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(PrefActivity.CHOSEN_ADDRESS);
                    mChatService.connect(device, true);//securely connect to chosen device
                    break;
                }

                default:
                    break;
            }

        }

    };

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

        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
                Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pi=PendingIntent.getActivity(this, 0,
                i, 0);

        startForeground(237, notif);
    }

    //Send message to connected device
    //TODO: send with \r\n
    public void sendMessage(String input){
        String temp="!$"+input+"*";
        temp+=crc7Check(temp.getBytes());
        lw.appendLog(logTag, "Sending: "+temp);
        mChatService.write(temp.getBytes());
    }


    // CRC7 checksum
    //TODO: generate correct checksum
    public static byte crc7Check(byte[] by1) {
        byte[] crc7byte = {
                0x00, 0x12, 0x24, 0x36, 0x48, 0x5a, 0x6c, 0x7e,
                0x19, 0x0b, 0x3d, 0x2f, 0x51, 0x43, 0x75, 0x67,
                0x32, 0x20, 0x16, 0x04, 0x7a, 0x68, 0x5e, 0x4c,
                0x2b, 0x39, 0x0f, 0x1d, 0x63, 0x71, 0x47, 0x55,
                0x64, 0x76, 0x40, 0x52, 0x2c, 0x3e, 0x08, 0x1a,
                0x7d, 0x6f, 0x59, 0x4b, 0x35, 0x27, 0x11, 0x03,
                0x56, 0x44, 0x72, 0x60, 0x1e, 0x0c, 0x3a, 0x28,
                0x4f, 0x5d, 0x6b, 0x79, 0x07, 0x15, 0x23, 0x31,
                0x41, 0x53, 0x65, 0x77, 0x09, 0x1b, 0x2d, 0x3f,
                0x58, 0x4a, 0x7c, 0x6e, 0x10, 0x02, 0x34, 0x26,
                0x73, 0x61, 0x57, 0x45, 0x3b, 0x29, 0x1f, 0x0d,
                0x6a, 0x78, 0x4e, 0x5c, 0x22, 0x30, 0x06, 0x14,
                0x25, 0x37, 0x01, 0x13, 0x6d, 0x7f, 0x49, 0x5b,
                0x3c, 0x2e, 0x18, 0x0a, 0x74, 0x66, 0x50, 0x42,
                0x17, 0x05, 0x33, 0x21, 0x5f, 0x4d, 0x7b, 0x69,
                0x0e, 0x1c, 0x2a, 0x38, 0x46, 0x54, 0x62, 0x70
        };

        byte result = 0;

        for (int i = 0; i < by1.length - 2; i++) {

            if (by1[i] < 0) {
                result = (byte) crc7byte[((256 + by1[i]) / 2) ^ result];
            } else {
                result = (byte) crc7byte[(by1[i] / 2) ^ result];
            }
            byte b = (byte) (by1[i] & (byte) 0x01);
            if (b == 0) {
                result ^= 0x00;
            } else {
                result ^= 0x09;

            }
        }
        return (byte) (((result * 2) + 0x01)& 0xFF);
    }

    /**
     *Send values to MainActivity every second
     */
    Runnable timedTask = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(MainActivity.BROADCAST_ACTION);

            intent.putExtra(MainActivity.PARAM_TASK, MainActivity.TASK_SET_STATE);
            intent.putExtra(MainActivity.PARAM_ARG, STATE);
            sendBroadcast(intent);

            intent.putExtra(MainActivity.PARAM_TASK, MainActivity.TASK_SET_STATUS);
            intent.putExtra(MainActivity.PARAM_ARG, STATUS);
            sendBroadcast(intent);

            intent.putExtra(MainActivity.PARAM_TASK, MainActivity.TASK_SET_PARAMS);
            intent.putExtra(MainActivity.PARAM_ARG, PARAMS);
            sendBroadcast(intent);

            intent.putExtra(MainActivity.PARAM_TASK, MainActivity.TASK_SET_FUNCT);
            intent.putExtra(MainActivity.PARAM_ARG, FUNCT);
            sendBroadcast(intent);

            intent.putExtra(MainActivity.PARAM_TASK, MainActivity.TASK_SET_SORBTIME);
            intent.putExtra(MainActivity.PARAM_ARG, SORBTIME);
            sendBroadcast(intent);

            intent.putExtra(MainActivity.PARAM_TASK, MainActivity.TASK_SET_BATT);
            intent.putExtra(MainActivity.PARAM_ARG, BATT);
            sendBroadcast(intent);

            intent.putExtra(MainActivity.PARAM_TASK, MainActivity.TASK_SET_LASTCONNECTED);
            intent.putExtra(MainActivity.PARAM_ARG, LASTCONNECTED);
            sendBroadcast(intent);
            LASTCONNECTED=-1;//Set default state to fix value on main screen

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

    /**
     * Parse received string, check checksum and set values
     * TODO: make chechsum check
     */
    private void parseandexecute(String input) {
        input = input.toUpperCase();
        if (input.indexOf("*") != -1 && input.indexOf("$") != -1) {//If string is barely correct
            String hash = input.substring(input.indexOf("*") + 1, input.indexOf("\n") - 1);//Get hash
            String commandLine = input.substring(input.indexOf("$"), input.indexOf("*") + 1);//Get commands

			/*if (commandLine.equals(checkSum(commandLine))) {//Perform check for checksum

            }
            lw.appendLog(logTag,"got checksum="+(int)checkSum(commandLine));*/
            lw.appendLog(logTag, "GOT CHECHSUM=" + crc7Check(input.getBytes()) + " and hash=" + hash);

            commandLine = commandLine.substring(input.indexOf("$"), input.indexOf("*") - 1);//Get commands
            String[] commands = commandLine.split(";");//Split line into different commands
            for (int i = 0; i < commands.length; i++) {
                String currentCommand = commands[i].substring(0, commands[i].indexOf("="));//Get command itself
                String currentArg = commands[i].substring(commands[i].indexOf("=") + 1);//Get arguments
                RequestType request = RequestType.getType(currentCommand);
                LASTCONNECTED=0;
                switch (request) {
                    case STATE: {
                        lw.appendLog(logTag, "got command STATE and " + currentArg);
                        RequestType requestArg = RequestType.getType(currentArg);
                        switch (requestArg) {
                            case A0: {
                                lw.appendLog(logTag, "setting STATE to ON");
                                STATE = 0;
                                break;
                            }
                            case A1: {
                                lw.appendLog(logTag, "setting STATE to OFF");
                                STATE = 1;
                                break;
                            }
                            default: {
                                lw.appendLog(logTag, "setting STATE to UNKNOWN");
                                STATE = 9;
                                break;
                            }
                        }
                        break;
                    }

                    case BATT: {
                        lw.appendLog(logTag, "got command BATT and " + currentArg);
                        lw.appendLog(logTag, "setting battery to " + currentArg + "%");
                        BATT = Integer.parseInt(currentArg);
                        break;
                    }

                    case STATUS: {
                        lw.appendLog(logTag, "got command STATUS and " + currentArg);
                        RequestType requestArg = RequestType.getType(currentArg);
                        switch (requestArg) {
                            case A0: {
                                lw.appendLog(logTag, "setting STATUS to FILLING");
                                STATUS = 0;
                                break;
                            }
                            case A1: {

                                lw.appendLog(logTag, "setting STATUS to DIALYSIS");
                                STATUS = 1;
                                break;
                            }
                            case A2: {
                                lw.appendLog(logTag, "setting STATUS to SHUTDOWN");
                                STATUS = 2;
                                break;
                            }
                            case A3: {
                                lw.appendLog(logTag, "setting STATUS to DISINFECTION");
                                STATUS = 3;
                                break;
                            }
                            default: {
                                lw.appendLog(logTag, "setting STATUS to UNKNOWN");
                                STATUS = 9;
                                break;
                            }
                        }
                        break;
                    }

                    case PARAMS: {
                        lw.appendLog(logTag, "got command PARAMS and " + currentArg);
                        RequestType requestArg = RequestType.getType(currentArg);
                        switch (requestArg) {
                            case A0: {
                                lw.appendLog(logTag, "setting PARAMS to NORMAL");
                                PARAMS = 0;
                                break;
                            }
                            case A1: {
                                lw.appendLog(logTag, "setting PARAMS to DANGER");
                                PARAMS = 1;
                                break;
                            }
                            default: {
                                lw.appendLog(logTag, "setting PARAMS to UNKNOWN");
                                PARAMS = 9;
                                break;
                            }
                        }
                        break;
                    }

                    case SORBTIME: {
                        lw.appendLog(logTag, "got command SORBTIME and " + currentArg);
                        SORBTIME = Integer.parseInt(currentArg);
                        break;
                    }

                    case FUNCT: {
                        lw.appendLog(logTag, "got command FUNCT and " + currentArg);
                        RequestType requestArg = RequestType.getType(currentArg);
                        switch (requestArg) {
                            case A0: {
                                lw.appendLog(logTag, "setting FUNCT to CORRECT");
                                FUNCT = 0;
                                break;
                            }
                            case A1: {
                                lw.appendLog(logTag, "setting FUNCT to FAULT");
                                FUNCT = 1;
                                break;
                            }
                            default: {
                                lw.appendLog(logTag, "setting FUNCT to UNKNOWN");
                                FUNCT = 9;
                                break;
                            }
                        }
                        break;
                    }

                    case NOTIF: {
                        Context context=ConnectionService.this;

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
                        if(sPref.getBoolean(PrefActivity.VIBRATION, false))
                            notification.vibrate= new long[] {1000, 1000, 1000};


                        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        if(sPref.getBoolean(PrefActivity.SOUND, false))
                            notification.sound=soundUri;

                        NotificationManager notificationManager = (NotificationManager) context
                                .getSystemService(Context.NOTIFICATION_SERVICE);
                        notificationManager.notify(NOTIFY_ID, notification);
                        NOTIFY_ID++;
                        break;
                    }

                    default:
                        break;
                }
            }
        }
    }


    enum RequestType {

        STATE("STATE"),
        BATT("BATT"),
        STATUS("STATUS"),
        PARAMS("PARAMS"),
        SORBTIME("SORBTIME"),
        FUNCT("FUNCT"),
        NOTIF("NOTIF"),
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

}