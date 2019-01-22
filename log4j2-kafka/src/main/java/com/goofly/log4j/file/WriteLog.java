package com.goofly.log4j.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.goofly.log4j.append.KafkaManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

import com.goofly.log4j.vo.LogMessageVO;

/**
 * @ClassName: WriteLog
 * @Description:
 * @author: goofly
 * @date: 2018年7月6日 上午9:23:01
 * 
 */
public class WriteLog extends Thread {

	private AtomicInteger counter = new AtomicInteger(0);
	static final Logger LOGGER = StatusLogger.getLogger();
	private static final String PATH_NAME = "./miss.log";
	
	private File file;
	private ConcurrentLinkedQueue<LogMessageVO> queue;
	private final KafkaManager manager;

	public WriteLog(ConcurrentLinkedQueue<LogMessageVO> queue, KafkaManager manager) {
		this.file = new File(PATH_NAME);
		this.queue = queue;
		this.manager = manager;
	}

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			LogMessageVO message = queue.poll();
			if (message != null) {
				this.writeStr2File(message.toJSONString());
			}

		}
	}

	public void writeStr2File(String str) {
		try (FileWriter fw = new FileWriter(file, true); BufferedWriter bw = new BufferedWriter(fw);) {
			bw.write(str);
			bw.newLine();
			bw.flush();
		} catch (IOException e) {
			LOGGER.error("WriteLog#writeStr2File error", e);
		}
	}

	/**
	 * 读取文件至kafka
	 */
	public void read2Kafka() {
		String str = null;
		try (FileReader fr = new FileReader(file); BufferedReader br = new BufferedReader(fr,10*1024*1024);) {
			// RandomAccessFile accessFile = new RandomAccessFile(file,"rw");
			// FileChannel channel = accessFile.getChannel();FileLock lock = channel.tryLock();
			while ((str = br.readLine()) != null) {
				manager.sendAsyn(str.getBytes());
				counter.incrementAndGet();
				if(counter.compareAndSet(10000, 0)) {
					Thread.sleep(8000);
				}
			}
			cleanFile();
		} catch (IOException e) {
			LOGGER.error("WriteLog#read2Kafka release lock error", e);
		} catch (InterruptedException e) {
			LOGGER.error("WriteLog#read2Kafka Thread Sleep error", e);
		}
	}

	/**
	 * 清空文件
	 */
	private void cleanFile() {
		try (FileWriter fw = new FileWriter(file, false); BufferedWriter bw = new BufferedWriter(fw);) {
			bw.write("");
			bw.flush();
		} catch (IOException e) {
			LOGGER.error("WriteLog#cleanFile  error", e);
		}
	}
}
