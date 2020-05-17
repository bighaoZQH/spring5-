package com.bighao.test;

import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * @Author: bighao周启豪
 * @Date: 2020/1/28 16:22
 * @Version 1.0
 */
public class TestMethodCallback implements MethodInterceptor {
	/**
	 * spring底层就是通过判断methodProxy和method是不是同一个方法来判断要不要new一个bean
	 * @param o 代理对象
	 * @param method 目标对象的方法
	 * @param objects 参数
	 * @param methodProxy 代理方法
	 * @return
	 * @throws Throwable
	 */
	@Override
	public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
		System.out.println("method---");
		return methodProxy.invokeSuper(o, objects);
	}
}
