package com.fuyi.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fuyi.exam.common.Result;
import com.fuyi.exam.entity.Course;
import com.fuyi.exam.mapper.CourseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/course")
@CrossOrigin
public class CourseController {

    @Autowired private CourseMapper courseMapper;

    @GetMapping("/list")
    public Result<List<Course>> list() {
        QueryWrapper<Course> query = new QueryWrapper<>();
        query.orderByDesc("id");
        return Result.success(courseMapper.selectList(query));
    }

    // 老师添加课程 (为了演示方便，先开放给所有登录用户)
    @PostMapping("/add")
    public Result<String> add(@RequestBody Course course) {
        course.setCreateTime(new Date());
        courseMapper.insert(course);
        return Result.success("添加成功");
    }
}