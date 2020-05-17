package com.bighao.mapperscan;

import com.bighao.mapperscan.mapper.MyTestDao;
import com.bighao.test.TestSpring;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.lang.reflect.Proxy;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/19 16:00
 * @Version 1.0
 *
 *
 * 模拟MapperScan
 */
public class MyImportBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {


	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		/**
		 * 1.得到bd
		 */
		// 扫描所有接口，我这里为了方便写死
		// 将dao接口构建成BeanDefinition
		MyTestDao dao = (MyTestDao) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class<?>[] {MyTestDao.class}, new MapperInvovcationHandler());
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MyTestDao.class);
		GenericBeanDefinition beanDefinition = (GenericBeanDefinition) builder.getBeanDefinition();
		System.out.println(beanDefinition.getBeanClassName());
		// 通过构造方法进行实例化，将参数类型告诉Spring，Spring会自己去找该类型的bean
		beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(beanDefinition.getBeanClassName());
		// 将beanClass设置为FactoryBean
		beanDefinition.setBeanClass(MapperFactoryBean.class);
		// 向ioc容器注册我们的bean信息
		registry.registerBeanDefinition("myTestDao", beanDefinition);
	}


}
