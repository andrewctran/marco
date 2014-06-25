package com.example.movr;

import java.util.ArrayList;
import java.util.Calendar;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
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
	private boolean on = false;

	private float[] acceleration = new float[3];
	private float[] prevAcceleration = new float[3];

	private ArrayList<Double> data = new ArrayList<Double>();
	private double[] data_as_array;

	private long start;
	private ArrayList<Long> times = new ArrayList<Long>();
	private long[] times_as_array;
	private Calendar c;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		// miscellaneous init
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		accelView = (TextView)findViewById(R.id.acceltxt);
		c = Calendar.getInstance(); 
		
		// Sensor setup
		senSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    	senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    	senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    	
    	// Start button setup
    	Button _button = (Button) findViewById(R.id.button1);
    	_button.setText("Start");
    	_button.setOnClickListener(this);
    	accelView.setText("ready");
    	
	}

	@Override
	//
	// NOTE: Should I take the largest of the 3 values in the acceleration array? 
	// The biggest one is probs the direction of travel
	//
	public void onSensorChanged(SensorEvent event) {
		if (on == true) 
		{
			Sensor mySensor = event.sensor;
			if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) 
			{			
				times.add(System.currentTimeMillis());

				// PROCESS SENSOR DATA
				acceleration = highPassFilter(event.values.clone(), prevAcceleration);	// remove DC components

				acceleration = lowPassFilter(event.values.clone(), acceleration);		// double exponential smoothing
				acceleration = lowPassFilter(event.values.clone(), acceleration);		// (low pass filter)

				// thresholding
				// run tests in R to find optimal threshold value
				if (Math.abs(acceleration[1]) < 0.3) {
					acceleration[1] = (float) 0.0;
				}


				// which entry do we want??
				data.add((double)acceleration[1]);
				Log.v("data.add", "" + acceleration[1]);
			}
		}
	}
	
	// High-pass filter to remove DC components
	public float[] highPassFilter(float[] input, float[] output) {
		float[] result = new float[3];
		float kFilteringFactor = 0.1f;
		output[0] = input[0] * kFilteringFactor + output[0] * (1.0f - kFilteringFactor);
		output[1] = input[1] * kFilteringFactor + output[1] * (1.0f - kFilteringFactor);
		output[2] = input[2] * kFilteringFactor + output[2] * (1.0f - kFilteringFactor);
		result[0] = input[0] - output[0];
		result[1] = input[1] - output[1];
		result[2] = input[2] - output[2];
		return result;
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

	// detect sudden movements (sudden spin, tossing phone, etc. that should not influence distance traveled)
	public boolean catchRotation(float[] input) {

	}

	// find the axis along which the user will move with regards to gravity
	public int selectAxis(float[] input) {
		double gravity = 9.8;
		double window = 1.5;
		double minWindow = gravity - window;
		double maxWindow = gravity + window;
		int count = 0;
		boolean[] probable = new boolean[input.length];
		int gAxis = -1;
		int direction = -1;

		// rope up all probable suspects
		for (int i = 0; i < input.length; i++) {
			if (input[i] >= minWindow && input[i] <= maxWindow) {
				count++;
				probable[i] = true;
			}
		}

		// get the direction of gravity
		if (count > 1) {
			int nearest = getNearest(input, gravity);
			if (nearest == -1) {
				Log.v("Axis discovery error", "Could not detect gravity (1)");
			}
			gAxis = nearest;
		}
		else if (count == 1) {
			for (int i = 0; i < probable.length; i++) {
				if (probable[i] == true) {
					gAxis = i;
				}
			}
		}
		else {	// gravity not detected
			Log.v("Axis discovery error", "Could not detect gravity (2)");
		}
		if (gAxis == -1) {
			Log.v("Axis discovery error", "Could not detect gravity (3)");
		}

		//calculate appropriate axis given the axis corresponding to gravity
		if (gAxis == 0) {
			direction = 2;
		}
		else if (gAxis == 1) {
			direction = 2;
		}
		else if (gAxis == 2) {
			direction = 1;
		}
		else {
			Log.v("Axis discovery error", "Could not detect direction of travel");
		}
		return direction;
	}

	// helper for returning closest match to target value in input array
	private int getNearest(float[] input, double target) {
		double diff = Double.POSITIVE_INFINITY;
		double nearest = -1;
		for (int i = 0; i < input.length; i++) {
			double x = (double)input[i];
			if (Math.abs(target - x) <= diff) {
				diff = Math.abs(target - x);
				nearest = i;
			}
		}
		return nearest;
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
	    switch (v.getId()){
	        case R.id.button1:
	        	Button _button = (Button) findViewById(R.id.button1);

	        	if (on == true) //STOP LISTENING...DO CALCULATIONS
	        	{
	        		senSensorManager.unregisterListener(this);
	        		_button.setText("Start");
	        		on = false;

	        		// copy data from ArrayList to array
					data_as_array = new double[data.size()];
					int count = 0;
					for (double entry : data) 
					{
						data_as_array[count] = (double)entry;
						count++;
					}

					// copy update times from ArrayList to array
					times_as_array = new long[times.size()];
					times_as_array = convertLongs(times);

	        		double[] velocity = getVelocity(data_as_array, times_as_array);
	        		double distance = getDistance(velocity, times_as_array);
	        		//Log.v("Sum acceleration", "" + sumArray(data_as_array));
	        		//Log.v("Sum velocity", "" + sumArray(velocity));
	        		//Log.v("Sum time", "" + sumArray(times_as_array));
	        		accelView.setText("You moved: " + distance);
	        	}
	        	else // on == false...START LISTENING
	        	{ 
					//start = c.get(Calendar.SECOND);
					start = System.currentTimeMillis();
					data.clear();
					times.clear();
	        		senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	        		_button.setText("Stop");
	        		on = true;
	        		accelView.setText("walking!");
	        	}
	            break;
	    }
	}

	private double getDistance(double[] velocity, long[] distribution) {
		double distance = 0;
		double[] durations = getDurations(distribution);
		int i = 0;
		//Log.v("velocity array length", "" + velocity.length);
		for (double v : velocity) 
		{
			distance += (v * durations[i]);
			i++;
		}
		return distance;
	}

	// Assumes v_initial = 0
	// potentially poor design decision
	private double[] getVelocity(double[] acceleration, long[] distribution) {
		double velocity[] = new double[acceleration.length];
		double[] durations = getDurations(distribution);
		//velocity[0] = 0;
		int i = 0;
		//Log.v("acceleration array length", "" + acceleration.length);
		//Log.v("time array length", "" + durations.length);
		for (double a : acceleration) 
		{
			if (i == 0) velocity[i] = 0;
			else velocity[i] = velocity[i - 1] + a * durations[i];
			//Log.v("dv/dt", "" + a);
			//Log.v("t", "" + durations[i]);
			//Log.v("raw t", "" + distribution[i]);
			i++;
		}
		return velocity;
	}

	private double[] getDurations(long[] times) {
		double[] durations = new double[times.length];
		for (int i = 0; i < times.length; i++) {
			System.out.println(times[i]);
			if (i == 0) durations[i] = times[i] - times[0];
			else durations[i] = times[i] - times[i - 1];
		}
		for (int i = 0; i < durations.length; i++) {
			durations[i] /= 1000.0;
		}
		return durations;
	}

	private double sumArray(double[] arr) {
		double sum = 0.0;
		for (double x : arr) {
			sum += x;
		}
		return sum;
	}

	private int sumArray(int[] arr) {
		int sum = 0;
		for (int x : arr) {
			sum += x;
		}
		return sum;
	}

	private long sumArray(long[] arr) {
		long sum = 0;
		for (long x : arr) {
			sum += x;
		}
		return sum;
	}

	private long[] convertLongs(ArrayList<Long> longs)
	{
	    long[] ret = new long[longs.size()];
	    for (int i = 0; i < ret.length; i++)
	    {
	        ret[i] = longs.get(i).longValue();
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