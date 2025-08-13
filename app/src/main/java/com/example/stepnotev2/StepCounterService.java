package com.example.stepnotev2;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

public class StepCounterService extends Service implements SensorEventListener {

    private static final String TAG = "StepCounterService";

    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private DatabaseHelper databaseHelper;
    private int initialStepCount = -1;
    private int todayStepsOffset = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        databaseHelper = new DatabaseHelper(this);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_FASTEST);
            Log.d(TAG, "Step counter sensor registered");
        } else {
            Log.w(TAG, "Step counter sensor not available");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // Restart service if killed
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int totalSteps = (int) event.values[0];

            if (initialStepCount == -1) {
                // First reading - set as baseline
                initialStepCount = totalSteps;
                User currentUser = databaseHelper.getCurrentLoggedInUser();
                if (currentUser != null) {
                    todayStepsOffset = databaseHelper.getTodaySteps(currentUser.getId());
                }
                Log.d(TAG, "Initial step count set: " + initialStepCount);
            } else {
                // Calculate steps since app started
                int stepsSinceStart = totalSteps - initialStepCount;
                int todaySteps = todayStepsOffset + stepsSinceStart;

                // Update database
                User currentUser = databaseHelper.getCurrentLoggedInUser();
                if (currentUser != null) {
                    databaseHelper.updateTodaySteps(currentUser.getId(), todaySteps);

                    // Broadcast update to UI
                    Intent broadcastIntent = new Intent("STEP_COUNT_UPDATED");
                    broadcastIntent.putExtra("step_count", todaySteps);
                    sendBroadcast(broadcastIntent);

                    Log.d(TAG, "Steps updated: " + todaySteps);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for step counter
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}