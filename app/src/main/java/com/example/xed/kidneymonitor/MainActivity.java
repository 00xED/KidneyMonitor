package com.example.xed.kidneymonitor;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Main activity handles main screen values, images, has buttons to open all other activities,
 * button for pausing and starting procedure, buton to choose current procedure
 */

public class MainActivity extends ActionBarActivity {

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

    //Is procedure paused? 0-no, 1-yes, other-unknown
    public int procedurePaused = 9;

    /**
     * Settings for BroadcastReceiver
     */
    public final static String PARAM_TASK = "task";
    public final static String PARAM_ARG = "arg";
    public final static String BROADCAST_ACTION = "SetValues";
    private BroadcastReceiver broadcastReceiver;

    //Initialisation of LogWriter
    private static final String logTag = "MainActivity";
    LogWriter lw = new LogWriter();

    /**
     * Parameters for loading preferences
     */
    private static final String SAVED_ADDRESS = "SAVED_ADDRESS";
    private static final String APP_PREFERENCES = "mysettings";
    private SharedPreferences sPref;

    public BluetoothAdapter mBluetoothAdapter = null;

    /**
     * TextViews for main screen
     */
    private TextView tvState, tvStatus, tvFunct, tvParams, tvSorbtime, tvBatt, tvLastConnected;
    private ImageView ivState, ivStatus, ivFunct, ivParams, ivBatt;
    private Button btPause;

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

        ivBatt = (ImageView) findViewById(R.id.iv_Battery);
        ivFunct = (ImageView) findViewById(R.id.iv_Functioning);
        ivState = (ImageView) findViewById(R.id.iv_State);
        ivStatus = (ImageView) findViewById(R.id.iv_Status);
        ivParams = (ImageView) findViewById(R.id.iv_Params);

        btPause = (Button) findViewById(R.id.bt_Pause);

