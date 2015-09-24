package com.photosynq.app.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.photosynq.app.MainActivity;
import com.photosynq.app.R;
import com.photosynq.app.db.DatabaseHelper;
import com.photosynq.app.http.HTTPConnection;
import com.photosynq.app.http.PhotosynqResponse;
import com.photosynq.app.model.ProjectResult;
import com.photosynq.app.response.MyProjects;
import com.photosynq.app.response.UpdateMacro;
import com.photosynq.app.response.UpdateProject;
import com.photosynq.app.response.UpdateProtocol;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by kalpesh on 30/11/14.
 * <p/>
 * Download data from photosynq website, it return projects, protocols and macros list.
 */
public class SyncHandler {

    private Context context = null;
    private Activity activity = null;
    private MainActivity navigationDrawer;
    private ProgressBar progressBar;

    public SyncHandler(Context context) {
        this.context = context;
    }

    public SyncHandler(Activity activity, ProgressBar progressBar) {
        this.context = activity;
        this.activity = activity;
        this.progressBar = progressBar;
    }

    public SyncHandler(MainActivity navigationDrawer) {
        this.context = navigationDrawer;
        this.activity = navigationDrawer;
        this.navigationDrawer = navigationDrawer;
    }

    public int DoSync() {
                new SyncTask().execute();
                return 0;
        }
    private class SyncTask extends AsyncTask<Integer, Object, String> {
        @Override
        protected void onPreExecute() {
            if (null != progressBar) {
                progressBar.setVisibility(View.VISIBLE);
            }

            if (null != navigationDrawer) {
                navigationDrawer.setProgressBarVisibility(View.VISIBLE);
            }

            super.onPreExecute();
        }

        protected synchronized String doInBackground(Integer... SyncMode) {
            try {

                String isCheckedWifiSync = PrefUtils.getFromPrefs(context, PrefUtils.PREFS_SYNC_WIFI_ON, "0");
                if(isCheckedWifiSync.equals("1")) {

                    ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                    if (mWifi != null && mWifi.isConnected() == false) {//if Wifi is connected

                        return Constants.SUCCESS;
                    }
                }

                Log.d("sync_handler", "in async task");

                // Sync with clear cache


                final Activity mainActivity = (Activity)activity;
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        syncData();
                    }
                });

                return Constants.SUCCESS;

            } catch (Exception e) {
                e.printStackTrace();
                return Constants.SERVER_NOT_ACCESSIBLE;
            }

        }

        private void syncData() {

            PrefUtils.saveToPrefs(context, PrefUtils.PREFS_CURRENT_LOCATION, null);
            // Upload all unuploaded results
            CommonUtils.uploadResults(context, -1);

        }

        // This is called each time you call publishProgress()
        @Override
        protected void onProgressUpdate(Object... result) {
            //Do anything with response..
            PhotosynqResponse delegate = (PhotosynqResponse) result[0];
            if (null != delegate) {
                delegate.onResponseReceived((String) result[1]);
            }
            if (null == result) {
                Log.d("sync_handler", "No results returned");
            }
            super.onProgressUpdate(result);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (null != progressBar) {
                progressBar.setVisibility(View.INVISIBLE);
            }

            if (null != navigationDrawer) {
                navigationDrawer.setProgressBarVisibility(View.INVISIBLE);
            }

        }

    }
}
