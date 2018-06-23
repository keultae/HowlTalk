package com.keultae.howltalk.model;

import java.util.HashMap;
import java.util.Map;

public class ChatModel
{
    public String pushId;   // 채팅방 PUSH ID
    public Map<String, Boolean> users = new HashMap<>();    // 채팅방 유저들
    public Map<String, Comment> comments = new HashMap<>(); // 채팅방의 대화 내용
    public long order;  // Long.MAX_VALUE - 마지막 채팅 시간, 오름 차순 정렬시 가장 최근 값이 먼저 나오도록 하기 위해서
    public long timestamp; // 마지막 채팅 시간

    public static class Comment {
        public String key;
        public String uid;
        public String message;
        public Object timestamp;
        public Map<String, Object> readUsers = new HashMap<>();

        @Override
        public String toString() {
            return "Comment{" +
                    "uid='" + uid + '\'' +
                    ", message='" + message + '\'' +
                    ", timestamp=" + timestamp +
                    ", readUsers.size()=" + readUsers.size() +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "ChatModel{" +
                "users=" + users.toString() +
                ", comments=" + comments.toString() +
                '}';
    }
}
