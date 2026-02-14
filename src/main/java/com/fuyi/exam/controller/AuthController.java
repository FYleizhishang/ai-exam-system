package com.fuyi.exam.controller;

import com.fuyi.exam.entity.User;
import com.fuyi.exam.service.UserService;
import com.fuyi.exam.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Slf4j
@RestController
public class AuthController { // ★★★ 注意：这里类名必须是 AuthController ★★★

    @Autowired private UserService userService;
    @Autowired private JwtUtil jwtUtil;

    // 验证码暂存 (商用建议放 Redis，这里简化放内存 Map)
    private static final Map<String, String> CAPTCHA_STORE = new HashMap<>();

    // 1. 获取验证码
    @GetMapping("/captcha")
    public void captcha(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int w=100, h=40; BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics g = img.getGraphics(); g.setColor(new Color(245,245,245)); g.fillRect(0,0,w,h);
        String s="ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; Random r=new Random(); StringBuilder sb=new StringBuilder();
        g.setFont(new Font("Arial",Font.BOLD,24));
        for(int i=0;i<4;i++){ sb.append(s.charAt(r.nextInt(s.length()))); g.setColor(Color.BLACK); g.drawString(sb.charAt(i)+"",20*i+10,28); }

        // 简单处理：用 SessionID 做 key 存验证码
        String key = req.getSession().getId();
        CAPTCHA_STORE.put(key, sb.toString());

        ImageIO.write(img, "JPEG", resp.getOutputStream());
    }

    // 2. 登录接口 (JWT 模式)
    @PostMapping("/login")
    public Map<String, Object> login(@RequestParam String username,
                                     @RequestParam String password,
                                     @RequestParam String code,
                                     HttpServletRequest req) {
        Map<String, Object> res = new HashMap<>();

        // 校验验证码
        String key = req.getSession().getId();
        String rightCode = CAPTCHA_STORE.get(key);

        if(rightCode == null || !rightCode.equalsIgnoreCase(code)) {
            res.put("code", 400); res.put("msg", "验证码错误或已过期");
            return res;
        }
        CAPTCHA_STORE.remove(key);

        User user = userService.login(username, password);
        if (user != null) {
            // 生成 Token
            String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

            res.put("code", 200);
            res.put("msg", "登录成功");
            res.put("token", token);
            res.put("role", user.getRole());
            res.put("avatar", user.getAvatar());
            res.put("username", user.getUsername());
        } else {
            res.put("code", 401); res.put("msg", "账号或密码错误");
        }
        return res;
    }

    // 3. 注册接口
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody User user) {
        Map<String, Object> res = new HashMap<>();
        try {
            boolean success = userService.register(user);
            if (success) {
                res.put("code", 200); res.put("msg", "注册成功");
            } else {
                res.put("code", 400); res.put("msg", "账号已存在");
            }
        } catch (Exception e) {
            log.error("注册失败", e);
            res.put("code", 500); res.put("msg", "服务器内部错误");
        }
        return res;
    }

    // 4. 获取当前用户信息
    @GetMapping("/api/user/current")
    public Map<String, Object> currentUser(@RequestHeader(value = "Authorization", required = false) String token) {
        Map<String, Object> res = new HashMap<>();
        if (token != null && !token.isEmpty()) {
            String username = jwtUtil.getUsernameFromToken(token);
            if (username != null) {
                res.put("username", username);
                res.put("status", "ok");
                return res;
            }
        }
        res.put("status", "error");
        return res;
    }

    @PostMapping("/logout")
    public String logout() { return "success"; }
}