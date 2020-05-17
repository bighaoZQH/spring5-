package com.bighao.mapperscan;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/19 16:07
 * @Version 1.0
 */
public class MapperInvovcationHandler implements InvocationHandler {
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		System.out.println("MapperInvovcationHandler invoke...");
		return null;
	}
}
