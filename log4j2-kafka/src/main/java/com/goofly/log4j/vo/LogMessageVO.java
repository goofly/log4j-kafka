package com.goofly.log4j.vo;

import com.alibaba.fastjson.JSON;

public class LogMessageVO {
	
	private String hostName;
	
	private String hostAddress;
	
	private String platform;
	
	private String serviceName;
	
	private long timeMillis; 
	
	private String threadName;
	
	private String level;
	
	private String log;

	public String getHostName() {
		return hostName;
	}

	public LogMessageVO setHostName(String hostName) {
		this.hostName = hostName;
		return this;
	}

	public String getHostAddress() {
		return hostAddress;
	}

	public LogMessageVO setHostAddress(String hostAddress) {
		this.hostAddress = hostAddress;
		return this;
	}

	public String getPlatform() {
		return platform;
	}

	public LogMessageVO setPlatform(String platform) {
		this.platform = platform;
		return this;
	}

	public String getServiceName() {
		return serviceName;
	}

	public LogMessageVO setServiceName(String serviceName) {
		this.serviceName = serviceName;
		return this;
	}

	public long getTimeMillis() {
		return timeMillis;
	}

	public LogMessageVO setTimeMillis(long timeMillis) {
		this.timeMillis = timeMillis;
		return this;
	}

	public String getThreadName() {
		return threadName;
	}

	public LogMessageVO setThreadName(String threadName) {
		this.threadName = threadName;
		return this;
	}

	public String getLevel() {
		return level;
	}

	public LogMessageVO setLevel(String level) {
		this.level = level;
		return this;
	}

	public String getLog() {
		return log;
	}

	public LogMessageVO setLog(String log) {
		this.log = log;
		return this;
	}

	public LogMessageVO(String serviceName, String platform) {
		this.serviceName = serviceName;
		this.platform = platform;
	}
	
	public byte[] toJSONBytes() {
		return JSON.toJSONBytes(this);
	}
	
	public String toJSONString() {
		return JSON.toJSONString(this);
	}
}
