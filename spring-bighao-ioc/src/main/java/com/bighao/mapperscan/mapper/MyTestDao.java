package com.bighao.mapperscan.mapper;


import com.bighao.mapperscan.anno.SelectBg;

/**
 * @Author: bighao周启豪
 * @Date: 2020/1/26 18:18
 * @Version 1.0
 */
public interface MyTestDao {
    @SelectBg("select * from user where id=#{id}")
    void query(String id);

    void query();
}
