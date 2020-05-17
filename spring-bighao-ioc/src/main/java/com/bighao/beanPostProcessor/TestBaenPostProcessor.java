package com.bighao.beanPostProcessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;

/**
 * @Author: bighao周启豪
 * @Date: 2020/1/24 20:43
 * @Version 1.0
 *
 * 自定义后置处理器 非常重要
 * BeanPostProcessor是Spring框架的提供的一个扩展类点（不止一个）
 * 通过实现BeanPostProcessor接口，程序员就可插手bean实例化的过程,从而减轻了beanFactory的负担
 *
 * AOP也正是通过BeanPostProcessor和IOC容器建立起了联系
 *
 * 实现PriorityOrdered可以来修改多个PostProcessor的执行顺序
 */
//@Component
public class TestBaenPostProcessor implements BeanPostProcessor, PriorityOrdered {

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (beanName.equals("userDao")) {
			System.out.println("BeforeInitialization");
		}
		//Proxy.newProxyInstance() 在这里可以把代理对象返回出去
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (beanName.equals("userDao")) {
			System.out.println("AfterInitialization");
		}
		return bean;
	}

	@Override
	public int getOrder() {
		return 102;
	}
}
