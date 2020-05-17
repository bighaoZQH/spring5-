package com.aop.dao.impl;

import com.aop.dao.TestDao;
import org.springframework.stereotype.Repository;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/23 22:58
 * @Version 1.0
 */

//@Repository
public class TestDaoImpl implements TestDao {
	@Override
	public void query() {
		System.out.println("TestDaoImpl query");
	}
}
