package com.example.xed.kidneymonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;


public class ParamsActivity extends ActionBarActivity {

    private TextView tvDPumpFlow1, tvDPumpFlow1Min, tvDPumpFlow1Max;
    private TextView tvDPumpFlow2, tvDPumpFlow2Min, tvDPumpFlow2Max;
    private TextView tvDPumpFlow3, tvDPumpFlow3Min, tvDPumpFlow3Max;
    private TextView tvDUFVolume, tvDUFVolumeMin, tvDUFVolumeMax;

    private TextView tvDPress1, tvDPress1Min, tvDPress1Max;
    private TextView tvDPress2, tvDPress2Min, tvDPress2Max;
    private TextView tvDPress3, tvDPress3Min, tvDPress3Max;

    private TextView tvDTemp, tvDTempMin, tvDTempMax;
    private TextView tvDCond, tvDCondMin, tvDCondMax;

    private TextView tvDCur1, tvDCur2, tvDCur3, tvDCur4;

    private ProgressBar pbDPumpFlow1, pbDPumpFlow2, pbDPumpFlow3, pbDUFVolume;
    private ProgressBar pbDPress1, pbDPress2, pbDPress3, pbDTemp, pbDCond;
    private ProgressBar pbDCur1, pbDCur2, pbDCur3, pbDCur4;

    private BroadcastReceiver brParams;
    public final static String PARAM_TASK = "SetParams";
    public final static String PARAM_ARG = "arg";
    public final static String BROADCAST_ACTION = "SetParams";

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

        pbDPumpFlow1 = (ProgressBar) findViewById(R.id.pb_DPumpFlow1);
            pbDPumpFlow1.setMax(200);
            pbDPumpFlow1.setProgress(ConnectionService.DPUMP1FLOW);
        tvDPumpFlow1Min = (TextView) findViewById(R.id.tv_DPumpFlow1Min);
        tvDPumpFlow1 = (TextView) findViewById(R.id.tv_DPumpFlow1);
            tvDPumpFlow1.setText(String.valueOf(Math.round(ConnectionService.DPUMP1FLOW)));
        tvDPumpFlow1Max = (TextView) findViewById(R.id.tv_DPumpFlow1Max);

        pbDPumpFlow2 = (ProgressBar) findViewById(R.id.pb_DPumpFlow2);
            pbDPumpFlow2.setMax(200);
            pbDPumpFlow2.setProgress(ConnectionService.DPUMP2FLOW);
        tvDPumpFlow1Min = (TextView) findViewById(R.id.tv_DPumpFlow1Min);
        tvDPumpFlow2 = (TextView) findViewById(R.id.tv_DPumpFlow2);
            tvDPumpFlow2.setText(String.valueOf(Math.round(ConnectionService.DPUMP2FLOW)));
        tvDPumpFlow1Max = (TextView) findViewById(R.id.tv_DPumpFlow1Max);

        pbDPumpFlow3 = (ProgressBar) findViewById(R.id.pb_DPumpFlow3);
            pbDPumpFlow3.setMax(200);
            pbDPumpFlow3.setProgress(ConnectionService.DPUMP3FLOW);
        tvDPumpFlow1Min = (TextView) findViewById(R.id.tv_DPumpFlow1Min);
        tvDPumpFlow3 = (TextView) findViewById(R.id.tv_DPumpFlow3);
         tvDPumpFlow3.setText(String.valueOf(Math.round(ConnectionService.DPUMP3FLOW)));
        tvDPumpFlow1Max = (TextView) findViewById(R.id.tv_DPumpFlow1Max);

        pbDUFVolume = (ProgressBar) findViewById(R.id.pb_DUFVolume);
        tvDUFVolumeMin = (TextView) findViewById(R.id.tv_DUFVolumeMin);
        tvDUFVolume = (TextView) findViewById(R.id.tv_DUFVolume);
        tvDUFVolumeMax = (TextView) findViewById(R.id.tv_DUFVolumeMax);

