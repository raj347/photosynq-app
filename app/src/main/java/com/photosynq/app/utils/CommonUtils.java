package com.photosynq.app.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.photosynq.app.response.UpdateData;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by kalpesh on 24/01/15.
 */
public class CommonUtils {

    // App context
    private Context mContext;

    // Singletone class
    private static CommonUtils instance = null;

    // Holds fonts instance
    Typeface uifontFace;
    Typeface openSansLightFace;
    Typeface robotoLightFace;
    Typeface robotoMediumFace;
    Typeface robotoRegularFace;

    private CommonUtils(Context context) {
        mContext = context;
    }

    public static CommonUtils getInstance(Context context){
        if (instance == null)
            instance = new CommonUtils(context);

        return instance;
    }

    public Typeface getFontUiFontSolid() {

        if(uifontFace == null)
            uifontFace = Typeface.createFromAsset(mContext.getAssets(), "uifont-solid.otf");

        return uifontFace;
    }
    public Typeface getFontOpenSansLight() {
        if(openSansLightFace == null)
            openSansLightFace = Typeface.createFromAsset(mContext.getAssets(), "opensans-light.ttf");

        return openSansLightFace;
    }
    public Typeface getFontRobotoLight() {
        if(robotoLightFace == null)
            robotoLightFace = Typeface.createFromAsset(mContext.getAssets(), "roboto-light.ttf");

        return robotoLightFace;
    }
    public Typeface getFontRobotoMedium() {
        if(robotoMediumFace == null)
            robotoMediumFace = Typeface.createFromAsset(mContext.getAssets(), "roboto-medium.ttf");

        return robotoMediumFace;
    }
    public Typeface getFontRobotoRegular() {
        if(robotoRegularFace == null)
            robotoRegularFace = Typeface.createFromAsset(mContext.getAssets(), "roboto-regular.ttf");

        return robotoRegularFace;
    }

    // Invoke this method only on Async task. Do not invoke on UI thread. it will throw exceptions anyway ;)
    public static boolean isConnected(Context context) {
        if (isNetworkAvailable(context)) {
            try {
                HttpURLConnection urlc = (HttpURLConnection) (new URL(Constants.SERVER_URL).openConnection());
                urlc.setRequestProperty("User-Agent", "Test");
                urlc.setRequestProperty("Connection", "close");
                urlc.setConnectTimeout(1500);
                urlc.connect();
                return (urlc.getResponseCode() == 200);
            } catch (IOException e) {
                Log.e("Connectivity", "Error checking internet connection", e);
            }
        } else {
            Log.d("Connectivity", "No network available!");
        }
        return false;
    }


    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }

    //Generate a MD5 hash from given string
    public static String getMD5EncryptedString(String encTarget){
        MessageDigest mdEnc = null;
        try {
            mdEnc = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Exception while encrypting to md5");
            e.printStackTrace();
        } // Encryption algorithm
        mdEnc.update(encTarget.getBytes(), 0, encTarget.length());
        String md5 = new BigInteger(1, mdEnc.digest()).toString(16);
        while ( md5.length() < 32 ) {
            md5 = "0"+md5;
        }
        return md5;
    }

    public synchronized static String uploadResults(Context context, String project_id, String row_id, String result){
        String authToken = PrefUtils.getFromPrefs(context, PrefUtils.PREFS_AUTH_TOKEN_KEY, PrefUtils.PREFS_DEFAULT_VAL);
        String email = PrefUtils.getFromPrefs(context, PrefUtils.PREFS_LOGIN_USERNAME_KEY, PrefUtils.PREFS_DEFAULT_VAL);
        StringEntity input = null;
        String responseString = null;
        JSONObject request_data = new JSONObject();

        try {
            JSONObject jo = new JSONObject(result);
            request_data.put("user_email", email);
            request_data.put("user_token", authToken);
            request_data.put("data", jo);
            input = new StringEntity(request_data.toString());
            input.setContentType("application/json");
        } catch (JSONException e) {
            e.printStackTrace();
            return Constants.SERVER_NOT_ACCESSIBLE;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return Constants.SERVER_NOT_ACCESSIBLE;
        }

        String strDataURI = Constants.PHOTOSYNQ_DATA_URL
                + project_id + "/data.json";

        Log.d("PHOTOSYNQ-HTTPConnection", "$$$$ URI" + strDataURI);

        HttpPost postRequest = new HttpPost(strDataURI);
        if (null != input) {
            postRequest.setEntity(input);
        }
        Log.d("PHOTOSYNQ-HTTPConnection", "$$$$ Executing POST request");
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
                } else {
                    //Closes the connection.
                    response.getEntity().getContent().close();
                    throw new IOException(statusLine.getReasonPhrase());
                }
            }

            UpdateData updateData = new UpdateData(context, row_id);
            updateData.onResponseReceived(responseString);

        } catch (ClientProtocolException e) {
            return Constants.SERVER_NOT_ACCESSIBLE;
        } catch (IOException e) {
            return Constants.SERVER_NOT_ACCESSIBLE;
        }
        return Constants.SUCCESS;

    }

    public static void writeStringToFile(Context context,String fileName, String dataString)
    {
        try {
            File myFile = new File(context.getExternalFilesDir(null), fileName);
            if (myFile.exists()){
                myFile.delete();
            }

            myFile.createNewFile();

            FileOutputStream fos;
            //dataString = dataString.replaceAll("\\{", "{\"time\":\""+time+"\",");
            byte[] data = dataString.getBytes();
            try {
                fos = new FileOutputStream(myFile);
                fos.write(data);
                fos.flush();
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }catch (Exception e) {
            e.printStackTrace();
        }

    }

}
