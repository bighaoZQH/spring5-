package com.bighao.test;

import com.bighao.beanFactoryPostProcessor.MyBeanFactoryProcessor;
import com.bighao.beanFactoryPostProcessor.MyRegistryPostProcessor;
import com.bighao.config.AppConfig;
import com.bighao.dao.TestDao;
import com.bighao.dao.UserDao;
import com.bighao.mapperscan.mapper.MyTestDao;
import org.springframework.cglib.core.SpringNamingPolicy;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @Author: bighao周启豪
 * @Date: 2020/1/22 13:30
 * @Version 1.0
 */
public class TestSpring {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(AppConfig.class);
		/*MyTestDao myTestDao = (MyTestDao) context.getBean("myTestDao");
		myTestDao.query();*/


		/*UserService userService = (UserService) context.getBean("userService");
		userService.find();*/

		/*AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(UserDao.class);
		context.refresh();*/

		/*AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(AppConfig.class);
		context.addBeanFactoryPostProcessor(new MyBeanFactoryProcessor());
		context.addBeanFactoryPostProcessor(new MyRegistryPostProcessor());
		context.refresh();
		UserDao userDao = (UserDao) context.getBean("userDao");
		UserDao userDao1 = (UserDao) context.getBean("userDao");
		System.out.println(userDao.hashCode() + " ------ " + userDao1.hashCode());
		userDao.query();*/

		// 测试ImportSelector
		/*BaseDao dao = (BaseDao) context.getBean("indexDao");
		dao.query();*/

		// 测试cglib 代理
		//Enhancer enhancer = new Enhancer();
		// 增强父类，地球人都知道cglib是基于继承来的
		//enhancer.setSuperclass(TestDao.class);
		//enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);

		// 过滤方法，不能每次都去new
		/*enhancer.setCallback(new TestMethodCallback());
		TestDao testDao = (TestDao) enhancer.create();
		testDao.query();*/
		//return enhancer;
		/*try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}*/
	}



}