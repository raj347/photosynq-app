package com.photosynq.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.photosynq.app.db.DatabaseHelper;
import com.photosynq.app.model.ResearchProject;
import com.photosynq.app.utils.CommonUtils;
import com.photosynq.app.utils.Constants;
import com.photosynq.app.utils.PrefUtils;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class DiscoverFragment extends Fragment {

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static int mSectionNumber;

    private ProjectArrayAdapter arrayAdapter;
    private ListView projectList;
    private static String mSearchString;
    private List<ResearchProject> projects;
    private int pageno;
    private int totalpages = 0;
    private ProgressDialog progress;
    private boolean loading=false;
    public final static String DISCOVER="discover";

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static DiscoverFragment newInstance(int sectionNumber) {
        DiscoverFragment fragment = new DiscoverFragment();
        mSectionNumber = sectionNumber;
        mSearchString = "";
        return fragment;
    }

    public static DiscoverFragment newInstance(int sectionNumber, String searchString) {
        DiscoverFragment fragment = new DiscoverFragment();
        mSectionNumber = sectionNumber;
        mSearchString = searchString;
        return fragment;
    }

    public DiscoverFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_discover_project_mode, container, false);
        // Initialize ListView
        pageno=1;
        projectList = (ListView) rootView.findViewById(R.id.lv_project);
        projects = new ArrayList<ResearchProject>();
        arrayAdapter = new ProjectArrayAdapter(getActivity().getApplicationContext(), projects);
        projectList.setAdapter(arrayAdapter);

        getprojects();

        projectList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                boolean loadMore = firstVisibleItem + visibleItemCount >= totalItemCount-1;
                if(loadMore) {
                    if (pageno < totalpages) {
                        if(!loading) {
                            getprojects();
                        }
                    }
                }

            }
        });
        projectList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
                ResearchProject project = (ResearchProject) projectList.getItemAtPosition(position);
                Intent intent = new Intent(getActivity(), ProjectDetailsActivity.class);
                intent.putExtra(DatabaseHelper.C_PROJECT_ID, project.getId());
                intent.putExtra(DISCOVER, true);
                startActivityForResult(intent, 555);

            }
        });


        return rootView;
    }

    private void getprojects(){
        String authToken = PrefUtils.getFromPrefs(getActivity().getApplicationContext(), PrefUtils.PREFS_AUTH_TOKEN_KEY, PrefUtils.PREFS_DEFAULT_VAL);
        String email = PrefUtils.getFromPrefs(getActivity().getApplicationContext(), PrefUtils.PREFS_LOGIN_USERNAME_KEY, PrefUtils.PREFS_DEFAULT_VAL);
        new DownloadDiscoverProjects().execute(getActivity().getApplicationContext(), Constants.PHOTOSYNQ_PROJECTS_LIST_URL
                + "all=1" + "&page=" + pageno
                + "&user_email=" + email + "&user_token="
                + authToken);

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
//            projects=dbHelper.getAllResearchProjects(mSearchString);
//            if(projects == null  || projects.isEmpty()){
//                Toast.makeText(getActivity(), "No project found", Toast.LENGTH_LONG).show();
//            }
//        }
//        else
//        {
//            projects = dbHelper.getAllResearchProjects();
//        }
//
//        arrayAdapter = new ProjectArrayAdapter(getActivity().getApplicationContext(), projects);
//        projectList.setAdapter(arrayAdapter);
//    }

    /**
     * Download list of research project and set to listview.
     */
//    private void refreshProjectList() {
//        dbHelper = DatabaseHelper.getHelper(getActivity());
//        projects.clear();
//        if(mSearchString.length() > 0) {
//            projects.addAll(dbHelper.getAllResearchProjects(mSearchString));
//
//            if(projects.isEmpty()){
//                Toast.makeText(getActivity(), "No project found", Toast.LENGTH_LONG).show();
//            }
//        }
//        else
//        {
//            projects.addAll(dbHelper.getAllResearchProjects());
//        }
//
//        //arrayAdapter = new ProjectArrayAdapter(getActivity(), projects);
//        //projectList.setAdapter(arrayAdapter);
//        arrayAdapter.notifyDataSetChanged();
//        projectList.invalidateViews();
//    }
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(mSectionNumber);
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
                            .resize(200, 200)
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

    public class DownloadDiscoverProjects extends AsyncTask<Object, Object, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loading=true;
            progress = ProgressDialog.show(getActivity(), "Updating . . .","Fetching more projects", true);
        }

        @Override
        protected String doInBackground(Object... uri) {
            HttpClient httpclient = new DefaultHttpClient();
            Context context = (Context) uri[0];
            HttpResponse response = null;
            HttpGet getRequest;
            String responseString = null;
            if (!CommonUtils.isConnected(context)) {
                return Constants.SERVER_NOT_ACCESSIBLE;
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
            progress.dismiss();
            arrayAdapter.notifyDataSetChanged();
            projectList.invalidateViews();
            loading=false;

        }

        private void processResult(Context context, String result) {

            Date date = new Date();
            System.out.println("Discover Start onResponseReceived: " + date.getTime());

            JSONArray jArray;

            if (null != result) {

                try {
                    JSONObject resultJsonObject = new JSONObject(result);
                    if (resultJsonObject.has("projects")) {
                        jArray = resultJsonObject.getJSONArray("projects");
                        int currentPage = Integer.parseInt(resultJsonObject.getString("page"));
                        totalpages = Integer.parseInt(resultJsonObject.getString("total_pages"));


                        for (int i = 0; i < jArray.length(); i++) {
                            JSONObject jsonProject = jArray.getJSONObject(i);

                            String projectImageUrl = jsonProject.getString("project_image");//get project image url.
                            JSONObject creatorJsonObj = jsonProject.getJSONObject("creator");//get project creator infos.
                            JSONObject creatorAvatar = creatorJsonObj.getJSONObject("avatar");//

                            ResearchProject rp = new ResearchProject(
                                    jsonProject.getString("id"),
                                    jsonProject.getString("name"),
                                    jsonProject.getString("description"),
                                    "",
                                    creatorJsonObj.getString("id"),
                                    "",
                                    "",
                                    projectImageUrl,
                                    "",
                                    "",
                                    "",
                                    creatorJsonObj.getString("name"),
                                    creatorJsonObj.getString("contributions"),
                                    creatorAvatar.getString("thumb")); // remove first and last square bracket and store as a comma separated string
                            projects.add(rp);
                        }
                        pageno = currentPage + 1;

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Date date1 = new Date();
            System.out.println("Discover End onResponseReceived: " + date1.getTime());
        }

    }
}