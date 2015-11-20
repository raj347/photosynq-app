package com.photosynq.app;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.photosynq.app.db.DatabaseHelper;
import com.photosynq.app.http.PhotosynqResponse;
import com.photosynq.app.model.AppSettings;
import com.photosynq.app.model.Data;
import com.photosynq.app.model.Macro;
import com.photosynq.app.model.Option;
import com.photosynq.app.model.Protocol;
import com.photosynq.app.model.Question;
import com.photosynq.app.model.ResearchProject;
import com.photosynq.app.questions.QuestionsList;
import com.photosynq.app.utils.CommonUtils;
import com.photosynq.app.utils.Constants;
import com.photosynq.app.utils.PrefUtils;
import com.squareup.picasso.Picasso;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class ProjectDetailsActivity extends ActionBarActivity {

    String projectID;
    private boolean discovermode = false;
    private boolean myprojectssearchmode = false;
    private boolean save_locally = false;
    private ProgressDialog progress;
    private final static String JOIN_PORJECT="+ Join Project";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);//or add in style.xml
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_project_details);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        ColorDrawable newColor = new ColorDrawable(getResources().getColor(R.color.green_light));//your color from res
        newColor.setAlpha(0);//from 0(0%) to 256(100%)
        actionBar.setBackgroundDrawable(newColor);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("");

        DatabaseHelper databaseHelper = DatabaseHelper.getHelper(this);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            projectID = extras.getString(DatabaseHelper.C_PROJECT_ID);
            discovermode = extras.getBoolean(DiscoverFragment.DISCOVER);
            myprojectssearchmode = extras.getBoolean(MyProjectsFragment.MYPROJECTSEARCHMODE);
            ResearchProject project = databaseHelper.getResearchProject(projectID);

            if( !discovermode && !myprojectssearchmode) {
                    loaddetails(project);
            }
            else
            {
                if(null!=project )
                {
                    loaddetails(project);
                }
                else {
                    String authToken = PrefUtils.getFromPrefs(getApplicationContext(), PrefUtils.PREFS_AUTH_TOKEN_KEY, PrefUtils.PREFS_DEFAULT_VAL);
                    String email = PrefUtils.getFromPrefs(getApplicationContext(), PrefUtils.PREFS_LOGIN_USERNAME_KEY, PrefUtils.PREFS_DEFAULT_VAL);
                    new DownloadProjectDetails().execute(getApplicationContext(), Constants.PHOTOSYNQ_PROJECT_DETAILS_URL
                            + projectID + ".json/"
                            + "?user_email=" + email + "&user_token="
                            + authToken);

                    Button takeMeasurementbtn = (Button)findViewById(R.id.btn_take_measurement);
                    takeMeasurementbtn.setText(JOIN_PORJECT);
                }
            }
        }
    }

    private void loaddetails(ResearchProject project )
    {


        SimpleDateFormat outputDate = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);

        ImageView projectImage = (ImageView) findViewById(R.id.im_projectImage);
        Picasso.with(this)
                .load(project.getImageUrl())
                .error(R.drawable.ic_launcher1)
                .into(projectImage);

        ImageView profileImage = (ImageView) findViewById(R.id.user_profile_image);
        String imageUrl = project.getLead_avatar();
        Picasso.with(this)
                .load(imageUrl)
                .error(R.drawable.ic_launcher1)
                .into(profileImage);

        Typeface tfRobotoRegular = CommonUtils.getInstance(this).getFontRobotoRegular();
        Typeface tfRobotoMedium = CommonUtils.getInstance(this).getFontRobotoMedium();

        TextView tvProjetTitle = (TextView) findViewById(R.id.tv_project_name);
        tvProjetTitle.setTypeface(tfRobotoRegular);
        tvProjetTitle.setText(project.getName());

