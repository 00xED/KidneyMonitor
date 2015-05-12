package com.example.xed.kidneymonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.widget.TextView;


public class ParamsActivity extends ActionBarActivity {

    private TextView tvDPumpFlow1, tvDPumpFlow2, tvDPumpFlow3, tvDUFVolume1;
    private TextView tvDPress1, tvDPress2, tvDPress3;
    private TextView tvDTemp1, tvDCond1, tvDCur1, tvDCur2, tvDCur3, tvDCur4;

    private BroadcastReceiver brParams;
    public final static String PARAM_TASK = "SetParams";
    public final static String PARAM_ARG = "arg";
    public final static String BROADCAST_ACTION = "SetParams";

    public final static int TASK_SET_DPUMPFLOW1 = 31;
    public final static int TASK_SET_DPUMPFLOW2 = 32;
    public final static int TASK_SET_DPUMPFLOW3 = 33;
    public final static int TASK_SET_DUFVOLUME1 = 34;
    public final static int TASK_SET_DPRESS1 = 35;
    public final static int TASK_SET_DPRESS2 = 36;
    public final static int TASK_SET_DPRESS3 = 37;
    public final static int TASK_SET_DTEMP1 = 38;
    public final static int TASK_SET_DCOND1 = 39;
    public final static int TASK_SET_DCUR1  = 40;
    public final static int TASK_SET_DCUR2  = 41;
    public final static int TASK_SET_DCUR3  = 42;
    public final static int TASK_SET_DCUR4  = 43;

    private LogWriter lw;
    private final String logTag = "ParamsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_params);

        tvDPumpFlow1 = (TextView) findViewById(R.id.tv_DPumpFlow1);
        tvDPumpFlow2 = (TextView) findViewById(R.id.tv_DPumpFlow2);
        tvDPumpFlow3 = (TextView) findViewById(R.id.tv_DPumpFlow3);
        tvDUFVolume1 = (TextView) findViewById(R.id.tv_DUFVolume);
        tvDPress1 = (TextView) findViewById(R.id.tv_DPress1);
        tvDPress2 = (TextView) findViewById(R.id.tv_DPress2);
        tvDPress3 = (TextView) findViewById(R.id.tv_DPress3);
        tvDTemp1 = (TextView) findViewById(R.id.tv_DTemp1);
        tvDCond1 = (TextView) findViewById(R.id.tv_DCond1);
        tvDCur1 = (TextView) findViewById(R.id.tv_DCur1);
        tvDCur2 = (TextView) findViewById(R.id.tv_DCur2);
        tvDCur3 = (TextView) findViewById(R.id.tv_DCur3);
        tvDCur4 = (TextView) findViewById(R.id.tv_DCur4);

        float dc=ConnectionService.DPUMP1FLOW;
        tvDCur1.setText(String.valueOf(dc));
       // lw.appendLog(logTag, "DCOND1MAX="+ConnectionService.DCOND1MAX);
        /**
         * Initialise broadcast receiver that listens for messages from ConnectionService
         * and sets params screen textviews values and images
         */
        brParams = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                int task = intent.getIntExtra(PARAM_TASK, -1);
                String args = intent.getStringExtra(PARAM_ARG);

                // switch tasks for setting main screen values
                if(ConnectionService.isServiceRunning)
                    switch (task) {
                        case TASK_SET_DPUMPFLOW1:
                        {
                            tvDPumpFlow1.setText(args);
                            break;
                        }

                        case TASK_SET_DPUMPFLOW2:
                        {
                            tvDPumpFlow2.setText(args);
                            break;
                        }

                        case TASK_SET_DPUMPFLOW3:
                        {
                            tvDPumpFlow3.setText(args);
                            break;
                        }

                        case TASK_SET_DUFVOLUME1:
                        {
                            tvDUFVolume1.setText(args);
                            break;
                        }

                        case TASK_SET_DPRESS1:
                        {
                            tvDPress1.setText(args);
                            break;
                        }

                        case TASK_SET_DPRESS2:
                        {
                            tvDPress2.setText(args);
                            break;
                        }

                        case TASK_SET_DPRESS3:
                        {
                            tvDPress3.setText(args);
                            break;
                        }

                        case TASK_SET_DTEMP1:
                        {
                            tvDTemp1.setText(args);
                            break;
                        }

                        case TASK_SET_DCOND1:
                        {
                            tvDCond1.setText(args);
                            break;
                        }

                        case TASK_SET_DCUR1:
                        {
                            tvDCur1.setText(args);
                            break;
                        }

                        case TASK_SET_DCUR2:
                        {
                            tvDCur2.setText(args);
                            break;
                        }

                        case TASK_SET_DCUR3:
                        {
                            tvDCur3.setText(args);
                            break;
                        }

                        case TASK_SET_DCUR4:
                        {
                            tvDCur4.setText(args);
                            break;
                        }


                        default:
                            break;
                    }
            }
        };

        //Create intent filter and register new receiver with it
        IntentFilter intFilt = new IntentFilter(BROADCAST_ACTION);
        registerReceiver(brParams, intFilt);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Deregister receiver
        unregisterReceiver(brParams);
    }
}
