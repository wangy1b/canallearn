package com.wyb.canallearn;

public class LogData {

    public LogData() {
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getAd_id() {
        return ad_id;
    }

    public void setAd_id(String ad_id) {
        this.ad_id = ad_id;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getC_time() {
        return c_time;
    }

    public void setC_time(String c_time) {
        this.c_time = c_time;
    }

    String user_id;
    String ip;
    String ad_id;
    String province;
    String city;
    String c_time;

    public LogData(String user_id, String ip, String ad_id, String province, String city, String c_time) {
        this.user_id = user_id;
        this.ip = ip;
        this.ad_id = ad_id;
        this.province = province;
        this.city = city;
        this.c_time = c_time;
    }

    @Override
    public String toString() {
        return "com.wyb.canallearn.LogData{" +
                "user_id='" + user_id + '\'' +
                ", ip='" + ip + '\'' +
                ", ad_id='" + ad_id + '\'' +
                ", province='" + province + '\'' +
                ", city='" + city + '\'' +
                ", c_time='" + c_time + '\'' +
                '}';
    }
}
