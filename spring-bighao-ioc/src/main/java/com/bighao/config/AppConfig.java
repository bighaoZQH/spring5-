package com.bighao.config;

import com.bighao.anno.EnableBg;
import com.bighao.dao.TestDao;
import com.bighao.dao.TestDao1;
import com.bighao.imports.MyImportSelector;
import com.bighao.mapperscan.MyImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.*;

/**
 * @Author: bighao周启豪
 * @Date: 2020/1/22 13:29
 * @Version 1.0
 */
@Configuration
@ComponentScan("com.bighao")
//@EnableBg
//@EnableAspectJAutoProxy //这个注解的作用是让spring的后置处理器添加一个处理能够让我们的spring的bean变成一个代理对象
@Import(MyImportBeanDefinitionRegistrar.class)
public class AppConfig {

	//@Bean //也可以注入一个bean
	public TestDao testDao() {
		testDao1();
		return new TestDao();
	}

	//@Bean
	public TestDao1 testDao1() {
		return new TestDao1();
	}

}
