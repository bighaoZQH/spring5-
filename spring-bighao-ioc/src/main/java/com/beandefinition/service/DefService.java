package com.beandefinition.service;

import org.springframework.beans.factory.annotation.Value;

/**
 * @Author: bighao周启豪
 * @Date: 2020/4/7 14:34
 * @Version 1.0
 */

public class DefService {

	@Value("defService")
	private String beanName;

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}
}
