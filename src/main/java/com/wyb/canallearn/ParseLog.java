package com.wyb.canallearn;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class ParseLog {
    public static void main(String[] args) {
        JSONObject json = JSONObject.parseObject("{\"data\":[{\"user_id\":\"user-581\",\"ip\":\"192.168.15.230\"," +
                "\"ad_id\":\"ad-15\",\"province\":\"Jiangsu\",\"city\":\"Wuxi\",\"c_time\":\"2021-04-25 19:08:07\"}],\"database\":\"test\",\"es\":1619348886000,\"id\":25,\"isDdl\":false,\"mysqlType\":{\"user_id\":\"varchar(20)\",\"ip\":\"varchar(20)\",\"ad_id\":\"varchar(20)\",\"province\":\"varchar(20)\",\"city\":\"varchar(20)\",\"c_time\":\"timestamp\"},\"old\":null,\"pkNames\":null,\"sql\":\"\",\"sqlType\":{\"user_id\":12,\"ip\":12,\"ad_id\":12,\"province\":12,\"city\":12,\"c_time\":93},\"table\":\"user_click_log\",\"ts\":1619348887483,\"type\":\"INSERT\"}\n");
        System.out.println(json.keySet());
        // [pkNames, table, sqlType, id, mysqlType, isDdl, database, es, old, sql, data, ts, type]


        for (String s : json.keySet()) {
            // System.out.println(s + " -> " + json.get(s));
            if ("data".equals(s)) {
                System.out.println("*****************************");
                JSONArray arr = json.getJSONArray(s);
                // JSONObject dataj = JSONObject.parseObject();
                // System.out.println(dataj.toJSONString());
                // arr.forEach(x -> System.out.println(x));
                for (Object o : arr) {
                    LogData logData = (LogData)JSON.parseObject(o.toString(),LogData.class);
                    System.out.println(logData.getUser_id());
                    System.out.println(logData.getAd_id());
                }

            }
        }

    }
}
