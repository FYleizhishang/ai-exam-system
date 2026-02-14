package com.fuyi.exam.entity;
import java.util.Date;

public class Post {
    public Integer id;
    public Integer userId;
    public String title;
    public String content;
    public String images; // JSON
    public Integer viewCount;
    public Integer likeCount;
    public Integer commentCount;
    public Date createTime;

    // 额外字段，用于前端显示，不存数据库
    public String authorName;
    public String authorAvatar;
}