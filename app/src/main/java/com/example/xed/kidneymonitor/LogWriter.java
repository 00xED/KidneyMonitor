package com.example.xed.kidneymonitor;

import android.app.Application;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class LogWriter extends Application {

    public void OnCreate() {
        super.onCreate();
    }

    public void appendLog(String tag, String msg) {

        /**
         * Initialising calendar for writing timestamp
         */
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String strDate = sdf.format(c.getTime());

        tag = strDate + "@" + tag;
        Log.d(tag, msg);
        msg = tag + "->" + msg;

        File logFile = new File(Environment.getExternalStorageDirectory(), "kidneymonitor.log");

        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
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
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}