package com.aop;

import com.aop.config.DBConfig;
import com.aop.config.SpringConfigByAop;
import com.aop.factorybean.TestFactory;
import com.aop.service.PayService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/22 1:20
 * @Version 1.0
 */
public class TestAop {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(SpringConfigByAop.class);

		Object bean = context.getBean("&testFactoryBean");
		TestFactory testService = (TestFactory) context.getBean("testFactoryBean");
		testService.query();

		/*PayService payService = context.getBean(PayService.class);
		payService.update("Ali");*/

		// 通过开关来动态赋值
		/*DBConfig dbConfig = context.getBean(DBConfig.class);
		System.out.println(dbConfig.getUsername());*/
	}
}
