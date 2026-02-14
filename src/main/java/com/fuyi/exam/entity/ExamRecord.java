package com.fuyi.exam.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("sys_exam_record") // 🔥 核心修复：明确告诉代码，数据库里的表名叫 sys_exam_record
public class ExamRecord implements Serializable {
    private Integer id;

    private Integer userId; // 考生ID
    private Integer paperId; // 试卷ID

    private Integer score; // 最终得分

    // 考生提交的答案(JSON字符串)，用于回显
    @TableField("answers_json") // 🔥 核心修复：对应数据库的 answers_json 列
    private String answers;

    // 考试时间
    @TableField("submit_time") // 🔥 核心修复：对应数据库的 submit_time 列
    private Date examTime;

    // 🔥🔥 [升级] 新增字段：AI 老师给出的诊断建议 🔥🔥
    private String aiDiagnosis;

    // 🔥🔥 [升级] 新增字段：防作弊状态 (0=正常, 1=强制交卷/作弊) 🔥🔥
    private Integer status;

    // 🔥🔥 [升级] 新增字段：考试期间切屏次数 🔥🔥
    private Integer switchCount;
}