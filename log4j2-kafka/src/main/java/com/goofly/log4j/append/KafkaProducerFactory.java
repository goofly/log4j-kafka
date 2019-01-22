package com.goofly.log4j.append;

import java.util.Properties;

import org.apache.kafka.clients.producer.Producer;

public interface KafkaProducerFactory {

    Producer<byte[], byte[]> newKafkaProducer(Properties config);

}