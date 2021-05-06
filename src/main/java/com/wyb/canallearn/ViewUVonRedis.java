package com.wyb.canallearn;

import redis.clients.jedis.Jedis;

import java.util.Set;

// 查看UV的变化
public class ViewUVonRedis {
    public static void main(String[] args) throws InterruptedException {
        Jedis jedis = JedisUtil.getJedis();
        // System.out.println(jedis.ping());
        for (;;) {
            System.out.println("*******************************");
            Set<String> keys = jedis.keys("2021-04*");
            long cnt = 0;
            for (String key : keys) {
                if (jedis.type(key).equals("set")) {
                    cnt = jedis.scard(key);
                    System.out.println("key : " + key + " cnt : " + String.valueOf(cnt));
                } else
                    System.out.println("key : " + key + " value : " + jedis.get(key));

            }
            Thread.sleep(5000);
        }
        // jedis.close();
    }

}
