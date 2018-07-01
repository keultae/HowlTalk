package com.keultae.howltalk.model;

import com.google.firebase.database.Exclude;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class RoomModel implements Serializable {
//    public Map<String, UserModel> users = new HashMap<>();  // 채팅방 유저들
    public Map<String, Boolean> users = new HashMap<>();  // 채팅방 유저들
    public String lastMessage;      // 마지막 채팅 메시
    public String lastUid;          // 마지막 채팅 메시를 작성한 사람
    public long descTimestamp;      // Long.MAX_VALUE - 마지막 채팅 시간, 오름 차순 정렬시 가장 최근 값이 먼저 나오도록 하기 위해서
    public String roomId;

//    public static class User {
//        public Map<String, Boolean> uids = new HashMap<>();  // UID
//        public Map<String, String> names = new HashMap<>();  // NAME
//
//        @Override
//        public String toString() {
//            return "User{" +
//                    "uids=" + uids +
//                    ", names=" + names +
//                    '}';
//        }
//    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("users", users);
        result.put("descTimestamp", descTimestamp);
        result.put("lastMessage", lastMessage);
        result.put("lastUid", lastUid);

        return result;
    }

//    public String toNames(String uid) {
//        StringBuffer sb = new StringBuffer();
//
//        for(String key : users.keySet()) {
//            if(!uid.equals(key)) {
//                sb.append(users.get(key).userName);
//                sb.append(",");
//            }
//        }
//        return sb.toString().substring(0, sb.toString().length()-1);
//    }

    @Override
    public String toString() {
        return "RoomModel{" +
                "users=" + users +
                ", lastMessage='" + lastMessage + '\'' +
                ", lastUid='" + lastMessage + '\'' +
                ", descTimestamp=" + descTimestamp +
                ", roomId='" + roomId + '\'' +
                '}';
    }
}
