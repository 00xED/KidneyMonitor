package com.example.xed.kidneymonitor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
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

public class MainActivity extends Activity {

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
    private TextView tvState, tvStatus, tvFunct, tvParams, tvSorbtime, tvBatt, tvLastConnected,
                     tvCaptionStatus, tvCaptionProcedureParams, tvCaptionState,
                     tvCaptionDeviceFunctioning, tvCaptionSorbentTime, tvCaptionBatteryCharge;
    private ImageView ivState, ivStatus, ivFunct, ivParams, ivBatt, ivSorbtime;
    private Button btPause, btState, btLog, btNotif;
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

        Typeface tfPlayBold = Typeface.createFromAsset(getAssets(), "fonts/Play-Bold.ttf");

        /**
         * Initialising TextViews for main screen
         */
        tvState = (TextView) findViewById(R.id.tv_ValueState);
        tvState.setTypeface(tfPlayBold);
        tvStatus = (TextView) findViewById(R.id.tv_ValueStatus);
        tvStatus.setTypeface(tfPlayBold);
        tvFunct = (TextView) findViewById(R.id.tv_ValueDeviceFunctioning);
        tvFunct.setTypeface(tfPlayBold);
        tvParams = (TextView) findViewById(R.id.tv_ValueProcedureParams);
        tvParams.setTypeface(tfPlayBold);
        tvSorbtime = (TextView) findViewById(R.id.tv_ValueSorbentTime);
        tvSorbtime.setTypeface(tfPlayBold);
        tvBatt = (TextView) findViewById(R.id.tv_ValueBatteryCharge);
        tvBatt.setTypeface(tfPlayBold);
        tvLastConnected = (TextView) findViewById(R.id.tv_LastConnected);
        tvLastConnected.setPaintFlags(tvLastConnected.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvLastConnected.setTypeface(tfPlayBold);
        tvCaptionStatus = (TextView) findViewById(R.id.tv_CaptionStatus);
        tvCaptionStatus.setTypeface(tfPlayBold);
        tvCaptionProcedureParams = (TextView) findViewById(R.id.tv_CaptionProcedureParams);
        tvCaptionProcedureParams.setTypeface(tfPlayBold);
        tvCaptionState = (TextView) findViewById(R.id.tv_CaptionState);
        tvCaptionState.setTypeface(tfPlayBold);
        tvCaptionDeviceFunctioning = (TextView) findViewById(R.id.tv_CaptionDeviceFunctioning);
        tvCaptionDeviceFunctioning.setTypeface(tfPlayBold);
        tvCaptionSorbentTime = (TextView) findViewById(R.id.tv_CaptionSorbentTime);
        tvCaptionSorbentTime.setTypeface(tfPlayBold);
        tvCaptionBatteryCharge = (TextView) findViewById(R.id.tv_CaptionBatteryCharge);
        tvCaptionBatteryCharge.setTypeface(tfPlayBold);

        ivBatt = (ImageView) findViewById(R.id.iv_Battery);
        ivFunct = (ImageView) findViewById(R.id.iv_Functioning);
        ivState = (ImageView) findViewById(R.id.iv_State);
        ivStatus = (ImageView) findViewById(R.id.iv_Status);
        ivParams = (ImageView) findViewById(R.id.iv_Params);
        ivSorbtime = (ImageView) findViewById(R.id.iv_SorbentTime);
        btPause = (Button) findViewById(R.id.bt_Pause);
        btPause.setTypeface(tfPlayBold);
        btState = (Button) findViewById(R.id.bt_State);
        btState.setTypeface(tfPlayBold);
        btLog = (Button) findViewById(R.id.bt_Log);
        btLog.setTypeface(tfPlayBold);
        btNotif = (Button) findViewById(R.id.bt_Notification);
        btNotif.setTypeface(tfPlayBold);

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
                                    //btPause.setText(getResources().getText(R.string.title_pause_procedure).toString());
                                    //btPause.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause, 0, 0, 0);
                                    btPause.setBackground(getResources().getDrawable(R.drawable.ic_paused_enabled));
                                    break;
                                }

