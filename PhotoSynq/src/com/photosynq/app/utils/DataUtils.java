package com.photosynq.app.utils;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.photosynq.app.UpdateData;
import com.photosynq.app.HTTP.HTTPConnection;
import com.photosynq.app.db.DatabaseHelper;
import com.photosynq.app.model.ProjectResult;

public class DataUtils {
/**
 * Download data from photosynq website, it return projects, protocols and macros list. 
 */
	public static void downloadData(Context context) 
	{
		System.out.println("Downloading data..............");
		if (CommonUtils.isConnected(context)) 
		{
			DatabaseHelper db;
			String authToken;
			String email;
			HTTPConnection mProtocolListTask = null;
			HTTPConnection mMacroListTask = null;
			HTTPConnection mUpdateDataTask = null;

			PrefUtils.saveToPrefs(context, PrefUtils.PREFS_CURRENT_LOCATION,
					null);
			authToken = PrefUtils
					.getFromPrefs(context, PrefUtils.PREFS_AUTH_TOKEN_KEY,
							PrefUtils.PREFS_DEFAULT_VAL);
			email = PrefUtils.getFromPrefs(context,
					PrefUtils.PREFS_LOGIN_USERNAME_KEY,
					PrefUtils.PREFS_DEFAULT_VAL);

			
			HTTPConnection mProjListTask = new HTTPConnection();
			mProjListTask
					.execute(context,HTTPConnection.PHOTOSYNQ_PROJECTS_LIST_URL
							+ "user_email=" + email + "&user_token="
							+ authToken, "GET");

			
			mProtocolListTask = new HTTPConnection();
			mProtocolListTask.execute(context,
					HTTPConnection.PHOTOSYNQ_PROTOCOLS_LIST_URL + "user_email="
							+ email + "&user_token=" + authToken, "GET");

			
			mMacroListTask = new HTTPConnection();
			mMacroListTask
					.execute(context,HTTPConnection.PHOTOSYNQ_MACROS_LIST_URL
							+ "user_email=" + email + "&user_token="
							+ authToken, "GET");

			//db = new DatabaseHelper(context);
			db = DatabaseHelper.getHelper(context);
			List<ProjectResult> listRecords = db.getAllUnUploadedResults();
			//db.closeDB();
			for (ProjectResult projectResult : listRecords) {
				StringEntity input = null;
				JSONObject request_data = new JSONObject();

				try {
					JSONObject jo = new JSONObject(projectResult.getReading());
					request_data.put("user_email", email);
					request_data.put("user_token", authToken);
					request_data.put("data", jo);
					input = new StringEntity(request_data.toString());
					input.setContentType("application/json");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				UpdateData updateData = new UpdateData(context, null,
						projectResult.getId());
				mUpdateDataTask = new HTTPConnection(input);
				mUpdateDataTask.execute(context,HTTPConnection.PHOTOSYNQ_DATA_URL
						+ projectResult.getProjectId() + "/data.json", "POST");
			}
		}
	}
}