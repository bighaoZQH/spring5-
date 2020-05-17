package com.beanpostprocessors;

import com.beanpostprocessors.config.TestBPConfig;
import com.beanpostprocessors.dao.VideoDao;
import com.beanpostprocessors.service.InjectService;
import com.beanpostprocessors.service.VideoService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/30 17:05
 * @Version 1.0
 */
public class TestBP {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(TestBPConfig.class);
		//context.getBean(VideoService.class).query();
		//System.out.println(context.getBean("videoService"));

		((InjectService) context.getBean("injectService")).query();
	}

}
