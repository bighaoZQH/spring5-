package com.bighao.beanFactoryPostProcessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class MyRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {

	/**
	 * BeanFactoryPostProcessor接口提供的方法，后执行
	 */
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		System.out.println("postProcessBeanFactory方法后执行,动态注册后的bean的数量为" + beanFactory.getBeanDefinitionCount());
	}

	/**
	 * BeanDefinitionRegistryPostProcessor接口扩展的方法
	 * 优先于postProcessBeanFactory方法执行
	 */
	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		System.out.println("postProcessBeanDefinitionRegistry方法先执行,动态注册bean之前此时bean的数量为:" + registry.getBeanDefinitionCount());
		// 动态注册10个bean
		for (int i = 0; i < 10; i++) {
			AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(DyService.class)
					.addPropertyValue("author", "bighao周启豪" + i).getBeanDefinition();
			registry.registerBeanDefinition("dyService" + i, beanDefinition);

		}
	}

}