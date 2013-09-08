package io.dge.slender;

import android.app.*;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import static io.dge.slender.SlenderService.started;
import static io.dge.slender.Utils.ltoast;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private SlenderService service;
    private volatile boolean bound = false;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SlenderService.LocalBinder binder = (SlenderService.LocalBinder) service;
            MainActivity.this.service = binder.getService();
            bound = true;
            (new Thread(new UIUpdater())).start();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
        }
    };

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private Button toggleServiceButton;
    private Fragment gameFragment;
    private String id;

    private volatile boolean updateUI;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        id = Installation.id(this);
        ltoast(this, id + " length " + id.length());
        toggleServiceButton = (Button) findViewById(R.id.start_button);

        if (!servicesConnected()) {
            ltoast(this, "You must enable Google Play Services");
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!servicesConnected()) {
            Log.e("Location Updates", "Services not connected");
            ltoast(this, "Services not connected");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateUI = false;
    }

    // Called by tapping big button
    public void toggleService(View _) {
        toggleServiceButtonView(!started);

        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        if (started) {
            unbindService(connection);
            bound = false;
            transaction.remove(gameFragment);
            transaction.commit();
        } else {
            Intent intent = new Intent(this, SlenderService.class);
            intent.putExtra("id", id);
            bindService(intent, connection, Context.BIND_AUTO_CREATE);

            gameFragment = new GameFragment();
            transaction.replace(R.id.fragment_container, gameFragment);
            transaction.commit();
            updateUI = true;
        }
    }

    private void toggleServiceButtonView(boolean started) {
        if (started) {
            toggleServiceButton.setText(getText(R.string.end_button_text));
            toggleServiceButton.setBackgroundColor(Color.RED);
        } else {
            toggleServiceButton.setText(getText(R.string.start_button_text));
            toggleServiceButton.setBackgroundColor(Color.GREEN);
        }

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

    private class UIUpdater implements Runnable {

        @Override
        public void run() {

            while (bound && updateUI) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Compass compass = (Compass) findViewById(R.id.compass);
                        TextView numPeople = (TextView) findViewById(R.id.num_people);
                        compass.update(service.getDirection());
                        numPeople.setText(String.valueOf(service.getNumPeople()));
                    }
                });

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    break;
                }
            }

            Log.d(TAG, "UI Thread ended");
        }
    }
}
