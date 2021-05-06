package com.wyb.canallearn;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JedisUtil {

    private static JedisPoolConfig config;

    private static JedisPool jedisPool;

    private static String password = "xxxxxxxxxx";

    static {
        config = new JedisPoolConfig();
        config.setMaxIdle(100);
        config.setMaxIdle(10);
        jedisPool = new JedisPool(config, "127.0.0.1", 6379);
    }

    /**
     * 获取jedis
     *
     * @return
     */
    public static Jedis getJedis() {
        Jedis jedis = jedisPool.getResource();
        // jedis.auth(password);
        return jedis;
    }

    /**
     * jedis放回连接池
     *
     * @param jedis
     */
    public static void close(Jedis jedis) {
        //从源码可以分析得到，如果是使用连接池的形式，这个并非真正的close,而是把连接放回连接池中
        if (jedis != null) {
            jedis.close();
        }
    }


    /**
     * 实现redis keys 模糊查询
     * @author hq
     * @param pattern
     * @return
     */
    // private List<String> redisKeys(String pattern){
    //     logger.info("开始模糊查询【{}】keys", pattern);
    //     List<String> keys = new ArrayList<>();
    //     //获取所有连接池节点
    //     Map<String, JedisPool> nodes = jedisCluster.getClusterNodes();
    //     //遍历所有连接池，逐个进行模糊查询
    //     for(String k : nodes.keySet()){
    //         logger.debug("从【{}】获取keys", k);
    //         JedisPool pool = nodes.get(k);
    //         //获取Jedis对象，Jedis对象支持keys模糊查询
    //         Jedis connection = pool.getResource();
    //         try {
    //             keys.addAll(connection.keys(pattern));
    //         } catch(Exception e){
    //             logger.error("获取key异常", e);
    //         } finally{
    //             logger.info("关闭连接");
    //             //一定要关闭连接！
    //             connection.close();
    //         }
    //     }
    //     logger.info("已获取所有keys");
    //     return keys;
    // }

    /**
     * get
     *
     * @param key
     * @return
     */
    public static String get(String key) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            return jedis.get(key);
        } catch (Exception e) {
            e.printStackTrace();
            throw new JedisException(e.getMessage(),e);
        } finally {
            close(jedis);
        }
    }

    /**
     * set
     *
     * @param key
     * @param value
     * @return
     */
    public static void set(String key, String value) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.set(key, value);
        } catch (Exception e) {
            e.printStackTrace();
            throw new JedisException(e.getMessage(),e);
        } finally {
            close(jedis);
        }
    }

    /**
     * set with expire milliseconds
     *
     * @param key
     * @param value
     * @param seconds
     * @return
     */
    public static void set(String key, String value, long seconds) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            //* @param nxxx NX|XX, NX -- Only set the key if it does not already exist. XX -- Only set the key
            //     *                     *          if it already exist.
            //     *                     * @param expx EX|PX, expire time units: EX = seconds; PX = milliseconds
            jedis.set(key, value, "NX", "EX", seconds);
        } catch (Exception e) {
            e.printStackTrace();
            throw new JedisException(e.getMessage(),e);
        } finally {
            close(jedis);
        }
    }


    public static Long incr(String key){
        Jedis jedis = null;
        try {
            jedis = getJedis();
            return jedis.incr(key);
        }catch (Exception e){
            e.printStackTrace();
            throw new JedisException(e.getMessage(),e);
        }finally {
            close(jedis);
        }
    }

    public static void hset(String key,String field,String value){
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.hset(key,field,value);
        }catch (Exception e){
            e.printStackTrace();
            throw new JedisException(e.getMessage(),e);
        }finally {
            close(jedis);
        }
    }

    public static String hget(String key,String field){
        Jedis jedis = null;
        try {
            jedis = getJedis();
            return jedis.hget(key,field);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            close(jedis);
        }
        return null;
    }

    public static Map<String,String> hgetAll(String key){
        Jedis jedis = null;
        try {
            jedis = getJedis();
            return jedis.hgetAll(key);
        }catch (Exception e){
            e.printStackTrace();
            throw new JedisException(e.getMessage(),e);
        }finally {
            close(jedis);
        }
    }

    /**
     *
     * @param timeout 0表示永久 单位秒
     * @param key key
     * @return [key,value]
     */
    public static String blpop(int timeout,String key){
        Jedis jedis = null;
        try {
            jedis = getJedis();
            List<String> list = jedis.blpop(timeout, key);
            return list.get(1);
        }catch (Exception e){
            e.printStackTrace();
            throw new JedisException(e.getMessage(),e);
        }finally {
            close(jedis);
        }
    }

    public static String blpop(String key){
        Jedis jedis = null;
        try {
            jedis = getJedis();
            List<String> list = jedis.blpop(0, key);
            return list.get(1);
        }catch (Exception e){
            e.printStackTrace();
            throw new JedisException(e.getMessage(),e);
        }finally {
            close(jedis);
        }
    }

    public static void lpush(String key,String... value){
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.lpush(key,value);
        }catch (Exception e){
            e.printStackTrace();
            throw new JedisException(e.getMessage(),e);
        }
    }

    /**
     *
     * @param timeout 0表示永久 单位秒
     * @param key key
     * @return [key,value]
     */
    public static String brpop(int timeout,String key){
        Jedis jedis = null;
        try {
            jedis = getJedis();
            List<String> list = jedis.brpop(timeout, key);
            return list.get(1);
        }catch (Exception e){
            e.printStackTrace();
            throw new JedisException(e.getMessage(),e);
        }finally {
            close(jedis);
        }
    }

    public static String brpop(String key){
        Jedis jedis = null;
        try {
            jedis = getJedis();
            List<String> list = jedis.brpop(0, key);
            return list.get(1);
        }catch (Exception e){
            e.printStackTrace();
            throw new JedisException(e.getMessage(),e);
        }finally {
            close(jedis);
        }
    }

    public static void rpush(String key,String... value){
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.rpush(key,value);
        }catch (Exception e){
            e.printStackTrace();
            throw new JedisException(e.getMessage(),e);
        }
    }

    /**
     * 获取key过期时间 -1表示永久 -2表示该key不存在
     * @param key
     * @return
     */
    public static long ttl(String key) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            return jedis.ttl(key);
        } catch (Exception e) {
            e.printStackTrace();
            throw new JedisException(e.getMessage(),e);
        } finally {
            close(jedis);
        }
    }

}