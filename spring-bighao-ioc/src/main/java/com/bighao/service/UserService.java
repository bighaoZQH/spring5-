package com.bighao.service;

import com.bighao.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author: bighao周启豪
 * @Date: 2020/1/22 13:29
 * @Version 1.0
 */
@Service
public class UserService {

	@Autowired
	private UserDao userDao;

	public void find() {
		userDao.query();
	}
}
