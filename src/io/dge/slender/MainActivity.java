package io.dge.slender;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements
        SensorEventListener,
        LocationListener {

    // Global constants
    /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    // Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;
    // Update frequency in seconds
    public static final int UPDATE_INTERVAL_IN_SECONDS = 5;
    // Update frequency in milliseconds
    private static final long UPDATE_INTERVAL =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    // The fastest update frequency, in seconds
    private static final int FASTEST_INTERVAL_IN_SECONDS = 1;
    // A fast frequency ceiling in milliseconds
    private static final long FASTEST_INTERVAL =
            MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;


    private LocationManager locationManager;

    private SensorManager sensorManager;
    private Sensor linAccelerometer;

    private TextView latText, lngText, altitudeText, accuracyText, speedText,
            xText, yText, zText;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        latText = (TextView) findViewById(R.id.latitude);
        lngText = (TextView) findViewById(R.id.longitude);
        altitudeText = (TextView) findViewById(R.id.altitude);
        accuracyText = (TextView) findViewById(R.id.accuracy);
        speedText = (TextView) findViewById(R.id.loc_speed);
        xText = (TextView) findViewById(R.id.lin_accel_x);
        yText = (TextView) findViewById(R.id.lin_accel_y);
        zText = (TextView) findViewById(R.id.lin_accel_z);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        linAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if (linAccelerometer == null) {
            ltoast("Warning: no linear accelerometer");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        locationManager.removeUpdates(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }

    private void displayLocation(Location location) {
        latText.setText(String.valueOf(location.getLatitude()));
        lngText.setText(String.valueOf(location.getLongitude()));
        altitudeText.setText(String.valueOf(location.getAltitude()));
        accuracyText.setText(String.valueOf(location.getAccuracy()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, linAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        displayLocation(lastKnownLocation);
    }

    @Override
    public void onLocationChanged(Location location) {
        displayLocation(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] values = event.values;
        xText.setText(String.valueOf(values[0]));
        yText.setText(String.valueOf(values[1]));
        zText.setText(String.valueOf(values[2]));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            ltoast("Warning: unreliable accelerometer");
        }

        linAccelerometer = sensor;
    }

    private void ltoast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    private void stoast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
