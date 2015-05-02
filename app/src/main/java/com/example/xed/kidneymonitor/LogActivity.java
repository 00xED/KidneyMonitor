package com.example.xed.kidneymonitor;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;


public class LogActivity extends ActionBarActivity {
    //Initialising LogWriter
    private static final String logTag = "LogActivity";
    LogWriter lw = new LogWriter();

    public static final String APP_PREFERENCES = "KIDNEYMON_SETTINGS";
    public static final String DEBUG = "DEBUG";

    public TextView mTvLog;
    public CheckBox mCbAutoscroll;

    Handler handler = new Handler();//refreshing handler

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        lw.appendLog(logTag, "+++ ON CREATE +++");

        mTvLog = (TextView) findViewById(R.id.tv_Log);
        mCbAutoscroll = (CheckBox) findViewById(R.id.cb_Autoscroll);

        handler.post(timedTask);
    }

    /**
     * Handler that updates textview every second
     */
    Runnable timedTask = new Runnable() {

        @Override
        public void run() {
            readLog();
            final ScrollView scrollview = ((ScrollView) findViewById(R.id.scrollView));
            if (mCbAutoscroll.isChecked()) scrollview.fullScroll(ScrollView.FOCUS_DOWN);
            handler.postDelayed(timedTask, 1000);//refresh after one second
        }
    };

    /**
     * Reads log file kidneymonitor.log and updates textview
     */
    public void readLog() {
        SharedPreferences sPref = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE); //Loading preferences
        SharedPreferences.Editor ed = sPref.edit(); //Setting for preference editing
        File file;
        if(sPref.getBoolean(DEBUG, false)) {
            //Get the text file
            file = new File(Environment.getExternalStorageDirectory(), "kidneymonitor_debug.log");
        }
        else
        {
            //Get the text file
            file = new File(Environment.getExternalStorageDirectory(), "kidneymonitor.log");
        }

        //Read text from file
        StringBuilder text = new StringBuilder();
        if (isExternalStorageReadable()) {

            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;

                while ((line = br.readLine()) != null) {
                    text.append(line);
                    text.append('\n');
                }

                br.close();
            } catch (IOException e) {
                //You'll need to add proper error handling here
            }
        }
        mTvLog.setText(text);
    }


    /**
     * Checks if we can read from internal storage
     */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state));
    }

    public void OnClick(View v) {
        switch (v.getId()) {
            case R.id.bt_ClearLog:
            {
                File logFile = new File(Environment.getExternalStorageDirectory(), "kidneymonitor.log");
                if (logFile.exists()) if (logFile.delete()) {
                    lw.appendLog(logTag, "Log file deleted");
                    mTvLog.setText("Log file deleted");
                } else {
                    lw.appendLog(logTag, "Log file NOT deleted");
                    mTvLog.append("Log file NOT deleted");
                }

                File verboseLogFile = new File(Environment.getExternalStorageDirectory(), "kidneymonitor_debug.log");
                if (verboseLogFile.exists()) if (verboseLogFile.delete()) {
                    lw.appendLog(logTag, "Debug log file deleted");
                    mTvLog.setText("Debug log file deleted");
                } else {
                    lw.appendLog(logTag, "Debug log file NOT deleted");
                    mTvLog.append("Debug log file NOT deleted");
                }
                break;
            }

        }
    }



}