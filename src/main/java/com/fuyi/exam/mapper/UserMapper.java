package com.fuyi.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fuyi.exam.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    // 🔥🔥 修复：查 sys_user 表
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND password = #{password}")
    User findByUsernameAndPassword(@Param("username") String username, @Param("password") String password);

    // 🔥🔥 修复：查 sys_user 表
    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    User findByUsername(@Param("username") String username);
}