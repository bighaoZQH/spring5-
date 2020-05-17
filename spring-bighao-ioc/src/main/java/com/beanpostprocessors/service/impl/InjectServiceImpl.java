package com.beanpostprocessors.service.impl;

import com.beanpostprocessors.dao.VideoDao;
import com.beanpostprocessors.service.InjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/30 19:37
 * @Version 1.0
 *
 *
 *
 * 测试Spring如何注入的
 *
 * Spring要注入对象的时候，就要去从容器中找到被注入的对象，
 * 找的方式有两种，byName和byType
 *
 * 而注入方式有三种，反射通过构造方法注入，反射通过set方法注入，反射通过属性注入
 */
@Service("injectService")
public class InjectServiceImpl implements InjectService {

	@Autowired // @Autowired加在属性上就是通过反射注入，先byName，然后byType
	VideoDao videoDao;

	public InjectServiceImpl() {
		System.out.println("InjectServiceImpl no args cons");
	}

	@Autowired // 构造注入，与参数名无关,和参数类型有关，与有没有这个全局变量也无关
	public InjectServiceImpl(VideoDao dao) {
		System.out.println("InjectServiceImpl cons with args==>" + dao);
		this.videoDao = dao;
	}

	@Autowired // set注入，与set方法名无关，与参数名无关，和参数类型有关，与有没有这个全局变量也无关
	public void setXxx(VideoDao dao) {
		System.out.println("inject by setVideoDao with args==>" + dao);
		this.videoDao = dao;
	}

	public void query() {
		videoDao.query();
	}

}
