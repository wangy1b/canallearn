package com.wyb.canallearn;

import lombok.SneakyThrows;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MockSUserDataToMysql {
    private void testmysql() throws SQLException {
        Connection conn = DBUtil.getConnection();
        Statement stmt = DBUtil.stmt;

        ResultSet rs = (ResultSet) DBUtil.execute("select now() as name");

        // 展开结果集数据库
        while (rs.next()) {
            // 通过字段检索
            String name = rs.getString("name");
            // 输出数据
            System.out.println("name : " + name);
        }
        // 完成后关闭
        rs.close();
        stmt.close();
        conn.close();
    }

    public static void main(String[] args) throws SQLException {
        /*
        userID + ip + adID + province + city  + c_time
         */

        String tableName = "test.user_click_log";

        String ddl = "drop TABLE if exists " + tableName + ";" +
                "CREATE TABLE "+ tableName+" (\n" +
                "        user_id varchar(20)\n" +
                "        ,ip  varchar(20)\n" +
                "        ,ad_id varchar(20)\n" +
                "        ,province varchar(20)\n" +
                "        ,city varchar(20)\n" +
                "        ,c_time timestamp\n" +
                "        )\n" +
                "        ;";
        // com.wyb.canallearn.DBUtil.execute(ddl);

        final Random random = new Random();
        final String[] provinces = new String[]{"Guangdong", "Zhejiang", "Jiangsu", "Fujian"};
        //城市
        final Map<String, String[]> cities = new HashMap<String, String[]>();
        cities.put("Guangdong", new String[]{"Guangzhou", "Shenzhen", "DongGuan"});
        cities.put("Zhejiang", new String[]{"Hangzhou", "Wenzhou", "Ningbo"});
        cities.put("Jiangsu", new String[]{"Nanjing", "Suzhou", "Wuxi"});
        cities.put("Fujian", new String[]{"Fuzhou", "xiamen", "Sanming"});


        new Thread(new Runnable() {

            Long c_time = new Date().getTime();

            @SneakyThrows
            @Override
            public void run() {
                while (true) {
                    //在线处理广告点击流 广告点击的基本数据格式：timestamp、ip、userID、adID、province、city
                    // Long c_time = new Date().getTime();
                    c_time += 1800000;// 每次加30分钟
                    String strDateFormat = "yyyy-MM-dd HH:mm:ss";
                    SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat);
                    String c_time_format = sdf.format(c_time);

                    String ip = String.format("192.168.%s.%s",random.nextInt(255),random.nextInt(255));
                    int userID = random.nextInt(1000);
                    int adID = random.nextInt(100);
                    String province = provinces[random.nextInt(4)];
                    String city = cities.get(province)[random.nextInt(3)];
                    String sql_text = String.format("insert into %s " +
                                    "values('user-%s','%s','ad-%s','%s','%s','%s')"
                            , tableName,userID, ip, adID, province, city, c_time_format);

                    // String clickedAd = userID + ip + adID + province + city  + c_time ;
                    System.out.println(sql_text);
                    DBUtil.execute(sql_text);

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        }).start();


    }
}