        pbDPress1 = (ProgressBar) findViewById(R.id.pb_DPress1);
            //pbDPress1.setMax(Math.round(ConnectionService.DPRESS1MAX));
        tvDPress1Min = (TextView) findViewById(R.id.tv_DPress1Min);
            tvDPress1Min.setText(String.valueOf(String.valueOf(ConnectionService.DPRESS1MIN)));
        tvDPress1 = (TextView) findViewById(R.id.tv_DPress1);
        tvDPress1Max = (TextView) findViewById(R.id.tv_DPress1Max);
            tvDPress1Max.setText(String.valueOf(ConnectionService.DPRESS1MAX));

        pbDPress2 = (ProgressBar) findViewById(R.id.pb_DPress2);
            //pbDPress2.setMax(Math.round(ConnectionService.DPRESS2MAX));
        tvDPress2Min = (TextView) findViewById(R.id.tv_DPress2Min);
            tvDPress2Min.setText(String.valueOf(ConnectionService.DPRESS2MIN));
        tvDPress2 = (TextView) findViewById(R.id.tv_DPress2);
        tvDPress2Max = (TextView) findViewById(R.id.tv_DPress2Max);
            tvDPress2Max.setText(String.valueOf(ConnectionService.DPRESS2MAX));

        pbDPress3 = (ProgressBar) findViewById(R.id.pb_DPress3);
            //pbDPress3.setMax(Math.round(ConnectionService.DPRESS3MAX));
        tvDPress3Min = (TextView) findViewById(R.id.tv_DPress3Min);
            tvDPress3Min.setText(String.valueOf(ConnectionService.DPRESS3MIN));
        tvDPress3 = (TextView) findViewById(R.id.tv_DPress3);
        tvDPress3Max = (TextView) findViewById(R.id.tv_DPress3Max);
            tvDPress3Max.setText(String.valueOf(ConnectionService.DPRESS3MAX));

        pbDTemp = (ProgressBar) findViewById(R.id.pb_DTemp);
            //pbDTemp.setMax(Math.round(ConnectionService.DTEMP1MAX));
        tvDTempMin = (TextView) findViewById(R.id.tv_DTempMin);
            tvDTempMin.setText(String.valueOf(Math.round(ConnectionService.DTEMP1MIN)));
        tvDTemp = (TextView) findViewById(R.id.tv_DTemp);
        tvDTempMax = (TextView) findViewById(R.id.tv_DTempMax);
            tvDTempMax.setText(String.valueOf(Math.round(ConnectionService.DTEMP1MAX)));

        pbDCond = (ProgressBar) findViewById(R.id.pb_DCond);
            //pbDCond.setMax(Math.round(ConnectionService.DCOND1MAX));
        tvDCondMin = (TextView) findViewById(R.id.tv_DCondMin);
            tvDCondMin.setText(String.valueOf(Math.round(ConnectionService.DCOND1MIN)));
        tvDCond = (TextView) findViewById(R.id.tv_DCond);
        tvDCondMax = (TextView) findViewById(R.id.tv_DCondMax);
            tvDCondMax.setText(String.valueOf(Math.round(ConnectionService.DCOND1MAX)));

        pbDCur1 = (ProgressBar) findViewById(R.id.pb_DCur1);
            pbDCur1.setMax(500);
        tvDCur1 = (TextView) findViewById(R.id.tv_DCur1);

        pbDCur2 = (ProgressBar) findViewById(R.id.pb_DCur2);
            pbDCur2.setMax(500);
        tvDCur2 = (TextView) findViewById(R.id.tv_DCur2);

        pbDCur3 = (ProgressBar) findViewById(R.id.pb_DCur3);
            pbDCur3.setMax(500);
        tvDCur3 = (TextView) findViewById(R.id.tv_DCur3);

        pbDCur4 = (ProgressBar) findViewById(R.id.pb_DCur4);
            pbDCur4.setMax(500);
        tvDCur4 = (TextView) findViewById(R.id.tv_DCur4);


        /**
         * Initialise broadcast receiver that listens for messages from ConnectionService
         * and sets params screen textviews and progressbars values
         */
        brParams = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                int task = intent.getIntExtra(PARAM_TASK, -1);
                String args = intent.getStringExtra(PARAM_ARG);

