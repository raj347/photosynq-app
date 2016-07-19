package com.photosynq.app;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.photosynq.app.db.DatabaseHelper;
import com.photosynq.app.model.Macro;
import com.photosynq.app.model.Option;
import com.photosynq.app.model.ProjectResult;
import com.photosynq.app.model.Protocol;
import com.photosynq.app.model.Question;
import com.photosynq.app.model.ResearchProject;
import com.photosynq.app.response.UpdateData;
import com.photosynq.app.utils.CommonUtils;
import com.photosynq.app.utils.Constants;
import com.photosynq.app.utils.LocationUtils;
import com.photosynq.app.utils.NxDebugEngine;
import com.photosynq.app.utils.PrefUtils;
import com.photosynq.app.utils.SyncHandler;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;


public class DisplayResultsActivity extends ActionBarActivity implements
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    String projectId;
    String protocolId;
    String reading;
    String protocolJson;
    String mConnectedDeviceName;
    String mProtocolName = "";
    String mProtocolDescription = "";
    String appMode;
    private ProgressBar progressBar;

    Button keep;
    Button discard;

    // A request to connect to Location Services
    private LocationRequest mLocationRequest;

    // Stores the current instantiation of the location client in this object
    private GoogleApiClient mLocationClient = null;

    ProgressDialog dialog;
    private boolean keepClickFlag = false;
    private boolean isResultSaved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_display_results);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        progressBar = (ProgressBar) findViewById(R.id.toolbar_progress_bar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar_bg));
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setTitle("Result");

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            projectId = extras.getString(DatabaseHelper.C_PROJECT_ID);
            protocolId = extras.getString(Protocol.ID);
            mConnectedDeviceName = extras.getString(Constants.DEVICE_NAME);
            reading = extras.getString(DatabaseHelper.C_READING);
            protocolJson = extras.getString(DatabaseHelper.C_PROTOCOL_JSON);
            mProtocolName = extras.getString(Protocol.NAME);
            mProtocolDescription = extras.getString(Protocol.DESCRIPTION);
            appMode = extras.getString(Constants.APP_MODE);
            System.out.println(this.getClass().getName()+"############app mode="+appMode);
        }
        keep = (Button)findViewById(R.id.keep_btn);
        discard = (Button)findViewById(R.id.discard_btn);

        if(appMode.equals(Constants.APP_MODE_QUICK_MEASURE))
        {
            keep.setText("Return");
            keep.setBackgroundResource(R.drawable.btn_layout_gray_light);
            keep.setVisibility(View.VISIBLE);
            discard.setText("Measure");
            discard.setBackgroundResource(R.drawable.btn_layout_orange);
            discard.setVisibility(View.VISIBLE);
        }
        reloadWebview();

        // Create a new global location parameters object
        mLocationRequest = LocationRequest.create();
        //  Set the update interval
        mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);
        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Set the interval ceiling to one minute
        mLocationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);
        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        //-------------------- Start your GPS Reading ------------------ //
        dialog = new ProgressDialog(this);
        dialog.setMessage("Acquiring GPS location");
        dialog.setCancelable(false);
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                saveResult();
            }
        });


    }

    /*
     * Called when the Activity is restarted, even before it becomes visible.
     */
    @Override
    public void onStart() {

        super.onStart();

        /*
         * Connect the client. Don't re-start any requests here;
         * instead, wait for onResume()
         */
        mLocationClient.connect();

        isResultSaved = false;
        keepClickFlag = false;

    }

    @Override
    protected void onPause() {
        super.onPause();

        stopLocationUpdates();
    }


    @Override
    public void onResume() {
        super.onResume();
        if (mLocationClient.isConnected()) {
            startLocationUpdates();
        }
    }

    private void  reloadWebview()
    {
        WebView webview = (WebView) findViewById(R.id.webView1);
        webview.clearCache(true); // Clear cache. This Mandates to load webview fresh. This is required because we are dynamically writing javascript files.
        String url = "file:///" + this.getExternalFilesDir(null)+ File.separator+"cellphone.html";
        webview.loadUrl(url);
        webview.getSettings().setJavaScriptEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

    }
    public void keep_click(View view) throws UnsupportedEncodingException, JSONException {

        if(appMode.equals(Constants.APP_MODE_QUICK_MEASURE))
        {
            finish();
        }
        else
        {
            keepClickFlag = true;
            //PrefUtils.saveToPrefs(getApplicationContext(), PrefUtils.PREFS_KEEP_BTN_CLICK, "KeepBtnCLickYes");

            if (!reading.contains("location")) {

                String currLocation = getLocation();

                new CountDownTimer(1000, 1000) {
                    public void onTick(long millisUntilFinished) {

                        System.out.print("@@@@@@@@@@@@@@ test tick");

                    }

                    public void onFinish() {
                        String checkLocation = PrefUtils.getFromPrefs(getApplicationContext(), PrefUtils.PREFS_CURRENT_LOCATION, "");
                        if(checkLocation.equals("")) {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (!isFinishing()) {

                                        new AlertDialog.Builder(DisplayResultsActivity.this, R.style.AppCompatAlertDialogStyle)
                                                .setIcon(android.R.drawable.ic_dialog_alert)
                                                .setMessage("Your location is temporarily not available\n\n" +
                                                        "Check if GPS is turned on.")
                                                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialogInterface, int which) {

                                                                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                                                                dialog.show();
                                                            }

                                                        }

                                                )
                                                .show();

                                    }
                                }
                            });



                        }
                    }
                }.start();



                if(!currLocation.equals(""))

                {

                    saveResult();
                }

            }else{

                saveResult();
            }
        }
    }

    public void discard_click(View view) {

        if(appMode.equals(Constants.APP_MODE_QUICK_MEASURE)){

            Intent intent = new Intent(getApplicationContext(), QuickMeasurmentActivity.class);
            intent.putExtra(Protocol.ID, protocolId);
            intent.putExtra(DatabaseHelper.C_PROTOCOL_JSON, protocolJson);
            intent.putExtra(Constants.DEVICE_NAME, mConnectedDeviceName);
            intent.putExtra(Constants.START_MEASURE, "TRUE");
            intent.putExtra(Protocol.NAME, mProtocolName);
            intent.putExtra(Protocol.DESCRIPTION, mProtocolDescription);
            startActivity(intent);

            finish();
        }else {
            keepClickFlag = false;
            Toast.makeText(this, R.string.result_discarded, Toast.LENGTH_LONG).show();
            view.setVisibility(View.INVISIBLE);
            keep.setVisibility(View.INVISIBLE);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_display_results, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {

        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {

            // Continue
            return true;
            // Google Play services was not available for some reason
        } else {
            // Display an error dialog
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
            if (dialog != null) {
                dialog.show();
//                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
//                errorFragment.setDialog(dialog);
//                errorFragment.show(getSupportFragmentManager(), "PHOTOSYNQ-RESULTACTIVITY");
            }
            return false;
        }
    }

    public String getLocation() {

        // If Google Play Services is available
        if (servicesConnected()) {

            // Get the current location
            Location currentLocation = LocationServices.FusedLocationApi.getLastLocation(mLocationClient);

            if (currentLocation == null) {

                startLocationUpdates();

            } else{

                String currLocation = LocationUtils.getLatLng(this, currentLocation);

                PrefUtils.saveToPrefs(getApplicationContext(), PrefUtils.PREFS_CURRENT_LOCATION, currLocation);
                dialog.dismiss();
              //  Toast.makeText(DisplayResultsActivity.this, "GPS acquisition complete!", Toast.LENGTH_SHORT).show();

            }

            return LocationUtils.getLatLng(this, currentLocation);
        }
        return "";
    }

    protected void startLocationUpdates () {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mLocationClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mLocationClient, this);
    }

    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates();
       // getLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {

        Log.d("PHOTOSYNQ", "Location changed:" + LocationUtils.getLatLng(this, location));
        PrefUtils.saveToPrefs(getApplicationContext(), PrefUtils.PREFS_CURRENT_LOCATION, LocationUtils.getLatLng(this, location));

        if(dialog.isShowing()) {
            dialog.dismiss();
            Toast.makeText(DisplayResultsActivity.this, "GPS acquisition complete!", Toast.LENGTH_SHORT).show();
        }

        if(null != reading && !reading.isEmpty() && keepClickFlag) {
            saveResult();
            reading = "";
            keepClickFlag = false;
        }
    }

    private void saveResult(){

        if(isResultSaved == false) {
            isResultSaved = true;

            int index = Integer.parseInt(PrefUtils.getFromPrefs(this, PrefUtils.PREFS_QUESTION_INDEX, "1"));
            PrefUtils.saveToPrefs(this, PrefUtils.PREFS_QUESTION_INDEX, "" + (index + 1));

            if (!reading.contains("location")) {

                String currentLocation = PrefUtils.getFromPrefs(this, PrefUtils.PREFS_CURRENT_LOCATION, "");
                if(!currentLocation.equals("")) {
                    reading = reading.replaceFirst("\\{", "{\"location\":[" + currentLocation + "],");
                }
            } else {
                String currentLocation = PrefUtils.getFromPrefs(this, PrefUtils.PREFS_CURRENT_LOCATION, "");
                String locationStr = "\"location\":[";
                int locationIdx = reading.indexOf(locationStr);
                String tempReading = reading.substring(0, locationIdx + locationStr.length());
                tempReading += currentLocation;
                tempReading += reading.substring(reading.indexOf("]", locationIdx));

                reading = tempReading;
                //reading = reading.replaceFirst("\"location\":", "{\"location\":[" + currentLocation + "],");
            }


            // Reading store into database if is in correct format (correct json format), Otherwise we discard reading.
            if (isJSONValid(reading)) {

                Log.d("IsJSONValid", "Valid Json");
                new Uploadresult().execute();


            } else {
                Log.d("IsJSONValid", "Invalid Json");
                Toast.makeText(getApplicationContext(), "Error: Invalid JSON. Restart device and try again", Toast.LENGTH_SHORT).show();
            }
// ################
            finish();

        }
    }

    public boolean isJSONValid(String jsonStr) {
        try {
            new JSONObject(jsonStr);
        } catch (JSONException ex) {
            try {
                new JSONArray(jsonStr);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }

    public class Uploadresult extends AsyncTask<Object, Object, String> {
        @Override
        protected String doInBackground(Object... uri) {
            String authToken = PrefUtils.getFromPrefs(getApplicationContext(), PrefUtils.PREFS_AUTH_TOKEN_KEY, PrefUtils.PREFS_DEFAULT_VAL);
            String email = PrefUtils.getFromPrefs(getApplicationContext(), PrefUtils.PREFS_LOGIN_USERNAME_KEY, PrefUtils.PREFS_DEFAULT_VAL);
            StringEntity input = null;
            String responseString = null;
            JSONObject request_data = new JSONObject();

            try {
                JSONObject jo = new JSONObject(reading);
                request_data.put("user_email", email);
                request_data.put("user_token", authToken);
                request_data.put("data", jo);

                PhotoSyncApplication.sApplication.log("content: for upload", request_data.toString(2), "upload");
                input = new StringEntity(request_data.toString());
                input.setContentType("application/json");
            } catch (JSONException e) {
                e.printStackTrace();
                //??return Constants.SERVER_NOT_ACCESSIBLE;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                //??return Constants.SERVER_NOT_ACCESSIBLE;
            }

            String strDataURI = Constants.PHOTOSYNQ_DATA_URL
                    + projectId + "/data.json";

            Log.d("DisplayResultActivity", "$$$$ URI" + strDataURI);

            HttpPost postRequest = new HttpPost(strDataURI);
            if (null != input) {
                postRequest.setEntity(input);
            }
            Log.d("DisplayResultActivity", "$$$$ Executing POST request");
            HttpClient httpclient = new DefaultHttpClient();
            try {
                HttpResponse response = httpclient.execute(postRequest);

                if (null != response) {
                    StatusLine statusLine = response.getStatusLine();
                    if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        response.getEntity().writeTo(out);
                        out.close();
                        responseString = out.toString();
                        final JSONObject jo = new JSONObject(responseString);
                        String status = jo.getString("status");

                        Handler handler = new Handler(Looper.getMainLooper());
                        if (status.toUpperCase().equals("SUCCESS"))
                        {
                            handler.postDelayed(new Runnable() {
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "Success! \nSubmitted", Toast.LENGTH_SHORT).show();
                                }
                            }, 0 );
                        }else if (status.toUpperCase().equals("FAILED"))
                        {
                            handler.postDelayed(new Runnable() {
                                public void run() {
                                    try {
                                        Toast.makeText(getApplicationContext(), "Submission Failed \nError:"+jo.getString("notice"), Toast.LENGTH_SHORT).show();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }, 0 );
                        }else
                        {
                            handler.postDelayed(new Runnable() {
                                public void run() {
                                    DatabaseHelper databaseHelper = DatabaseHelper.getHelper(getApplicationContext());
                                    ProjectResult result = new ProjectResult(projectId, reading, "N");
                                    databaseHelper.createResult(result);
                                    Toast.makeText(getApplicationContext(), "Success! \nCached", Toast.LENGTH_SHORT).show();
                                }
                            }, 0 );
                        }
                    } else {
                        //Closes the connection.
                        response.getEntity().getContent().close();
                        throw new IOException(statusLine.getReasonPhrase());
                    }
                }


            } catch (Exception e) {
                //??return Constants.SERVER_NOT_ACCESSIBLE;
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    public void run() {
                        DatabaseHelper databaseHelper = DatabaseHelper.getHelper(getApplicationContext());
                        ProjectResult result = new ProjectResult(projectId, reading, "N");
                        databaseHelper.createResult(result);
                        Toast.makeText(getApplicationContext(), "Success! \nCached", Toast.LENGTH_SHORT).show();
                    }
                }, 0 );
                //e.printStackTrace();
            }
            return responseString;
        }

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
                        LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

                /*
                * Thrown if Google Play services canceled the original
                * PendingIntent
                */

            } catch (IntentSender.SendIntentException e) {

                // Log the error
                e.printStackTrace();
            }
        } else {

            // If no resolution is available, display a dialog to the user with the error.
            showErrorDialog(connectionResult.getErrorCode());
        }
    }

    private void showErrorDialog(int errorCode) {

        // Get the error dialog from Google Play services
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                errorCode,
                this,
                LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

        // If Google Play services can provide an error dialog
        if (errorDialog != null) {

            errorDialog.show();
        }
    }
}
