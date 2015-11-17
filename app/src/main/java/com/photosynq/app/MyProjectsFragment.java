package com.photosynq.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;

import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.photosynq.app.db.DatabaseHelper;
import com.photosynq.app.http.PhotosynqResponse;
import com.photosynq.app.model.Macro;
import com.photosynq.app.model.Option;
import com.photosynq.app.model.Protocol;
import com.photosynq.app.model.Question;
import com.photosynq.app.model.ResearchProject;
import com.photosynq.app.utils.CommonUtils;
import com.photosynq.app.utils.Constants;
import com.photosynq.app.utils.PrefUtils;
import com.photosynq.app.utils.SyncHandler;
import com.squareup.picasso.Picasso;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;


public class MyProjectsFragment extends Fragment implements PhotosynqResponse, SwipeRefreshLayout.OnRefreshListener {

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static int mSectionNumber;

    private DatabaseHelper dbHelper;
    private ProjectArrayAdapter arrayAdapter;
    private ListView projectList;
    private static String mSearchString;
    private String pCreatorId;
    private List<ResearchProject> projects;
    private SwipeRefreshLayout mListViewContainer;
    private ImageView pulltorefreshimage;
    private TextView pulltorefreshtext;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static MyProjectsFragment newInstance(int sectionNumber) {
        MyProjectsFragment fragment = new MyProjectsFragment();
        mSectionNumber = sectionNumber;
        mSearchString = "";
        return fragment;
    }

    public static MyProjectsFragment newInstance(int sectionNumber, String searchString) {
        MyProjectsFragment fragment = new MyProjectsFragment();
        mSectionNumber = sectionNumber;
        mSearchString = searchString;
        return fragment;
    }

    public MyProjectsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_my_project_mode, container, false);
        // SwipeRefreshLayout
        mListViewContainer = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeRefreshLayout_listView);
        onCreateSwipeToRefresh(mListViewContainer);
        dbHelper = DatabaseHelper.getHelper(getActivity());
        pCreatorId = PrefUtils.getFromPrefs(getActivity(), PrefUtils.PREFS_CREATOR_ID, PrefUtils.PREFS_DEFAULT_VAL);


        // Initialize ListView
        projectList = (ListView) rootView.findViewById(R.id.lv_project);
         pulltorefreshimage = (ImageView)rootView.findViewById(R.id.imageView);
         pulltorefreshtext = (TextView)rootView.findViewById(R.id.pulltorefreshtext);
        //showProjectList();
        projects= dbHelper.getAllResearchProjects();

        arrayAdapter = new ProjectArrayAdapter(getActivity(), projects);
        projectList.setAdapter(arrayAdapter);

        if(projects.isEmpty())
        {
            pulltorefreshimage.setVisibility(View.VISIBLE);
            pulltorefreshtext.setVisibility(View.VISIBLE);
        }
        else
        {
            pulltorefreshimage.setVisibility(View.INVISIBLE);
            pulltorefreshtext.setVisibility(View.INVISIBLE);
        }
        if(arrayAdapter.isEmpty())
        {
            if(mSearchString.length() == 0) {
                MainActivity mainActivity = (MainActivity) getActivity();
                SyncHandler syncHandler = new SyncHandler(mainActivity);
                syncHandler.DoSync();
            }
        }

        projectList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
                ResearchProject project = (ResearchProject) projectList.getItemAtPosition(position);
                Intent intent = new Intent(getActivity(), ProjectDetailsActivity.class);
                intent.putExtra(DatabaseHelper.C_PROJECT_ID, project.getId());
                startActivityForResult(intent, 555);

            }
        });


        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (resultCode == 555) {

            ((MainActivity) getActivity()).openDrawer();

        }
    }

