package com.photosynq.app.response;

import android.app.ProgressDialog;
import android.content.Context;
import android.widget.Toast;

import com.photosynq.app.HTTP.HTTPConnection;
import com.photosynq.app.HTTP.PhotosynqResponse;
import com.photosynq.app.R;
import com.photosynq.app.db.DatabaseHelper;
import com.photosynq.app.utils.Constants;
import com.photosynq.app.utils.PrefUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class UpdateData implements PhotosynqResponse{
	private Context context;
	private String rowid;
    DatabaseHelper db;

	
	public UpdateData(Context context, String rowid)
	{
		this.context = context;
		this.rowid = rowid;
	}
	@Override
	public void onResponseReceived(final String result) {

        Thread t = new Thread(new Runnable() {
            public void run() {
                processResult(result);
            }
        });

        t.start();

	}

    private void processResult(String result) {
        System.out.println("data update result :"+result);
        Date date = new Date();
        System.out.println("UpdateData Start onResponseReceived: " + date.getTime());
        try {
            if(result.equals(Constants.SERVER_NOT_ACCESSIBLE))
            {
                //Toast.makeText(context, R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
                String sync_interval = PrefUtils.getFromPrefs(context,PrefUtils.PREFS_SAVE_SYNC_INTERVAL,PrefUtils.PREFS_DEFAULT_VAL);
                Toast.makeText(context, context.getResources().getString(R.string.error_sending_data,sync_interval), Toast.LENGTH_LONG).show();
                return;
            }

            JSONObject jo = new JSONObject(result);
            String status = jo.getString("status");


            if (status.toUpperCase().equals("SUCCESS"))
            {
                //Toast.makeText(context, R.string.data_uploaded_to_server, Toast.LENGTH_LONG).show();
                long row_id = Long.parseLong(rowid);
                if(row_id != -1) {
                    db = DatabaseHelper.getHelper(context);
                    System.out.println("Deleting row id:" + rowid);
                    db.deleteResult(rowid);
                }
            }else {
                //Toast.makeText(context, jo.getString("notice"), Toast.LENGTH_SHORT).show();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        Date date1 = new Date();
        System.out.println("UpdateData End onResponseReceived: " + date1.getTime());
    }

}
