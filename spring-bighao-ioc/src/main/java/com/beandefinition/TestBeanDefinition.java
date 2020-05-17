package com.beandefinition;

import com.beandefinition.config.DefConfig;
import com.beandefinition.service.DefService;
import com.beandefinition.service.GenDefService;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.ChildBeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @Author: bighao周启豪
 * @Date: 2020/4/7 14:26
 * @Version 1.0
 */
public class TestBeanDefinition {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext();
		context.register(DefConfig.class);


		// 1.手动把一个类变成beanDefinition
		/*GenericBeanDefinition gbd1 = new GenericBeanDefinition();
		gbd1.setBeanClass(GenDefService.class);
		gbd1.setScope(BeanDefinition.SCOPE_SINGLETON);*/

		// 然后发现我再把一个类变成beanDefinition，就有重复的代码了
		// 因此可以通过模版来解决，spring中提供了RootBeanDefinition，设置abstract为true，给其他bean继承
		/*GenericBeanDefinition gbd2 = new GenericBeanDefinition();
		gbd2.setBeanClass(DefService.class);
		gbd2.setScope(BeanDefinition.SCOPE_SINGLETON);*/

		/**
		 * 作用：1.bean定义模版 2.描述真实的bean
		 */
		RootBeanDefinition root = new RootBeanDefinition();
		root.getPropertyValues().add("beanName", "defService");
		root.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		// 如果不是抽象，就必须设置一个beanClass
		//root.setBeanClass(DefService.class);
		// 用于模版，spring去实例化的时候会判断，如果是abstract是不能实例化的，且此时也不需要提供一个beanClass了
		root.setAbstract(true);

		// 子bean，创建时要指定父bean的名字
		ChildBeanDefinition child = new ChildBeanDefinition("defService");
		child.setBeanClass(GenDefService.class);

		/**
		 * 但这里有个问题，为什么Spring开发了一个ChildBeanDefinition
		 * RootBeanDefinition也有setParentName，那不是也可以实现父子bean吗?
		 *
		 * 首先root只能作为父亲，不能作为子
		 * child只能作为子，而且必须要有父
		 * 只有GenericBeanDefinition可以完成root和child都能做的
		 *
		 * 换言之，GenericBeanDefinition是通用的bean定义，
		 * 而Spring之所以写了一个child是因为可以用来预先确定父子bean的关系
		 *
		 * 但在Spring2.5以后，绝大多数情况还是可以使用GenericBeanDefinition来做
		 *
		 * @Bean是被包装为ConfigurationClassBeanDefinition
		 * @Compoent标注的被包装为ScannedGenericBeanDefinition
		 * javaConfig被包装为AnnotatedGenericBeanDefinition
		 *
		 */

		context.registerBeanDefinition("defService", root);
		context.registerBeanDefinition("genDefService", child);


		context.refresh();

		System.out.println(context.getBean(GenDefService.class));

	}

}
