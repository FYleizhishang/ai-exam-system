package com.fuyi.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fuyi.exam.entity.ExamRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ExamRecordMapper extends BaseMapper<ExamRecord> {

    // 🔥 1. 用于考情分析 (TeacherController 用到了)
    @Select("SELECT * FROM sys_exam_record WHERE paper_id = #{paperId}") // 修复为物理表名 sys_exam_record
    List<ExamRecord> findByPaperId(@Param("paperId") Integer paperId);

    // 🔥 2. 用于获取待阅卷列表 (ExamService 用到了)
    @Select("SELECT r.*, u.username as student_name " +
            "FROM sys_exam_record r " + // 修复为物理表名 sys_exam_record
            "LEFT JOIN sys_user u ON r.user_id = u.id " +
            "WHERE r.status != 2 " +
            "ORDER BY r.submit_time DESC") // 修复为 submit_time
    List<Map<String, Object>> findAllRecords();

    // 🔥 3. 用于 AI 异步回写分数 (ExamService 用到了)
    @Update("UPDATE sys_exam_record SET score = #{score}, ai_diagnosis = #{diagnosis} WHERE id = #{id}") // 修复为物理表名
    void updateScoreAndDiagnosis(@Param("id") Integer id,
                                 @Param("score") Integer score,
                                 @Param("diagnosis") String diagnosis);

    // ================== 以下为新增：用于学生获取历史成绩单 ==================

    // 🔥 4. 学生查询自己已出分的历史成绩
    @Select("SELECT r.id, r.score, r.ai_diagnosis as aiDiagnosis, r.submit_time as submitTime, p.title as paperTitle " +
            "FROM sys_exam_record r " +
            "LEFT JOIN sys_paper p ON r.paper_id = p.id " +
            "WHERE r.user_id = #{userId} AND r.status = 2 " +
            "ORDER BY r.submit_time DESC")
    List<Map<String, Object>> findHistoryByUserId(@Param("userId") Integer userId);
}