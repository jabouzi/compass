package com.skander.compass;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;
import java.util.Arrays;

public class CompassActivity extends Activity {
		
	private ImageView image1;
	private ImageView image2;
	private ImageView image3;	
	private TextView view1;
	private TextView view2;
	private TextView view3;
	private TextView view4;	
	private float[] aValues = new float[3];
	private float[] mValues = new float[3];
	private float[] rValues = new float[3];
	private float[] values = new float[3];
	private float[] mEvents = new float[3];
	private float orientation[] = new float[3];	
	private float current_heading = 0f;
	private float current_pitch = 0f;
	private float current_roll = 0f;		
	static final float ALPHA = 0.25f;
	private SensorManager sensorManager;
	
	
    /** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
	  super.onCreate(icicle); 
	  setContentView(R.layout.qibla);
	  image1 = (ImageView) findViewById(R.id.compass1);
	  image2 = (ImageView) findViewById(R.id.compass2);
	  image3 = (ImageView) findViewById(R.id.compass3);
	  
	  view1 = (TextView) findViewById(R.id.view1);
	  view2 = (TextView) findViewById(R.id.view2);
	  view3 = (TextView) findViewById(R.id.view3);
	  view4 = (TextView) findViewById(R.id.view4);
	  rotate(image3, 0f, 58.64f, 0);
	  sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
	}
    
    private void updateOrientation() {
		//rotate(image2, current_heading, orientation[0], 100);		
		rotate(image2, current_heading, rValues[0], 100);		
		current_heading = -rValues[0];
		current_pitch = orientation[1];
		current_roll = orientation[2];
		view3.setText("PITCH : "+String.valueOf((int)values[1]));
		view4.setText("ROLL : "+String.valueOf((int)values[2]));
   	}
   	
   	private void calculateOrientation() {
		float[] R = new float[9];
		float[] outR = new float[9];

		SensorManager.getRotationMatrix(R, null, aValues, mValues);
		SensorManager.getOrientation(R, values);

		// Convert from Radians to Degrees.
		values[0] = (float) Math.toDegrees(values[0]);
		values[1] = (float) Math.toDegrees(values[1]);
		values[2] = (float) Math.toDegrees(values[2]);
		
		updateOrientation();
    }
    
    
	protected float[] lowPass(float[] input, float[] output)
	{
		if (output == null)	return input;

		for (int i = 0; i < input.length; i++)
		{
			output[i] = ALPHA * input[i] + (1.0f - ALPHA) * input[i];
			//output[i] = output[i] + ALPHA * (input[i] - output[i]);
		}
		return output;
	}
    
    private void rotate(ImageView imgview, float current_degree,  float degree, int duration) {
		RotateAnimation rotateAnim = new RotateAnimation(current_degree, -degree,
		RotateAnimation.RELATIVE_TO_SELF, 0.5f,
		RotateAnimation.RELATIVE_TO_SELF, 0.5f);

		rotateAnim.setDuration(duration);
		rotateAnim.setFillAfter(true);
		imgview.startAnimation(rotateAnim);
	}
	
    private void move(ImageView imgview, float x1,  float x2, float y1, float y2, int duration) {
		TranslateAnimation translateAnim = new TranslateAnimation(TranslateAnimation.RELATIVE_TO_SELF, x1, TranslateAnimation.RELATIVE_TO_SELF, 
		x2, TranslateAnimation.RELATIVE_TO_SELF, y1, TranslateAnimation.RELATIVE_TO_SELF, y2);
		translateAnim.setDuration(duration);
		translateAnim.setFillAfter(true);
		imgview.startAnimation(translateAnim);
	}
	
    private final SensorEventListener sensorEventListener = new SensorEventListener() {
      public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		{
			mEvents[0] = Math.round(event.values[0]);
			mEvents[1] = Math.round(event.values[1]);
			mEvents[2] = Math.round(event.values[2]);
			aValues = lowPass(mEvents, aValues);
			//aValues = lowPass(event.values.clone(), aValues);
		}
		
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) 
		{
			mEvents[0] = Math.round(event.values[0]);
			mEvents[1] = Math.round(event.values[1]);
			mEvents[2] = Math.round(event.values[2]);
			mValues = lowPass(mEvents, mValues);
			//mValues = lowPass(event.values.clone(), mValues);
		}
		
		if (event.sensor.getType() == Sensor.TYPE_ORIENTATION)
		{
			mEvents[0] = Math.round(event.values[0]);
			mEvents[1] = Math.round(event.values[1]);
			mEvents[2] = Math.round(event.values[2]);
			rValues = lowPass(mEvents, rValues);
			//rValues = mEvents;
			view1.setText("ROT : "+String.valueOf((int)rValues[0]));
		}
		
		if (rValues != null && aValues != null && mValues != null) {
			calculateOrientation();
		}
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
   	};


   	@Override
   	protected void onResume() {
   	  super.onResume();

   	  Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
   	  Sensor magField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
   	  Sensor tilt = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

   	  sensorManager.registerListener(sensorEventListener, 
   	                                 accelerometer, 
   	                                 SensorManager.SENSOR_DELAY_NORMAL);
   	  sensorManager.registerListener(sensorEventListener, 
   	                                 magField,
   	                                 SensorManager.SENSOR_DELAY_NORMAL);
   	  sensorManager.registerListener(sensorEventListener, 
   	                                 tilt,
   	                                 SensorManager.SENSOR_DELAY_NORMAL);
   	}

   	@Override
   	protected void onStop() {
   	  sensorManager.unregisterListener(sensorEventListener);
   	  super.onStop();
   	}
}
