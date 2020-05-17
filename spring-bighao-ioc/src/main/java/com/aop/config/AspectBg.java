package com.aop.config;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/22 1:13
 * @Version 1.0
 */

@Component
@Aspect
public class AspectBg {


	@Pointcut("execution(* com.aop.service..*.*(..))")
	public void pointCut() {

	}

	@Before("pointCut()")
	public void before() {
		System.out.println("proxy before");
	}

}