                                case "1": {
                                    tvStatus.
                                            setText(getResources().getText(R.string.value_status_dialysis).toString());
                                    ivStatus.setImageResource(R.drawable.ic_dialysis);
                                    //btPause.setText(getResources().getText(R.string.title_pause_procedure).toString());
                                    //btPause.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause, 0, 0, 0);
                                    btPause.setBackground(getResources().getDrawable(R.drawable.ic_paused_enabled));
                                    break;
                                }

                                case "2": {
                                    tvStatus.
                                            setText(getResources().getText(R.string.value_status_shutdown).toString());
                                    ivStatus.setImageResource(R.drawable.ic_shutdown);
                                    //btPause.setText(getResources().getText(R.string.title_pause_procedure).toString());
                                    //btPause.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause, 0, 0, 0);
                                    btPause.setBackground(getResources().getDrawable(R.drawable.ic_paused_enabled));
                                    break;
                                }

                                case "3": {
                                    tvStatus.
                                            setText(getResources().getText(R.string.value_status_disinfection).toString());
                                    ivStatus.setImageResource(R.drawable.ic_disinfection);
                                    //btPause.setText(getResources().getText(R.string.title_pause_procedure).toString());
                                    //btPause.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause, 0, 0, 0);
                                    btPause.setBackground(getResources().getDrawable(R.drawable.ic_paused_enabled));
                                    break;
                                }

                                case "4": {
                                    tvStatus.
                                            setText(getResources().getText(R.string.value_status_ready).toString());
                                    ivStatus.setImageResource(R.drawable.ic_ready);
                                    //btPause.setText(getResources().getText(R.string.title_continue_procedure).toString());
                                    //btPause.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_arrow, 0, 0, 0);
                                    btPause.setBackground(getResources().getDrawable(R.drawable.ic_play_enabled));
                                    break;
                                }