                // switch tasks for setting main screen values
                if(ConnectionService.isServiceRunning)
                    switch (task) {

                        case TASK_SET_DUFVOLUME1:
                        {
                            tvDUFVolume.setText(args);
                            break;
                        }

                        case TASK_SET_DPRESS1:
                        {
                            pbDPress1.setMax(0);
                            pbDPress1.setProgress(0);
                            float percent = ((Float.valueOf(args)-ConnectionService.DPRESS1MIN));
                            int progress = Math.round(percent);
                            tvDPress1.setText(args);
                            pbDPress1.setMax(Math.round(ConnectionService.DPRESS1MAX-ConnectionService.DPRESS1MIN));
                            pbDPress1.setProgress(progress);
                            break;
                        }

                        case TASK_SET_DPRESS2:
                        {
                            pbDPress2.setMax(0);
                            pbDPress2.setProgress(0);
                            float percent = ((Float.valueOf(args)-ConnectionService.DPRESS2MIN));
                            int progress = Math.round(percent);
                            tvDPress2.setText(args);
                            pbDPress2.setMax(Math.round(ConnectionService.DPRESS2MAX-ConnectionService.DPRESS2MIN));
                            pbDPress2.setProgress(progress);
                            break;
                        }

                        case TASK_SET_DPRESS3:
                        {
                            pbDPress3.setMax(0);
                            pbDPress3.setProgress(0);
                            float percent = ((Float.valueOf(args)-ConnectionService.DPRESS3MIN));
                            int progress = Math.round(percent);
                            tvDPress3.setText(args);
                            pbDPress3.setMax(Math.round(ConnectionService.DPRESS3MAX-ConnectionService.DPRESS3MIN));
                            pbDPress3.setProgress(progress);
                            break;
                        }

                        case TASK_SET_DTEMP1:
                        {
                            pbDTemp.setMax(0);
                            pbDTemp.setProgress(0);
                            float percent = ((Float.valueOf(args)-ConnectionService.DTEMP1MIN));
                            int progress = Math.round(percent);
                            tvDTemp.setText(args);
                            pbDTemp.setMax(Math.round(ConnectionService.DTEMP1MAX-ConnectionService.DTEMP1MIN));
                            pbDTemp.setProgress(progress);
                            break;
                        }

                        case TASK_SET_DCOND1:
                        {
                            pbDCond.setMax(0);
                            pbDCond.setProgress(0);
                            float percent = ((Float.valueOf(args)-ConnectionService.DCOND1MIN));
                            int progress = Math.round(percent);
                            tvDCond.setText(args);
                            pbDCond.setMax(Math.round(ConnectionService.DCOND1MAX-ConnectionService.DCOND1MIN));
                            pbDCond.setProgress(progress);
                            break;
                        }

                        case TASK_SET_DCUR1:
                        {
                            pbDCur1.setMax(0);
                            pbDCur1.setProgress(0);
                            int progress = Math.round(Float.valueOf(args));
                            tvDCur1.setText(args);
                            pbDCur1.setMax(500);
                            pbDCur1.setProgress(progress);
                            break;
                        }

                        case TASK_SET_DCUR2:
                        {
                            pbDCur2.setMax(0);
                            pbDCur2.setProgress(0);
                            int progress = Math.round(Float.valueOf(args));
                            tvDCur2.setText(args);
                            pbDCur2.setMax(500);
                            pbDCur2.setProgress(progress);
                            break;
                        }

                        case TASK_SET_DCUR3:
                        {
                            pbDCur3.setMax(0);
                            pbDCur3.setProgress(0);
                            int progress = Math.round(Float.valueOf(args));
                            tvDCur3.setText(args);
                            pbDCur3.setMax(500);
                            pbDCur3.setProgress(progress);
                            break;
                        }

                        case TASK_SET_DCUR4:
                        {
                            pbDCur4.setMax(0);
                            pbDCur4.setProgress(0);
                            int progress = Math.round(Float.valueOf(args));
                            tvDCur4.setText(args);
                            pbDCur4.setMax(500);
                            pbDCur4.setProgress(progress);
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