//    private void showProjectList() {
//        if(mSearchString.length() > 0) {
//            projects = dbHelper.getAllResearchProjects(mSearchString);
//
//            if(projects == null  || projects.isEmpty()){
//                Toast.makeText(getActivity(), "No project found", Toast.LENGTH_LONG).show();
//            }
//        }else{
//            projects= dbHelper.getAllResearchProjects();
//            if(projects.size() <= 0)
//            {
//                mListViewContainer.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        mListViewContainer.setRefreshing(true);
//                        onRefresh();
//                    }
//                });
//            }
//        }
//
//        arrayAdapter = new ProjectArrayAdapter(getActivity(), projects);
//        projectList.setAdapter(arrayAdapter);
//    }

    /**
     * Download list of research project and set to listview.
     */
    private void refreshProjectList() {
        dbHelper = DatabaseHelper.getHelper(getActivity());
        projects.clear();
        if(mSearchString.length() > 0) {
            projects.addAll(dbHelper.getAllResearchProjects(mSearchString));
            if(projects.isEmpty()){
                Toast.makeText(getActivity(), "No project found", Toast.LENGTH_LONG).show();
            }
        }else{
            projects.addAll(dbHelper.getAllResearchProjects());
        }

        if(projects.isEmpty())
        {
            pulltorefreshimage.setVisibility(View.VISIBLE);
            pulltorefreshtext.setVisibility(View.VISIBLE);
        }
        else
        {
            pulltorefreshimage.setVisibility(View.INVISIBLE);
            pulltorefreshtext.setVisibility(View.INVISIBLE);
        }
        //arrayAdapter = new ProjectArrayAdapter(getActivity(), projects);
        //projectList.setAdapter(arrayAdapter);
        arrayAdapter.notifyDataSetChanged();
        projectList.invalidateViews();
    }

    @Override
    public void onResponseReceived(String result) {

        if(result.equals(Constants.SERVER_NOT_ACCESSIBLE)){
            Toast.makeText(getActivity(), R.string.server_not_reachable, Toast.LENGTH_LONG).show();
        }else {
            refreshProjectList();
        }

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(mSectionNumber);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private class ProjectArrayAdapter extends BaseAdapter implements ListAdapter {

        public final Context context;
        public final List<ResearchProject> projectList;
        LayoutInflater mInflater;

        public ProjectArrayAdapter(Context context, List<ResearchProject> projectList) {
            assert context != null;
            assert projectList != null;

            this.projectList = projectList;
            this.context = context;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            if (null == projectList)
                return 0;
            else
                return projectList.size();
        }

        @Override
        public ResearchProject getItem(int position) {
            if (null == projectList)
                return null;
            else
                return projectList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            ViewHolder holder;

//            TextView tvLastCont = (TextView) convertView.findViewById(R.id.tv_last_contribution);
//            tvLastCont.setTypeface(CommonUtils.getInstance(getActivity()).getFontRobotoRegular());

            if (view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.project_list_item, parent, false);
                holder = new ViewHolder();
                holder.imageview = (ImageView) view.findViewById(R.id.im_projectImage);
                holder.tvProjectName = (TextView) view.findViewById(R.id.tv_project_name);
                holder.tvProjectName.setTypeface(CommonUtils.getInstance(getActivity()).getFontRobotoRegular());
                holder.tvProjectBy = (TextView) view.findViewById(R.id.tv_project_by);
                holder.tvProjectBy.setTypeface(CommonUtils.getInstance(getActivity()).getFontRobotoRegular());
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            ResearchProject project = getItem(position);
            if (null != project) {
                try {
                    holder.tvProjectName.setText(project.getName());
                    holder.tvProjectBy.setText("by " + project.getLead_name());


                    Picasso.with(context)
                            .load(project.getImageUrl())
                            .placeholder(R.drawable.ic_launcher1)
                            .error(R.drawable.ic_launcher1)
                            .resize(200,200)
                            .centerCrop()
                            .into(holder.imageview);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return view;
        }
    }
    static class ViewHolder {
        TextView tvProjectName;
        TextView tvProjectBy;
        ImageView imageview;
    }
    private void onCreateSwipeToRefresh(SwipeRefreshLayout refreshLayout) {

        refreshLayout.setOnRefreshListener(this);

        refreshLayout.setColorScheme(
                android.R.color.holo_blue_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_green_light,
                android.R.color.holo_red_light);

    }
    @Override
    public void onRefresh() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                String authToken = PrefUtils.getFromPrefs(getActivity().getApplicationContext(), PrefUtils.PREFS_AUTH_TOKEN_KEY, PrefUtils.PREFS_DEFAULT_VAL);
                String email = PrefUtils.getFromPrefs(getActivity().getApplicationContext(), PrefUtils.PREFS_LOGIN_USERNAME_KEY, PrefUtils.PREFS_DEFAULT_VAL);
                new DownloadMyProjects().execute(getActivity().getApplicationContext(), Constants.PHOTOSYNQ_MY_PROJECTS_LIST_URL
                        + "user_email=" + email + "&user_token="
                        + authToken);
            }
        }, 10);
    }

    public class DownloadMyProjects extends AsyncTask<Object, Object, String> {
        @Override
        protected String doInBackground(Object... uri) {
            HttpClient httpclient = new DefaultHttpClient();
            Context context = (Context)uri[0];
            HttpResponse response = null;
            HttpGet getRequest;
            String responseString = null;
            if(!CommonUtils.isConnected(context))
            {
                return Constants.SERVER_NOT_ACCESSIBLE;
            }
            Log.d("PHTTPC", "in async task");
            try {
                Log.d("PHTTPC", "$$$$ URI"+uri[1]);
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

                            processResult(context, responseString);

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

            // Notify swipeRefreshLayout that the refresh has finished
            refreshProjectList();
            mListViewContainer.setRefreshing(false);

        }

        private void processResult(Context context,String result) {

            Date date = new Date();
            System.out.println("MyProject Start onResponseReceived: " + date.getTime());

            DatabaseHelper db;
            db = DatabaseHelper.getHelper(context);

            JSONArray jArray;

            if (null != result) {

                try {
                    JSONObject resultJsonObject = new JSONObject(result);
                    if (resultJsonObject.has("projects")) {
                        jArray = resultJsonObject.getJSONArray("projects");

                        for (int i = 0; i < jArray.length(); i++) {
                            JSONObject jsonProject = jArray.getJSONObject(i);


                            JSONObject projectImageUrl = jsonProject.getJSONObject("project_image");//get project image url.
                            JSONObject creatorJsonObj = jsonProject.getJSONObject("creator");//get project creator infos.
                            JSONObject creatorAvatar = creatorJsonObj.getJSONObject("avatar");//
                            JSONArray protocols = jsonProject.getJSONArray("protocols");


                            ResearchProject rp = new ResearchProject(
                                    jsonProject.getString("id"),
                                    jsonProject.getString("name"),
                                    jsonProject.getString("description"),
                                    jsonProject.getString("directions_to_collaborators"),
                                    creatorJsonObj.getString("id"),
                                    jsonProject.getString("start_date"),
                                    jsonProject.getString("end_date"),
                                    projectImageUrl.getString("small"),
                                    jsonProject.getString("beta"),
                                    jsonProject.getString("is_contributed"),
                                    "",
                                    creatorJsonObj.getString("name"),
                                    creatorJsonObj.getString("contributions"),
                                    creatorAvatar.getString("thumb"),
                                    jsonProject.getString("protocol_json")); // remove first and last square bracket and store as a comma separated string

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
                                String protocol_ids = "";//jsonProject.getJSONArray("protocol_ids").toString().trim();
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
                                    System.out.println("Macro ID "+macro.getId());
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
            System.out.println("MyProject End onResponseReceived: " + date1.getTime());
        }

    }


}