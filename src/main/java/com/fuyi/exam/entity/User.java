package com.fuyi.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

// 🔥🔥🔥 就是这一行！加上它，后端就认识 sys_user 表了 🔥🔥🔥
@TableName("sys_user")
@Data
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String username;
    private String password;
    private String name;
    private String role;
    private String avatar;
    private Integer points;
    private String tags;
    private Integer isDeleted;
    private Date createTime;
}