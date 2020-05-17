package com.bighao.imports;

import com.bighao.imports.dao.BaseDao;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Proxy;

/**
 * @Author: bighao周启豪
 * @Date: 2020/1/22 13:28
 * @Version 1.0
 */
public class AopBeanPostProcessor implements BeanPostProcessor {

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (beanName.equals("indexDao")) {
			Class<?>[] clazzs = new Class<?>[]{BaseDao.class};
			bean = Proxy.newProxyInstance(this.getClass().getClassLoader(), clazzs, new MyInvocationHandler(bean));
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return null;
	}
}
