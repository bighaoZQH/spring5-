package com.beanpostprocessors.dao.impl;

import com.beanpostprocessors.dao.VideoDao;
import org.springframework.stereotype.Repository;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/30 17:08
 * @Version 1.0
 */
@Repository
public class VideoDaoImpl implements VideoDao {
	@Override
	public void query() {
		System.out.println("voide dao query");
	}
}
