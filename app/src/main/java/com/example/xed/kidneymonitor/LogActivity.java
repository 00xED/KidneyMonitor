package com.example.xed.kidneymonitor;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;


public class LogActivity extends ActionBarActivity {
    //Initialising LogWriter
    private static final String logTag = "LogActivity";
    LogWriter lw = new LogWriter();

    public static final String APP_PREFERENCES = "KIDNEYMON_SETTINGS";
    public static final String DEBUG = "DEBUG";

    public TextView mTvLog;
    public CheckBox mCbAutoscroll;

    Handler handler = new Handler();//refreshing handler
    MyTask mt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        lw.appendLog(logTag, "+++ ON CREATE +++");

        mTvLog = (TextView) findViewById(R.id.tv_Log);
        mCbAutoscroll = (CheckBox) findViewById(R.id.cb_Autoscroll);
        mCbAutoscroll.setChecked(true);
        handler.post(timedTask);
    }

    /**
     * Handler that updates textview every second
     */
    Runnable timedTask = new Runnable() {

        @Override
        public void run() {
            //mTvLog.setText("");
            //readLog();
            mt = new MyTask();
            mt.execute();

            final ScrollView scrollview = ((ScrollView) findViewById(R.id.scrollView));
            if (mCbAutoscroll.isChecked()) scrollview.fullScroll(ScrollView.FOCUS_DOWN);
            handler.postDelayed(timedTask, 1000);//refresh after one second
        }
    };




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
                File logFile = new File(Environment.getExternalStorageDirectory(), "kidneymonitor/kidneymonitor.log");
                if (logFile.exists()) if (logFile.delete()) {
                    lw.appendLog(logTag, "Log file deleted");
                    mTvLog.setText("Log file deleted");
                } else {
                    lw.appendLog(logTag, "Log file NOT deleted");
                    mTvLog.append("Log file NOT deleted");
                }

                File verboseLogFile = new File(Environment.getExternalStorageDirectory(), "kidneymonitor/kidneymonitor_debug.log");
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

    class MyTask extends AsyncTask<Void, Void, Void> {

        StringBuilder text;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected Void doInBackground(Void... params) {

                text = readLog();
            if(text.length()>20000)
                text = text.delete(0, text.length()-20000);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mTvLog.setText(text);
        }

        /**
         * Reads log file kidneymonitor.log and updates textview
         */
        public StringBuilder readLog() {
            SharedPreferences sPref = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE); //Loading preferences
            File file;
            if(sPref.getBoolean(PrefActivity.TESTMODE, false)) {
                //Get the text file
                file = new File(Environment.getExternalStorageDirectory(), "kidneymonitor/kidneymonitor_debug.log");
            }
            else
            {
                //Get the text file
                file = new File(Environment.getExternalStorageDirectory(), "kidneymonitor/kidneymonitor.log");
            }
            if (!file.exists()) {
                try {
                    if(!file.createNewFile())
                        Log.d("LogWriter", "can't create new file");
                } catch (IOException e) {
                    Log.d("LogWriter", e.toString());
                    e.printStackTrace();
                }
            }

            //Read text from file
            StringBuilder text = new StringBuilder();
            if (isExternalStorageReadable()) {

                try {
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    String line;

                    while ((line = br.readLine()) != null) {
                        text.append(line);
                        //mTvLog.append(line);
                        text.append('\n');
                        //mTvLog.append("\n");
                    }

                    br.close();
                } catch (IOException e) {
                    lw.appendLog(logTag, e.toString());
                }
            }
            return text;
            //mTvLog.append(text);
        }

    }



}