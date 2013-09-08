package io.dge.slender;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import java.io.IOException;

import static io.dge.slender.Utils.GyroscopeFilter;
import static io.dge.slender.Utils.Triplet;

public class SlenderService extends Service implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener
{
    public class LocalBinder extends Binder {
        SlenderService getService() {
            return SlenderService.this;
        }
    }
    private final IBinder binder = new LocalBinder();

    private static final String TAG = "SlenderService";

    public static boolean started = false;

    // Global constants
    /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
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
    private static final long SERVER_UPDATE_INTERVAL = 2000;
    private static final long BEAT_DURATION = 100;
    private static final int STARTING_BPM = 90;

    private static final int SLENDER_SERVICE_ERROR_NOTIFICATION_ID = 0x100;
    private static final int SLENDER_STICKY_NOTIFICATION_ID = 0x101;

    private LocationClient locationClient;
    private LocationRequest locationRequest;
    private volatile Location location;

    private SensorManager sensorManager;
    private Sensor linAccelerometer;
    private SensorEventListener linAccListener;
    private Sensor gyroscope;
    private SensorEventListener gyroListener;

    private NotificationManager notificationManager;
    private Vibrator vibrator;

    private volatile boolean networkActive;
    private volatile SlenderClient.SessionInfo sessionInfo;
    private String id;

