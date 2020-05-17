package com.bighao.cglib;

import com.bighao.cglib.config.SpringConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/20 19:33
 * @Version 1.0
 */
public class TestCglib {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(SpringConfig.class);

		SpringConfig bean = context.getBean(SpringConfig.class);
		System.out.println("aa");
	}

}
