package com.photosynq.app;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.photosynq.app.utils.Constants;
import com.photosynq.app.utils.PrefUtils;

import de.cketti.library.changelog.ChangeLog;

import static com.photosynq.app.utils.NxDebugEngine.log;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    static int progressRefCount = 0;
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    private NavigationItem mCurrentSelectedAction = NavigationItem.PROJECTS;

    //private boolean mIsSearchView = false;
    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private static ProgressBar progressBar;
    //private boolean mIsSearchView = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((PhotoSyncApplication) getApplicationContext()).registerActivity(this);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar_bg));
        progressBar = (ProgressBar) findViewById(R.id.toolbar_progress_bar);

        String prevSelPos = PrefUtils.getFromPrefs(this, PrefUtils.PREFS_PREV_SELECTED_POSITION, NavigationItem.PROJECTS.getPosition() + "");

        try {
            mCurrentSelectedAction = NavigationItem.fromPos(Integer.valueOf(prevSelPos));
        } catch (NumberFormatException e) {
           mCurrentSelectedAction = NavigationItem.PROJECTS;
        }

        log("prev selected %s", prevSelPos);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        //??onNavigationDrawerItemSelected(mCurrentSelectedAction);
        ChangeLog cl = new ChangeLog(this);
        if (cl.isFirstRun()) {
            cl.getLogDialog().show();
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        handleIntent(intent);
    }

    public void openDrawer() {
        // If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
        // per the navigation drawer design guidelines.
        if (mNavigationDrawerFragment != null) {
            mNavigationDrawerFragment.openDrawer();
        }
    }

    private void handleIntent(Intent intent) {

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            //mIsSearchView = true;

            String query = intent.getStringExtra(SearchManager.QUERY);

            FragmentManager fragmentManager = getSupportFragmentManager();
            //use the query to search your data somehow
            if (mCurrentSelectedAction == NavigationItem.PROJECTS) { //My Projects
                fragmentManager.beginTransaction()
                        .replace(R.id.container, MyProjectsFragment.newInstance(mCurrentSelectedAction.getPosition(), query), MyProjectsFragment.class.getName())
                        .commit();
            } else {
                fragmentManager.beginTransaction()
                        .replace(R.id.container, DiscoverFragment.newInstance(mCurrentSelectedAction.getPosition(), query), DiscoverFragment.class.getName())
                        .commit();
            }


        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //Handle the back button
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Quit")
                    .setMessage("Do you want to close the application")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //Stop the activity
                            PrefUtils.saveToPrefs(MainActivity.this, PrefUtils.PREFS_PREV_SELECTED_POSITION, NavigationItem.PROJECTS.getPosition() + "");
                            MainActivity.this.finish();
                        }

                    })
                    .setNegativeButton("No", null)
                    .show();

            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(NavigationItem navigationItem) {
        // update the main content by replacing fragments
        if (navigationItem != NavigationItem.DEVICE) // Do not keep selection of Select measurement device option
            mCurrentSelectedAction = navigationItem;

        FragmentManager fragmentManager = getSupportFragmentManager();

        switch (navigationItem) {
            case PROFILE:
                fragmentManager.beginTransaction()
                        .replace(R.id.container, ProfileFragment.newInstance(navigationItem.getPosition()), ProfileFragment.class.getName())
                        .commit();
                break;
            case PROJECTS:
                fragmentManager.beginTransaction()
                        .replace(R.id.container, MyProjectsFragment.newInstance(navigationItem.getPosition()), MyProjectsFragment.class.getName())
                        .commit();
                break;
            case DISCOVER:
                fragmentManager.beginTransaction()
                        .replace(R.id.container, DiscoverFragment.newInstance(navigationItem.getPosition()), DiscoverFragment.class.getName())
                        .commit();
                break;
            case QUICK_MEASUREMENT:
                QuickModeFragment quickModeFragment = QuickModeFragment.newInstance(navigationItem.getPosition());

                fragmentManager.beginTransaction()
                        .replace(R.id.container, quickModeFragment, QuickModeFragment.class.getName())
                        .commit();
                break;
            case SYNC_SETTINGS:
                fragmentManager.beginTransaction()
                        .replace(R.id.container, SyncFragment.newInstance(navigationItem.getPosition()), SyncFragment.class.getName())
                        .commit();
                break;
            case ABOUT:
                fragmentManager.beginTransaction()
                        .replace(R.id.container, AboutFragment.newInstance(navigationItem.getPosition()), AboutFragment.class.getName())
                        .commit();
                break;
            case SEND_DEBUG:
                break;
            case SYNC_DATA:
                break;
            case SHOW_CACHED:
                break;
            case DEVICE:
                SelectDeviceDialog selectDeviceDialog = new SelectDeviceDialog();
                selectDeviceDialog.show(fragmentManager, "Select Measurement Device");
                break;
        }
//        fragmentManager.beginTransaction()
//                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
//                .commit();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(Constants.STATE_SELECTED_POSITION, mCurrentSelectedAction.getPosition());
        super.onSaveInstanceState(outState);

        PrefUtils.saveToPrefs(this, PrefUtils.PREFS_PREV_SELECTED_POSITION, mCurrentSelectedAction.getPosition() + "");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ((PhotoSyncApplication) getApplicationContext()).unRegisterActivity();

        //??PrefUtils.saveToPrefs(this, PrefUtils.PREFS_PREV_SELECTED_POSITION, "0");

    }

    public void onSectionAttached(int position) {
        NavigationItem navigationItem = NavigationItem.fromPos(position);
        switch (navigationItem) {
            case PROJECTS:
            case DISCOVER:
            case QUICK_MEASUREMENT:
            case SYNC_SETTINGS:
            case ABOUT:
            case PROFILE:
                mTitle = navigationItem.getTitle();
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        log("title and selection are: %s, %s", mTitle, mCurrentSelectedAction);

        MenuInflater inflater = getMenuInflater();

        boolean isSearchableView = false;
        if (mTitle.equals(getString(R.string.discover_title))) {
            inflater.inflate(R.menu.menu_discover, menu);
            isSearchableView = true;
        } else if (mTitle.equals(getString(R.string.my_projects_title))) {
            inflater.inflate(R.menu.menu_my_projects, menu);
            //isSearchableView = true;
        } else if (mTitle.equals(getString(R.string.about))) {
            inflater.inflate(R.menu.about_menu, menu);
            //isSearchableView = true;
        }

        if(mCurrentSelectedAction == NavigationItem.PROJECTS){
            inflater.inflate(R.menu.menu_my_projects, menu);
        }




        if (isSearchableView) {

            MenuItem searchItem = menu.findItem(R.id.action_search);
            final SearchView searchView = (SearchView) searchItem.getActionView();

            // Associate searchable configuration with the SearchView
            SearchManager searchManager =
                    (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            searchView.setSearchableInfo(
                    searchManager.getSearchableInfo(getComponentName()));

            MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    closeSearchView();
                    return true;
                }
            });


//            if (android.os.Build.VERSION.SDK_INT > 10){
//                String[] columnNames = {"_id","text"};
//                MatrixCursor cursor = new MatrixCursor(columnNames);
//                String[] array = getResources().getStringArray(R.array.all_strings); //if strings are in resources
//                String[] temp = new String[2];
//                int id = 0;
//                for(String item : array){
//                    temp[0] = Integer.toString(id++);
//                    temp[1] = item;
//                    cursor.addRow(temp);
//                }
//                String[] from = {"text"};
//                int[] to = {R.id.text1};
//                final CursorAdapter cursorAdapter = new SimpleCursorAdapter(getApplicationContext(), R.layout.simple_spinner_item, cursor, from, to);
//
//                searchView.setSuggestionsAdapter(cursorAdapter);
//                searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
//
//                    @Override
//                    public boolean onSuggestionClick(int position) {
//                        String selectedItem = (String)cursorAdapter.getItem(position);
//                        Log.v("search view", selectedItem);
//                        return false;
//                    }
//
//                    @Override
//                    public boolean onSuggestionSelect(int position) {
//                        return false;
//                    }
//                });
//            }


            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    searchView.clearFocus();
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String s) {
                    System.out.println("String-" + s);
                    return false;
                }
            });

        }

        restoreActionBar();

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.refreshmenu) {
            if (mCurrentSelectedAction == NavigationItem.ABOUT) { //About
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.container, AboutFragment.newInstance(mCurrentSelectedAction.getPosition()), AboutFragment.class.getName())
                        .commit();
            }

            return true;
        }
        //noinspection SimplifiableIfStatement
