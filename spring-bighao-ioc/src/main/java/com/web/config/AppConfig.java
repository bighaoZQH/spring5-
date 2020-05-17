package com.web.config;

import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/29 15:22
 * @Version 1.0
 */

@Configuration
@ComponentScan("com.web")
//@EnableWebMvc // 已结不是一个web项目了，就不要再加了
public class AppConfig implements WebMvcConfigurer {

	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		converters.add(new FastJsonHttpMessageConverter());
	}
}
