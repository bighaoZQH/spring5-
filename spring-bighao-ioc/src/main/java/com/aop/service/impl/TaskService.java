package com.aop.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/25 15:30
 * @Version 1.0
 */
@Service
//@Scope("prototype")
public class TaskService {

	@Autowired
	private IndexService indexService;

}
