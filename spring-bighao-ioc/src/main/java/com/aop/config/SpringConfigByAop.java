package com.aop.config;

import com.aop.anno.EnableDBSession;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @Author: bighao周启豪
 * @Date: 2020/1/22 13:29
 * @Version 1.0
 */
@Configuration
@ComponentScan("com.aop")
@EnableAspectJAutoProxy
//@EnableDBSession // 自己模拟的动态开关
public class SpringConfigByAop {


}
