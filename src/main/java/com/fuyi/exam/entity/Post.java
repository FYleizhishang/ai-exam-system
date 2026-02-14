package com.fuyi.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("sys_post")
public class Post implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;
    private String title;
    private String content;
    private Integer viewCount;
    private Integer likeCount;

    // 🔥 修复：指定时区为 GMT+8，解决时间不对的问题
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @TableField(exist = false)
    private String authorName;
    @TableField(exist = false)
    private String authorAvatar; // 头像
    @TableField(exist = false)
    private Boolean isLiked; // 当前用户是否点赞
}