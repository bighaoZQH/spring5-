package com.bighao.beanFactoryPostProcessor;

/**
 * @Author: bighao周启豪
 * @Date: 2020/3/18 10:57
 * @Version 1.0
 */
public class DyService {

	private String author;

	public DyService() {
		System.out.println("DyService被创建");
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	@Override
	public String toString() {
		return "DyService{" +
				"author='" + author + '\'' +
				'}';
	}
}
