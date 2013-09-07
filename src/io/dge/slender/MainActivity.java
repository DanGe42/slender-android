package io.dge.slender;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class MainActivity extends Activity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener,
        SensorEventListener {

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


    private LocationClient locationClient;
    private LocationRequest locationRequest;
    private Location location;

    private SensorManager sensorManager;
    private Sensor linAccelerometer;

    private TextView latText, lngText, altitudeText, speedText, xText, yText, zText;

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
        speedText = (TextView) findViewById(R.id.loc_speed);
        xText = (TextView) findViewById(R.id.lin_accel_x);
        yText = (TextView) findViewById(R.id.lin_accel_y);
        zText = (TextView) findViewById(R.id.lin_accel_z);

        locationClient = new LocationClient(this, this, this);
        // Create the LocationRequest object
        locationRequest = LocationRequest.create();
        // Use high accuracy
        locationRequest.setPriority(
                LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Set the update interval to 5 seconds
        locationRequest.setInterval(UPDATE_INTERVAL);
        // Set the fastest update interval to 1 second
        locationRequest.setFastestInterval(FASTEST_INTERVAL);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        linAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if (linAccelerometer == null) {
            ltoast("Warning: no linear accelerometer");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // TODO: Actually stop location updates?
        if (locationClient.isConnected()) {
            locationClient.removeLocationUpdates(this);
        }

        locationClient.disconnect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Will also activate location updates
        locationClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!servicesConnected()) {
            Log.e("Location Updates", "Services not connected");
            ltoast("Services not connected");
        } else {
            stoast("Services connected!");
        }
        sensorManager.registerListener(this, linAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /*
         * Handle results returned to the FragmentActivity
         * by Google Play services
         */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        // Decide what to do based on the original request code
        switch (requestCode) {
            case CONNECTION_FAILURE_RESOLUTION_REQUEST :
            /*
             * If the result code is Activity.RESULT_OK, try
             * to connect again
             */
                switch (resultCode) {
                    case Activity.RESULT_OK :
                    /*
                     * Try the request again
                     */
                        // TODO: fill in
                        break;
                }
                // TODO: fill in
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        location = locationClient.getLastLocation();
        latText.setText(String.valueOf(location.getLatitude()));
        lngText.setText(String.valueOf(location.getLongitude()));
        speedText.setText(String.valueOf(location.getSpeed()));

        locationClient.requestLocationUpdates(locationRequest, this);
    }

    @Override
    public void onDisconnected() {
        ltoast("Location disconnected");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            GooglePlayServicesUtil.getErrorDialog(
                    connectionResult.getErrorCode(),
                    this,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST
            ).show();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i("Location update", "Location changed");
        latText.setText(String.valueOf(location.getLatitude()));
        lngText.setText(String.valueOf(location.getLongitude()));
        speedText.setText(String.valueOf(location.getSpeed()));
        altitudeText.setText(String.valueOf(location.getAltitude()));
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

    // Define a DialogFragment that displays the error dialog
    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;
        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }
        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }
        // Return a Dialog to the DialogFragment.
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d("Location Updates",
                    "Google Play services is available.");
            // Continue
            return true;
            // Google Play services was not available for some reason
        } else {
            // Get the error code
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                    resultCode,
                    this,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST);

            // If Google Play services can provide an error dialog
            if (errorDialog != null) {
                // Create a new DialogFragment for the error dialog
                ErrorDialogFragment errorFragment =
                        new ErrorDialogFragment();
                // Set the dialog in the DialogFragment
                errorFragment.setDialog(errorDialog);
                // Show the error dialog in the DialogFragment
                errorFragment.show(getFragmentManager(),
                        "Location Updates");
            }
        }

        return false;
    }

    private void ltoast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    private void stoast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
