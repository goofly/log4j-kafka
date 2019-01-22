package com.goofly.log4j.append;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;

public class DefaultKafkaProducerFactory implements KafkaProducerFactory {

	@Override
	public Producer<byte[], byte[]> newKafkaProducer(final Properties config) {
		return new KafkaProducer<>(config);
	}

}