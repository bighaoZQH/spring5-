package com.bighao.imports;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @Author: bighao周启豪
 * @Date: 2020/1/27 15:09
 * @Version 1.0
 */
public class MyInvocationHandler implements InvocationHandler {

	private Object target;

	public MyInvocationHandler(Object target) {
		this.target = target;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		System.out.println("invoke 我是代理方法...");
		return method.invoke(target, args);
	}
}
