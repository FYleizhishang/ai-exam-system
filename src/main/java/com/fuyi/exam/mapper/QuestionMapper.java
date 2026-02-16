package com.fuyi.exam.mapper;

import com.fuyi.exam.entity.Question;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface QuestionMapper {

    @Insert("INSERT INTO sys_question(title, type, options, answer, analysis, score, difficulty, knowledge_point) " +
            "VALUES(#{title}, #{type}, #{options}, #{answer}, #{analysis}, #{score}, #{difficulty}, #{knowledgePoint})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Question question);

    // 🔥 1. 查所有题目（带排序）
    @Select("SELECT * FROM sys_question ORDER BY type ASC, id DESC")
    List<Question> findAll();

    // 🔥 2. 查所有科目（用于左侧分类菜单，去重）
    @Select("SELECT DISTINCT knowledge_point FROM sys_question WHERE knowledge_point IS NOT NULL")
    List<String> findAllSubjects();

    // 🔥 3. 按科目查题
    @Select("SELECT * FROM sys_question WHERE knowledge_point = #{subject} ORDER BY type ASC")
    List<Question> findBySubject(String subject);

    // 🔥 4. 修改题目 (CRUD 的 Update)
    @Update("UPDATE sys_question SET title=#{title}, type=#{type}, options=#{options}, answer=#{answer}, " +
            "analysis=#{analysis}, score=#{score}, difficulty=#{difficulty} WHERE id=#{id}")
    int update(Question question);

    // 5. 删除题目
    @Delete("DELETE FROM sys_question WHERE id = #{id}")
    int deleteById(Integer id);

    // ... 其他原有的 batch 查询保持不变 ...
    @Select("<script>SELECT * FROM sys_question WHERE id IN <foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<Question> selectBatchIds(@Param("ids") List<Integer> ids);
    @Select("SELECT * FROM sys_question WHERE id = #{id}")
    Question selectById(Integer id);
}