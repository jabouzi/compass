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

public class CompassActivity2 extends Activity {
		
	private ImageView image1;
	private ImageView image2;
	private ImageView image3;	
	private TextView view1;
	private TextView view2;
	private TextView view3;
	private TextView view4;	
	private float[] aValues = new float[3];
	private float[] mValues = new float[3];
	private float[] mEvents = new float[3];
	private float[] mGravity = new float[3];
	private float orientation[] = new float[3];	
	private float current_heading = 0f;
	private float current_pitch = 0f;
	private float current_roll = 0f;	
	private float currentAcceleration = 0;
	private float maxAcceleration = 0; 	
	static final float ALPHA = 0.25f;
	private SensorManager sensorManager;
	private float rValues[] = new float[3];
	private float mAccelCurrent;
	private float mAccelLast; 
	
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
	  //mAccel = 0.00f;
	  //mAccelCurrent = SensorManager.GRAVITY_EARTH;
	  //mAccelLast = SensorManager.GRAVITY_EARTH;
	  //updateOrientation();
	  //Timer updateTimer = new Timer("gForceUpdate");
		//updateTimer.scheduleAtFixedRate(new TimerTask() {
		  //public void run() {
			//updateOrientation();
		  //}
		//}, 0, 100);
	  	//int loc[] = new int[2];
		//image1.getLocationOnScreen(loc);
		//Log.d("COMPASS : X", String.valueOf(loc[0]));
		//Log.d("COMPASS : Y", String.valueOf(loc[1]));
		//move(image1, 0, 5, 0, 5, 0);
		//image1.getLocationOnScreen(loc);
		//Log.d("COMPASS : X", String.valueOf(loc[0]));
		//Log.d("COMPASS : Y", String.valueOf(loc[1]));
	}
    
    private void updateOrientation() {
		//runOnUiThread(new Runnable() {
			//public void run() {
			
				if (current_heading == 0 && rValues[0] == 359) current_heading = 360;
				else if (current_heading == 359 && rValues[0] == 0) current_heading = 360;
				//rotate(image2, current_heading, orientation[0], 100);		
				rotate(image2, current_heading, rValues[0], 100);		
				current_heading = -rValues[0];
				current_pitch = orientation[1];
				current_roll = orientation[2];
				view4.setText("ORIENTATION : "+String.valueOf((int)orientation[0]));
				//view2.setText("PITCH : "+Float.toString(values[1]));
				//view3.setText("ROLL : "+Float.toString(values[2]));
				//Log.d("COMPASS : HEADING2", String.valueOf(current_heading));
			//}
		//});
   	}
    
    private void calculateOrientation() {
		
		float R[] = new float[9];
		//float I[] = new float[9];

		//view1.setText("ACCELERATION 1 : "+String.valueOf((int)mAccel));
		boolean success = SensorManager.getRotationMatrix(R, null, aValues, mValues);
		//SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_X, SensorManager.AXIS_Z, R);
		
		if (success) {
			SensorManager.getOrientation(R, orientation);
      
		    // Convert from Radians to Degrees.
			orientation[0] = (float) Math.toDegrees(orientation[0]);
			//view2.setText("ACCELERATION 2 : "+String.valueOf(currentAcceleration));
			orientation[0] = normalizeDegree(orientation[0]);
			//view3.setText("ORIENTATION 3 : "+String.valueOf(orientation[0]));
			orientation[1] = (float) Math.toDegrees(orientation[1]);
			orientation[2] = (float) Math.toDegrees(orientation[2]);
			updateOrientation();
		}
		
        //return orientation;
    }
    
	protected float[] lowPass(float[] input, float[] output)
	{
		if (output == null)	return input;

		for (int i = 0; i < input.length; i++)
		{
			output[i] = output[i] + ALPHA * (input[i] - output[i]);
			//output[i] = ALPHA * input[i] + (1.0f - ALPHA) * input[i];
			//output[i] = (input[i] * ALPHA) + (output[i] * (1.0f - ALPHA));
		}
		return output;
	}
	
	private float normalizeDegree(float value){
		  value = Math.round(value);
          if(value >= 0.0f && value <= 180.0f){
              return value;
          }else{
              return 180.0f + (180.0f + value);
          }
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
		//synchronized (this)
		//{
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
			{
				mEvents[0] = Math.round(event.values[0]);
				mEvents[1] = Math.round(event.values[1]);
				mEvents[2] = Math.round(event.values[2]);
				aValues = lowPass(mEvents, aValues);
				//aValues = lowPass(event.values.clone(), aValues);
				mGravity = event.values.clone();
				// Shake detection
				float x = mGravity[0];
				float y = mGravity[1];
				float z = mGravity[2];
				mAccelCurrent = (float)Math.round(Math.sqrt(x*x + y*y + z*z)) - SensorManager.GRAVITY_EARTH;
				view2.setText("ACCELEROMETER : "+String.valueOf(mAccelCurrent));
			}
			
			if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) 
			{
				view3.setText("MAGNETIC_FIELD : "+String.valueOf(event.values[0]));
				mEvents[0] = Math.round(event.values[0]);
				mEvents[1] = Math.round(event.values[1]);
				mEvents[2] = Math.round(event.values[2]);
				mValues = lowPass(mEvents, mValues);
				//mValues = lowPass(event.values.clone(), mValues);
				//mValues = event.values;
			}
			
			if (event.sensor.getType() == Sensor.TYPE_ORIENTATION)
			{
				rValues = lowPass(event.values.clone(), rValues);
				float aR = 0f;
				//aR = rot % 360;
				//if ( aR < 0 ) { aR += 360; }
				//if ( aR < 180 && (nR > (aR + 180)) ) { rot -= 360; }
				//if ( aR >= 180 && (nR <= (aR - 180)) ) { rot += 360; }
				//rot += (nR - aR);
				if (rValues[0] == 360f) rValues[0] = 0f;
				view1.setText("ROT : "+String.valueOf((int)rValues[0]));
			}
			
			if (rValues != null /*aValues != null && mValues != null*/) {
				calculateOrientation();
				updateOrientation();
			}
		//}
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
