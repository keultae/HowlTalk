package com.keultae.howltalk.model;

public class DataMessageModel {
    public String to;
    public Data data = new Data();

    public static class Data {
        public String senderName;   // 보낸 사람 이름
        public String message;      // 메시지
        public String roomId;       // 채팅방 ID
    }
}
