package com.web.web;

import com.web.config.AppConfig;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.ServletRegistration;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/29 14:49
 * @Version 1.0
 */
public class SpringApplication {

	public static void run() {

		Tomcat tomcat = new Tomcat();
		try {
			tomcat.setPort(10086);
			// addWebapp表示告诉tomcat我这个项目是web项目，会去自动执行MyWebApplicationInitializer
			// 但是会产生一个异常，需要添加jar包，如何不加jar包也解决问题呢?
			// 用addContext，但是用了addContext就不会执行MyWebApplicationInitializer了，
			// 因此就不用MyWebApplicationInitializer了，直接把代码写在这里
			//tomcat.addWebapp("/", "D:\\code_yl\\tomcat10086\\");
			/**
			 * 但是springboot是通过addContext来做的，那怎么初始化spring环境呢，很简单，
			 * 直接把代码也写到这里就行了
			 */
			tomcat.addContext("/", "D:\\code_yl\\tomcat10086\\");

			// Load Spring web application configuration
			// spring ioc环境 init
			AnnotationConfigWebApplicationContext ac = new AnnotationConfigWebApplicationContext();
			ac.register(AppConfig.class);
			// 配置类加了@EnableMVC后，这里不需要去初始化spring环境，会报错，因为没有DispatcherServlet
			// 而且实际上DispatcherServlet也会去初始化spring环境
			//ac.refresh();

			// Create and register the DispatcherServlet
			// 通过构造传入spring的context，让DispatcherServlet与配置类关联起来
			DispatcherServlet servlet = new DispatcherServlet(ac);
			// 通过tomcat来获取servlet spring web 环境
			// 初始化controller和请求映射
			Wrapper mvc = tomcat.addServlet("/", "mvc", servlet);
			// 容器启动后执行DispatcherServlet的init()方法
			mvc.setLoadOnStartup(1);
			mvc.addMapping("/");

			tomcat.start();
			tomcat.getServer().await();
		} catch (LifecycleException e) {
			e.printStackTrace();
		}
	}

}
