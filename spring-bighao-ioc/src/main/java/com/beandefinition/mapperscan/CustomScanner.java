package com.beandefinition.mapperscan;

import com.beandefinition.mapperscan.anno.BigMapperScan;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

/**
 * @Author: bighao周启豪
 * @Date: 2020/4/7 18:47
 * @Version 1.0
 *
 * 扩展spring的自定义扫描
 */
public class CustomScanner extends ClassPathBeanDefinitionScanner {

	public CustomScanner(BeanDefinitionRegistry registry) {
		super(registry);
	}


	@Override
	protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
		AnnotationMetadata metadata = beanDefinition.getMetadata();
		return metadata.isInterface();
	}

	/**
	 * 扫描加了BigMapperScan注解的类 这里写死了，也可以在外面传入
	 * @param includeFilter
	 */
	@Override
	public void addIncludeFilter(TypeFilter includeFilter) {
		super.addIncludeFilter(new AnnotationTypeFilter(BigMapperScan.class));
	}
}
