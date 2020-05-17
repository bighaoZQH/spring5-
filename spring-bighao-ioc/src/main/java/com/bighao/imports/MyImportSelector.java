package com.bighao.imports;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @Author: bighao周启豪
 * @Date: 2020/1/27 13:58
 * @Version 1.0
 */
public class MyImportSelector implements ImportSelector {

	@Override
	public String[] selectImports(AnnotationMetadata importingClassMetadata) {
		return new String[]{MyImportSelector2.class.getName()};
	}

}
