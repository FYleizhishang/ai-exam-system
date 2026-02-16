package com.fuyi.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fuyi.exam.entity.Post;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostMapper extends BaseMapper<Post> {
    // 继承 BaseMapper 后，自动拥有 selectList, insert, selectById 等方法
}