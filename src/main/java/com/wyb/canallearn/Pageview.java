package com.wyb.canallearn;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.Optional;
import org.apache.spark.api.java.function.*;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.State;
import org.apache.spark.streaming.StateSpec;
import org.apache.spark.streaming.api.java.*;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import redis.clients.jedis.Jedis;
import scala.Option;
import scala.Tuple2;

import java.util.*;

public class Pageview {
    public static void main(String[] args) throws InterruptedException {
        SparkConf sparkConf = new SparkConf().setAppName("com.wyb.canallearn.JavaNetworkWordCount").setMaster("local[2]");
        JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, Durations.seconds(10));


        // updateStateByKey操作， 要求必须开启Checkpoint机制
        jssc.checkpoint("C:\\Users\\32006\\Desktop\\test\\SparkCheckPoints");


        Map<String, Object> kafkaParams = new HashMap<>();
        kafkaParams.put("bootstrap.servers", "localhost:9092");
        kafkaParams.put("key.deserializer", StringDeserializer.class);
        kafkaParams.put("value.deserializer", StringDeserializer.class);
        // kafkaParams.put("group.id", "use_a_separate_group_id_for_each_stream");
        kafkaParams.put("group.id", "test");
        kafkaParams.put("auto.offset.reset", "latest");
        // kafkaParams.put("enable.auto.commit", false);

        Collection<String> topics = Arrays.asList("example");

        JavaInputDStream<ConsumerRecord<String, String>> stream =
                KafkaUtils.createDirectStream(
                        jssc,
                        LocationStrategies.PreferConsistent(),
                        ConsumerStrategies.<String, String>Subscribe(topics, kafkaParams)
                );


        JavaDStream<LogData> rowData = stream.flatMap(
                new FlatMapFunction<ConsumerRecord<String, String>, LogData>() {
                    @Override
                    public Iterator<LogData> call(ConsumerRecord<String, String> stringStringConsumerRecord) throws Exception {
                        ArrayList arrayList = new ArrayList();
                        if (!stringStringConsumerRecord.value().isEmpty()) {
                            JSONObject allJson = JSONObject.parseObject(stringStringConsumerRecord.value());
                            // System.out.println("allJson: "+allJson);
                            if (allJson.getBoolean("isDdl") == false) {
                                JSONArray arr = allJson.getJSONArray("data");
                                // System.out.println("arr: "+arr);
                                for (Object o : arr) {
                                    LogData logData = (LogData) JSON.parseObject(o.toString(), LogData.class);
                                    arrayList.add(logData);
                                }
                            }
                        }
                        return arrayList.iterator();
                    }
                });

        // calcPV(rowData);
        // calcUV(rowData);
        // calcPVByProvince(rowData);
        calcUVInRedis(rowData);


