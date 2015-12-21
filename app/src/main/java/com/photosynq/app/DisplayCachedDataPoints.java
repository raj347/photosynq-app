package com.photosynq.app;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.photosynq.app.db.DatabaseHelper;
import com.photosynq.app.model.ProjectResult;
import com.photosynq.app.model.ResearchProject;
import com.photosynq.app.utils.CommonUtils;
import com.squareup.picasso.Picasso;

import java.util.List;


public class DisplayCachedDataPoints extends ActionBarActivity {

    private float fetchedPoints=0;
    private ListView lv;
    private DPListAdapter dpListAdapter;
    private List<ProjectResult> listRecords;
    private DatabaseHelper db;
    private int batchCount = 10;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_cached_data_points);
        db = DatabaseHelper.getHelper(getApplicationContext());
        final float totalPoints = Float.parseFloat("" + db.getAllUnuploadedResultsCount(null));
        listRecords = db.getAllUnUploadedResults(batchCount, "0");
        fetchedPoints = batchCount;
        lv = (ListView)findViewById(R.id.cacheddatapointslist);
        dpListAdapter = new DPListAdapter(getApplicationContext(),listRecords);
        lv.setAdapter(dpListAdapter);

        lv.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                boolean loadMore = firstVisibleItem + visibleItemCount >= totalItemCount - 1;
                if (loadMore) {
                    if ((fetchedPoints/batchCount) < (totalPoints/batchCount) ){
                            getMorePoints();
                    }
                }

            }
        });
//TODO shekhar improve this
//        DatabaseHelper db = DatabaseHelper.getHelper(getApplicationContext());
//        final List<ProjectResult> listRecords = db.getAllUnUploadedResults();

//        TextView txt = (TextView) findViewById(R.id.text1);
//        for(int i = 0; i < listRecords.size(); i++){
//            txt.append(Html.fromHtml("<br/><b>PROJECT_ID - </b>"+listRecords.get(i).getProjectId()+ "\n\n" + "<br/><b>IS_UPLOADED - </b>" + listRecords.get(i).getUploaded() + "\n\n" + "<br/><b>READINGS - </b>" + listRecords.get(i).getReading() + "\n\n<br/><br/>-------------------------------------------"));
//        }
    }

    private void getMorePoints() {

        listRecords.addAll(db.getAllUnUploadedResults(batchCount, Float.toString(fetchedPoints + batchCount) ));
        fetchedPoints = fetchedPoints + batchCount;
        dpListAdapter.notifyDataSetChanged();
        lv.invalidateViews();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_display_cached_data_points, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private class DPListAdapter extends BaseAdapter implements ListAdapter {

        public final Context context;
        public final List<ProjectResult> projectResultList;
        LayoutInflater mInflater;

        public DPListAdapter(Context context, List<ProjectResult> projectResultList) {
            assert context != null;
            assert projectResultList != null;

            this.projectResultList = projectResultList;
            this.context = context;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            if (null == projectResultList)
                return 0;
            else
                return projectResultList.size();
        }

        @Override
        public ProjectResult getItem(int position) {
            if (null == projectResultList)
                return null;
            else
                return projectResultList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            view = LayoutInflater.from(context).inflate(R.layout.cached_data_point_item, parent, false);
            TextView tv = (TextView)view.findViewById(R.id.cacheddatapoint);
            tv.setTypeface(CommonUtils.getInstance().getFontRobotoRegular());
            tv.setText(getItem(position).getReading());
            return view;
        }
    }
}


