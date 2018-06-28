package com.keultae.howltalk.model;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

public class RoomModel {
    public User user = new User();  // 채팅방 유저들
    public String lastMessage;      // 마지막 채팅 메시
    public long descTimestamp;      // Long.MAX_VALUE - 마지막 채팅 시간, 오름 차순 정렬시 가장 최근 값이 먼저 나오도록 하기 위해서
    public String roomId;

    public static class User {
        public Map<String, Boolean> uids = new HashMap<>();  // UID
        public Map<String, String> names = new HashMap<>();  // NAME

        @Override
        public String toString() {
            return "User{" +
                    "uids=" + uids +
                    ", names=" + names +
                    '}';
        }
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("descTimestamp", descTimestamp);
        result.put("lastMessage", lastMessage);

        return result;
    }

    public String toNames(String uid) {
        StringBuffer sb = new StringBuffer();

        for(String key : user.names.keySet()) {
            if(!uid.equals(key)) {
                sb.append(user.names.get(key));
                sb.append(",");
            }
        }
        return sb.toString().substring(0, sb.toString().length()-1);
    }

    @Override
    public String toString() {
        return "RoomModel{" +
                "user=" + user +
                ", lastMessage='" + lastMessage + '\'' +
                ", descTimestamp=" + descTimestamp +
                ", roomId='" + roomId + '\'' +
                '}';
    }
}
