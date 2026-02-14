package com.fuyi.exam.mapper;

import com.fuyi.exam.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND is_deleted = 0")
    User findByUsername(String username);

    @Insert("INSERT INTO sys_user(username, password, name, role, create_time) VALUES(#{username}, #{password}, #{name}, #{role}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);

    @Update("UPDATE sys_user SET points = points + #{points} WHERE id = #{id}")
    void addPoints(@Param("id") Integer id, @Param("points") Integer points);
}