package com.photosynq.app.HTTP;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

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

import android.os.AsyncTask;
import android.util.Log;

public class HTTPConnection extends AsyncTask<String, String, String>{
	public static final String PHOTOSYNQ_LOGIN_URL = "http://photosynq.venturit.org/api/v1/sign_in.json";
	public PhotosynqResponse delegate = null;
	private String username;
	private String password;
	private StringEntity input = null;
	
	public HTTPConnection(String username, String password)
	{
		this.username=username;
		this.password=password;
	}
	
	@Override
	protected void onPreExecute() {
		if(null != username && null != password)
		{
			JSONObject credentials = new JSONObject();
			JSONObject user = new JSONObject();	
			try {
					credentials.put("email", username);
					credentials.put("password", password);
					user.put("user", credentials);
					input = new StringEntity(user.toString());
					input.setContentType("application/json");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		super.onPreExecute();	
	}

	@Override
    protected String doInBackground(String... uri) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;
        HttpPost postRequest;
        String responseString = null;
        try {
        	postRequest = new HttpPost(uri[0]);
        	if(null!=input)
        	{
        		postRequest.setEntity(input);
        	}
            response = httpclient.execute(postRequest);
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                responseString = out.toString();
            } else{
                //Closes the connection.
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (ClientProtocolException e) {
            //TODO Handle problems..
        } catch (IOException e) {
            //TODO Handle problems..
        }
        return responseString;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        //Do anything with response..
        delegate.onResponseReceived(result);
        if (null != result)
        {
        	Log.d("PHOTOSYNQ-HTTPConnection",result);
        }
        else 
        {
        	Log.d("PHOTOSYNQ-HTTPConnection","No results returned");
        }
    }
}
