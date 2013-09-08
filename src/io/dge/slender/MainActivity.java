package io.dge.slender;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import static io.dge.slender.Utils.ltoast;

public class MainActivity extends Activity {

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private Button toggleServiceButton;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

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

    // Called by tapping big button
    public void toggleService(View _) {
        toggleServiceButtonView(!SlenderService.started);

        if (SlenderService.started) {
            stopService(new Intent(this, SlenderService.class));
        } else {
            startService(new Intent(this, SlenderService.class));
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


}