//        if(mIsSearchView){
//            if (id == android.R.id.home) {
//                closeSearchView();
//                return true;
//            }
//        }

        return super.onOptionsItemSelected(item);
    }

    private void closeSearchView() {
        //mIsSearchView = false;

        FragmentManager fragmentManager = getSupportFragmentManager();
        if (mCurrentSelectedAction == NavigationItem.DISCOVER) {//Discover
            fragmentManager.beginTransaction()
                    .replace(R.id.container, DiscoverFragment.newInstance(mCurrentSelectedAction.getPosition()), DiscoverFragment.class.getName())
                    .commit();
        } else if (mCurrentSelectedAction == NavigationItem.PROJECTS) { //My Projects
            fragmentManager.beginTransaction()
                    .replace(R.id.container, MyProjectsFragment.newInstance(mCurrentSelectedAction.getPosition()), MyProjectsFragment.class.getName())
                    .commit();
        }

    }

    public void setDeviceConnected(String deviceName, String deviceAddress) {
        mNavigationDrawerFragment.setDeviceConnected(deviceName, deviceAddress);
    }

    public static ProgressBar getProgressBar() {
        return progressBar;
    }

    public void setProgressBarVisibility(int visible) {

        if (View.VISIBLE == visible) {
            progressRefCount++;

            if (null != progressBar) {
                progressBar.setVisibility(visible);
            }
        } else if (View.INVISIBLE == visible) {
            progressRefCount--;

            if (progressRefCount == 0) {
                if (null != progressBar) {
                    progressBar.setVisibility(visible);
                }
            }
        }

    }
}
