package com.aop.anno;

import com.aop.config.DBConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/22 2:15
 * @Version 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Import(DBConfig.class)
public @interface EnableDBSession {

	// 模拟一个动态的开关，动态给db配置文件赋值
	String username() default "root";

}
