package com.beanpostprocessors.beanprocessor;

import com.beanpostprocessors.service.VideoService;
import org.apache.ibatis.annotations.Select;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.stereotype.Component;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/30 17:11
 * @Version 1.0
 */
@Component
public class TestInstantiation01 implements InstantiationAwareBeanPostProcessor, InvocationHandler {

	/**
	 * InstantiationAwareBeanPostProcessor这个后置处理器是spring bean的生命周期中最先执行的
	 */

	/**
	 * 在目标对象实例化之前调用，方法的返回值类型是Object，我们可以返回任何类型的值。
	 * 由于这个时候目标对象还未实例化，所以这个返回值可以用来代替原本该生成的目标对象的实例(一般都是代理对象)。
	 * 如果该方法的返回值代替原本该生成的目标对象，后续只有postProcessAfterInitialization方法会调用，
	 * 其它方法不再调用；否则按照正常的流程走
	 */
	@Override
	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		if ("videoService".equals(beanName)) {
			// 可以在这里返回代理对象
			return Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class<?>[] {VideoService.class}, this);
		}
		return null;
	}

	/**
	 * 方法在目标对象实例化之后调用，这个时候对象已经被实例化，但是该实例的属性还未被设置，都是null。
	 * 如果该方法返回false，会忽略属性值的设置；如果返回true，会按照正常流程设置属性值。
	 * 方法不管postProcessBeforeInstantiation方法的返回值是什么都会执行
	 */
	@Override
	public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
		if ("videoService".equals(beanName)) {
			System.out.println("AfterInstantiation===>" + beanName);
		}
		return true;
	}


	/**
	 * 方法对属性值进行修改(这个时候属性值还未被设置，但是我们可以修改原本该设置进去的属性值)。
	 * 如果postProcessAfterInstantiation方法返回false，该方法不会被调用。可以在该方法内对属性值进行修改
	 */
	/*@Override
	public PropertyValues postProcessPropertyValues(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {
		return null;
	}*/


	/**
	 * 父接口BeanPostProcessor的2个方法postProcessBeforeInitialization
	 * 和postProcessAfterInitialization都是在目标对象被实例化之后，并且属性也被设置之后调用的
	 */
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if ("videoService".equals(beanName))
			System.out.println("Before " + beanName);
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if ("videoService".equals(beanName))
			System.out.println("After " + beanName);
		return bean;
	}


	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		System.out.println("invoke");
		System.out.println("connection db");
		if (method.isAnnotationPresent(Select.class)) {
			Select select = method.getAnnotation(Select.class);
			System.out.println(select.value()[0]);
		}

		if (method.getName().equals("toString")) {
			return "invoke toString";
		}

		return proxy;
	}

}
