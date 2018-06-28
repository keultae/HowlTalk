package com.keultae.howltalk.model;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

public class MessageModel {
    public String uid;
    public String message;
    public Object timestamp;
    public Map<String, Boolean> readUsers = new HashMap<>();
    public String messageId;

    @Override
    public String toString() {
        return "MessageModel{" +
                "uid='" + uid + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", readUsers=" + readUsers +
                ", messageId='" + messageId + '\'' +
                '}';
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("message", message);
        result.put("timestamp", timestamp);
        result.put("readUsers", readUsers);

        return result;
    }
}
