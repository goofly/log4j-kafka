//package com.goofly.log4j;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//public class LoggerTest {
//
//	private static final Logger LOG = LoggerFactory.getLogger(LoggerTest.class);
//	private static final Logger LOG_DZ = LoggerFactory.getLogger("DZ");
//
//	public static void main(String[] args) throws InterruptedException {
//		int count = 15;
//		for (int i = 0; i < count; i++) {
//			LOG_DZ.warn("first to DAZONG");
//		}
//
//		for (int i = 0; i < count; i++) {
//			LOG.warn("FIRST SHOW CONSOLE");
//		}
//
//		for (int i = 0; i < count; i++) {
//			LOG_DZ.warn("SECOND TO DAZONG");
//		}
//	}
//}
