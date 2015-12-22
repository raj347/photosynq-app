package com.photosynq.app;

/**
 * (c) Nexus-Computing GmbH Switzerland, 2015
 * Created by Manuel Di Cerbo on 22.12.15.
 */
public enum NavigationItem {
    PROFILE(9, "Profile"),
    PROJECTS(0, "Projects"),
    DISCOVER(1,  "Discover"),
    QUICK_MEASUREMENT(2, "Select Measurement"),
    SYNC_SETTINGS(3, "Sync Settings"),
    ABOUT(4, "About"),
    SEND_DEBUG(5, "Send Debug"),
    SYNC_DATA(6, "Sync Data"),
    SHOW_CACHED(7, "Cached"),
    DEVICE(8, "Device");

    private final int mPosition;
    private final String mTitle;

    NavigationItem(int position, String title) {
        mPosition = position;
        mTitle = title;
    }

    public static NavigationItem fromPos(int id){
        for(NavigationItem item : NavigationItem.values()){
            if(item.mPosition == id){
                return item;
            }
        }
        throw new IllegalArgumentException(String.format("navigation id not found: %d", id));
    }

    public int getPosition() {
        return mPosition;
    }

    public String getTitle() {
        return mTitle;
    }
}