package com.fuyi.exam.mapper;

import com.fuyi.exam.entity.Paper;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface PaperMapper {

    // 1. 发布试卷 (自动记录当前时间 NOW())
    @Insert("INSERT INTO sys_paper(title, total_score, duration, question_ids, status, create_time) " +
            "VALUES(#{title}, #{totalScore}, #{duration}, #{questionIds}, #{status}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Paper paper);

    // 2. 查单个试卷
    @Select("SELECT * FROM sys_paper WHERE id = #{id}")
    @Results({
            @Result(property = "totalScore", column = "total_score"),
            @Result(property = "questionIds", column = "question_ids"),
            @Result(property = "createTime", column = "create_time")
    })
    Paper selectById(Integer id);

    // 3. 查所有试卷
    @Select("SELECT * FROM sys_paper ORDER BY create_time DESC, id DESC")
    @Results({
            @Result(property = "totalScore", column = "total_score"),
            @Result(property = "questionIds", column = "question_ids"),
            @Result(property = "createTime", column = "create_time")
    })
    List<Paper> findAll();

    // 🔥🔥 4. 新增：删除试卷 🔥🔥
    @Delete("DELETE FROM sys_paper WHERE id = #{id}")
    int deleteById(Integer id);
}