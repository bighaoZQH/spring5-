package com.bighao.cglib.config;

import com.bighao.cglib.service.OrderService;
import com.bighao.cglib.service.UserService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/20 19:31
 * @Version 1.0
 *
 * 测试cglib
 */

@Configuration
public class SpringConfig implements BeanFactoryAware {

	private BeanFactory beanFactory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Bean
	public UserService userService() {
		return new UserService();
	}

	@Bean
	public OrderService orderService() {
		userService();
		return new OrderService();
	}


}
