package com.bighao.imports.dao;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

/**
 * @Author: bighao周启豪
 * @Date: 2020/1/22 13:28
 * @Version 1.0
 */

@Component
public class IndexDao implements BaseDao {

	@Override
	public void query() {
		System.out.println("IndexDao111");
	}

}
