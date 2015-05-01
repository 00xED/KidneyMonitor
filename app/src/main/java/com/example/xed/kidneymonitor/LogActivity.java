package com.example.xed.kidneymonitor;

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

    private static final String logTag = "LogActivity";
    public TextView mTvLog;
    public CheckBox mCbAutoscroll;
    LogWriter lw = new LogWriter();

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

    Runnable timedTask = new Runnable() {

        @Override
        public void run() {
            readLog();
            final ScrollView scrollview = ((ScrollView) findViewById(R.id.scrollView));
            if (mCbAutoscroll.isChecked()) scrollview.fullScroll(ScrollView.FOCUS_DOWN);
            handler.postDelayed(timedTask, 1000);//refresh after one second
        }
    };

    public void readLog() {
        //Get the text file
        File file = new File(Environment.getExternalStorageDirectory(), "kidneymonitor.log");

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



    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
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
                break;
            }

        }
    }



}