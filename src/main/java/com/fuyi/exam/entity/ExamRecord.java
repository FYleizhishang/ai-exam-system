package com.fuyi.exam.entity;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
public class ExamRecord implements Serializable {
    private Integer id;

    private Integer userId;    // 考生ID
    private Integer paperId;   // 试卷ID

    private Integer score;     // 最终得分

    // 考生提交的答案(JSON字符串)，用于回显
    private String answers;

    // 考试时间
    private Date examTime;

    // 🔥🔥 [升级] 新增字段：AI 老师给出的诊断建议 🔥🔥
    private String aiDiagnosis;

    // 🔥🔥 [升级] 新增字段：防作弊状态 (0=正常, 1=强制交卷/作弊) 🔥🔥
    private Integer status;

    // 🔥🔥 [升级] 新增字段：考试期间切屏次数 🔥🔥
    private Integer switchCount;
}