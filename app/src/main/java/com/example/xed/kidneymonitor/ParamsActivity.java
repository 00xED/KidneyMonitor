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
    private TextView tvDTemp1, tvDCond1, tvDCur1;

    private BroadcastReceiver brParams;
    public final static String PARAM_TASK = "task";
    public final static String PARAM_ARG = "arg";
    public final static String BROADCAST_ACTION = "SetParams";

    public final static int TASK_SET_DPUMPFLOW1 = 1;
    public final static int TASK_SET_DPUMPFLOW2 = 2;
    public final static int TASK_SET_DPUMPFLOW3 = 3;
    public final static int TASK_SET_DUFVOLUME1 = 4;
    public final static int TASK_SET_DPRESS1 = 5;
    public final static int TASK_SET_DPRESS2 = 6;
    public final static int TASK_SET_DPRESS3 = 7;
    public final static int TASK_SET_DTEMP1 = 8;
    public final static int TASK_SET_DCOND1 = 9;
    public final static int TASK_SET_DCUR1  = 10;

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

        /**
         * Initialise broadcast receiver that listens for messages from ConnectionService
         * and sets params screen textviews values and images
         */
        brParams = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                int task = intent.getIntExtra(PARAM_TASK, 0);
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
