package com.photosynq.app.utils;

/**
 * Created by kalpesh on 24/01/15.
 */
public class Constants {

    public static final String SUCCESS = "SUCCESS";
    public static final String SERVER_NOT_ACCESSIBLE = "SERVER_NOT_ACCESSIBLE";

    public static final String SERVER_URL = "https://www.photosynq.org/";
    // public static final String SERVER_URL = "http://staging.photosynq.venturit.net/";


    public static final String API_VER = "api/v3/";

    public static final String PHOTOSYNQ_LOGIN_URL = SERVER_URL+API_VER+"sign_in.json";
    public static final String PHOTOSYNQ_PROJECTS_LIST_URL = SERVER_URL+API_VER+"projects.json?";
    public static final String PHOTOSYNQ_PROJECT_DETAILS_URL = SERVER_URL+API_VER+"projects/";
    public static final String PHOTOSYNQ_MY_PROJECTS_LIST_URL = SERVER_URL+API_VER+"users/active_projects.json?";
    public static final String PHOTOSYNQ_PRE_SEL_PROTOCOLS_LIST_URL = SERVER_URL+API_VER+"protocols.json?preselected=true";
    public static final String PHOTOSYNQ_PROTOCOLS_LIST_URL = SERVER_URL+API_VER+"protocols.json?";
    public static final String PHOTOSYNQ_MACRO_URL =SERVER_URL+API_VER+ "macros/";
    public static final String PHOTOSYNQ_DATA_URL = SERVER_URL+API_VER+"projects/";
    public static final String PHOTOSYNQ_USER_INFO = SERVER_URL+AP$I_VER+"users/info.json?";
    public static final String PHOTOSYNQ_SEARCH_URL = SERVER_URL+API_VER+"projects/search.json?keyword=";
    // Message types sent from the BluetoothService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_STOP = 6;
    public static final int MESSAGE_FIRST_RESP = 7;
    public static final int MESSAGE_STREAM = 8;
    public static final boolean D = true;
    // Key names received from the BluetoothService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // App mode
    public static final String APP_MODE = "APP_MODE";
    public static final String APP_MODE_QUICK_MEASURE = "APP_MODE_QUICK_MEASURE";
    public static final String APP_MODE_PROJECT_MEASURE = "APP_MODE_PROJECT_MEASURE";

    //For remember option.
    public static final String IS_NOT_REMEMBER = "0";
    public static final String IS_REMEMBER = "1";

    /**
     * Remember the position of the selected item.
     */
    public static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";
    public static final String START_MEASURE = "start_measure";

    //
    public enum QuestionType {
        SCAN_CODE("SCAN_CODE"),
        USER_SELECTED("USER_SELECTED"),
        PROJECT_SELECTED("PROJECT_SELECTED"),
        FIXED_VALUE("FIXED_VALUE"),
        AUTO_INCREMENT("AUTO_INCREMENT");

        private String statusCode;
        private QuestionType(String s) {
            statusCode = s;
        }
        public String getStatusCode() {
            return statusCode;
        }
    }
}
