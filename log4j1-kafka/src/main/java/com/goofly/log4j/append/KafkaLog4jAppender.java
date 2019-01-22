package com.goofly.log4j.append;

import com.goofly.log4j.vo.LogMessageVO;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;

import com.alibaba.fastjson.JSON;
import com.goofly.log4j.queue.MessageQueue;
import com.goofly.log4j.util.InterAddressUtil;

import java.util.Properties;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class KafkaLog4jAppender extends AppenderSkeleton {

	private String bootstrapServers;
	private String topic;
	private String serviceName;
	private String platform;
	private final String hostName = InterAddressUtil.getInetAddress().getHostName();
	private final String hostAddress = InterAddressUtil.getInetAddress().getHostAddress();
	DelayQueue<MessageQueue<LogMessageVO>> queue = null;
	private AtomicBoolean flag = new AtomicBoolean(true);

	public void setBootstrapServers(String bootstrapServers) {
		this.bootstrapServers = bootstrapServers;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	private final Properties properties = new Properties();
	// 独立线程写日志
	private final Thread writeLoggerThread = new Thread(new Runnable() {
		@Override
		public void run() {
			Producer<String, String> producer = new KafkaProducer<String, String>(properties);

			while ((flag.get() && !Thread.currentThread().isInterrupted())) {
				MessageQueue<LogMessageVO> msg = null;
				try {
					msg = queue.take();
				} catch (InterruptedException e) {
					LogLog.error("LOG KAKFA THREAD ERROR", e);
				}
				if (msg != null && msg.getData() != null) {
					producer.send(new ProducerRecord<String, String>(topic, JSON.toJSONString(msg.getData())),
							new Callback() {

								@Override
								public void onCompletion(RecordMetadata metadata, Exception exception) {
									if (exception != null) {
										flag.compareAndSet(true, false);
										LogLog.error("KAFKA 回调异常!", exception);

										// 只要响应异常,终止向KAFKA发送消息(后续做心跳检测)
										queue.clear();
										producer.close();
										writeLoggerThread.interrupt();
									}
								}
							});
				}

			}
		}
	});

	@Override
	public void activateOptions() {
		properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
		properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		// 启动写logger线程
		queue = new DelayQueue<>();
		writeLoggerThread.start();
	}

	@Override
	protected void append(LoggingEvent event) {
		String loggerContent = subAppend(event);
		if (flag.get()) {
			queue.offer(new MessageQueue<LogMessageVO>(2000,
					new LogMessageVO(serviceName, platform).setHostAddress(hostAddress).setHostName(hostName)
							.setLevel(event.getLevel().toString()).setLog(loggerContent).setPlatform(platform)
							.setServiceName(serviceName).setThreadName(event.getThreadName())
							.setTimeMillis(event.getTimeStamp())));
		}
	}

	private String subAppend(LoggingEvent event) {
		return this.layout == null ? event.getRenderedMessage() : this.layout.format(event);
	}

	@Override
	public void close() {
		if (!this.closed) {
			this.closed = true;
		}
		writeLoggerThread.interrupt();
		queue.clear();
	}

	@Override
	public boolean requiresLayout() {
		return true;
	}
}