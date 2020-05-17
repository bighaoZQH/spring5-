package com.beandefinition;

import com.beandefinition.config.DefConfig;
import com.beandefinition.mapperscan.CustomScanner;
import com.beandefinition.service.GenDefService;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.ChildBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @Author: bighao周启豪
 * @Date: 2020/4/7 14:26
 * @Version 1.0
 */
public class TestScan {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext();
		context.register(DefConfig.class);

		context.refresh();

		CustomScanner customScanner = new CustomScanner(context);
		customScanner.addIncludeFilter(null);
		int scan = customScanner.scan("com.beandefinition");
		System.out.println(scan);

	}

}
