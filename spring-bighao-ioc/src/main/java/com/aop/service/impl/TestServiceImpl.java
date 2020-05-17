package com.aop.service.impl;

import com.aop.dao.TestDao;
import com.aop.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/23 22:58
 * @Version 1.0
 */

//@Service
public class TestServiceImpl implements TestService {

	//public TestServiceImpl() {}


	/*@Autowired
	public TestServiceImpl(String str, Object obj, Integer i) {

	}*/

	/*public TestServiceImpl(String str, Object obj) {

	}*/

	@Autowired
	private TestDao testDao;

	@Override
	public void query() {
		testDao.query();
	}

}
