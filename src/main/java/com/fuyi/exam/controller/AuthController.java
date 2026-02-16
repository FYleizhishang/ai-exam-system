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
public class AuthController {

    @Autowired private UserService userService;

    // 验证码暂存
    private static final Map<String, String> CAPTCHA_STORE = new HashMap<>();

    @GetMapping("/captcha")
    public void captcha(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int w=100, h=40; BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics g = img.getGraphics(); g.setColor(new Color(245,245,245)); g.fillRect(0,0,w,h);
        String s="ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; Random r=new Random(); StringBuilder sb=new StringBuilder();
        g.setFont(new Font("Arial",Font.BOLD,24));
        for(int i=0;i<4;i++){ sb.append(s.charAt(r.nextInt(s.length()))); g.setColor(Color.BLACK); g.drawString(sb.charAt(i)+"",20*i+10,28); }

        String key = req.getSession().getId();
        CAPTCHA_STORE.put(key, sb.toString());

        ImageIO.write(img, "JPEG", resp.getOutputStream());
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestParam String username,
                                     @RequestParam String password,
                                     @RequestParam String code,
                                     HttpServletRequest req) {
        Map<String, Object> res = new HashMap<>();
        String key = req.getSession().getId();
        String rightCode = CAPTCHA_STORE.get(key);

        if(rightCode == null || !rightCode.equalsIgnoreCase(code)) {
            res.put("code", 400); res.put("msg", "验证码错误");
            return res;
        }
        CAPTCHA_STORE.remove(key);

        User user = userService.login(username, password);
        if (user != null) {
            String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
            res.put("code", 200);
            res.put("msg", "登录成功");
            res.put("token", token); // 返回 token
            res.put("role", user.getRole());
        } else {
            res.put("code", 401); res.put("msg", "账号或密码错误");
        }
        return res;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody User user) {
        Map<String, Object> res = new HashMap<>();
        try {
            boolean success = userService.register(user);
            res.put("code", success ? 200 : 400);
            res.put("msg", success ? "注册成功" : "账号已存在");
        } catch (Exception e) {
            res.put("code", 500); res.put("msg", "系统错误");
        }
        return res;
    }

    @GetMapping("/api/user/current")
    public Map<String, Object> currentUser(HttpServletRequest request) {
        Map<String, Object> res = new HashMap<>();

        // 兼容两种 header
        String token = request.getHeader("token");
        if (token == null) token = request.getHeader("Authorization");

        // 去掉 Bearer
        if (token != null && token.startsWith("Bearer ")) token = token.substring(7);

        if (token != null) {
            String username = JwtUtil.getUsernameFromToken(token);
            if (username != null) {
                res.put("username", username);
                res.put("status", "ok");
                return res;
            }
        }
        res.put("status", "error");
        return res;
    }
}