        /**
         * Load preferences; If saved device address is default - open preferences to find device
         */
        sPref = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE); //Load preferences
        String address = sPref.getString(SAVED_ADDRESS,
                getResources().getText(R.string.value_current_device_default_address).toString());
        if (("00:00:00:00:00:00").equals(address)) { //If address is default start PrefActivity
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
        }

        /**
         * Initialise broadcast receiver that listens for messages from ConnectionService
         * and sets main screen textviews values and images
         */
        broadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                int task = intent.getIntExtra(PARAM_TASK, 9);
                int arg = intent.getIntExtra(PARAM_ARG, 9);

                // switch tasks for setting main screen values
                if(ConnectionService.isServiceRunning)
                switch (task) {
                    case TASK_SET_STATE:
                    {
                        switch (arg) {
                            case 0:
                            {
                                tvState.
                                        setText(getResources().getText(R.string.value_state_on).toString());
                                ivState.setImageResource(R.drawable.ic_flash_on_grey600_24dp);
                                break;
                            }
                            case 1:
                            {
                                tvState.
                                        setText(getResources().getText(R.string.value_state_off).toString());
                                ivState.setImageResource(R.drawable.ic_flash_off_grey600_24dp);
                                break;
                            }
                            default:
                            {
                                tvState.
                                        setText(getResources().getText(R.string.value_state_unknown).toString());
                                ivState.setImageResource(R.drawable.ic_help_grey600_24dp);
                                break;
                            }
                        }
                        break;
                    }
                    case TASK_SET_STATUS:
                    {
                        switch (arg) {
                            case 0:
                            {
                                tvStatus.
                                        setText(getResources().getText(R.string.value_status_dialysis).toString());
                                break;
                            }
                            case 1:
                            {
                                tvStatus.
                                        setText(getResources().getText(R.string.value_status_filling).toString());
                                break;
                            }
                            case 2:
                            {
                                tvStatus.
                                        setText(getResources().getText(R.string.value_status_shutdown).toString());
                                break;
                            }
                            case 3:
                            {
                                tvStatus.
                                        setText(getResources().getText(R.string.value_status_disinfection).toString());
                                break;
                            }
                            case 4:
                            {
                                tvStatus.
                                        setText(getResources().getText(R.string.value_status_testing).toString());
                                break;
                            }
                            default:
                            {
                                tvStatus.
                                        setText(getResources().getText(R.string.value_status_unknown).toString());
                                ivStatus.setImageResource(R.drawable.ic_help_grey600_24dp);
                                break;
                            }
                        }
                        break;
                    }
                    case TASK_SET_PARAMS:
                    {
                        switch (arg) {
                            case 0:
                            {
                                tvParams.
                                        setText(getResources().getText(R.string.value_procedure_params_normal).toString());
                                ivParams.setImageResource(R.drawable.ic_check_circle_grey600_24dp);
                                break;
                            }
                            case 1:
                            {
                                tvParams.
                                        setText(getResources().getText(R.string.value_procedure_params_danger).toString());
                                ivParams.setImageResource(R.drawable.ic_highlight_remove_grey600_24dp);
                                break;
                            }
                            default:
                            {
                                tvParams.
                                        setText(getResources().getText(R.string.value_procedure_params_unknown).toString());
                                ivParams.setImageResource(R.drawable.ic_help_grey600_24dp);
                                break;
                            }
                        }
                        break;
                    }
                    case TASK_SET_FUNCT:
                    {
                        switch (arg) {
                            case 0:
                            {
                                tvFunct.
                                        setText(getResources().getText(R.string.value_device_functioning_correct).toString());
                                ivFunct.setImageResource(R.drawable.ic_check_circle_grey600_24dp);
                                break;
                            }
                            case 1:
                            {
                                tvFunct.
                                        setText(getResources().getText(R.string.value_device_functioning_fault).toString());
                                ivFunct.setImageResource(R.drawable.ic_highlight_remove_grey600_24dp);
                                break;
                            }
                            default:
                            {
                                tvFunct.
                                        setText(getResources().getText(R.string.value_device_functioning_unknown).toString());
                                ivFunct.setImageResource(R.drawable.ic_help_grey600_24dp);
                                break;
                            }
                        }
                        break;
                    }
                    case TASK_SET_SORBTIME:
                    {
                        if (arg == -1)//If received value is default then set to unknown
                        {
                            tvSorbtime.setText(getResources().getText(R.string.value_time_sorbent_unknown).toString());
                        }
                        else    //Convert received time in seconds to hours and minutes
                        {
                            int hours = (int) TimeUnit.SECONDS.toHours(arg);
                            arg -= TimeUnit.HOURS.toSeconds(hours);
                            int mins = (int) TimeUnit.SECONDS.toMinutes(arg);
                            tvSorbtime.setText(hours +
                                               getResources().getText(R.string.value_sorbtime_hours).toString() +
                                               mins +
                                               getResources().getText(R.string.value_sorbtime_mins).toString());
                        }
                        break;
                    }
                    case TASK_SET_BATT:
                    {
                        if (arg == -1)//If received value is default then set battery to unknown
                        {
                            tvBatt.setText(getResources().getText(R.string.value_battery_charge_unknown).toString());
                            ivBatt.setImageResource(R.drawable.ic_battery_unknown_grey600_24dp);
                        }
                        else{//Otherwise set value and image
                            tvBatt.setText(arg + "%");
                            if(arg>=95)ivBatt.setImageResource(R.drawable.ic_battery_full_grey600_24dp);
                            else if(arg>=90)ivBatt.setImageResource(R.drawable.ic_battery_90_grey600_24dp);
                            else if(arg>=80)ivBatt.setImageResource(R.drawable.ic_battery_80_grey600_24dp);
                            else if(arg>=60)ivBatt.setImageResource(R.drawable.ic_battery_60_grey600_24dp);
                            else if(arg>=50)ivBatt.setImageResource(R.drawable.ic_battery_50_grey600_24dp);
                            else if(arg>=30)ivBatt.setImageResource(R.drawable.ic_battery_30_grey600_24dp);
                            else ivBatt.setImageResource(R.drawable.ic_battery_20_grey600_24dp);
                        }
                        break;
                    }
                    case TASK_SET_LASTCONNECTED:{
                        if(arg!=-1){
                            Calendar c = Calendar.getInstance();
                            SimpleDateFormat sdf = new SimpleDateFormat("dd:MM HH:mm");
                            String strDate = sdf.format(c.getTime());
                            tvLastConnected.setText(getResources().getText(R.string.last_connected).toString()+strDate);
                        }
                        break;
                    }

                    case TASK_SET_PAUSE:
                    {
                        procedurePaused=arg;
                        switch (arg) {
                            case 0:
                            {
                                btPause.
                                        setText(getResources().getText(R.string.title_pause_procedure).toString());
                                btPause.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_grey600_24dp, 0, 0, 0);

                                break;
                            }
                            case 1:
                            {
                                btPause.
                                        setText(getResources().getText(R.string.title_continue_procedure).toString());
                                btPause.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_arrow_grey600_24dp, 0, 0, 0);
                                break;
                            }
                            default:
                            {
                                btPause.
                                        setText(getResources().getText(R.string.button_start).toString());
                                btPause.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_help_grey600_24dp, 0, 0, 0);
                                break;
                            }
                        }
                        break;
                    }

                    default:
                        break;
                }
            }
        };

        //Create intent filter and register new receiver with it
        IntentFilter intFilt = new IntentFilter(BROADCAST_ACTION);
        registerReceiver(broadcastReceiver, intFilt);

        //If service is not running - start it
        if(!ConnectionService.isServiceRunning)
            startService(new Intent(this, ConnectionService.class));

    }

    @Override
    public void onStart() {
        super.onStart();
        lw.appendLog(logTag, "+++ ON START +++");

        // If BT is not on, request that it be enabled.
        if (!mBluetoothAdapter.isEnabled()) {
            lw.appendLog(logTag, "Trying to start bluetooth...");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Deregister receiver
        unregisterReceiver(broadcastReceiver);
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
                alertSingleChooseStatus();
                break;
            }
            case R.id.bt_Pause:// pause current procedure
            {
                if(procedurePaused==0) {
                    //TODO: implement starting function
                    Intent intent = new Intent(ConnectionService.BROADCAST_ACTION);
                    intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_PAUSE);
                    sendBroadcast(intent);
                }
                else if(procedurePaused==1){
                    Intent intent = new Intent(ConnectionService.BROADCAST_ACTION);
                    intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_RESUME);
                    sendBroadcast(intent);
                }
                break;
            }
            default:
                break;
        }
    }

    /**
     * Creates dialog, where user chooses what procedure to run
     */
    public void alertSingleChooseStatus(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        // Set the dialog title
        builder.setTitle(getResources().getText(R.string.title_status_select).toString())

                // specify the list array, the items to be selected by default (null for none),
                // and the listener through which to receive call backs when items are selected
                // again, R.array.choices were set in the resources res/values/strings.xml
                .setSingleChoiceItems(R.array.status_selection, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {}
                })
                        // Set the action buttons
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // user clicked OK, so save the mSelectedItems results somewhere
                        // or return them to the component that opened the dialog
                        int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                        switch (selectedPosition){
                            case 0://If user chooses "Filling" show confirmation to check if patient is connected
                            {
                                AlertDialog.Builder confirm = new AlertDialog.Builder(MainActivity.this);
                                confirm.setTitle(getResources().getText(R.string.title_filling_confirmation).toString());
                                confirm.setMessage(getResources().getText(R.string.filling_confirmation).toString());
                                confirm.setPositiveButton(getResources().getText(R.string.yes).toString(), new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface dialog, int arg1) {//If user choose "OK" then send command to start filling
                                        Intent intent = new Intent(ConnectionService.BROADCAST_ACTION);
                                        intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_STATUS);
                                        intent.putExtra(ConnectionService.PARAM_ARG, ConnectionService.TASK_ARG_FILLING);
                                        sendBroadcast(intent);
                                        Toast.makeText(MainActivity.this, getResources().getText(R.string.starting_filling).toString(),
                                                Toast.LENGTH_LONG).show();
                                    }
                                });

                                confirm.setNegativeButton(getResources().getText(R.string.no).toString(), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int arg1) {

                                    }
                                });

                                confirm.setCancelable(true);
                                confirm.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                    public void onCancel(DialogInterface dialog) {

                                    }
                                });
                                confirm.show();
                                break;
                            }

                            case 1:{
                                Intent intent = new Intent(ConnectionService.BROADCAST_ACTION);
                                intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_STATUS);
                                intent.putExtra(ConnectionService.PARAM_ARG, ConnectionService.TASK_ARG_DIALYSIS);
                                sendBroadcast(intent);
                                break;
                            }

                            case 2:{
                                Intent intent = new Intent(ConnectionService.BROADCAST_ACTION);
                                intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_STATUS);
                                intent.putExtra(ConnectionService.PARAM_ARG, ConnectionService.TASK_ARG_SHUTDOWN);
                                sendBroadcast(intent);
                                break;
                            }

                            case 3:{
                                Intent intent = new Intent(ConnectionService.BROADCAST_ACTION);
                                intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_STATUS);
                                intent.putExtra(ConnectionService.PARAM_ARG, ConnectionService.TASK_ARG_DISINFECTION);
                                sendBroadcast(intent);
                                break;
                            }

                            default:
                                break;
                        }
                    }
                })
                .show();
    }

}