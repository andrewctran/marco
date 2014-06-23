package com.example.movr;

import java.util.ArrayList;
import java.util.Calendar;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.os.Build;
import android.hardware.SensorEventListener;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

public class MainActivity extends ActionBarActivity implements SensorEventListener, OnClickListener {
	private SensorManager senSensorManager;
	private Sensor senAccelerometer;
	private TextView accelView;
	private int on = 0;
	private float[] cachedAcceleration = new float[3];
	private ArrayList<Double> accelerationAtTimeT = new ArrayList<Double>();
	private double[] arrayListCopy;
	private int start;
	private ArrayList<Integer> updateTimes = new ArrayList<Integer>();
	private int updateTimesArr[];
	private Calendar c;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		accelView = (TextView)findViewById(R.id.acceltxt);
		c = Calendar.getInstance(); 
		
		// Sensor setup
		senSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    	senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    	senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    	
    	Button _button = (Button) findViewById(R.id.button1);
    	_button.setText("Start");
    	_button.setOnClickListener(this);
    	
	}

	@Override
	//
	// NOTE: Should I take the largest of the 3 values in the acceleration array? 
	// The biggest one is probs the direction of travel
	//
	public void onSensorChanged(SensorEvent event) {
		if (on == 1 && (c.get(Calendar.SECOND) - start >= 10)) {
			updateTimes.add(c.get(Calendar.SECOND));
			float[] rawAcceleration = new float[3];
			float[] results = new float[3];
			
			Sensor mySensor = event.sensor;
			if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {			
				rawAcceleration = event.values;
				cachedAcceleration = highPassFilter(rawAcceleration, cachedAcceleration);
				results = lowPassFilter(cachedAcceleration, results);
				accelerationAtTimeT.add((double) results[1]);
				//accelView.setText("" + Math.sqrt(cachedAcceleration[0]*cachedAcceleration[0] + cachedAcceleration[1]*cachedAcceleration[1] + cachedAcceleration[2]* cachedAcceleration[2]));
				//accelView.setText("" + Math.sqrt(rawAcceleration[0]*rawAcceleration[0] + rawAcceleration[1]*rawAcceleration[1] + rawAcceleration[2]* rawAcceleration[2]));
			}
		}
		else {
			senSensorManager.unregisterListener(this);
			arrayListCopy = new double[accelerationAtTimeT.size()];
			int z = 0;
			for (double a : accelerationAtTimeT) {
				arrayListCopy[z] = (float) a;
				z++;
			}
			updateTimesArr = new int[updateTimes.size()];
			updateTimesArr = convertIntegers(updateTimes);
			accelView.setText("" + getDistance(getVelocity((double[])arrayListCopy)));
		}
	}
	
	// High-pass filter to remove DC components
	public float[] highPassFilter(float[] raw, float[] ramped) {
		float[] output = new float[3];
		float kFilteringFactor = 0.1f;
		ramped[0] = raw[0] * kFilteringFactor + ramped[0] * (1.0f - kFilteringFactor);
		ramped[1] = raw[1] * kFilteringFactor + ramped[1] * (1.0f - kFilteringFactor);
		ramped[2] = raw[2] * kFilteringFactor + ramped[2] * (1.0f - kFilteringFactor);
		output[0] = raw[0] - ramped[0];
		output[1] = raw[1] - ramped[1];
		output[2] = raw[2] - ramped[2];
		return output;
	}
	
	// Low-pass filter to remove noise
	public float[] lowPassFilter(float[] input, float[] output) {
		if (output == null) return input;
		float alpha = 0.15f;
		for (int i = 0; i < input.length; i++) {
			output[i] = output[i] + alpha * (input[i] - output[i]);
		}
		return output;
	}
	 
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	 
	}

	protected void onPause() {
    	super.onPause();
    	senSensorManager.unregisterListener(this);
	}

	protected void onResume() {
	    super.onResume();
	    senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	@Override
	public void onClick(View v) {
	    // TODO Auto-generated method stub
	    switch (v.getId()){
	        case R.id.button1:
	        	Button _button = (Button) findViewById(R.id.button1);
	        	if (on == 1) {
	        		senSensorManager.unregisterListener(this);
	        		_button.setText("Start");
	        		on = 0;
	        	}
	        	else {
					start = c.get(Calendar.SECOND);

	        		senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	        		_button.setText("Stop");
	        		accelerationAtTimeT.clear();
	        		on = 1;
	        	}
	            break;
	    }
	}

	private double getDistance(double[] velocity) {
		double distance = 0;
		int i = 1;
		for (double v : velocity) {
			distance += (v * (updateTimesArr[i] - updateTimesArr[i - 1]));
			i++;
		}
		return distance;
	}

	// Assumes v_initial = 0
	private double[] getVelocity(double[] acceleration) {
		double velocity[] = new double[acceleration.length];
		velocity[0] = 0;
		int i = 1;
		for (double a : acceleration) {
			velocity[i] = velocity[i - 1] + a*(updateTimesArr[i] - updateTimesArr[i - 1]);
			i++;
		}
		return velocity;
	}

	private int[] convertIntegers(ArrayList<Integer> integers)
	{
	    int[] ret = new int[integers.size()];
	    for (int i=0; i < ret.length; i++)
	    {
	        ret[i] = integers.get(i).intValue();
	    }
	    return ret;
	}	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}