//            TextView tvEndsIn = (TextView) findViewById(R.id.tv_ends_in);
//            tvEndsIn.setTypeface(tfRobotoRegular);

        TextView tvBeta = (TextView) findViewById(R.id.tv_beta);
        tvBeta.setTypeface(tfRobotoMedium);
        String isBeta = project.getBeta();
        if(!"null".equals(isBeta))
        {
            if("true".equals(isBeta)) {
                tvBeta.setVisibility(View.VISIBLE);
                tvBeta.setText("BETA");
            }else{
                tvBeta.setVisibility(View.INVISIBLE);
                tvBeta.setText("");
            }
        }else{
            tvBeta.setVisibility(View.INVISIBLE);
            tvBeta.setText("");
        }

        TextView tvOverview = (TextView) findViewById(R.id.tv_overview);
        tvOverview.setTypeface(tfRobotoRegular);

        final TextView tvOverviewText = (TextView) findViewById(R.id.tv_overview_text);
        tvOverviewText.setTypeface(tfRobotoRegular);
        tvOverviewText.setText(Html.fromHtml(project.getDescription()));


        final TextView tvShowHideOverview = (TextView) findViewById(R.id.show_hide_overview);
        tvShowHideOverview.setTypeface(tfRobotoRegular);
        tvShowHideOverview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if("Read More".equals(tvShowHideOverview.getText())) {
                    tvShowHideOverview.setText("Less");
                    tvOverviewText.setMaxLines(Integer.MAX_VALUE);
                }else{
                    tvShowHideOverview.setText("Read More");
                    tvOverviewText.setLines(2);
                }
            }
        });

        TextView tvInstructions = (TextView) findViewById(R.id.tv_instructions);
        tvInstructions.setTypeface(tfRobotoRegular);

        final TextView tvInstructionsText = (TextView) findViewById(R.id.tv_instructions_text);
        tvInstructionsText.setTypeface(tfRobotoRegular);
        tvInstructionsText.setText(Html.fromHtml(project.getDirToCollab()));

        final TextView tvShowHideInstructions = (TextView) findViewById(R.id.show_hide_instructions);
        tvShowHideInstructions.setTypeface(tfRobotoRegular);
        tvShowHideInstructions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if("Read More".equals(tvShowHideInstructions.getText())) {
                    tvShowHideInstructions.setText("Less");
                    tvInstructionsText.setMaxLines(Integer.MAX_VALUE);
                }else{
                    tvShowHideInstructions.setText("Read More");
                    tvInstructionsText.setMaxLines(5);
                }
            }
        });
    }

    public void take_measurement_click(View view){
        String btnLabel = ((Button)view).getText().toString();
        if(btnLabel.equals(JOIN_PORJECT))
        {
            String authToken = PrefUtils.getFromPrefs(getApplicationContext(), PrefUtils.PREFS_AUTH_TOKEN_KEY, PrefUtils.PREFS_DEFAULT_VAL);
            String email = PrefUtils.getFromPrefs(getApplicationContext(), PrefUtils.PREFS_LOGIN_USERNAME_KEY, PrefUtils.PREFS_DEFAULT_VAL);
            new JoinProject().execute(getApplicationContext(), Constants.PHOTOSYNQ_PROJECT_DETAILS_URL
                    + projectID + "/join.json/"
                    + "?user_email=" + email + "&user_token="
                    + authToken);
            progress = ProgressDialog.show(ProjectDetailsActivity.this, "Please wait . . .","", true);
        }
        else {
//            String userId = PrefUtils.getFromPrefs(this, PrefUtils.PREFS_LOGIN_USERNAME_KEY, PrefUtils.PREFS_DEFAULT_VAL);
//            DatabaseHelper databaseHelper = DatabaseHelper.getHelper(this);
            //TODO shekhar check if appsettings are needed anymore
//            AppSettings appSettings = databaseHelper.getSettings(userId);
//            appSettings.setProjectId(projectID);
//            databaseHelper.updateSettings(appSettings);

//            List<Question> questions = databaseHelper.getAllQuestionForProject(projectID);
//            for (int i = 0; i < questions.size(); i++) {
//                Question question = questions.get(i);
//                int queType = question.getQuestionType();
//                if (queType == Question.USER_DEFINED) { //question type is user selected.
//                    Data data = databaseHelper.getData(userId, projectID, question.getQuestionId());
//                    if (null == data.getValue() || data.getValue().isEmpty()) {
//                        data.setUser_id(userId);
//                        data.setProject_id(projectID);
//                        data.setQuestion_id(question.getQuestionId());
//                        data.setValue(Data.NO_VALUE);
//                        data.setType(Constants.QuestionType.USER_SELECTED.getStatusCode());
//                        databaseHelper.updateData(data);
//                    }
//                }
//            }

            Intent intent = new Intent(this, QuestionsList.class);
            intent.putExtra(DatabaseHelper.C_PROJECT_ID, projectID);
            startActivityForResult(intent, 555);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (resultCode == 555) {

            setResult(555);
            finish();
        }
    }

    private class DownloadProjectDetails extends AsyncTask<Object, Object, ResearchProject> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
//            progress = ProgressDialog.show(getApplicationContext(), "Updating . . .", "Fetching project details", true);
        }

        @Override
        protected ResearchProject doInBackground(Object... uri) {
            HttpClient httpclient = new DefaultHttpClient();
            Context context = (Context) uri[0];
            HttpResponse response = null;
            HttpGet getRequest;
            ResearchProject rp = new ResearchProject();
            String responseString = null;
            if (!CommonUtils.isConnected(context)) {
                return new ResearchProject();
            }
            Log.d("PHTTPC", "in async task");
            try {
                Log.d("PHTTPC", "$$$$ URI" + uri[1]);
                getRequest = new HttpGet((String) uri[1]);
                Log.d("PHTTPC", "$$$$ Executing GET request");
                response = httpclient.execute(getRequest);

                if (null != response) {
                    try {
                        StatusLine statusLine = response.getStatusLine();
                        if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            response.getEntity().writeTo(out);
                            out.close();
                            responseString = out.toString();

                            rp = processResult(context, responseString);

                        } else {
                            //Closes the connection.
                            response.getEntity().getContent().close();
                            throw new IOException(statusLine.getReasonPhrase());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            } catch (IOException e) {
            }
            return rp;
        }

        @Override
        protected void onPostExecute(ResearchProject rp) {
            super.onPostExecute(rp);
            loaddetails(rp);
            if(save_locally)
            {
                save_locally=false;
                Button takeMeasurementbtn = (Button)findViewById(R.id.btn_take_measurement);
                takeMeasurementbtn.setText(R.string.btn_take_measurement);
                Intent intent = new Intent(getApplicationContext(), QuestionsList.class);
                intent.putExtra(DatabaseHelper.C_PROJECT_ID, projectID);
                startActivityForResult(intent, 555);


            }
            //progress.dismiss();
        }

        private ResearchProject processResult(Context context, String result) {

            Date date = new Date();
            System.out.println("Project Details onResponseReceived: " + date.getTime());
            ResearchProject rp = new ResearchProject();
            DatabaseHelper db;
            db = DatabaseHelper.getHelper(context);

            if (null != result) {

                try {
                    JSONObject resultJsonObject = new JSONObject(result);
                    if (resultJsonObject.has("project")) {
                        JSONObject jsonProject = resultJsonObject.getJSONObject("project");

                        //String protocol_ids = jsonProject.getJSONArray("protocol_ids").toString().trim();

                        String projectImageUrl = jsonProject.getString("project_image");//get project image url.
                        JSONObject creatorJsonObj = jsonProject.getJSONObject("creator");//get project creator infos.
                        JSONObject creatorAvatar = creatorJsonObj.getJSONObject("avatar");//
                        JSONArray protocols = jsonProject.getJSONArray("protocols");

                        rp = new ResearchProject(
                                jsonProject.getString("id"),
                                jsonProject.getString("name"),
                                jsonProject.getString("description"),
                                jsonProject.getString("directions_to_collaborators"),
                                creatorJsonObj.getString("id"),
                                jsonProject.getString("start_date"),
                                jsonProject.getString("end_date"),
                                projectImageUrl,
                                jsonProject.getString("beta"),
                                jsonProject.getString("is_contributed"),
                                "",
                                creatorJsonObj.getString("name"),
                                creatorJsonObj.getString("contributions"),
                                creatorAvatar.getString("thumb"),
                                jsonProject.getString("protocol_json")); // remove first and last square bracket and store as a comma separated string
                        if (save_locally) {
                            db.deleteOptions(rp.id);

                            db.deleteQuestions(rp.id);

                            JSONArray customFields = jsonProject.getJSONArray("filters");
                            for (int j = 0; j < customFields.length(); j++) {
                                JSONObject jsonQuestion = customFields.getJSONObject(j);
                                int questionType = Integer.parseInt(jsonQuestion.getString("value_type"));
                                JSONArray optionValuesJArray = jsonQuestion.getJSONArray("value");
                                //Sometime option value is empty i.e we need to set "" parameter.
                                if (optionValuesJArray.length() == 0) {
                                    Option option = new Option(jsonQuestion.getString("id"), "", jsonProject.getString("id"));
                                    db.updateOption(option);
                                }
                                for (int k = 0; k < optionValuesJArray.length(); k++) {
                                    if (Question.PROJECT_DEFINED == questionType) { //If question type is project_defined then save options.
                                        String getSingleOption = optionValuesJArray.getString(k);
                                        Option option = new Option(jsonQuestion.getString("id"), getSingleOption, jsonProject.getString("id"));
                                        db.updateOption(option);
                                    } else if (Question.PHOTO_TYPE_DEFINED == questionType) { //If question type is photo_type then save options and option image.
                                        JSONObject options = optionValuesJArray.getJSONObject(k);
                                        String optionString = options.getString("answer");
                                        String optionImage = options.getString("medium");//get option image if question type is Photo_Type
                                        Option option = new Option(jsonQuestion.getString("id"), optionString + "," + optionImage, jsonProject.getString("id"));
                                        db.updateOption(option);
                                    }
                                }

                                Question question = new Question(
                                        jsonQuestion.getString("id"),
                                        jsonProject.getString("id"),
                                        jsonQuestion.getString("label"),
                                        questionType);
                                db.updateQuestion(question);
                                String protocol_ids = "";

                                for (int proto = 0; proto < protocols.length(); proto++) {
                                    if (!protocol_ids.isEmpty())
                                    {
                                        protocol_ids = protocol_ids +",";
                                    }
                                    JSONObject protocolobj = protocols.getJSONObject(proto);
                                    String id = protocolobj.getString("id");
                                    protocol_ids = protocol_ids + id;

                                    System.out.println("Protocol ID " + id);
                                    Protocol protocol = new Protocol(id,
                                            protocolobj.getString("name"),
                                            protocolobj.getString("protocol_json"),
                                            protocolobj.getString("description"),
                                            protocolobj.getString("macro_id"), "slug",
                                            protocolobj.getString("pre_selected"));
                                    JSONObject macroobject = protocolobj.getJSONObject("macro");
                                    Macro macro = new Macro(macroobject.getString("id"),
                                            macroobject.getString("name"),
                                            macroobject.getString("description"),
                                            macroobject.getString("default_x_axis"),
                                            macroobject.getString("default_y_axis"),
                                            macroobject.getString("javascript_code"),
                                            "slug");
                                    System.out.println("Macro ID " + macro.getId());
                                    db.updateMacro(macro);

                                    db.updateProtocol(protocol);
                                }
                                rp.setProtocols_ids(protocol_ids);
                            }
                            db.updateResearchProject(rp);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Date date1 = new Date();
            System.out.println("Project details End onResponseReceived: " + date1.getTime());
            return rp;
        }

    }

    private class JoinProject extends AsyncTask<Object, Object, String>{
        private StringEntity input = null;

        public JoinProject() {}
        @Override
        protected String doInBackground(Object... uri) {
            HttpClient httpclient = new DefaultHttpClient();
            Context context = (Context)uri[0];
            HttpResponse response = null;
            String responseString= null;
            HttpPost postRequest;
            if(!CommonUtils.isConnected(context))
            {
                return Constants.SERVER_NOT_ACCESSIBLE;
            }
            try {
                Log.d("join project", "$$$$ URI"+uri[1]);
                        postRequest = new HttpPost((String)uri[1]);
                    if(null!=input)
                    {
                        postRequest.setEntity(input);
                    }
                    Log.d("join project", "$$$$ Executing POST request");
                    response = httpclient.execute(postRequest);

                if (null != response) {
                    try {
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
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            } catch (IOException e) {
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String result) {

            super.onPostExecute(result);
            progress.dismiss();
            if (null == result)
            {
                Toast.makeText(getApplicationContext(),"There was a problem joining this project, please try again!",Toast.LENGTH_LONG).show();
                Log.d("PHTTPC","No results returned");
            }
            else {
                try {
                    JSONObject jobj = new JSONObject(result);

                    if (jobj.getString("status").equals("success")) {
                        save_locally = true;
                        String authToken = PrefUtils.getFromPrefs(getApplicationContext(), PrefUtils.PREFS_AUTH_TOKEN_KEY, PrefUtils.PREFS_DEFAULT_VAL);
                        String email = PrefUtils.getFromPrefs(getApplicationContext(), PrefUtils.PREFS_LOGIN_USERNAME_KEY, PrefUtils.PREFS_DEFAULT_VAL);
                        new DownloadProjectDetails().execute(getApplicationContext(), Constants.PHOTOSYNQ_PROJECT_DETAILS_URL
                                + projectID + ".json/"
                                + "?user_email=" + email + "&user_token="
                                + authToken);
                    } else {
                        Toast.makeText(getApplicationContext(), jobj.getString("notice"), Toast.LENGTH_LONG).show();
                        Log.d("PHTTPC", "Project joining failed.");

                    }
                }
                    catch(JSONException e){
                        e.printStackTrace();
                    }

            }

        }
    }
}