        jssc.start();
        jssc.awaitTermination();


    }


    private static void calcPV(JavaDStream<LogData> rowData) {
        JavaPairDStream<String, Integer> pagePair = rowData.mapToPair(new PairFunction<LogData, String, Integer>() {
            @Override
            public Tuple2<String, Integer> call(LogData logData) throws Exception {
                String dt = logData.c_time.substring(0, 10);
                return new Tuple2<>(dt, 1);
            }
        });

        JavaPairDStream<String, Integer> pv = pagePair.updateStateByKey(new Function2<List<Integer>, Optional<Integer>,
                Optional<Integer>>() {
            @Override
            public Optional<Integer> call(List<Integer> v1, Optional<Integer> state) throws Exception {
                Integer newValues = 0;

                if (state.isPresent()) {
                    newValues = state.get();
                }

                for (Integer value : v1) {
                    newValues += value;
                }
                return Optional.of(newValues);
            }
        });

        pv.print();

    }

    private static void calcPVByProvince(JavaDStream<LogData> rowData) {

        JavaDStream<Tuple2<String, String>> provinceDs = rowData.map(new Function<LogData,
                Tuple2<String, String>>() {
            @Override
            public Tuple2<String, String> call(LogData logData) throws Exception {
                String dt = logData.getC_time().substring(0, 10);
                String province = logData.getProvince();
                return new Tuple2<>(dt, province);
            }
        });



        JavaPairDStream<Tuple2<String, String>, Integer> pv = provinceDs.transform(new Function<JavaRDD<Tuple2<String, String>>, JavaRDD<Tuple2<String, String>>>() {
            @Override
            public JavaRDD<Tuple2<String, String>> call(JavaRDD<Tuple2<String, String>> v1) throws Exception {
                return v1.distinct();
            }
        }).mapToPair(new PairFunction<Tuple2<String, String>, Tuple2<String, String>, Integer>() {

            @Override
            public Tuple2<Tuple2<String, String>, Integer> call(Tuple2<String, String> s) throws Exception {
                return new Tuple2<>(s, 1);
            }
        });



        JavaPairDStream<Tuple2<String, String>, Integer> pv1 = pv.updateStateByKey(
                new Function2<List<Integer>, Optional<Integer>, Optional<Integer>>() {
            @Override
            public Optional<Integer> call(List<Integer> v1, Optional<Integer> state) throws Exception {
                Integer newValues = 0;

                if (state.isPresent()) {
                    newValues = state.get();
                }

                for (Integer value : v1) {
                    newValues += value;
                }
                return Optional.of(newValues);
            }
        });

                ////// 只计算窗口内的
        // .reduceByKey(new Function2<Integer, Integer, Integer>() {
        //     @Override
        //     public Integer call(Integer v1, Integer v2) throws Exception {
        //         return v1 + v2;
        //     }
        // });


        // .reduceByKeyAndWindow(new Function2<Integer, Integer, Integer>() {
        //     @Override
        //     public Integer call(Integer v1, Integer v2) throws Exception {
        //         return v1 + v2;
        //     }
        // },Durations.seconds(30),
        //         Durations.seconds(20)
        // );

        // pv.print();


        JavaMapWithStateDStream<Tuple2<String, String>, Integer, Integer, Tuple2<Tuple2<String, String>, Integer>> stateWordCounts = pv.mapWithState
                (StateSpec.function(
                        new org.apache.spark.api.java.function.Function3<Tuple2<String, String>, Optional<Integer>, State<Integer>,
                                Tuple2<Tuple2<String, String>, Integer>>() {
                            @Override
                            public Tuple2<Tuple2<String, String>, Integer> call(Tuple2<String, String> word, Optional<Integer> one, State<Integer> state) throws Exception {
                                Option<Integer> stateCount = state.getOption();
                                Integer sum = one.orElse(0);
                                if (stateCount.isDefined()) {
                                    sum += stateCount.get();
                                }
                                state.update(sum);
                                return new Tuple2<Tuple2<String, String>, Integer>(word, sum);
                            }
                        }));

        stateWordCounts.print();

    }


    private static void calcUV(JavaDStream<LogData> rowData) {

        JavaPairDStream<String, Integer> userPair = rowData.mapToPair(new PairFunction<LogData, String, Integer>() {
            @Override
            public Tuple2<String, Integer> call(LogData logData) throws Exception {
                return new Tuple2<>(logData.getUser_id(), 1);
            }
        });

        JavaPairDStream<String, Integer> uv = userPair.updateStateByKey(new Function2<List<Integer>, org.apache.spark
                .api.java.Optional<Integer>, org.apache.spark.api.java.Optional<Integer>>() {
            @Override
            public org.apache.spark.api.java.Optional<Integer> call(List<Integer> values, org.apache.spark.api.java.Optional<Integer> state) {
                Integer newValues = 0;

                if (state.isPresent()) {
                    newValues = state.get();
                }

                for (Integer value : values) {
                    newValues += value;
                }

                return Optional.of(newValues);
            }
        });

        uv.print();



    }
    private static void calcUVInRedis(JavaDStream<LogData> rowData) {

        rowData.foreachRDD(new VoidFunction<JavaRDD<LogData>>() {
            @Override
            public void call(JavaRDD<LogData> logDataJavaRDD) throws Exception {
                logDataJavaRDD.foreachPartition(new VoidFunction<Iterator<LogData>>() {
                    @Override
                    public void call(Iterator<LogData> logDataIterator) throws Exception {
                        Jedis jedis = JedisUtil.getJedis();

                        while (logDataIterator.hasNext()) {
                            LogData logData = logDataIterator.next();
                            String user_id = logData.getUser_id();
                            String date_key = logData.getC_time().substring(0,10);
                            jedis.sadd(date_key,user_id);
                            // 测试保留20秒
                            jedis.expire(date_key,20);
                            // jedis.expireAt(date_key,1);

                        }
                        jedis.close();
                    }
                });

            }
        });



    }
}
