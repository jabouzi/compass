package com.skander.compass;

import android.app.Activity;
import android.content.Context;
import android.widget.TextView;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

public class CompassActivity extends Activity {
	float[] aValues = new float[3];
	float[] mValues = new float[3];
	float current_heading = 0.0f;
	SensorManager sensorManager;
	private TextView view1;
	private TextView view2;
	private TextView view3;
	private TextView view4;	
	static final float ALPHA = 0.25f;
	
    /** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
	  super.onCreate(icicle); 
	  setContentView(R.layout.main);
		view1 = (TextView) findViewById(R.id.view1);
	  view2 = (TextView) findViewById(R.id.view2);
	  view3 = (TextView) findViewById(R.id.view3);
	  view4 = (TextView) findViewById(R.id.view4);
	  sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
	  updateOrientation(new float[] {0, 0, 0});
	}
    
    private void updateOrientation(float[] values) {
		current_heading = normalizeDegree(values[0]);
		view4.setText("ORIENTATION : "+String.valueOf(current_heading));
   	}
    
    private float[] calculateOrientation() {
      float[] values = new float[3];
      float[] R = new float[9];
      float[] outR = new float[9];
      
      	//int rotation = Compatibility.getRotation(this);
//
		//if (rotation == 1) {
			//SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_X, SensorManager.AXIS_MINUS_Z, outR);
		//} else {
			//SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_Z, outR);
		//}
//
		//SensorManager.getOrientation(outR, values);

      SensorManager.getRotationMatrix(R, null, aValues, mValues);
      //SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_X, SensorManager.AXIS_Z, outR);
      //SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_Y, SensorManager.AXIS_Z, outR);
//
      SensorManager.getOrientation(R, values);

      // Convert from Radians to Degrees.
      values[0] = (float) Math.toDegrees(values[0]);
      values[1] = (float) Math.toDegrees(values[1]);
      values[2] = (float) Math.toDegrees(values[2]);

      return values;
    }
    
    private final SensorEventListener sensorEventListener = new SensorEventListener() {
      public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
          //aValues = event.values;
          aValues = lowPass(event.values.clone(), aValues);
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
          //mValues = event.values;
          mValues = lowPass(event.values.clone(), mValues);

        updateOrientation(calculateOrientation());
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
   	                                 SensorManager.SENSOR_DELAY_FASTEST);
   	  sensorManager.registerListener(sensorEventListener, 
   	                                 magField,
   	                                 SensorManager.SENSOR_DELAY_FASTEST);
   	}

   	@Override
   	protected void onStop() {
   	  sensorManager.unregisterListener(sensorEventListener);
   	  super.onStop();
   	}
   	
   	private float[] lowPass(float[] input, float[] output)
	{
		if (output == null)	return input;

		for (int i = 0; i < input.length; i++)
		{
			output[i] = output[i] + ALPHA * (input[i] - output[i]);
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
}
