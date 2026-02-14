package com.fuyi.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import com.fuyi.exam.entity.Post; // 为了不想新建实体类，这里临时复用一下，实际上只用到 BaseMapper 的方法

@Mapper
public interface PostLikeMapper extends BaseMapper<Post> { // 这里泛型其实不重要，主要用 SQL

    @Select("SELECT count(*) FROM sys_post_like WHERE post_id = #{postId} AND user_id = #{userId}")
    int countLike(Integer postId, Integer userId);

    @Select("INSERT INTO sys_post_like(post_id, user_id) VALUES(#{postId}, #{userId})")
    void addLike(Integer postId, Integer userId);

    @Select("DELETE FROM sys_post_like WHERE post_id = #{postId} AND user_id = #{userId}")
    void removeLike(Integer postId, Integer userId);
}