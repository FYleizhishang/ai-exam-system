package com.fuyi.exam.service;

import com.fuyi.exam.entity.User;
import com.fuyi.exam.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired private UserMapper userMapper;

    /**
     * 登录逻辑
     */
    public User login(String username, String password) {
        User u = userMapper.findByUsername(username);
        // 使用 getPassword() 只有当 User 类加了 @Data 且字段为 private 时才有效
        if (u != null && u.getPassword() != null && u.getPassword().equals(password)) {
            return u;
        }
        return null;
    }

    /**
     * 注册逻辑
     */
    public boolean register(User user) {
        if (userMapper.findByUsername(user.getUsername()) != null) {
            return false; // 账号已存在
        }

        user.setRole("student"); // 默认注册为学生

        if (user.getName() == null || user.getName().isEmpty()) {
            user.setName(user.getUsername());
        }

        if (user.getAvatar() == null) {
            user.setAvatar("https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png");
        }

        userMapper.insert(user);
        return true;
    }
}