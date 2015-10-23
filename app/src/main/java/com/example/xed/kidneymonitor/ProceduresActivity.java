package com.example.xed.kidneymonitor;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TableRow;

public class ProceduresActivity extends Activity {

    private RadioButton rbFilling, rbDialisys, rbFlush, rbShutdown, rbDisinfection;
    private ImageView ivFilling, ivDialisys, ivFlush, ivShutdown, ivDisinfection;
    private TableRow trFilling, trDialisys, trFlush, trShutdown, trDisinfection;
    private SharedPreferences sPref;
    private String selectedProcedure = "-1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_procedures);

        rbFilling = (RadioButton) findViewById(R.id.rb_menu_filling);
        rbDialisys = (RadioButton) findViewById(R.id.rb_menu_dialisys);
        rbFlush = (RadioButton) findViewById(R.id.rb_menu_flush);
        rbShutdown = (RadioButton) findViewById(R.id.rb_menu_shutdown);
        rbDisinfection = (RadioButton) findViewById(R.id.rb_menu_disinfection);

        ivFilling = (ImageView) findViewById(R.id.iv_menu_filling);
        ivDialisys = (ImageView) findViewById(R.id.iv_menu_dialisys);
        ivFlush = (ImageView) findViewById(R.id.iv_menu_flush);
        ivShutdown = (ImageView) findViewById(R.id.iv_menu_shutdown);
        ivDisinfection = (ImageView) findViewById(R.id.iv_menu_disinfection);

        trFilling = (TableRow) findViewById(R.id.tr_menu_filling);
        trDialisys = (TableRow) findViewById(R.id.tr_menu_dialisys);
        trFlush = (TableRow) findViewById(R.id.tr_menu_flush);
        trShutdown = (TableRow) findViewById(R.id.tr_menu_shutdown);
        trDisinfection = (TableRow) findViewById(R.id.tr_menu_disinfection);

        switch (ConnectionService.STATUS) {
            case ConnectionService.STATUS_DIALYSIS: {
                disableAllTableRows();
                trFlush.setEnabled(true);
                ivFlush.setImageResource(R.drawable.menu_item_flush);
                break;
            }

            case ConnectionService.STATUS_FILLING: {
                disableAllTableRows();
                trDialisys.setEnabled(true);
                ivDialisys.setImageResource(R.drawable.menu_item_dialisys);
                break;
            }

            case ConnectionService.STATUS_SHUTDOWN: {
                disableAllTableRows();
                trShutdown.setEnabled(true);
                ivShutdown.setImageResource(R.drawable.menu_item_shutdown);
                break;
            }

            case ConnectionService.STATUS_DISINFECTION: {
                disableAllTableRows();
                trShutdown.setEnabled(true);
                ivShutdown.setImageResource(R.drawable.menu_item_shutdown);
                break;
            }

            case ConnectionService.STATUS_FLUSH: {
                disableAllTableRows();
                trShutdown.setEnabled(true);
                ivShutdown.setImageResource(R.drawable.menu_item_shutdown);
                break;
            }

            case ConnectionService.STATUS_READY: {
                switch (ConnectionService.PREV_STATUS) {
                    case ConnectionService.STATUS_DIALYSIS: {
                        disableAllTableRows();
                        trFlush.setEnabled(true);
                        ivFlush.setImageResource(R.drawable.menu_item_flush);
                        break;
                    }

                    case ConnectionService.STATUS_FILLING: {
                        disableAllTableRows();
                        trDialisys.setEnabled(true);
                        ivDialisys.setImageResource(R.drawable.menu_item_dialisys);
                        break;
                    }

                    case ConnectionService.STATUS_SHUTDOWN: {
                        disableAllTableRows();
                        trShutdown.setEnabled(true);
                        ivShutdown.setImageResource(R.drawable.menu_item_shutdown);
                        break;
                    }

                    case ConnectionService.STATUS_DISINFECTION: {
                        disableAllTableRows();
                        trShutdown.setEnabled(true);
                        ivShutdown.setImageResource(R.drawable.menu_item_shutdown);
                        break;
                    }

                    case ConnectionService.STATUS_FLUSH: {
                        disableAllTableRows();
                        trShutdown.setEnabled(true);
                        ivShutdown.setImageResource(R.drawable.menu_item_shutdown);
                        break;
                    }

                    default: {
                        enableAllTableRows();
                        break;
                    }
                }
                break;
            }

            default: {
                enableAllTableRows();
                break;
            }
        }

