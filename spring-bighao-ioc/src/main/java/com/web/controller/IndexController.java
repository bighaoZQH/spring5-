package com.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/29 15:22
 * @Version 1.0
 */

@Controller
@RequestMapping("/app")
public class IndexController {

	@RequestMapping("/index")
	public String index() {
		System.out.println("index controller");
		// 需要视图解析器
		return "index";
	}


	@ResponseBody
	@RequestMapping("/map")
	public Map<String, String> map() {
		HashMap<String, String> map = new HashMap<>();
		map.put("xx", "xxx");
		// 需要json解析器
		return map;
	}

}
