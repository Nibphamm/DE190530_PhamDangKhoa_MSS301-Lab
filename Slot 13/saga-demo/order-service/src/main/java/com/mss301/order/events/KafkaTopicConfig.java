package com.mss301.order.events;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.TopicBuilder;

@Configuration(proxyBeanMethods = false)
@Profile("choreography")
public class KafkaTopicConfig {
    @Bean
    NewTopic sagaEvents() { return TopicBuilder.name("saga.events").partitions(3).replicas(1).build(); }
}
