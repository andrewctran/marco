package com.example.movr;

import java.util.ArrayList;
import java.util.Calendar;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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
	private SharedPreferences sharedPref;

	private boolean on = false;
	private boolean calibrating = false;
	private long start;
	private float[] gravity = new float[3];
	private float[] acceleration = new float[3];
	private ArrayList<Double> data = new ArrayList<Double>();
	private double[] data_as_array;
	private ArrayList<Long> times = new ArrayList<Long>();
	private long[] times_as_array;
	private final float[] correction = new float[3];
	private ArrayList<float[]> calibration_data = new ArrayList<float[]>();

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		// Miscellaneous inits
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		accelView = (TextView)findViewById(R.id.acceltxt);
		c = Calendar.getInstance(); 
		getSupportActionBar().setDisplayShowHomeEnabled(false);
		getSupportActionBar().setDisplayShowTitleEnabled(false);
		sharedPref = this.getSharedPreferences("com.example.movr.CALIBRATION_DATA", Context.MODE_PRIVATE);
		
		// Sensor setup
		senSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    	senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    	senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    	
    	// Start button setup
    	Button _button = (Button) findViewById(R.id.button1);
    	_button.setText("Start");
    	_button.setOnClickListener(this);
    	accelView.setText("Ready");
	}

	@Override
	public void onSensorChanged(SensorEvent event) 
	{
		if (on == true) 
		{
			Sensor mySensor = event.sensor;
			if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) 
			{	
				times.add(System.currentTimeMillis());

			  	final float alpha = 0.8;

			  	// Isolate the force of gravity with the low-pass filter.
			  	gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
			  	gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
			  	gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

			  	// Remove the gravity contribution with the high-pass filter.
			  	acceleration[0] = event.values[0] - gravity[0] - correction[0];
			 	acceleration[1] = event.values[1] - gravity[1] - correction[1];
			  	acceleration[2] = event.values[2] - gravity[2] - correction[2];			

				if (Math.abs(acceleration[1]) < 0.05) 
				{
					acceleration[1] = (float) 0.0;
				}
				data.add((double)acceleration[1]);
			}
			if (mySensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) 
			{
				if (calibrating == true) 
				{
					calibration_data.add(event.values.clone());
				}
				else 
				{
					acceleration[0] = event.values[0] - correction[0];
					acceleration[1] = event.values[1] - correction[1];
					acceleration[2] = event.values[2] - correction[2];

					if (Math.abs(acceleration[1]) < 0.05) 
					{
						acceleration[1] = (float) 0.0;
					}
					data.add((double)acceleration[1]);
				}
			}
		}
	}

	public double[] kalman(double[] input) 
	{
		// play with these values
		double q = 0.000001;
		double r = 0.01;

		double[] z = new double[input.length + 1];
		// placeholder to start indices at 1
		z[0] = 0.0;
		for (int i = 0; i < 10; i++) {
			z[i + 1] = (double)input[i];
		}

		double[] xhat = new double[input.length + 1];
		double[] xhat_prime = new double[input.length + 1];
		double[] p = new double[input.length + 1];
		double[] p_prime = new double[input.length + 1];
		double[] k = new double[input.length + 1];
		double[] output = new double[input.length];

		// initial guesses
		xhat[0] = 1;
		p[0] = 0.001;

		for (int i = 1; i <= input.length; i++) {
			// time update
			xhat_prime[i] = xhat[i - 1];
			p_prime[i] = p[i - 1] + q;

			// measurement update
			k[i] = p_prime[i] / (p_prime[i] + r);
			xhat[i] = xhat_prime[i] + k[i] * (z[i] - xhat_prime[i]);
			p[i] = (1 - k[i]) * p_prime[i];
		}

		// just for consistency of indices
		for (int i = 1; i <= input.length; i++) {
			output[i - 1] = xhat[i];
		}

		return output;
	}
	 
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) 
	{
	 
	}

	protected void onPause() 
	{
    	super.onPause();
    	senSensorManager.unregisterListener(this);
	}

	protected void onResume() 
	{
	    super.onResume();
	    senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	@Override
	public void onClick(View v) 
	{
	    switch (v.getId()){
	        case R.id.button1:
	        	Button _button = (Button) findViewById(R.id.button1);

	        	if (on == true)
	        	{
	        		senSensorManager.unregisterListener(this);
	        		_button.setText("Start");
	        		on = false;

					data_as_array = new double[data.size()];
					int count = 0;
					for (double entry : data) 
					{
						data_as_array[count] = (double)entry;
						count++;
					}

					data_as_array = kalman(data_as_array);

					times_as_array = new long[times.size()];
					times_as_array = convertLongs(times);

	        		double[] velocity = getVelocity(data_as_array, times_as_array);
	        		double distance = getDistance(velocity, times_as_array);

	        		accelView.setText("You moved " + truncate(distance));
	        	}
	        	else
	        	{ 
					start = System.currentTimeMillis();
					data.clear();
					times.clear();
	        		senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	        		_button.setText("Stop");
	        		on = true;
	        		accelView.setText("...");
	        	}
	            break;
	    }
	}

	private double getDistance(double[] velocity, long[] distribution) 
	{
		double distance = 0;
		double[] durations = getDurations(distribution);
		int i = 0;
		for (double v : velocity) 
		{
			distance += (v * durations[i]);
			i++;
		}
		return distance;
	}

	// Assumes v_initial = 0
	// Measures distance traveled rather than displacement
	private double[] getVelocity(double[] acceleration, long[] distribution) 
	{
		double velocity[] = new double[acceleration.length];
		double[] durations = getDurations(distribution);
		int i = 0;

		for (double a : acceleration) 
		{
			if (i == 0) velocity[i] = 0;
			else velocity[i] = Math.abs(velocity[i - 1] + a * durations[i]);
			i++;
		}
		return velocity;
	}

	private double[] getDurations(long[] times) 
	{
		double[] durations = new double[times.length];
		for (int i = 0; i < times.length; i++) {
			if (i == 0) durations[i] = times[i] - times[0];
			else durations[i] = times[i] - times[i - 1];
		}
		for (int i = 0; i < durations.length; i++) {
			durations[i] /= 1000.0;
		}
		return durations;
	}

	private static double truncate(double d) 
	{
    	return Math.floor(d * 1e2) / 1e2;
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
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		int timeRemaining = 10000;
		if (id == R.id.calibrate) {
			if (on == false && calibrating == false) {
				calibration_data.clear();

				calibrating = true;
				accelView.setText("Set the device on a flat surface...");
				while (calibration_data.size() < 10000) {
					accelView.setText("Set the device on a flat surface for...");
				}

				calibrating = false;

				accelView.setText("Done! Now take it for a spin!");

				float correction_x = 0;
				float correction_y = 0;
				float correction_z = 0;

				for (float[] f : calibration_data) {
					correction_x += f[0];
					correction_y += f[1];
					correction_z += f[2];
				}

				correction_x /= calibration_data.size();
				correction_y /= calibration_data.size();
				correction_z /= calibration_data.size();

				correction[0] = correction_x;
				correction[1] = correction_y;
				correction[2] = correction_z;

				sharedPref.edit().putFloat("correction_x", correction_x).commit();
				sharedPref.edit().putFloat("correction_y", correction_y).commit();
				sharedPref.edit().putFloat("correction_z", correction_z).commit();
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}