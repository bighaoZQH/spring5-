package com.bighao.test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/14 22:39
 * @Version 1.0
 */
public class TestXml {
	public static void main(String[] args) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("application.xml");
	}
}
