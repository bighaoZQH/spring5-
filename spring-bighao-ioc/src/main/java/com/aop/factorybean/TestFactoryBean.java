package com.aop.factorybean;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/26 13:57
 * @Version 1.0
 */
@Component
public class TestFactoryBean implements FactoryBean<Object> {
	@Override
	public Object getObject() throws Exception {
		return new TestFactory();
	}

	@Override
	public Class<?> getObjectType() {
		return null;
	}
}
