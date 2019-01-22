package com.goofly.log4j.append;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.goofly.log4j.file.WriteLog;
import com.goofly.log4j.util.InterAddressUtil;
import com.goofly.log4j.vo.LogMessageVO;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

/**
 * Sends log events to an Apache Kafka topic.
 */
@Plugin(name = "Kafka", category = "Core", elementType = "appender", printObject = true)
public final class KafkaAppender extends AbstractAppender {

	private static final String HEARTBEAT_TEST = "heartbeat_test";
	private static final long serialVersionUID = 1L;

	@PluginFactory
	public static KafkaAppender createAppender(@PluginElement("Layout") final Layout<? extends Serializable> layout,
			@PluginElement("Filter") final Filter filter,
			@Required(message = "No name provided for KafkaAppender") @PluginAttribute("name") final String name,
			@PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) final boolean ignoreExceptions,
			@Required(message = "No topic provided for KafkaAppender") @PluginAttribute("topic") final String topic,
			@PluginElement("Properties") final Property[] properties) {

		Properties kafkaProps = new Properties();
		for (Property property : properties) {
			kafkaProps.put(property.getName(), property.getValue());
		}

		final KafkaManager kafkaManager = new KafkaManager(name, topic, properties, flag, fileFlag);
		return new KafkaAppender(name, layout, filter, ignoreExceptions, kafkaManager, kafkaProps);
	}

	private final String platform;
	private final String serviceName;
	private final String hostName;
	private final String hostAddress;
	private final KafkaManager manager;
	private static AtomicBoolean flag = new AtomicBoolean(true);
	private static AtomicBoolean fileFlag = new AtomicBoolean(false);
	private ConcurrentLinkedQueue<LogMessageVO> queue = new ConcurrentLinkedQueue<>();
	// 放弃使用延时队列
	// private DelayQueue<MessageQueue<LogMessageVO>> delayQueue = new
	// DelayQueue<>();
	private ScheduledExecutorService scheduledThreadPool = Executors.newSingleThreadScheduledExecutor();
	private WriteLog writeLog;

	private KafkaAppender(final String name, final Layout<? extends Serializable> layout, final Filter filter,
			final boolean ignoreExceptions, final KafkaManager manager, Properties kafkaProps) {
		super(name, filter, layout, ignoreExceptions);
		this.manager = manager;

		this.platform = kafkaProps.getProperty("platform");
		this.serviceName = kafkaProps.getProperty("serviceName");
		this.hostName = InterAddressUtil.getInetAddress().getHostName();
		this.hostAddress = InterAddressUtil.getInetAddress().getHostAddress();
		writeLog = new WriteLog(queue, manager);
	}

	@Override
	public void append(final LogEvent event) {
		byte[] byteArray = null;
		if (getLayout() != null) {
			byteArray = getLayout().toByteArray(event);
		} else {
			byteArray = event.getMessage().getFormattedMessage().getBytes(StandardCharsets.UTF_8);
		}

		LogMessageVO logMessage = new LogMessageVO(serviceName, platform).setHostAddress(hostAddress)
				.setHostName(hostName).setLevel(event.getLevel().toString()).setLog(new String(byteArray))
				.setThreadName(event.getThreadName()).setTimeMillis(event.getTimeMillis());

		if (flag.get()) {
			manager.send(logMessage.toJSONBytes());
		} else {
			queue.offer(logMessage);
		}
	}

	@Override
	public void start() {
		super.start();
		manager.startup();
		writeLog.start();
		this.heartbeatStart();
	}

	@Override
	public void stop() {
		super.stop();
		manager.release();
		scheduledThreadPool.shutdown();
		writeLog.interrupt();
	}

	/**
	 * @Description: 心跳检测
	 */
	public void heartbeatStart() {
		scheduledThreadPool.scheduleAtFixedRate(() -> {
			manager.sendAsyn(HEARTBEAT_TEST.getBytes());
			if (fileFlag.get()) {
				fileFlag.compareAndSet(true, false);
				writeLog.read2Kafka();
			}
		}, 10, 60, TimeUnit.SECONDS);
	}
}
