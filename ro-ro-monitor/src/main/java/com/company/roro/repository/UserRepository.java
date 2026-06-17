package com.company.roro.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.roro.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserRepository extends BaseMapper<User> {

    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(String username);
}
