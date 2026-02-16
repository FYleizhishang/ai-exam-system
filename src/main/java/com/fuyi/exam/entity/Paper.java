package com.fuyi.exam.entity;

import java.io.Serializable;
import java.util.Date; // 必须导入这个

public class Paper implements Serializable {
    private Integer id;
    private String title;
    private Integer totalScore;
    private Integer duration;
    private String questionIds;
    private Integer status;
    private Date createTime; // 🔥 必须加回来，否则前端拿不到时间！

    // 标准 Getter/Setter
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getTotalScore() { return totalScore; }
    public void setTotalScore(Integer totalScore) { this.totalScore = totalScore; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public String getQuestionIds() { return questionIds; }
    public void setQuestionIds(String questionIds) { this.questionIds = questionIds; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}