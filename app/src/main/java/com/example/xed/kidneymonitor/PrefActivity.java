package com.example.xed.kidneymonitor;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

/**
 * Writing and reading preferences, device choosing and pairing,
 * starting and stopping ConnectionService
 */

public class PrefActivity extends ActionBarActivity {

    //Initialisation of LogWriter
    private static final String logTag = "PreferenceActivity";
    LogWriter lw = new LogWriter();

    private static final int CHOOSE_DEVICE = 1; //Tag for DeviceListActivity

    /**
     * Parameters for loading and writing preferences
     */
    public static final String APP_PREFERENCES = "KIDNEYMON_SETTINGS";
    public static final String SAVED_NAME = "SAVED_NAME";
    public static final String SAVED_ADDRESS = "SAVED_ADDRESS";
    public static final String IS_FOREGROUND = "FOREGROUND_SERVICE";
    public static final String VIBRATION = "VIBRATION";
    public static final String SOUND = "SOUND";
    public static final String TESTMODE = "TESTMODE";
    public static final String TIME_REMAINING = "TIME_REMAINING";
    public static final String LAST_TICK = "LAST_TICK";
    public static final String AUTOCONNECT = "AUTOCONNECT";

    public static String CHOSEN_ADDRESS = "00:00:00:00:00:00";
    public static String CHOSEN_NAME = "NONE";

    private SharedPreferences sPref;

    private Boolean isServiceRunning = true;
    private Button btStopService;

