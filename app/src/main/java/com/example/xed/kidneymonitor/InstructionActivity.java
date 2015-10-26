package com.example.xed.kidneymonitor;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class InstructionActivity extends Activity {

    private ImageView ivBackground, ivInstructionImage;
    private TextView tvInstructionText;
    private String selectedProcedure;
    private int stage = 0;
    private String[] instructionsStrings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instruction);

        ivBackground = (ImageView) findViewById(R.id.iv_Background);
        ivInstructionImage = (ImageView) findViewById(R.id.iv_InstructionImage);
        tvInstructionText = (TextView) findViewById(R.id.tv_InstructionText);

        Bundle parameters = getIntent().getExtras();
        selectedProcedure = parameters.getString("procedure");

        switch (selectedProcedure){
            case(ConnectionService.STATUS_FILLING):{
                ivBackground.setImageResource(R.drawable.bg_filling);
                instructionsStrings = getResources().getStringArray(R.array.new_instruction_filling);
                break;
            }

            case(ConnectionService.STATUS_DIALYSIS):{
                ivBackground.setImageResource(R.drawable.bg_dialisys);
                instructionsStrings = getResources().getStringArray(R.array.new_instruction_dialysis);
                break;
            }

            case(ConnectionService.STATUS_FLUSH):{
                ivBackground.setImageResource(R.drawable.bg_flush);
                instructionsStrings = getResources().getStringArray(R.array.new_instruction_flush);
                break;
            }

            case(ConnectionService.STATUS_SHUTDOWN):{
                ivBackground.setImageResource(R.drawable.bg_shutdown);
                instructionsStrings = getResources().getStringArray(R.array.new_instruction_shutdown);
                break;
            }

            case(ConnectionService.STATUS_DISINFECTION):{
                ivBackground.setImageResource(R.drawable.bg_disinfection);
                instructionsStrings = getResources().getStringArray(R.array.new_instruction_disinfection);
                break;
            }

            default:
                break;
        }
        updateScreen();

    }

    public void OnClick(View v) {
        switch (v.getId()) {
            case R.id.ib_inst_cancel: {
                if(stage==0)
                    InstructionActivity.this.finish();
                else {
                    stage--;
                    updateScreen();
                }
                break;
            }

            case R.id.ib_inst_ok: {
                stage++;
                updateScreen();
                break;
            }

            default:
                break;
        }
    }

    public void updateScreen()
    {
        switch (selectedProcedure){
            case(ConnectionService.STATUS_FILLING):{
                if(stage==instructionsStrings.length){
                    Intent intent = new Intent(ConnectionService.BROADCAST_ACTION);
                    intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_STATUS);
                    intent.putExtra(ConnectionService.PARAM_ARG, ConnectionService.TASK_ARG_FILLING);
                    sendBroadcast(intent);
                    InstructionActivity.this.finish();
                }
                else{
                    tvInstructionText.setText(instructionsStrings[stage]);
                    ivInstructionImage.setImageResource(R.drawable.instruct_filling);
                }
                break;
            }

            case(ConnectionService.STATUS_DIALYSIS):{
                if(stage==instructionsStrings.length){
                    Intent intent = new Intent(ConnectionService.BROADCAST_ACTION);
                    intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_STATUS);
                    intent.putExtra(ConnectionService.PARAM_ARG, ConnectionService.TASK_ARG_DIALYSIS);
                    sendBroadcast(intent);
                    InstructionActivity.this.finish();
                }
                else{
                    tvInstructionText.setText(instructionsStrings[stage]);
                    ivInstructionImage.setImageResource(R.drawable.instruct_dialysis);
                }
                break;
            }

            case(ConnectionService.STATUS_FLUSH):{
                if(stage==instructionsStrings.length){
                    Intent intent = new Intent(ConnectionService.BROADCAST_ACTION);
                    intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_STATUS);
                    intent.putExtra(ConnectionService.PARAM_ARG, ConnectionService.TASK_ARG_FLUSH);
                    sendBroadcast(intent);
                    InstructionActivity.this.finish();
                }
                else{
                    tvInstructionText.setText(instructionsStrings[stage]);
                    ivInstructionImage.setImageResource(R.drawable.instruct_flush);
                }
                break;
            }

            case(ConnectionService.STATUS_SHUTDOWN):{
                if(stage==instructionsStrings.length){
                    Intent intent = new Intent(ConnectionService.BROADCAST_ACTION);
                    intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_STATUS);
                    intent.putExtra(ConnectionService.PARAM_ARG, ConnectionService.TASK_ARG_SHUTDOWN);
                    sendBroadcast(intent);
                    InstructionActivity.this.finish();
                }
                else{
                    tvInstructionText.setText(instructionsStrings[stage]);
                    ivInstructionImage.setImageResource(R.drawable.instruct_shutdown);
                }
                break;
            }

            case(ConnectionService.STATUS_DISINFECTION):{
                if(stage==instructionsStrings.length){
                    Intent intent = new Intent(ConnectionService.BROADCAST_ACTION);
                    intent.putExtra(ConnectionService.PARAM_TASK, ConnectionService.TASK_SET_STATUS);
                    intent.putExtra(ConnectionService.PARAM_ARG, ConnectionService.TASK_ARG_DISINFECTION);
                    sendBroadcast(intent);
                    InstructionActivity.this.finish();
                }
                else{
                    tvInstructionText.setText(instructionsStrings[stage]);
                    ivInstructionImage.setImageResource(R.drawable.instruct_disinfection);
                }
                break;
            }

            default:
                break;
        }
    }
}
