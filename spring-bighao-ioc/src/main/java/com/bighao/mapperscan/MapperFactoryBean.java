package com.bighao.mapperscan;

import com.bighao.mapperscan.anno.SelectBg;
import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/19 19:00
 * @Version 1.0
 */

//@Component //这里一定不要让Spring去扫描到
public class MapperFactoryBean implements FactoryBean<Object>, InvocationHandler {

	Class<?> clazz;

	public MapperFactoryBean(Class<?> clazz) {
		this.clazz = clazz;
	}

	@Override
	public Object getObject() throws Exception {
		Class<?>[] clazzs = new Class<?>[] {clazz};
		Object proxy = Proxy.newProxyInstance(this.getClass().getClassLoader(), clazzs, this);
		return proxy;
	}

	@Override
	public Class<?> getObjectType() {
		return clazz;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		System.out.println("MapperFactoryBean proxy");
		// 获取mapper上的sql语句
		// 1.获取对应方法 我这边简单写死了
		Method method1 = proxy.getClass().getInterfaces()[0].getMethod(method.getName(), String.class);
		// 2.获取注解 我这边也简单写死了
		SelectBg select = method1.getDeclaredAnnotation(SelectBg.class);
		System.out.println(select.value()[0]);
		return null;
	}

}
