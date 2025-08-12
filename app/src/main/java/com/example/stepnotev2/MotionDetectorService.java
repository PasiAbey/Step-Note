package com.example.stepnotev2;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class MotionDetectorService extends Service implements SensorEventListener {

    private static final String TAG = "MotionDetectorService";
    private static final int MOTION_DETECTION_INTERVAL = 3000; // 3 seconds
    private static final float MOTION_THRESHOLD = 1.5f; // Movement threshold

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor stepDetector;

    private boolean isDetecting = false;
    private boolean isMoving = false;
    private long lastMotionTime = 0;
    private Handler motionHandler = new Handler();

    private MotionListener motionListener;

    // Accelerometer data
    private float[] lastAccelerometerValues = new float[3];
    private boolean hasLastAccelerometerValues = false;

    // Binder for activity communication
    private final IBinder binder = new MotionDetectorBinder();

    public interface MotionListener {
        void onMotionStarted();
        void onMotionStopped();
        void onMotionUpdate(boolean isMoving);
    }

    public class MotionDetectorBinder extends Binder {
        MotionDetectorService getService() {
            return MotionDetectorService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initializeSensors();
    }

    private void initializeSensors() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Primary: Accelerometer for motion detection
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Secondary: Step detector if available
        stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        Log.d(TAG, "Sensors initialized - Accelerometer: " + (accelerometer != null) +
                ", Step Detector: " + (stepDetector != null));
    }

    public void startMotionDetection() {
        if (isDetecting) return;

        isDetecting = true;
        lastMotionTime = System.currentTimeMillis();

        // Register accelerometer listener
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            Log.d(TAG, "Accelerometer listener registered");
        }

        // Register step detector listener
        if (stepDetector != null) {
            sensorManager.registerListener(this, stepDetector, SensorManager.SENSOR_DELAY_NORMAL);
            Log.d(TAG, "Step detector listener registered");
        }

        // Start motion timeout checker
        startMotionTimeoutChecker();

        Log.d(TAG, "Motion detection started");
    }

    public void stopMotionDetection() {
        if (!isDetecting) return;

        isDetecting = false;
        sensorManager.unregisterListener(this);
        motionHandler.removeCallbacksAndMessages(null);

        Log.d(TAG, "Motion detection stopped");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isDetecting) return;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            handleAccelerometerChange(event);
        } else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            handleStepDetected();
        }
    }

    private void handleAccelerometerChange(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        if (hasLastAccelerometerValues) {
            // Calculate movement magnitude
            float deltaX = Math.abs(x - lastAccelerometerValues[0]);
            float deltaY = Math.abs(y - lastAccelerometerValues[1]);
            float deltaZ = Math.abs(z - lastAccelerometerValues[2]);

            float movementMagnitude = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

            if (movementMagnitude > MOTION_THRESHOLD) {
                onMotionDetected();
            }
        }

        // Store current values for next comparison
        lastAccelerometerValues[0] = x;
        lastAccelerometerValues[1] = y;
        lastAccelerometerValues[2] = z;
        hasLastAccelerometerValues = true;
    }

    private void handleStepDetected() {
        // Step detected from step sensor
        onMotionDetected();
        Log.d(TAG, "Step detected by step sensor");
    }

    private void onMotionDetected() {
        long currentTime = System.currentTimeMillis();
        lastMotionTime = currentTime;

        if (!isMoving) {
            isMoving = true;
            Log.d(TAG, "Motion started");
            if (motionListener != null) {
                motionListener.onMotionStarted();
                motionListener.onMotionUpdate(true);
            }
        }
    }

    private void startMotionTimeoutChecker() {
        motionHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isDetecting) return;

                long currentTime = System.currentTimeMillis();
                long timeSinceLastMotion = currentTime - lastMotionTime;

                if (timeSinceLastMotion > MOTION_DETECTION_INTERVAL) {
                    if (isMoving) {
                        isMoving = false;
                        Log.d(TAG, "Motion stopped (timeout)");
                        if (motionListener != null) {
                            motionListener.onMotionStopped();
                            motionListener.onMotionUpdate(false);
                        }
                    }
                }

                // Check again in 1 second
                motionHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    public void setMotionListener(MotionListener listener) {
        this.motionListener = listener;
    }

    public boolean isMoving() {
        return isMoving;
    }

    public boolean isDetecting() {
        return isDetecting;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used but required by interface
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMotionDetection();
        Log.d(TAG, "MotionDetectorService destroyed");
    }
}