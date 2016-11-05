package com.example.myfirstapp;

import android.app.Activity;
//import android.support.v7.app.AppCompatActivity;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import java.text.DecimalFormat;

import android.hardware.SensorManager;
import java.util.LinkedList;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.Button;
import android.widget.TextView;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

public class MainActivity extends Activity implements SensorEventListener {
    TextView DFTView=null;
    TextView tv1=null;
    TextView tv2=null;
    TextView tv3=null;
    TextView AccelView=null;
    Button button=null;
    private SensorManager mSensorManager;
    Sensor linearSensor;
    FFT fft;

    float linear[];
    LinkedList<Float> x = new LinkedList<Float>();
    DecimalFormat precision = new DecimalFormat("0000000.00");
    DecimalFormat precision2 = new DecimalFormat("0.000");
    double[] t;
    int FFTSize ;
    int orientation;
    int SampleRate;
    int iteration;
    int update_rate;
    int max_iterations;
    int one_second_ms = 1000000;
    boolean live = false;
    float accel;
    float freezeI;
    float energy;
    float locoBand;
    float freezeBand;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SampleRate = 64;
        FFTSize = 256;
        orientation = 1;
        iteration = 0;
        update_rate = 2;
        max_iterations = SampleRate/update_rate;

        int SampleDelay = one_second_ms/SampleRate;
        fft = new FFT(FFTSize);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv1 = (TextView) findViewById(R.id.textView1);
        tv2 = (TextView) findViewById(R.id.textView2);
        tv3 = (TextView) findViewById(R.id.textView3);
        DFTView = (TextView) findViewById(R.id.DFTView);
        AccelView = (TextView) findViewById(R.id.AccelView);
        button = (Button) findViewById(R.id.buttonFFT);

        tv1.setVisibility(View.VISIBLE);
        tv2.setVisibility(View.VISIBLE);
        tv3.setVisibility(View.VISIBLE);
        DFTView.setVisibility(View.VISIBLE);
        AccelView.setVisibility(View.VISIBLE);

        tv1.setText("Sample rate: " + SampleRate + "Hz;   DFT Size: " + FFTSize );

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        linearSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        //tv1.append("\n | " + linearSensor.getMinDelay() + " | " + linearSensor.getMaxDelay() + " | " +  linearSensor.getMaximumRange());

        mSensorManager.registerListener(this, linearSensor, SampleDelay,SampleDelay);

        for (int i = 0; i <FFTSize;i++){
            x.add(0f);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this, linearSensor);
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
            linear = event.values.clone();
            iteration++;
            //tv3.setText("0: " + linear[0] + "  1: " + linear[1] +"  2: " + linear[2]);
            //x.add(linear[orientation]);
            x.add(linear[0]*(float)(1000/9.8)); //  m/ s^2
            accel = linear[0];
            x.removeFirst();
        }
        /*AccelView.setText("Live samples:");
        for (Float d: x){
            AccelView.append("\n" + d);
        }*/
        if (live  && iteration > max_iterations){
            iteration = 0;
            DoCalc();
        }
        Log.i("Values " ,"=" + String.valueOf(accel) + "=" + String.valueOf(freezeI) + "=" + String.valueOf(energy) + "=" + String.valueOf(locoBand) + "=" + String.valueOf(freezeBand) );

    }

    public void DoCalc(){
        Float[] x_array = new Float[x.size()];
        Float[] y_array = new Float[x.size()];
        float[] bin_power = new float[x.size()];
        float mean_x=0;
        for(int i = 0; i<x.size();i++){
            y_array[i] = 0f;
        }
        x.toArray(x_array);
        for(int i = 0; i<x.size();i++){
            mean_x = mean_x + x_array[i];
        }
        mean_x = mean_x/x.size();
        for(int i = 0; i<x.size();i++){
            x_array[i] -= mean_x;
        }
        int cutoff_bin_loco = 3*FFTSize/SampleRate;
        int cutoff_bin_freeze = 8*FFTSize/SampleRate;
        float sum_magnitude_loco = 0;
        float sum_magnitude_freeze = 0;
        float power_sum=0;
        float integration_multiplier;
        //tv1.setText(" ");
        fft.fft(x_array,y_array);
        DFTView.setText("Frequency :  Power");
        for (int i = 0; i<=cutoff_bin_freeze;i++){
            //DFTView.append("\n " +i+ ":" + x_array[i] + ":" + y_array[i] );
            bin_power[i] = (y_array[i]*y_array[i]+ x_array[i]*x_array[i])/FFTSize;
            power_sum = power_sum + bin_power[i];
        }
        for (int i = 0; i<=cutoff_bin_freeze;i++){
            //DFTView.append("\n " +i+ ":" + x_array[i] + ":" + y_array[i] );
            integration_multiplier = 1/(float)SampleRate;
            if( i == 1 || i == cutoff_bin_loco || i == cutoff_bin_loco + 1 || i == cutoff_bin_freeze )
                integration_multiplier = integration_multiplier/2;
            if(i >1  && i <=cutoff_bin_loco)
                sum_magnitude_loco = sum_magnitude_loco +  bin_power[i]*integration_multiplier;
            if(i >cutoff_bin_loco  && i <=cutoff_bin_freeze)
                sum_magnitude_freeze = sum_magnitude_freeze +  bin_power[i]*integration_multiplier;
            //tv1.append("\n " + i + " , " + integration_multiplier + ", " + sum_magnitude_freeze);
            DFTView.append("\n " + precision2.format((double)i*SampleRate/(double)FFTSize) + " Hz :  "+  precision.format(bin_power[i]) + "  |  " + precision2.format(100*bin_power[i]/power_sum)  );
        }
        locoBand = sum_magnitude_loco;
        freezeBand = sum_magnitude_freeze;
        freezeI = sum_magnitude_freeze / sum_magnitude_loco;
        energy = sum_magnitude_freeze + sum_magnitude_loco;
        tv2.setText("Freeze Index : " + precision2.format(sum_magnitude_freeze) + "/" + precision2.format(sum_magnitude_loco) + "=" + precision2.format(sum_magnitude_freeze / sum_magnitude_loco) );
        tv3.setText("Energy : " + precision2.format(sum_magnitude_freeze + sum_magnitude_loco) );
    }

    public void Doo(View view){
        live = !(live);
        if(live)
            button.setText("PAUSE");
        if(!live)
            button.setText("RESUME");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu_main; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
