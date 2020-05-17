package com.auto.dao.impl;

import com.auto.dao.OrderDao;
import org.springframework.stereotype.Repository;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/27 0:24
 * @Version 1.0
 */
@Repository
public class OrderDaoImpl implements OrderDao {
	@Override
	public void query() {
		System.out.println("order query");
	}

}
