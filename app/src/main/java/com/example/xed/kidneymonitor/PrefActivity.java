package com.example.xed.kidneymonitor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

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
    public static final String DEBUG = "DEBUG";

    public static String CHOSEN_ADDRESS = "00:00:00:00:00:00";
    public static String CHOSEN_NAME = "NONE";

    private SharedPreferences sPref;

    private Boolean isServiceRunning = true;
    private Button btStopService;

    private TextView tvCurrentDeviceName, tvCurrentDeviceAddress;
    private CheckBox cbForegroundService, cbVibrate, cbSound, cbDebug;

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
            cbForegroundService = (CheckBox)findViewById(R.id.cb_ForegroundService);
            if(sPref.getBoolean(IS_FOREGROUND, false))
                cbForegroundService.setChecked(true);
            else
                cbForegroundService.setChecked(false);

            cbVibrate = (CheckBox)findViewById(R.id.cb_Vibtation);
            if(sPref.getBoolean(VIBRATION, false))
                cbVibrate.setChecked(true);
            else
                cbVibrate.setChecked(false);

            cbSound = (CheckBox)findViewById(R.id.cb_Sound);
            if(sPref.getBoolean(SOUND, false))
                cbSound.setChecked(true);
            else
                cbSound.setChecked(false);

            cbDebug = (CheckBox)findViewById(R.id.cb_Debug);
            if(sPref.getBoolean(DEBUG, false))
                cbDebug.setChecked(true);
            else
                cbDebug.setChecked(false);

        btStopService = (Button) findViewById(R.id.bt_StopService);
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
                lw.appendLog(logTag, "Deleting settings");
                break;
            }

            case R.id.cb_ForegroundService://Save setting for foreground service
            {
                sPref = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE); //Loading preferences
                Editor ed = sPref.edit(); //Setting for preference editing
                if(cbForegroundService.isChecked())
                    ed.putBoolean(IS_FOREGROUND, true);
                else
                    ed.putBoolean(IS_FOREGROUND, false);
                ed.commit();

                break;
            }

            case R.id.cb_Vibtation://Save setting for vibration on notification
            {
                sPref = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE); //Loading preferences
                Editor ed = sPref.edit(); //Setting for preference editing
                if(cbVibrate.isChecked())
                    ed.putBoolean(VIBRATION, true);
                else
                    ed.putBoolean(VIBRATION, false);
                ed.commit();
                break;
            }

            case R.id.cb_Sound://Save setting for sound on notification
            {
                sPref = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE); //Loading preferences
                Editor ed = sPref.edit(); //Setting for preference editing
                if(cbSound.isChecked())
                    ed.putBoolean(SOUND, true);
                else
                    ed.putBoolean(SOUND, false);
                ed.commit();
                break;
            }

            case R.id.cb_Debug://Save setting for debug mode
            {
                sPref = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE); //Loading preferences
                Editor ed = sPref.edit(); //Setting for preference editing
                if(cbDebug.isChecked())
                    ed.putBoolean(DEBUG, true);
                else
                    ed.putBoolean(DEBUG, false);
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