    private TextView tvCurrentDeviceName, tvCurrentDeviceAddress;
    private Switch swForegroundService, swVibrate, swSound, swTestMode, swAutoconnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pref);
        lw.appendLog(logTag, "+++ ON CREATE +++");

        isServiceRunning=ConnectionService.isServiceRunning;

        sPref = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE); //Load preferences

        tvCurrentDeviceName = (TextView) findViewById(R.id.tv_ValueCurrentDeviceName);
        tvCurrentDeviceName.setText(sPref.getString(SAVED_NAME,
                getResources().getText(R.string.value_current_device_none).toString()));

        tvCurrentDeviceAddress = (TextView) findViewById(R.id.tv_ValueCurrentDeviceAddress);
        tvCurrentDeviceAddress.setText(sPref.getString(SAVED_ADDRESS,
                getResources().getText(R.string.value_current_device_default_address).toString()));

        /**
         * Setting checkboxes states
         */
        swForegroundService = (Switch)findViewById(R.id.sw_ForegroundService);
            if(sPref.getBoolean(IS_FOREGROUND, false))
                swForegroundService.setChecked(true);
            else
                swForegroundService.setChecked(false);


            swVibrate = (Switch)findViewById(R.id.sw_Vibration);
            if(sPref.getBoolean(VIBRATION, false))
                swVibrate.setChecked(true);
            else
                swVibrate.setChecked(false);

            swSound = (Switch)findViewById(R.id.sw_Sound);
            if(sPref.getBoolean(SOUND, false))
                swSound.setChecked(true);
            else
                swSound.setChecked(false);

            swTestMode = (Switch)findViewById(R.id.sw_TestMode);
            if(sPref.getBoolean(TESTMODE, false))
                swTestMode.setChecked(true);
            else
                swTestMode.setChecked(false);

            swAutoconnect = (Switch)findViewById(R.id.sw_Autoconnect);
            if(sPref.getBoolean(AUTOCONNECT, false))
                swAutoconnect.setChecked(true);
            else
                swAutoconnect.setChecked(false);

        btStopService = (Button) findViewById(R.id.bt_StopService);
        if(ConnectionService.isServiceRunning)
            btStopService.setText(
                    getResources().getText(R.string.title_service_stop).toString());
        else
            btStopService.setText(
                    getResources().getText(R.string.title_service_start).toString());

    }

    /**
     * Handling result of DeviceListActivity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        sPref = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE); //Loading preferences
        Editor ed = sPref.edit(); //Setting for preference editing

        if (requestCode == CHOOSE_DEVICE) {//If DeviceListActivity responsed
            if (resultCode == RESULT_OK) {//And user has choosen device to connect with
                //Getting chosen device data from DeviceListActivity
                String name = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_NAME);
                String address = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

                //Setting values for textviews
                tvCurrentDeviceName.setText(name);
                tvCurrentDeviceAddress.setText(address);

                //Saving values
                ed.putString(SAVED_NAME, name);
                ed.putString(SAVED_ADDRESS, address);
                ed.commit();
                Toast.makeText(this,
                        getResources().getText(R.string.title_prefs_saved).toString(),
                        Toast.LENGTH_SHORT).show();

                lw.appendLog(logTag, "Bluetooth device chosen:" + name + "@" + address);
                CHOSEN_ADDRESS = address;
                CHOSEN_NAME = name;
                //Sending broadcast to connection service to perform pairing with chosen device
                    Intent intent = new Intent(ConnectionService.BROADCAST_ACTION);
                    intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_DO_PAIRING);
                    sendBroadcast(intent);
            } else {
                lw.appendLog(logTag, "Bluetooth device NOT CHOSEN");
            }
        }
    }

    public void OnClick(View v) {

        switch (v.getId()) {
            case R.id.bt_Scan://Start device choosing activity DeviceListActivity
            {
                Intent intent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(intent, CHOOSE_DEVICE);
                lw.appendLog(logTag, "Starting DeviceListActivity");
                break;
            }

            case R.id.bt_SetDefaults://Reset all preferences
            {
                sPref = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE);
                Editor ed = sPref.edit();
                ed.clear();
                ed.apply();
                tvCurrentDeviceName.setText(getResources().getText(R.string.value_current_device_none).toString());
                tvCurrentDeviceAddress.setText(getResources().getText(R.string.value_current_device_default_address).toString());
                Toast.makeText(this,
                        getResources().getText(R.string.title_prefs_cleared).toString(),
                        Toast.LENGTH_SHORT).show();
                lw.appendLog(logTag, "Deleting settings and logs");
                File logFile = new File(Environment.getExternalStorageDirectory(), "kidneymonitor.log");
                if (logFile.exists()) if (logFile.delete()) {
                    lw.appendLog(logTag, "Log file deleted");
                } else {
                    lw.appendLog(logTag, "Log file NOT deleted");
                }

                File verboseLogFile = new File(Environment.getExternalStorageDirectory(), "kidneymonitor_debug.log");
                if (verboseLogFile.exists()) if (verboseLogFile.delete()) {
                    lw.appendLog(logTag, "Debug log file deleted");
                } else {
                    lw.appendLog(logTag, "Debug log file NOT deleted");
                }

                Intent intent = getIntent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                finish();
                overridePendingTransition(0, 0);
                startActivity(intent);
                overridePendingTransition(0, 0);
                break;
            }

            case R.id.sw_ForegroundService://Save setting for foreground service
            {
                sPref = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE); //Loading preferences
                Editor ed = sPref.edit(); //Setting for preference editing
                if(swForegroundService.isChecked())
                    ed.putBoolean(IS_FOREGROUND, true);
                else
                    ed.putBoolean(IS_FOREGROUND, false);
                ed.commit();
                AlertDialog.Builder builder = new AlertDialog.Builder(PrefActivity.this);
                builder.setMessage(getResources().getText(R.string.notif_restart))
                        .setCancelable(false)
                        .setNegativeButton(getResources().getText(R.string.ok),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                AlertDialog alert = builder.create();
                alert.show();
                break;
            }

            case R.id.sw_Vibration://Save setting for vibration on notification
            {
                sPref = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE); //Loading preferences
                Editor ed = sPref.edit(); //Setting for preference editing
                if(swVibrate.isChecked())
                    ed.putBoolean(VIBRATION, true);
                else
                    ed.putBoolean(VIBRATION, false);
                ed.commit();
                break;
            }

            case R.id.sw_Sound://Save setting for sound on notification
            {
                sPref = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE); //Loading preferences
                Editor ed = sPref.edit(); //Setting for preference editing
                if(swSound.isChecked())
                    ed.putBoolean(SOUND, true);
                else
                    ed.putBoolean(SOUND, false);
                ed.commit();
                break;
            }

            case R.id.sw_TestMode:
            {
                sPref = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE); //Loading preferences
                Editor ed = sPref.edit(); //Setting for preference editing
                if(swTestMode.isChecked())
                    ed.putBoolean(TESTMODE, true);
                else
                    ed.putBoolean(TESTMODE, false);
                ed.commit();
                break;
            }

            case R.id.sw_Autoconnect:
            {
                sPref = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE); //Loading preferences
                Editor ed = sPref.edit(); //Setting for preference editing
                if(swAutoconnect.isChecked())
                    ed.putBoolean(AUTOCONNECT, true);
                else
                    ed.putBoolean(AUTOCONNECT, false);
                ed.commit();
                break;
            }

            case R.id.bt_StopService://Start or stop service
            {
					if(!ConnectionService.isServiceRunning){
                    isServiceRunning=true;
                    lw.appendLog(logTag, "Starting service");
                    startService(new Intent(this, ConnectionService.class));
                    btStopService.setText(
                            getResources().getText(R.string.title_service_stop).toString());
                }
                else{
                    isServiceRunning=false;
                    lw.appendLog(logTag, "Stopping service");
                    stopService(new Intent(this, ConnectionService.class));
                    ConnectionService.isServiceRunning=true;
					btStopService.setText(
                            getResources().getText(R.string.title_service_start).toString());
                }
                break;
            }

            default:
                break;
        }
    }

}