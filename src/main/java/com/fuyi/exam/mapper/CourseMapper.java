package com.fuyi.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fuyi.exam.entity.Course;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {
    // 继承 BaseMapper 后，自动拥有 selectList, insert 等方法
}