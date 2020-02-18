package com.phone.fuxi.catchbest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URISyntaxException;

public class Main2Activity extends AppCompatActivity {



    Intent acresult;
    SeekBar SBsetShutterWidth;
    SeekBar setGreen2Gain;
    SeekBar setBlueGain;
    SeekBar setRedGain;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        String uri = null;
        SharedPreferences stateshared;




        SBsetShutterWidth = (SeekBar)findViewById(R.id.seekBar3);
        setGreen2Gain = (SeekBar)findViewById(R.id.seekBar2);


        stateshared = getSharedPreferences("save", Context.MODE_PRIVATE);
        if(stateshared!=null)
        {
            acresult=new Intent();
            uri = stateshared.getString("intent",null);

//            Toast debugToast = Toast.makeText(getApplicationContext(),"acresult"+uri, Toast.LENGTH_SHORT);
//            debugToast.show();

            if(uri!=null) {

                try {
                    acresult = Intent.parseUri(uri, 0);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }



                int Green2Gain = acresult.getExtras().getInt("Green2Gain",0);
//                Toast debugToast = Toast.makeText(getApplicationContext(),"acresult  Green2Gain "+String.valueOf(Green2Gain), Toast.LENGTH_SHORT);
//                debugToast.show();
                setGreen2Gain.setProgress(Green2Gain);

                int ShutterWidth = acresult.getExtras().getInt("ShutterWidth",0);
                SBsetShutterWidth.setProgress(ShutterWidth);


            }

        }else
        {
            acresult=new Intent();

            acresult.putExtra("ff",666);

        }


        SBsetShutterWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                TextView tv = (TextView)findViewById(R.id.textView4);
                tv.setText(String.valueOf(progress));


                acresult.putExtra("ShutterWidth",progress);
                setResult(RESULT_OK,acresult);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        setGreen2Gain.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                TextView tv = (TextView)findViewById(R.id.textView7);
                tv.setText(String.valueOf(progress));

//                Toast debugToast = Toast.makeText(getApplicationContext(),"seekBar clicked", Toast.LENGTH_SHORT);
//                debugToast.show();
                acresult.putExtra("Green2Gain",progress);
                setResult(RESULT_OK,acresult);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });







    }

    @Override
    protected void onPause() {


        SharedPreferences.Editor editor = null
                ;

        editor = getSharedPreferences("save",Context.MODE_PRIVATE).edit();

//        Toast debugToast = Toast.makeText(getApplicationContext(),"onPause  "+acresult.toUri(0), Toast.LENGTH_SHORT);
//        debugToast.show();

       editor.putString("intent",acresult.toUri(0));
        editor.commit();

        setResult(RESULT_OK,acresult);





        super.onPause();
    }

    @Override
    protected void onStop() {

//        Toast debugToast = Toast.makeText(getApplicationContext(),"seekBar onStop", Toast.LENGTH_SHORT);
//        debugToast.show();

        super.onStop();
    }

    @Override
    protected void onDestroy() {
//        Toast debugToast = Toast.makeText(getApplicationContext(),"seekBar onDestroy", Toast.LENGTH_SHORT);
//        debugToast.show();


        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        outState.putAll(acresult.getExtras());

        Toast debugToast = Toast.makeText(getApplicationContext(),"onSaveInstanceState", Toast.LENGTH_SHORT);
        debugToast.show();



        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {

        Toast debugToast = Toast.makeText(getApplicationContext(),"onRestoreInstanceState", Toast.LENGTH_SHORT);
        debugToast.show();


//        SeekBar setGreen2Gain;
//        SeekBar setBlueGain;
//        SeekBar setRedGain;




        super.onRestoreInstanceState(savedInstanceState);
    }
}
