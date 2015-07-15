package com.example.xed.kidneymonitor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Main activity handles main screen values, images, has buttons to open all other activities,
 * button for pausing and starting procedure, buton to choose current procedure
 */

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_ENABLE_BT = 1;

    /**
     * Codes for setting main screen values
     */
    public final static int TASK_SET_STATE = 0;
    public final static int TASK_SET_STATUS = 1;
    public final static int TASK_SET_PARAMS = 2;
    public final static int TASK_SET_FUNCT = 3;
    public final static int TASK_SET_SORBTIME = 4;
    public final static int TASK_SET_BATT = 5;
    public final static int TASK_SET_LASTCONNECTED = 6;
    public final static int TASK_SET_PAUSE = 7;
    public final static int TASK_SET_SETTINGSOK = 8;

    public static int selectedProcedure = -1;

    /**
     * Settings for BroadcastReceiver
     */
    public final static String PARAM_TASK = "SetValues";
    public final static String PARAM_ARG = "arg";
    public final static String BROADCAST_ACTION = "SetValues";
    private static BroadcastReceiver broadcastReceiver;

    //Initialisation of LogWriter
    private static final String logTag = "MainActivity";
    LogWriter lw = new LogWriter();

    private SharedPreferences sPref;

    public BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Views of main screen
     */
    private TextView tvState, tvStatus, tvFunct, tvParams, tvSorbtime, tvBatt, tvLastConnected, tvCaptionStatus, tvCaptionProcedureParams;
    private ImageView ivState, ivStatus, ivFunct, ivParams, ivBatt, ivSorbtime;
    private Button btPause, btState;
    private TableRow trStatusRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * Initialising calendar for writing starting time to log
         */
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd:MM:yyyy HH:mm:ss");
        String strDate = sdf.format(c.getTime());
        lw.appendLog(logTag, "Start@" + strDate);
        lw.appendLog(logTag, "+++ ON CREATE +++");

        /**
         * Initialising TextViews for main screen
         */
        tvState = (TextView) findViewById(R.id.tv_ValueState);
        tvStatus = (TextView) findViewById(R.id.tv_ValueStatus);
        tvFunct = (TextView) findViewById(R.id.tv_ValueDeviceFunctioning);
        tvParams = (TextView) findViewById(R.id.tv_ValueProcedureParams);
        tvSorbtime = (TextView) findViewById(R.id.tv_ValueSorbentTime);
        tvBatt = (TextView) findViewById(R.id.tv_ValueBatteryCharge);
        tvLastConnected = (TextView) findViewById(R.id.tv_LastConnected);
        tvCaptionStatus = (TextView) findViewById(R.id.tv_CaptionStatus);
        tvCaptionProcedureParams = (TextView) findViewById(R.id.tv_CaptionProcedureParams);

        ivBatt = (ImageView) findViewById(R.id.iv_Battery);
        ivFunct = (ImageView) findViewById(R.id.iv_Functioning);
        ivState = (ImageView) findViewById(R.id.iv_State);
        ivStatus = (ImageView) findViewById(R.id.iv_Status);
        ivParams = (ImageView) findViewById(R.id.iv_Params);
        ivSorbtime = (ImageView) findViewById(R.id.iv_SorbentTime);
        btPause = (Button) findViewById(R.id.bt_Pause);
        btState = (Button) findViewById(R.id.bt_State);

        trStatusRow = (TableRow) findViewById(R.id.tr_StatusRow);

        /**
         * Load preferences; If saved device address is default - open preferences to find device
         */
        sPref = getSharedPreferences(PrefActivity.APP_PREFERENCES, MODE_PRIVATE); //Load preferences
        if (!sPref.contains(PrefActivity.SAVED_ADDRESS)) { //If address is default start PrefActivity
            Intent intent = new Intent(this, PrefActivity.class);
            startActivity(intent);
            Toast.makeText(this,
                    getResources().getText(R.string.title_prefs_new).toString(),
                    Toast.LENGTH_SHORT).show();
        } else Toast.makeText(this,
                getResources().getText(R.string.title_prefs_loaded).toString(),
                Toast.LENGTH_SHORT).show();

        /**
         * Initialising Bluetooth adapter; If there's none - show toast and exit
         */
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            lw.appendLog(logTag, "Bluetooth is not available");
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
        } else
            // If BT is not on, request that it be enabled.
            if (!mBluetoothAdapter.isEnabled()) {
                lw.appendLog(logTag, "Trying to start bluetooth...");
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                // Otherwise, setup the chat session
            }

        /**
         * Initialise broadcast receiver that listens for messages from ConnectionService
         * and sets main screen textviews values and images
         */
        broadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                int task = intent.getIntExtra(PARAM_TASK, -1);
                //int arg = intent.getIntExtra(PARAM_ARG, -1);
                String arg = intent.getStringExtra(PARAM_ARG);
                // switch tasks for setting main screen values
                if (ConnectionService.isServiceRunning)
                    switch (task) {
                        case TASK_SET_STATE: {
                            switch (arg) {
                                case "0": {
                                    tvState.
                                            setText(getResources().getText(R.string.value_state_on).toString());
                                    ivState.setImageResource(R.drawable.ic_on);
                                    break;
                                }

                                case "1": {
                                    tvState.
                                            setText(getResources().getText(R.string.value_state_off).toString());
                                    ivState.setImageResource(R.drawable.ic_off);
                                    break;
                                }

                                default: {
                                    tvState.
                                            setText(getResources().getText(R.string.value_state_unknown).toString());
                                    ivState.setImageResource(R.drawable.ic_help);
                                    break;
                                }
                            }
                            break;
                        }

                        case TASK_SET_STATUS: {
                            switch (arg) {
                                case "0": {
                                    tvStatus.
                                            setText(getResources().getText(R.string.value_status_filling).toString());
                                    ivStatus.setImageResource(R.drawable.ic_filling);
                                    btPause.
                                            setText(getResources().getText(R.string.title_pause_procedure).toString());
                                    btPause.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause, 0, 0, 0);
                                    break;
                                }

                                case "1": {
                                    tvStatus.
                                            setText(getResources().getText(R.string.value_status_dialysis).toString());
                                    ivStatus.setImageResource(R.drawable.ic_dialysis);
                                    btPause.
                                            setText(getResources().getText(R.string.title_pause_procedure).toString());
                                    btPause.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause, 0, 0, 0);
                                    break;
                                }

                                case "2": {
                                    tvStatus.
                                            setText(getResources().getText(R.string.value_status_shutdown).toString());
                                    ivStatus.setImageResource(R.drawable.ic_shutdown);
                                    btPause.
                                            setText(getResources().getText(R.string.title_pause_procedure).toString());
                                    btPause.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause, 0, 0, 0);
                                    break;
                                }

                                case "3": {
                                    tvStatus.
                                            setText(getResources().getText(R.string.value_status_disinfection).toString());
                                    ivStatus.setImageResource(R.drawable.ic_disinfection);
                                    btPause.
                                            setText(getResources().getText(R.string.title_pause_procedure).toString());
                                    btPause.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause, 0, 0, 0);
                                    break;
                                }

                                case "4": {
                                    tvStatus.
                                            setText(getResources().getText(R.string.value_status_ready).toString());
                                    ivStatus.setImageResource(R.drawable.ic_ready);
                                    btPause.
                                            setText(getResources().getText(R.string.title_continue_procedure).toString());
                                    btPause.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_arrow, 0, 0, 0);
                                    break;
                                }

                                case "5": {
                                    tvStatus.
                                            setText(getResources().getText(R.string.value_status_flush).toString());

                                    ivStatus.setImageResource(R.drawable.ic_flush);
                                    btPause.
                                            setText(getResources().getText(R.string.title_pause_procedure).toString());
                                    btPause.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause, 0, 0, 0);
                                    break;
                                }

                                default: {
                                    tvStatus.
                                            setText(getResources().getText(R.string.value_status_unknown).toString());
                                    ivStatus.setImageResource(R.drawable.ic_help);
                                    break;
                                }
                            }
                            break;
                        }
                        case TASK_SET_PARAMS: {
                            switch (arg) {
                                case "0": {
                                    tvParams.
                                            setText(getResources().getText(R.string.value_procedure_params_normal).toString());
                                    ivParams.setImageResource(R.drawable.ic_check_circle);
                                    break;
                                }

                                case "1": {
                                    tvParams.
                                            setText(getResources().getText(R.string.value_procedure_params_danger).toString());
                                    ivParams.setImageResource(R.drawable.ic_cross);
                                    break;
                                }

                                default: {
                                    tvParams.
                                            setText(getResources().getText(R.string.value_procedure_params_unknown).toString());
                                    ivParams.setImageResource(R.drawable.ic_help);
                                    break;
                                }
                            }
                            break;
                        }
                        case TASK_SET_FUNCT: {
                            switch (arg) {
                                case "0": {
                                    tvFunct.
                                            setText(getResources().getText(R.string.value_device_functioning_correct).toString());
                                    ivFunct.setImageResource(R.drawable.ic_check_circle);
                                    break;
                                }

                                case "1": {
                                    tvFunct.
                                            setText(getResources().getText(R.string.value_device_functioning_fault).toString());
                                    ivFunct.setImageResource(R.drawable.ic_cross);
                                    break;
                                }

                                default: {
                                    tvFunct.
                                            setText(getResources().getText(R.string.value_device_functioning_unknown).toString());
                                    ivFunct.setImageResource(R.drawable.ic_help);
                                    break;
                                }
                            }
                            break;
                        }
                        case TASK_SET_SORBTIME: {
                            SharedPreferences sPref = getSharedPreferences(PrefActivity.APP_PREFERENCES, MODE_PRIVATE); //Loading preferences
                            long remaining_time = sPref.getLong(PrefActivity.TIME_REMAINING, -1);

                            if (remaining_time == -1)//If received value is default then set to unknown
                            {
                                tvSorbtime.setText(getResources().getText(R.string.value_time_sorbent_unknown).toString());
                                ivSorbtime.setImageResource(R.drawable.ic_time_grey);
                            } else    //Convert received time in seconds to hours and minutes
                            {
                                int hours = (int) TimeUnit.MILLISECONDS.toHours(remaining_time);
                                remaining_time -= TimeUnit.HOURS.toMillis(hours);
                                int mins = (int) TimeUnit.MILLISECONDS.toMinutes(remaining_time);
                                remaining_time -= TimeUnit.MINUTES.toMillis(mins);
                                int sec = (int) TimeUnit.MILLISECONDS.toSeconds(remaining_time);
                                tvSorbtime.setText(hours +
                                        getResources().getText(R.string.value_sorbtime_hours).toString() +
                                        mins +
                                        getResources().getText(R.string.value_sorbtime_mins).toString() + sec);

                                ivSorbtime.setImageResource(R.drawable.ic_time_green);
                            }
                            break;
                        }
                        case TASK_SET_BATT: {
                            if (arg.equals("-1"))//If received value is default then set battery to unknown
                            {
                                tvBatt.setText(getResources().getText(R.string.value_battery_charge_unknown).toString());
                                ivBatt.setImageResource(R.drawable.ic_battery_unknown);
                            } else {//Otherwise set value and image
                                tvBatt.setText(arg + "%");
                                int batts = Integer.parseInt(arg);
                                if (batts >= 95)
                                    ivBatt.setImageResource(R.drawable.ic_battery_full);
                                else if (batts >= 90)
                                    ivBatt.setImageResource(R.drawable.ic_battery_90);
                                else if (batts >= 80)
                                    ivBatt.setImageResource(R.drawable.ic_battery_80);
                                else if (batts >= 60)
                                    ivBatt.setImageResource(R.drawable.ic_battery_60);
                                else if (batts >= 50)
                                    ivBatt.setImageResource(R.drawable.ic_battery_50);
                                else if (batts >= 30)
                                    ivBatt.setImageResource(R.drawable.ic_battery_30);
                                else ivBatt.setImageResource(R.drawable.ic_battery_20);
                            }
                            break;
                        }

                        case TASK_SET_LASTCONNECTED: {
                            if (!arg.equals("-1")) {
                                Calendar c = Calendar.getInstance();
                                SimpleDateFormat sdf = new SimpleDateFormat("dd:MM HH:mm");
                                String strDate = sdf.format(c.getTime());
                                tvLastConnected.setText(getResources().getText(R.string.last_connected).toString() + strDate);
                            }
                            break;
                        }

                        default:
                            break;
                    }
                if(ConnectionService.STATUS.equals(ConnectionService.STATUS_UNKNOWN)){
                    btPause.setEnabled(false);
                    btState.setEnabled(false);
                    trStatusRow.setEnabled(false);
                    ivStatus.setEnabled(false);
                }
                else{
                    btPause.setEnabled(true);
                    btState.setEnabled(true);
                    trStatusRow.setEnabled(true);
                    ivStatus.setEnabled(true);
                }
            }
        };

        //Create intent filter and register new receiver with it
        IntentFilter intFilt = new IntentFilter(BROADCAST_ACTION);
        registerReceiver(broadcastReceiver, intFilt);

        //If service is not running - start it
        if (!ConnectionService.isServiceRunning && (mBluetoothAdapter != null))
            startService(new Intent(this, ConnectionService.class));
    }

    @Override
    public void onStart() {
        super.onStart();
        lw.appendLog(logTag, "+++ ON START +++");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Deregister receiver
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == MainActivity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    public void OnClick(View v) {
        switch (v.getId()) {

            case R.id.bt_Settings://Start settings activity
            {
                Intent intent = new Intent(this, PrefActivity.class);
                startActivity(intent);
                break;
            }

            case R.id.bt_Log://Start log activity
            {
                Intent intent = new Intent(this, LogActivity.class);
                startActivity(intent);
                break;
            }

            case R.id.bt_State://Start log activity
            {
                sPref = getSharedPreferences(PrefActivity.APP_PREFERENCES, MODE_PRIVATE); //Load preferences
                if (sPref.getBoolean(PrefActivity.TESTMODE, false))
                    alertSingleChooseStatusTest();
                else
                    alertSingleChooseStatus();
                break;
            }

            case R.id.bt_Pause:// pause current procedure
            {
                sPref = getSharedPreferences(PrefActivity.APP_PREFERENCES, MODE_PRIVATE); //Load preferences
                if (sPref.getBoolean(PrefActivity.TESTMODE, false))
                    PauseConfirmationTest();
                else {
                    if (ConnectionService.STATUS.equals("-1"))
                            break;
                    else
                        PauseConfirmation();
                }
                break;
            }

            case R.id.tv_CaptionStatus: {
                sPref = getSharedPreferences(PrefActivity.APP_PREFERENCES, MODE_PRIVATE); //Load preferences
                if (sPref.getBoolean(PrefActivity.TESTMODE, false))
                    alertSingleChooseStatusTest();
                else
                    alertSingleChooseStatus();
                break;
            }

            case R.id.iv_Status: {
                sPref = getSharedPreferences(PrefActivity.APP_PREFERENCES, MODE_PRIVATE); //Load preferences
                if (sPref.getBoolean(PrefActivity.TESTMODE, false))
                    alertSingleChooseStatusTest();
                else
                    alertSingleChooseStatus();
                break;
            }

            case R.id.tv_ValueStatus: {
                sPref = getSharedPreferences(PrefActivity.APP_PREFERENCES, MODE_PRIVATE); //Load preferences
                if (sPref.getBoolean(PrefActivity.TESTMODE, false))
                    alertSingleChooseStatusTest();
                else
                    alertSingleChooseStatus();
                break;
            }

            case R.id.tr_StatusRow: {
                sPref = getSharedPreferences(PrefActivity.APP_PREFERENCES, MODE_PRIVATE); //Load preferences
                if (sPref.getBoolean(PrefActivity.TESTMODE, false))
                    alertSingleChooseStatusTest();
                else
                    alertSingleChooseStatus();
                break;
            }

            case R.id.tv_CaptionProcedureParams: {
                Intent intent = new Intent(this, ParamsActivity.class);
                startActivity(intent);
                break;
            }

            case R.id.tv_ValueProcedureParams: {
                Intent intent = new Intent(this, ParamsActivity.class);
                startActivity(intent);
                break;
            }

            case R.id.iv_Params: {
                Intent intent = new Intent(this, ParamsActivity.class);
                startActivity(intent);
                break;
            }

            case R.id.tr_ParamsRow: {
                Intent intent = new Intent(this, ParamsActivity.class);
                startActivity(intent);
                break;
            }

            case R.id.iv_TimerReset: {
                sPref = getSharedPreferences(PrefActivity.APP_PREFERENCES, MODE_PRIVATE); //Loading preferences
                SharedPreferences.Editor ed = sPref.edit(); //Setting for preference editing
                ed.remove(PrefActivity.TIME_REMAINING);
                ed.remove(PrefActivity.LAST_TICK);
                ed.commit();
            }

            default:
                break;
        }
    }

    void PauseConfirmationTest(){
        final Context context = MainActivity.this;
        AlertDialog.Builder ad = new AlertDialog.Builder(context);
        if (!ConnectionService.STATUS.equals(ConnectionService.STATUS_READY)) {
            ad.setTitle(getResources().getText(R.string.stop_confirmation).toString());
            ad.setMessage(getResources().getText(R.string.stop_confirmation).toString());
        } else {
            ad.setTitle(getResources().getText(R.string.resume_confirmation).toString());
            ad.setMessage(getResources().getText(R.string.resume_confirmation).toString());
        }
        ad.setPositiveButton(getResources().getText(R.string.yes).toString(), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                Intent intent = new Intent(ConnectionService.BROADCAST_ACTION);
                intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_PAUSE);
                sendBroadcast(intent);
            }
        });
        ad.setNegativeButton(getResources().getText(R.string.no).toString(), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {

            }
        });
        ad.setCancelable(true);
        ad.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {

            }
        });
        ad.show();
    }

    void PauseConfirmation(){
        final Context context = MainActivity.this;
        AlertDialog.Builder ad = new AlertDialog.Builder(context);
        if (!ConnectionService.STATUS.equals(ConnectionService.STATUS_READY)) {
            ad.setTitle(getResources().getText(R.string.stop_confirmation).toString());
            ad.setMessage(getResources().getText(R.string.stop_confirmation).toString());
        } else {
            ad.setTitle(getResources().getText(R.string.resume_confirmation).toString());
            ad.setMessage(getResources().getText(R.string.resume_confirmation).toString());
        }

        ad.setPositiveButton(getResources().getText(R.string.yes).toString(), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                if (!ConnectionService.STATUS.equals(ConnectionService.STATUS_READY)) {

                    Intent intent = new Intent(ConnectionService.BROADCAST_ACTION);
                    intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_PAUSE);
                    sendBroadcast(intent);
                } else {
                    Intent intent = new Intent(ConnectionService.BROADCAST_ACTION);
                    switch (ConnectionService.PREV_STATUS) {
                        case ConnectionService.STATUS_DIALYSIS: {
                            intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_STATUS);
                            intent.putExtra(ConnectionService.PARAM_ARG, ConnectionService.TASK_ARG_DIALYSIS);
                            sendBroadcast(intent);
                            break;
                        }
                        case ConnectionService.STATUS_DISINFECTION: {
                            intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_STATUS);
                            intent.putExtra(ConnectionService.PARAM_ARG, ConnectionService.TASK_ARG_DISINFECTION);
                            sendBroadcast(intent);
                            break;
                        }
                        case ConnectionService.STATUS_FILLING: {
                            intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_STATUS);
                            intent.putExtra(ConnectionService.PARAM_ARG, ConnectionService.TASK_ARG_FILLING);
                            sendBroadcast(intent);
                            break;
                        }
                        case ConnectionService.STATUS_FLUSH: {
                            intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_STATUS);
                            intent.putExtra(ConnectionService.PARAM_ARG, ConnectionService.TASK_ARG_FLUSH);
                            sendBroadcast(intent);
                            break;
                        }
                        case ConnectionService.STATUS_SHUTDOWN: {
                            intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_STATUS);
                            intent.putExtra(ConnectionService.PARAM_ARG, ConnectionService.TASK_ARG_SHUTDOWN);
                            sendBroadcast(intent);
                            break;
                        }

                        default:
                            break;
                    }

                }
            }
        });
        ad.setNegativeButton(getResources().getText(R.string.no).toString(), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {

            }
        });
        ad.setCancelable(true);
        ad.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {

            }
        });
        ad.show();
    }

    /**
     * Creates dialog, where user chooses what procedure to run
     */
    public void alertSingleChooseStatus() {
        int variant;
        String procedures[];
        int defaultSelection = 0;

        switch (ConnectionService.STATUS) {
            case ConnectionService.STATUS_DIALYSIS: {
                variant = 1;
                procedures = new String[]{getResources().getText(R.string.value_status_flush).toString()};
                break;
            }

            case ConnectionService.STATUS_FILLING: {
                variant = 2;
                procedures = new String[]{getResources().getText(R.string.value_status_dialysis).toString()};
                break;
            }

            case ConnectionService.STATUS_SHUTDOWN: {
                variant = 3;
                procedures = new String[]{getResources().getText(R.string.value_status_shutdown).toString()};
                break;
            }

            case ConnectionService.STATUS_DISINFECTION://////////////
            {
                variant = 4;
                procedures = new String[]{getResources().getText(R.string.value_status_shutdown).toString()};
                break;
            }

            case ConnectionService.STATUS_FLUSH:////////////
            {
                variant = 5;
                procedures = new String[]{getResources().getText(R.string.value_status_shutdown).toString()};
                break;
            }

            case ConnectionService.STATUS_READY: {
                switch (ConnectionService.PREV_STATUS) {
                    case ConnectionService.STATUS_DIALYSIS: {
                        variant = 1;
                        procedures = new String[]{getResources().getText(R.string.value_status_flush).toString()};
                        break;
                    }

                    case ConnectionService.STATUS_FILLING: {
                        variant = 2;
                        lw.appendLog(logTag, "now READY, can FILL", true);
                        procedures = new String[]{getResources().getText(R.string.value_status_dialysis).toString()};
                        break;
                    }

                    case ConnectionService.STATUS_SHUTDOWN: {
                        variant = 3;
                        procedures = new String[]{getResources().getText(R.string.value_status_shutdown).toString()};
                        break;
                    }

                    case ConnectionService.STATUS_DISINFECTION: {
                        variant = 4;
                        procedures = new String[]{getResources().getText(R.string.value_status_shutdown).toString()};
                        break;
                    }

                    case ConnectionService.STATUS_FLUSH: {
                        variant = 5;
                        procedures = new String[]{getResources().getText(R.string.value_status_shutdown).toString()};
                        break;
                    }

                    default: {
                        variant = 6;
                        defaultSelection = -1;
                        procedures = new String[]{getResources().getText(R.string.value_status_filling).toString(),
                                getResources().getText(R.string.value_status_dialysis).toString(),
                                getResources().getText(R.string.value_status_shutdown).toString(),
                                getResources().getText(R.string.value_status_disinfection).toString(),
                                getResources().getText(R.string.value_status_flush).toString()};
                        break;
                    }
                }
                break;
            }

            default: {
                variant = 6;
                defaultSelection = -1;
                procedures = new String[]{getResources().getText(R.string.value_status_filling).toString(),
                        getResources().getText(R.string.value_status_dialysis).toString(),
                        getResources().getText(R.string.value_status_shutdown).toString(),
                        getResources().getText(R.string.value_status_disinfection).toString(),
                        getResources().getText(R.string.value_status_flush).toString()};
                break;
            }
        }

        AlertDialog.Builder statusdialog = new AlertDialog.Builder(MainActivity.this);

        statusdialog.setTitle(getResources().getText(R.string.title_status_select).toString());// Set the dialog title

        statusdialog.setSingleChoiceItems(procedures, defaultSelection, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
            }
        });

        final int finalvariant = variant;

        statusdialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {// Set the action buttons
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // user clicked OK, so save the mSelectedItems results somewhere
                // or return them to the component that opened the dialog
                int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                switch (finalvariant) {
                    case 1://current or previous procedure is DIALYSIS - can only change to FLUSH
                    {
                        showInstructionDialog(4);
                        break;
                    }

                    case 2://current or previous procedure is FILLING - can only change to DIALYSIS
                    {
                        showInstructionDialog(1);

                        break;
                    }

                    case 3://current or previous procedure is SHUTDOWN - can only change to SHUTDOWN
                    {
                        showInstructionDialog(2);
                        break;
                    }

                    case 4://current or previous procedure is DISINFECTION - can only change to SHUTDOWN
                    {
                        showInstructionDialog(2);
                        break;
                    }

                    case 5://current or previous procedure is FLUSH - can only change to SHUTDOWN
                    {
                        showInstructionDialog(2);
                        break;
                    }

                    case 6: {//all other cases - can change to any procedure
                        if (selectedPosition >= 0 && selectedPosition <= 4)
                            showInstructionDialog(selectedPosition);
                        else
                            break;
                        break;
                    }

                    default: {
                        if (selectedPosition >= 0 && selectedPosition <= 4)
                            showInstructionDialog(selectedPosition);
                        else
                            break;
                        break;
                    }
                }
            }
        });
        statusdialog.show();
    }

    public void alertSingleChooseStatusTest() {
        int defaultSelection = selectedProcedure;
        if (selectedProcedure == 4)//if state=ready
            defaultSelection = -1;
        if (selectedProcedure == 5)//if state=flush
            defaultSelection = 4;

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        // Set the dialog title
        builder.setTitle(getResources().getText(R.string.title_status_select).toString())

                // specify the list array, the items to be selected by default (null for none),
                // and the listener through which to receive call backs when items are selected
                // again, R.array.choices were set in the resources res/values/strings.xml
                .setSingleChoiceItems(R.array.status_selection, defaultSelection, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                    }
                })
                        // Set the action buttons
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // user clicked OK, so save the mSelectedItems results somewhere
                        // or return them to the component that opened the dialog
                        int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        if (selectedPosition >= 0 && selectedPosition <= 4)
                            showInstructionDialog(selectedPosition);
                    }
                })
                .show();
    }


    public void showInstructionDialog(int variant) {
        final Dialog instructionDialog = new Dialog(MainActivity.this);
        instructionDialog.setContentView(R.layout.shutdowndialog);
        // set the custom dialog components - text, image and button
        TextView text = (TextView) instructionDialog.findViewById(R.id.text);
        ImageView image = (ImageView) instructionDialog.findViewById(R.id.image);
        Button dialogButtonOK = (Button) instructionDialog.findViewById(R.id.dialogButtonOK);

        switch (variant) {
            case 0://filling
            {
                instructionDialog.setTitle(getResources().getText(R.string.value_status_filling).toString());
                text.setText(getResources().getText(R.string.instruction_filling).toString());
                image.setImageResource(R.drawable.instruct_filling);
                // if button is clicked, close the custom dialog
                dialogButtonOK.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(ConnectionService.BROADCAST_ACTION);
                        intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_STATUS);
                        intent.putExtra(ConnectionService.PARAM_ARG, ConnectionService.TASK_ARG_FILLING);
                        sendBroadcast(intent);

                        instructionDialog.dismiss();
                    }
                });
                break;
            }

            case 1://dialysis
            {
                instructionDialog.setTitle(getResources().getText(R.string.value_status_dialysis).toString());
                text.setText(getResources().getText(R.string.instruction_dialysis).toString());
                image.setImageResource(R.drawable.instruct_dialysis);
                // if button is clicked, close the custom dialog
                dialogButtonOK.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(ConnectionService.BROADCAST_ACTION);
                        intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_STATUS);
                        intent.putExtra(ConnectionService.PARAM_ARG, ConnectionService.TASK_ARG_DIALYSIS);
                        sendBroadcast(intent);

                        instructionDialog.dismiss();
                    }
                });
                break;
            }

            case 2://shutdown
            {
                instructionDialog.setTitle(getResources().getText(R.string.value_status_shutdown).toString());
                text.setText(getResources().getText(R.string.instruction_shutdown).toString());
                image.setImageResource(R.drawable.instruct_shutdown);
                // if button is clicked, close the custom dialog
                dialogButtonOK.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(ConnectionService.BROADCAST_ACTION);
                        intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_STATUS);
                        intent.putExtra(ConnectionService.PARAM_ARG, ConnectionService.TASK_ARG_SHUTDOWN);
                        sendBroadcast(intent);

                        instructionDialog.dismiss();
                    }
                });
                break;
            }

            case 3://disinfection
            {
                instructionDialog.setTitle(getResources().getText(R.string.value_status_disinfection).toString());
                text.setText(getResources().getText(R.string.instruction_disinfection).toString());
                image.setImageResource(R.drawable.instruct_disinfection);
                // if button is clicked, close the custom dialog
                dialogButtonOK.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(ConnectionService.BROADCAST_ACTION);
                        intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_STATUS);
                        intent.putExtra(ConnectionService.PARAM_ARG, ConnectionService.TASK_ARG_DISINFECTION);
                        sendBroadcast(intent);

                        instructionDialog.dismiss();
                    }
                });
                break;
            }

            case 4://flush
            {
                instructionDialog.setTitle(getResources().getText(R.string.value_status_flush).toString());
                text.setText(getResources().getText(R.string.instruction_flush).toString());
                image.setImageResource(R.drawable.instruct_flush);
                // if button is clicked, close the custom dialog
                dialogButtonOK.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(ConnectionService.BROADCAST_ACTION);
                        intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_STATUS);
                        intent.putExtra(ConnectionService.PARAM_ARG, ConnectionService.TASK_ARG_FLUSH);
                        sendBroadcast(intent);

                        instructionDialog.dismiss();
                    }
                });
                break;
            }

            default:
                break;
        }

        Button dialogButtonCancel = (Button) instructionDialog.findViewById(R.id.dialogButtonCancel);
        // if button is clicked, close the custom dialog
        dialogButtonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                instructionDialog.dismiss();
            }
        });

        instructionDialog.show();
    }

}