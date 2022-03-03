package com.virtualbeamer.models;

import com.virtualbeamer.constants.AppConstants;

public class User {
    public String username;
    public int ID;
    public AppConstants.UserType userType;

    public User() {
        this.userType = AppConstants.UserType.VIEWER;
    }

    public AppConstants.UserType getUserType() {
        return userType;
    }

    public void setUserType(AppConstants.UserType userType) {
        this.userType = userType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }
}