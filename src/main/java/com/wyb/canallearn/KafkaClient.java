package com.wyb.canallearn;

import com.alibaba.fastjson.JSONObject;
// import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@Slf4j
public class KafkaClient {
    final static String bootstrapServers = "localhost:9092";
    int sessionTimeout  = 1000;
    int maxPollSize  = 5000;
    String groupId = "test";
    // final static String topic = "test_canal";
    final static String topic = "example";

    public static void main(String[] args) {

        // ClientProducer();
        ClientConsumer();
    }


    private static void ClientProducer() {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        // 判别请求是否为完整的条件（判断是不是成功发送了）。指定了“all”将会阻塞消息，这种设置性能最低，但是是最可靠的
        props.put("acks", "all");
        // 如果请求失败，生产者会自动重试，我们指定是0次，如果启用重试，则会有重复消息的可能性
        props.put("retries", 0);
        // 生产者缓存每个分区未发送的消息。缓存的大小是通过 batch.size 配置指定的。值较大的话将会产生更大的批。并需要更多的内存（每个“活跃”的分区都有1个缓冲区）
        props.put("batch.size", 16384);
        // 默认缓冲可立即发送，即便缓冲空间还没有满，但是，如果想减少请求的数量，可以设置 linger.ms 大于0。
        // 这将指示生产者发送请求之前等待一段时间，希望更多的消息填补到未满的批中。这类似于TCP的算法，例如，可能100条消息在一个请求发送，因为我们设置了linger(逗留)时间为1毫秒，然后，如果我们没有填满缓冲区，这个设置将增加1毫秒的延迟请求以等待更多的消息。需要注意的是，在高负载下，相近的时间一般也会组成批，即使是 linger.ms=0。在不处于高负载的情况下，如果设置比0大，以少量的延迟代价换取更少的，更有效的请求。
        props.put("linger.ms", 1);
        // 控制生产者可用的缓存总量，如果消息发送速度比其传输到服务器的快，将会耗尽这个缓存空间。当缓存空间耗尽，其他发送调用将被阻塞，阻塞时间的阈值通过 max.block.ms 设定，之后它将抛出一个TimeoutException。
        props.put("buffer.memory", 33554432);
        // key.serializer 和 value.serializer，将用户提供的 key 和 value 对象 ProducerRecord 转换成字节，可以使用附带的ByteArraySerializaer或StringSerializer处理简单的string或byte类型。
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        Producer<String, String> producer = new KafkaProducer<>(props);
        for (int i = 100; i < 500; i++)
            // send()方法是异步的，添加消息到缓冲区等待发送，并立即返回。生产者将单个的消息批量在一起发送来提高效率
            producer.send(new ProducerRecord<String, String>(topic, Integer.toString(i), Integer.toString(i)));

        producer.close();

    }

    private static void ClientConsumer() {
        log.info("test log");
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        // 消费者组名称
        props.put("group.id", "test");
        // 设置 enable.auto.commit,偏移量由 auto.commit.interval.ms 控制自动提交的频率。
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("org.apache.kafka.clients.consumer.ConsumerConfig","off");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        // 指定订阅 topic 名称
        consumer.subscribe(Arrays.asList(topic));
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(100);
            for (ConsumerRecord<String, String> record : records)
                System.out.printf("offset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());
        }

    }
}
