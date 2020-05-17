package com.bighao.beanFactoryPostProcessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

/**
 * @Author: bighao周启豪
 * @Date: 2020/1/25 13:45
 * @Version 1.0
 *
 * 通过BeanFactoryPostProcessor可以插手在容器实例化bean之前，读取bean的信息，并修改
 * 而BeanPostProcessor是在bean的实例化过程中进行修改
 */
//@Component
public class MyBeanFactoryProcessor implements BeanFactoryPostProcessor {
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		// 得到bean的定义
		AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) beanFactory.getBeanDefinition("userDao");
		// 修改bean的作用域
		annotatedBeanDefinition.setScope("prototype");
	}
}
