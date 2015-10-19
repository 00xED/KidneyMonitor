package com.example.xed.kidneymonitor;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Writes logs to kidneymoonitor.log and to Log.d
 */

public class LogWriter extends Application {

    public void OnCreate() {
        super.onCreate();
    }

    public static final String APP_PREFERENCES = "KIDNEYMON_SETTINGS";
    public static final String DEBUG = "DEBUG";

    public void appendLog(String tag, String msg) {

        /**
         * Initialising calendar for writing timestamp
         */
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String strDate = sdf.format(c.getTime());

        tag = strDate + "@" + tag;
        Log.i(tag, msg);
        msg = tag + "->" + msg;

        File logFile = new File(Environment.getExternalStorageDirectory(), "kidneymonitor/kidneymonitor_debug.log");

        if (!logFile.exists()) {
            try {
                if(!logFile.createNewFile())
                    Log.i("LogWriter", "can't create new file");
            } catch (IOException e) {
                Log.i("LogWriter", e.toString());
                e.printStackTrace();
            }
        }
        try {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(msg);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            Log.i("LogWriter", e.toString());
            e.printStackTrace();
        }

    }

    public void appendLog(String tag, String msg, boolean noVerbose) {

        /**
         * Initialising calendar for writing timestamp
         */
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String strDate = sdf.format(c.getTime());

        tag = strDate + "@" + tag;
            Log.i(tag, msg);
        msg = tag + "->" + msg;

        File verboseLogFile = new File(Environment.getExternalStorageDirectory(), "kidneymonitor/kidneymonitor_debug.log");

        if (!verboseLogFile.exists()) {
            try {
                if(!verboseLogFile.createNewFile())
                    Log.i("LogWriter", "can't create new file");
            } catch (IOException e) {
                Log.i("LogWriter", e.toString());
                e.printStackTrace();
            }
        }
        try {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(verboseLogFile, true));
            buf.append(msg);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            Log.i("LogWriter", e.toString());
            e.printStackTrace();
        }

        File logFile = new File(Environment.getExternalStorageDirectory(), "kidneymonitor/kidneymonitor.log");

        if (!logFile.exists()) {
            try {
                if(!logFile.createNewFile())
                    Log.i("LogWriter", "can't create new file");
            } catch (IOException e) {
                Log.i("LogWriter", e.toString());
                e.printStackTrace();
            }
        }
        try {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(msg);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            Log.i("LogWriter", e.toString());
            e.printStackTrace();
        }

    }

}