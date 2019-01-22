package com.goofly.log4j.queue;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class MessageQueue<T> implements Delayed {

	private final long delayTime; // 延迟时间
	private final long expire; // 到期时间
	private T data; // 数据

	public MessageQueue(long delay, T data) {
		delayTime = delay;
		this.data = data;
		expire = System.currentTimeMillis() + delay;
	}

	/**
	 * 剩余时间=到期时间-当前时间
	 */
	@Override
	public long getDelay(TimeUnit unit) {
		return unit.convert(this.expire - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
	}

	/**
	 * 优先队列里面优先级规则
	 */
	@Override
	public int compareTo(Delayed o) {
		return (int) (this.getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
	}
	
	public T getData() {
		return data;
	}
}