                                case "5": {
                                    tvStatus.
                                            setText(getResources().getText(R.string.value_status_flush).toString());

                                    ivStatus.setImageResource(R.drawable.ic_flush);
                                    //btPause.setText(getResources().getText(R.string.title_pause_procedure).toString());
                                    //btPause.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause, 0, 0, 0);
                                    btPause.setBackground(getResources().getDrawable(R.drawable.ic_paused_enabled));
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

                            if (remaining_time == -1 || !ConnectionService.STATE.equals(ConnectionService.STATE_ON))//If received value is default then set to unknown
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
                                if (batts >= 75)
                                    ivBatt.setImageResource(R.drawable.ic_battery_full);
                                else if (batts >= 50)
                                    ivBatt.setImageResource(R.drawable.ic_battery_75);
                                else if (batts >= 25)
                                    ivBatt.setImageResource(R.drawable.ic_battery_50);
                                else ivBatt.setImageResource(R.drawable.ic_battery_25);
                            }
                            break;
                        }

                        case TASK_SET_LASTCONNECTED: {
                            if (!arg.equals("-1")) {
                                tvLastConnected.setPaintFlags( tvLastConnected.getPaintFlags() & (~ Paint.UNDERLINE_TEXT_FLAG));
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
                    btPause.setBackground(getResources().getDrawable(R.drawable.ic_paused_disabled));
                    btState.setEnabled(false);
                    btState.setBackground(getResources().getDrawable(R.drawable.ic_bt_state_disabled));
                    trStatusRow.setEnabled(false);
                    ivStatus.setEnabled(false);
                }
                else{
                    btPause.setEnabled(true);
                    if(ConnectionService.STATUS.equals(ConnectionService.STATUS_READY))
                        btPause.setBackground(getResources().getDrawable(R.drawable.ic_play_enabled));
                    else
                        btPause.setBackground(getResources().getDrawable(R.drawable.ic_paused_enabled));
                    btState.setEnabled(true);
                    btState.setBackground(getResources().getDrawable(R.drawable.ic_bt_state));
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
                Intent intent = new Intent(this, ProceduresActivity.class);
                startActivity(intent);
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

            case R.id.tr_StatusRow: {
                Intent intent = new Intent(this, ProceduresActivity.class);
                startActivity(intent);
                break;
            }

            case R.id.tr_ParamsRow: {        Intent intent = new Intent(this, ParamsActivity.class);
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

            case R.id.tr_StateRow:{
                enableAutoconnect();
                break;
            }

            case R.id.tv_LastConnected:{
                enableAutoconnect();
            }

            default:
                break;
        }
    }


    void enableAutoconnect(){
        sPref = getSharedPreferences(PrefActivity.APP_PREFERENCES, MODE_PRIVATE); //Loading preferences
        SharedPreferences.Editor ed = sPref.edit(); //Setting for preference editing
        ed.putBoolean(PrefActivity.AUTOCONNECT, true);
        ed.commit();
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
        final Intent intent = new Intent(ConnectionService.BROADCAST_ACTION);
        AlertDialog.Builder ad = new AlertDialog.Builder(context);
        if (!ConnectionService.STATUS.equals(ConnectionService.STATUS_READY)) {
            ad.setTitle(getResources().getText(R.string.stop_confirmation).toString());
            ad.setMessage(getResources().getText(R.string.stop_confirmation).toString());
            intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_PAUSE);
        } else {
            switch (ConnectionService.PREV_STATUS) {
                case ConnectionService.STATUS_DIALYSIS: {
                    ad.setTitle(getResources().getText(R.string.resume_confirmation).toString());
                    ad.setMessage(getResources().getText(R.string.resume_confirmation).toString() +
                            getResources().getText(R.string.value_status_dialysis).toString() + "?");
                    intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_STATUS);
                    intent.putExtra(ConnectionService.PARAM_ARG, ConnectionService.TASK_ARG_DIALYSIS);
                    break;
                }

                case ConnectionService.STATUS_DISINFECTION: {
                    ad.setTitle(getResources().getText(R.string.resume_confirmation).toString());
                    ad.setMessage(getResources().getText(R.string.resume_confirmation).toString() +
                            getResources().getText(R.string.value_status_disinfection).toString() + "?");
                    intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_STATUS);
                    intent.putExtra(ConnectionService.PARAM_ARG, ConnectionService.TASK_ARG_DISINFECTION);
                    break;
                }

                case ConnectionService.STATUS_FILLING: {
                    ad.setTitle(getResources().getText(R.string.resume_confirmation).toString());
                    ad.setMessage(getResources().getText(R.string.resume_confirmation).toString() +
                            getResources().getText(R.string.value_status_filling).toString() + "?");
                    intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_STATUS);
                    intent.putExtra(ConnectionService.PARAM_ARG, ConnectionService.TASK_ARG_FILLING);
                    break;
                }

                case ConnectionService.STATUS_FLUSH: {
                    ad.setTitle(getResources().getText(R.string.resume_confirmation).toString());
                    ad.setMessage(getResources().getText(R.string.resume_confirmation).toString() +
                            getResources().getText(R.string.value_status_flush).toString() + "?");
                    intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_STATUS);
                    intent.putExtra(ConnectionService.PARAM_ARG, ConnectionService.TASK_ARG_FLUSH);
                    break;
                }

                case ConnectionService.STATUS_SHUTDOWN: {
                    ad.setTitle(getResources().getText(R.string.resume_confirmation).toString());
                    ad.setMessage(getResources().getText(R.string.resume_confirmation).toString() +
                            getResources().getText(R.string.value_status_shutdown).toString() + "?");
                    intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_STATUS);
                    intent.putExtra(ConnectionService.PARAM_ARG, ConnectionService.TASK_ARG_SHUTDOWN);
                    break;
                }

                default:
                    break;
            }

        }

        ad.setPositiveButton(getResources().getText(R.string.yes).toString(), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
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
}