        sPref = getSharedPreferences(PrefActivity.APP_PREFERENCES, MODE_PRIVATE); //Load preferences
        if (sPref.getBoolean(PrefActivity.TESTMODE, false))
            enableAllTableRows();
    }

    public void OnClick(View v) {
        switch (v.getId()) {
            case R.id.tr_menu_filling:{
                resetRadioButtons();
                rbFilling.setChecked(true);
                selectedProcedure = ConnectionService.STATUS_FILLING;
                break;
            }

            case R.id.tr_menu_dialisys:{
                resetRadioButtons();
                rbDialisys.setChecked(true);
                selectedProcedure = ConnectionService.STATUS_DIALYSIS;
                break;
            }

            case R.id.tr_menu_flush:{
                resetRadioButtons();
                rbFlush.setChecked(true);
                selectedProcedure = ConnectionService.STATUS_FLUSH;
                break;
            }

            case R.id.tr_menu_shutdown:{
                resetRadioButtons();
                rbShutdown.setChecked(true);
                selectedProcedure = ConnectionService.STATUS_SHUTDOWN;
                break;
            }

            case R.id.tr_menu_disinfection:{
                resetRadioButtons();
                rbDisinfection.setChecked(true);
                selectedProcedure = ConnectionService.STATUS_DISINFECTION;
                break;
            }

            case R.id.ib_cancel:{
                ProceduresActivity.this.finish();
                break;
            }

            case R.id.ib_ok:{
                if(!selectedProcedure.equals("-1")){
                    Intent intent = new Intent(this, InstructionActivity.class);
                    Bundle parameters = new Bundle();
                    parameters.putString("procedure", selectedProcedure); //Your id
                    intent.putExtras(parameters); //Put your id to your next Intent
                    startActivity(intent);
                }

                break;
            }

            default:
                break;
        }
    }

    public void resetRadioButtons(){
        rbFilling.setChecked(false);
        rbDialisys.setChecked(false);
        rbFlush.setChecked(false);
        rbShutdown.setChecked(false);
        rbDisinfection.setChecked(false);
    }

    public void disableAllTableRows(){
        trFilling.setEnabled(false);
        ivFilling.setImageResource(R.drawable.menu_item_filling_disabled);
        trDialisys.setEnabled(false);
        ivDialisys.setImageResource(R.drawable.menu_item_dialisys_disabled);
        trFlush.setEnabled(false);
        ivFlush.setImageResource(R.drawable.menu_item_flush_disabled);
        trShutdown.setEnabled(false);
        ivShutdown.setImageResource(R.drawable.menu_item_shutdown_disabled);
        trDisinfection.setEnabled(false);
        ivDisinfection.setImageResource(R.drawable.menu_item_disinfection_disabled);
    }

    public void enableAllTableRows(){
        trFilling.setEnabled(true);
        ivFilling.setImageResource(R.drawable.menu_item_filling);
        trDialisys.setEnabled(true);
        ivDialisys.setImageResource(R.drawable.menu_item_dialisys);
        trFlush.setEnabled(true);
        ivFlush.setImageResource(R.drawable.menu_item_flush);
        trShutdown.setEnabled(true);
        ivShutdown.setImageResource(R.drawable.menu_item_shutdown);
        trDisinfection.setEnabled(true);
        ivDisinfection.setImageResource(R.drawable.menu_item_disinfection);
    }
}
