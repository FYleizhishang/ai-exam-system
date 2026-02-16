package com.fuyi.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("sys_course") // 确保表名正确
public class Course implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String title;

    private String description;

    // 封面图片链接
    private String coverImage;

    // 课程资料链接（PDF或视频地址）
    private String link;

    private Date createTime;
}