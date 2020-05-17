package com.beanpostprocessors.service.impl;

import com.beanpostprocessors.dao.VideoDao;
import com.beanpostprocessors.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/30 17:07
 * @Version 1.0
 */
@Service("videoService")
public class VideoServiceImpl implements VideoService {

	@Autowired
	private VideoDao videoDao;

	@Override
	public void query() {
		System.out.println(videoDao.getClass().getSimpleName());
	}
}
