package com.auto.service.impl;

import com.auto.service.AService;
import com.auto.service.OrderService;
import com.auto.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/27 0:24
 * @Version 1.0
 */
@Service("orderService")
public class OrderServiceImpl implements OrderService {

	private TaskService taskService;

	private AService aService;


	public OrderServiceImpl() {}

	@Autowired(required = false)
	public OrderServiceImpl(AService aService, TaskService taskService) {
		this.aService = aService;
		this.taskService = taskService;
	}

	@Autowired(required = false)
	public OrderServiceImpl(String s, Integer i) {
	}

	@Autowired(required = false)
	public OrderServiceImpl(Integer i, String s) {
	}

	@Autowired(required = false)
	public OrderServiceImpl(TaskService taskService, AService aService) {
		this.aService = aService;
		this.taskService = taskService;
	}



	@Autowired(required = false)
	public OrderServiceImpl(TaskService taskService) {
		this.taskService = taskService;
	}

	@Override
	public void query() {
		System.out.println("order query");
	}

}
