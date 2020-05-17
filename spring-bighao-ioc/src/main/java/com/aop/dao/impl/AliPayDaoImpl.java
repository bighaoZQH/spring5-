package com.aop.dao.impl;

import com.aop.dao.PayDao;
import org.springframework.stereotype.Repository;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/22 1:22
 * @Version 1.0
 */
//@Repository
public class AliPayDaoImpl implements PayDao {

	@Override
	public void update(String sql) {
		System.out.println("AliPayDaoImpl target update...");
	}

}
