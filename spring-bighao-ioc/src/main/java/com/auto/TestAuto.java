package com.auto;

import com.auto.config.SpringConfigByAuto;
import com.auto.service.OrderService;
import com.auto.service.TaskService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/22 1:20
 * @Version 1.0
 */
public class TestAuto {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(SpringConfigByAuto.class);

		/*OrderService orderService = (OrderService) context.getBean("orderService");
		orderService.query();*/

		context.getBean(OrderService.class);


	}
}
