package com.bighao.anno;

import com.bighao.imports.MyImportSelector;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @Author: bighao周启豪
 * @Date: 2020/1/27 14:58
 * @Version 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Import(MyImportSelector.class)
public @interface EnableBg {
}
