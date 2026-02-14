package com.fuyi.exam.mapper;

import com.fuyi.exam.entity.ExamRecord;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface ExamRecordMapper {

    // 🔥🔥 [升级] 插入语句：增加了 status, switch_count, answers, exam_time 🔥🔥
    @Insert("INSERT INTO sys_exam_record(user_id, paper_id, total_score, exam_time, answers, ai_diagnosis, status, switch_count) " +
            "VALUES(#{userId}, #{paperId}, #{score}, #{examTime}, #{answers}, #{aiDiagnosis}, #{status}, #{switchCount})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ExamRecord record);

    // 根据ID查询
    @Select("SELECT * FROM sys_exam_record WHERE id = #{id}")
    ExamRecord selectById(Integer id);

    // 🔥🔥 [新增] 获取某张试卷的所有考试记录 (用于统计及格率) 🔥🔥
    @Select("SELECT * FROM sys_exam_record WHERE paper_id = #{paperId}")
    List<ExamRecord> findByPaperId(Integer paperId);

    // 🔥🔥 [新增] 教师端：获取所有待批改/已批改的记录 🔥🔥
    @Select("SELECT r.id, r.user_id, r.paper_id, r.total_score as score, r.exam_time, p.title as paperTitle " +
            "FROM sys_exam_record r " +
            "LEFT JOIN sys_paper p ON r.paper_id = p.id " +
            "ORDER BY r.exam_time DESC")
    List<Map<String, Object>> findAllRecords();

    // 🔥🔥 [新增] 教师端：更新成绩和评语 (用于老师手动改分) 🔥🔥
    @Update("UPDATE sys_exam_record SET total_score = #{score}, ai_diagnosis = #{diagnosis} WHERE id = #{id}")
    void updateScoreAndDiagnosis(@Param("id") Integer id, @Param("score") Integer score, @Param("diagnosis") String diagnosis);
}