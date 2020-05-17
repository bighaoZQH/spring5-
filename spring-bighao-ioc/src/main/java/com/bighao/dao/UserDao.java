package com.bighao.dao;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Description;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;

/**
 * @Author: bighao周启豪
 * @Date: 2020/1/22 13:28
 * @Version 1.0
 *
 * 为了解决单例对象中获取原型对象的问题，除了@Lockup以外，还可以通过实现ApplicationContextAware接口
 * 来获取spring上下文，再从spring上下文中getBean
 * 那这个ApplicationContextAware是怎么做到获取spring上下文的
 * 就是要看源码refresh()==>prepareBeanFactory(beanFactory);==>
 * beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));===>
 * ((ApplicationContextAware) bean).setApplicationContext(this.applicationContext);
 */
@Repository
//@Scope("prototpye")
//@Description("dao")
public class UserDao implements ApplicationContextAware {

	private ApplicationContext applicationContext;

	public UserDao() {
		System.out.println("UserDao构造");
	}

	@PostConstruct
	public void init() {
		System.out.println("UserDao init");
	}


	public void query() {
		System.out.println("dao query");
		//applicationContext.getBean("");
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
		System.out.println(applicationContext);
	}
}
