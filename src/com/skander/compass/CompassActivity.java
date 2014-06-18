package com.skander.compass;

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
	
	float[] aValues = new float[3];
	float[] mValues = new float[3];
	float orientation[] = new float[3];
	
	float current_heading = 0f;
	float current_pitch = 0f;
	float current_roll = 0f;
	
	static final float ALPHA = 0.15f;
	
	SensorManager sensorManager;
	
    /** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
	  super.onCreate(icicle); 
	  setContentView(R.layout.qibla);
	  image1 = (ImageView) findViewById(R.id.compass1);
	  image2 = (ImageView) findViewById(R.id.compass2);
	  image3 = (ImageView) findViewById(R.id.compass3);
	  rotate(image3, 0f, 58.64f, 0);
	  sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
	  updateOrientation(new float[] {0, 0, 0});
	  	//int loc[] = new int[2];
		//image1.getLocationOnScreen(loc);
		//Log.d("COMPASS : X", String.valueOf(loc[0]));
		//Log.d("COMPASS : Y", String.valueOf(loc[1]));
		//move(image1, 0, 5, 0, 5, 0);
		//image1.getLocationOnScreen(loc);
		//Log.d("COMPASS : X", String.valueOf(loc[0]));
		//Log.d("COMPASS : Y", String.valueOf(loc[1]));
	}
    
    private void updateOrientation(float[] values) {
		TextView view1 = (TextView) findViewById(R.id.view1);
		//TextView view2 = (TextView) findViewById(R.id.view2);
		//TextView view3 = (TextView) findViewById(R.id.view3);
		
		rotate(image2, current_heading, values[0], 210);		
		current_heading = -values[0];
		current_pitch = values[1];
		current_roll = values[2];
		view1.setText("HEADING : "+Float.toString(values[0]));
		//view2.setText("PITCH : "+Float.toString(values[1]));
		//view3.setText("ROLL : "+Float.toString(values[2]));
		//Log.d("COMPASS : HEADING2", String.valueOf(current_heading));
   	}
    
    private float[] calculateOrientation() {
		
		float R[] = new float[9];
		float I[] = new float[9];

		boolean success = SensorManager.getRotationMatrix(R, I, aValues, mValues);
		//SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_X, SensorManager.AXIS_Z, R);
		
		if (success) {
			SensorManager.getOrientation(R, orientation);
      
		    // Convert from Radians to Degrees.
			orientation[0] = (float) Math.toDegrees(orientation[0]);
			orientation[0] = (orientation[0]+360)%360;
			orientation[1] = (float) Math.toDegrees(orientation[1]);
			orientation[2] = (float) Math.toDegrees(orientation[2]);

		}
        return orientation;
    }
    
	protected float[] lowPass(float[] input, float[] output)
	{
		if (output == null)
			return input;

		for (int i = 0; i < input.length; i++)
		{
			output[i] = output[i] + ALPHA * (input[i] - output[i]);
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
		synchronized (this)
		{
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) 
				aValues = lowPass(event.values.clone(), aValues);
				//aValues = event.values;
			if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) 
				mValues = lowPass(event.values.clone(), mValues);
				//mValues = event.values;
			if (aValues != null && mValues != null) {
				updateOrientation(calculateOrientation());
			}
		}
     }

      public void onAccuracyChanged(Sensor sensor, int accuracy) {}
   	};


   	@Override
   	protected void onResume() {
   	  super.onResume();

   	  Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
   	  Sensor magField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

   	  sensorManager.registerListener(sensorEventListener, 
   	                                 accelerometer, 
   	                                 SensorManager.SENSOR_DELAY_NORMAL);
   	  sensorManager.registerListener(sensorEventListener, 
   	                                 magField,
   	                                 SensorManager.SENSOR_DELAY_NORMAL);
   	}

   	@Override
   	protected void onStop() {
   	  sensorManager.unregisterListener(sensorEventListener);
   	  super.onStop();
   	}
}
