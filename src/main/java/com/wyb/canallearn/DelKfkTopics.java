package com.wyb.canallearn;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;

import java.util.Arrays;
import java.util.Properties;

@Slf4j
public class DelKfkTopics {

    public static void del(String brokerList,String topic){ ;
        log.info("will del kafka topic:","exapmle");

        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,brokerList);
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG,30000);
        AdminClient client = AdminClient.create(props);
        client.deleteTopics(Arrays.asList(topic));
        client.close();
        log.info("finished del kafka topic");
    }

    public static void main(String[] args) {
        DelKfkTopics.del(KafkaClient.bootstrapServers,"exapmle");
    }

}
