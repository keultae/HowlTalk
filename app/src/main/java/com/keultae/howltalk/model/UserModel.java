package com.keultae.howltalk.model;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

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

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("userName", userName);
        result.put("profileImageUrl", profileImageUrl);
        result.put("uid", uid);
        result.put("pushToken", pushToken);
        result.put("comment", comment);

        return result;
    }
}
