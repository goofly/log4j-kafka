package com.goofly.log4j.append;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.config.Property;

public class KafkaManager extends AbstractManager {

	public static final String DEFAULT_TIMEOUT_MILLIS = "10000";

	static KafkaProducerFactory producerFactory = new DefaultKafkaProducerFactory();

	private final Properties config = new Properties();
	private Producer<byte[], byte[]> producer = null;
	private final int timeoutMillis;

	private final String topic;
	
	private AtomicBoolean flag;
	private AtomicBoolean fileFlag;

	public KafkaManager(final String name, final String topic, final Property[] properties,AtomicBoolean flag,AtomicBoolean fileFlag) {
		super(name);
		this.topic = topic;
		config.setProperty("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
		config.setProperty("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
		config.setProperty("batch.size", "0");
		config.setProperty("max.block.ms", "5000");
		config.setProperty("request.timeout.ms", DEFAULT_TIMEOUT_MILLIS);
		for (final Property property : properties) {
			config.setProperty(property.getName(), property.getValue());
		}
		this.timeoutMillis = Integer.parseInt(config.getProperty("timeout.ms", DEFAULT_TIMEOUT_MILLIS));
		this.flag = flag;
		this.fileFlag = fileFlag;
	}

	@Override
	public void releaseSub() {
		if (producer != null) {
			// This thread is a workaround for this Kafka issue:
			// https://issues.apache.org/jira/browse/KAFKA-1660
			final Thread closeThread = new Thread(new Runnable() {
				@Override
				public void run() {
					producer.close();
				}
			});
			closeThread.setName("KafkaManager-CloseThread");
			closeThread.setDaemon(true); // avoid blocking JVM shutdown
			closeThread.start();
			try {
				closeThread.join(timeoutMillis);
			} catch (final InterruptedException ignore) {
				// ignore
			}
		}
	}

	/**  
	 * 同步发送 
	 */ 
	public void send(final byte[] msg){
		if (producer != null) {
			try {
				producer.send(new ProducerRecord<byte[], byte[]>(topic, msg)).get(Long.valueOf(DEFAULT_TIMEOUT_MILLIS),TimeUnit.SECONDS);
			} catch (NumberFormatException | InterruptedException | ExecutionException | TimeoutException e) {
				// 此条数据暂不考虑放入队列
				flag.compareAndSet(true, false);
				LOGGER.error("KAFKA 回调异常!", e);
			}
		}
	}
	
	/**   
	 * 异步发送
	 */ 
	public void sendAsyn(final byte[] msg) {
		if (producer != null) {
			producer.send(new ProducerRecord<byte[], byte[]>(topic, msg), new Callback() {
				@Override
				public void onCompletion(RecordMetadata metadata, Exception exception) {
					if (exception == null) {
						if (flag.get() == false) {
							fileFlag.compareAndSet(false, true);
							LOGGER.info("============  KAFKA 恢复正常!  ============");
						}
						flag.compareAndSet(false, true);
						LOGGER.info("============  Kafka正常  =========");
					}
				}
			});
		}
	}

	public void startup() {
		producer = producerFactory.newKafkaProducer(config);
	}

}
