package com.aop.config;

import com.aop.anno.EnableDBSession;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/22 2:14
 * @Version 1.0
 *
 * 通过ImportAware接口，实现一个动态开关
 */
//@Configuration
public class DBConfig implements ImportAware {

	// 通过开关来动态赋值
	private String username = "test";

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		//System.out.println(this.username);
		Map<String, Object> map = importMetadata.getAnnotationAttributes(EnableDBSession.class.getName());
		AnnotationAttributes attrs = AnnotationAttributes.fromMap(map);
		this.username = attrs.getString("username");
		//System.out.println(this.username);
	}

	public String getUsername() {
		return username;
	}
}
