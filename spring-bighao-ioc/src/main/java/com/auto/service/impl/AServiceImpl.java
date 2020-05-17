package com.auto.service.impl;

import com.auto.service.AService;
import com.auto.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/27 0:49
 * @Version 1.0
 */
@Service("aService")
public class AServiceImpl implements AService {

	// 循环依赖
	@Autowired
	private TaskService taskService;

	@Override
	public void query() {

	}
}