    @Override
    public void onCreate() {
        super.onCreate();

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

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        linAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if (linAccelerometer == null) {
            Notification notification = new Notification.Builder(getApplicationContext())
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(getText(R.string.app_name))
                    .setContentText("No linear accelerometer was detected.")
                    .setContentIntent(
                            PendingIntent.getActivity(getApplicationContext(),
                                    0,
                                    new Intent(this, MainActivity.class),
                                    0
                            ))
                    .getNotification();
            notificationManager.notify(SLENDER_SERVICE_ERROR_NOTIFICATION_ID, notification);
        } else  {
            // Set up linear acceleration listener
            linAccListener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                    if (accuracy >= SensorManager.SENSOR_STATUS_UNRELIABLE) {
                        Log.w(TAG, "Linear acceleration sensor is unreliable");
                    }

                    linAccelerometer = sensor;
                }
            };
        }

        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (gyroscope == null) {
            Notification notification = new Notification.Builder(getApplicationContext())
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(getText(R.string.app_name))
                    .setContentText("No gyroscope was detected.")
                    .setContentIntent(
                            PendingIntent.getActivity(getApplicationContext(),
                                    0,
                                    new Intent(this, MainActivity.class),
                                    0
                            ))
                    .getNotification();
            notificationManager.notify(SLENDER_SERVICE_ERROR_NOTIFICATION_ID, notification);
        } else {
            gyroListener = new SensorEventListener() {
                private long previousTime = -1;
                private GyroscopeFilter gyroscopeFilter = new GyroscopeFilter();

                @Override
                public void onSensorChanged(SensorEvent event) {
                    if (previousTime == -1) {
                        previousTime = System.currentTimeMillis();
                    } else {
                        long currentTime = System.currentTimeMillis();
                        long delay = currentTime - previousTime;

                        float[] values = event.values;
                        Triplet<Double, Double, Double> xyz =
                                gyroscopeFilter.update(values[0], values[1], values[2], delay);

                        previousTime = currentTime;
                    }
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                    if (accuracy >= SensorManager.SENSOR_STATUS_UNRELIABLE) {
                        Log.w(TAG, "Gyroscope sensor is unreliable");
                    }

                    gyroscope = sensor;
                }
            };
        }

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    public IBinder onBind(Intent intent) {
        id = intent.getStringExtra("id");

        locationClient.connect();

        if (linAccListener != null) {
            sensorManager.registerListener(linAccListener, linAccelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (gyroListener != null) {
            sensorManager.registerListener(gyroListener, gyroscope,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }

        Thread thread = new Thread(new NetworkThread());
        networkActive = true;
        thread.start();

        // Start this service in the foreground
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        Notification notification = new Notification.Builder(getApplicationContext())
                .setContentTitle(getText(R.string.app_name))
                .setContentText(getText(R.string.ticker_text))
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(pendingIntent)
                .getNotification();
        startForeground(SLENDER_STICKY_NOTIFICATION_ID, notification);
        started = true;

        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        started = false;
        networkActive = false;

        sensorManager.unregisterListener(linAccListener);
        sensorManager.unregisterListener(gyroListener);

        if (locationClient.isConnected()) {
            locationClient.removeLocationUpdates(this);
        }
        locationClient.disconnect();
        vibrator.cancel();
    }

    @Override
    public void onConnected(Bundle bundle) {
        location = locationClient.getLastLocation();
        locationClient.requestLocationUpdates(locationRequest, this);
    }

    @Override
    public void onDisconnected() {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            Notification notification = new Notification.Builder(getApplicationContext())
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(getText(R.string.app_name))
                    .setContentText("Resolve Google Play Services issue")
                    .setContentIntent(connectionResult.getResolution())
                    .getNotification();

            notificationManager.notify(SLENDER_SERVICE_ERROR_NOTIFICATION_ID, notification);
        } else {
            Notification notification = new Notification.Builder(getApplicationContext())
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(getText(R.string.app_name))
                    .setContentText("Application not compatible with this phone.")
                    .setContentIntent(
                            PendingIntent.getActivity(getApplicationContext(),
                            0,
                            new Intent(this, MainActivity.class),
                            0
                    ))
                    .getNotification();
            notificationManager.notify(SLENDER_SERVICE_ERROR_NOTIFICATION_ID, notification);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
    }

    public SlenderClient.SessionInfo getSessionInfo() {
        return sessionInfo;
    }

    public Direction getDirection() {
        if (sessionInfo != null) {
            return sessionInfo.directionArrow;
        } else {
            return Direction.INVALID;
        }
    }

    public int getNumPeople() {
        if (sessionInfo != null) {
            return sessionInfo.numPartyMembersAround;
        } else {
            return 0;
        }
    }

    private class NetworkThread implements Runnable {
        @Override
        public void run() {
            while (networkActive) {
                if (location != null) {
                    SlenderClient client = null;
                    try  {
                        client = new SlenderClient(id);
                        boolean success = client.sendUserInfo(
                                location.getLatitude(), location.getLongitude(), true);

                        if (success) {
                            Log.d(TAG, "Successfully sent data");
                        } else {
                            Log.e(TAG, "A problem occurred in sending data");
                        }

                        SlenderClient.SessionInfo info = client.fetchSessionInfo();
                        sessionInfo = (info == null) ? client.getSessionInfo() : info;

                        if (sessionInfo == null) {
                            Log.e(TAG, "session info is null");
                        }

                        processInfo(sessionInfo);

                    } catch (IOException e) {
                        Log.e(TAG, "Connection failed. Try again...");

                    } finally {
                        if (client != null) {
                            client.close();
                        }
                    }
                }

                try {
                    Thread.sleep(SERVER_UPDATE_INTERVAL);
                } catch (InterruptedException e) {
                    networkActive = false;
                }
            }
        }
    }

    private void processInfo(SlenderClient.SessionInfo sessionInfo) {
        // Vibrate on intensity
        if (sessionInfo.sessionIntensity > 0) {
            long beat_duration = (long) (1.0 / (STARTING_BPM + 10 * sessionInfo.sessionIntensity) * 60 * 1000);
            vibrator.vibrate(new long[] {beat_duration - BEAT_DURATION, BEAT_DURATION}, 0);
        } else {
            vibrator.cancel();
        }

        Log.d(TAG, "Countdown: " + sessionInfo.newSessionCountdown);
        Log.d(TAG, "Active: " + sessionInfo.isSessionActive);
        Log.d(TAG, "Client Active: " + sessionInfo.isClientActive);
    }
}
