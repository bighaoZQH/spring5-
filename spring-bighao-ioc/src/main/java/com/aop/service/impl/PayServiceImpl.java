package com.aop.service.impl;

import com.aop.dao.PayDao;
import com.aop.service.PayService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/22 1:22
 * @Version 1.0
 */
//@Service
public class PayServiceImpl implements PayService, ApplicationContextAware {

	private ApplicationContext applicationContext;

	/*@Autowired
	private PayDao payDao;*/

	@Autowired
	private Map<String, PayDao> payDaoMap;

	// 两种方式来解决类型重复的问题
	@Override
	public void update(String userName) {
		if ("Ali".equals(userName)) {
			((PayDao) applicationContext.getBean("aliPayDaoImpl")).update("");
			payDaoMap.get("aliPayDaoImpl").update("");
		} else if ("WeChat".equals(userName)) {
			((PayDao) applicationContext.getBean("wechatPayDaoImpl")).update("");
			payDaoMap.get("wechatPayDaoImpl").update("");
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
