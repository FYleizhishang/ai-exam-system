package com.fuyi.exam.entity;

import lombok.Data;
import java.util.Date;

@Data // 这个注解会自动生成 getPassword(), setPassword() 等方法
public class User {
    private Integer id;
    private String username;
    private String password;
    private String name;
    private String role;   // student, teacher, admin
    private String avatar;
    private Integer points;
    private String tags;   // 兴趣标签 JSON
    private Integer isDeleted;
    private Date createTime; // 对应数据库 create_time
}