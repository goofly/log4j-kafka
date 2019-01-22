package com.goofly.log4j.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**   
 * @ClassName:  InterAddressUtil   
 * @Description:
 * @author: goofly
 * @date:   2018年6月28日 下午6:55:35   
 *      
 */
public class InterAddressUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(InterAddressUtil.class);
	
	public static InetAddress getInetAddress() {
		InetAddress add = null;
		try {
			add = InetAddress.getLocalHost();;
		} catch (UnknownHostException e) {
			logger.error("获取网络地址失败!",e);
		}
		return add;
	}

}
