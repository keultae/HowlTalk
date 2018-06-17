package com.keultae.howltalk.model;

public class UserModel {
    public String userName;
    public String profileImageUrl;
    public String uid;
    public String pushToken;
    public String comment;

    @Override
    public String toString() {
        return "UserModel{" +
                "userName='" + userName + '\'' +
                ", profileImageUrl='" + profileImageUrl + '\'' +
                ", uid='" + uid + '\'' +
                ", pushToken='" + pushToken + '\'' +
                ", comment='" + comment + '\'' +
                '}';
    }
}
