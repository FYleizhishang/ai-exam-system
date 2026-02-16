package com.fuyi.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fuyi.exam.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}