package com.fuyi.exam.controller;

import com.fuyi.exam.service.GeminiService;
import com.fuyi.exam.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 全能助教接口：学生/老师可以直接问它任何问题
     * 比如："帮我解释一下刚才考试的第三题" 或者 "Java里的HashMap原理是什么"
     */
    @PostMapping("/ask")
    public Map<String, Object> askAi(@RequestBody Map<String, String> body,
                                     @RequestHeader(value = "Authorization", required = false) String token) {
        Map<String, Object> res = new HashMap<>();

        // 1. 简单的鉴权
        String username = null;
        if (token != null && !token.isEmpty()) {
            username = jwtUtil.getUsernameFromToken(token);
        }

        if (username == null) {
            res.put("code", 401);
            res.put("msg", "请先登录");
            return res;
        }

        // 2. 获取问题
        String question = body.get("question");
        if (question == null || question.trim().isEmpty()) {
            res.put("code", 400);
            res.put("msg", "问题不能为空");
            return res;
        }

        // 3. 调用 AI (使用 chat 方法)
        try {
            // 构造一个包含用户名字的 Prompt，让 AI 更亲切
            String prompt = String.format("我是学生 %s。请回答我的问题：%s", username, question);
            String answer = geminiService.chat(prompt);

            res.put("code", 200);
            res.put("answer", answer);
        } catch (Exception e) {
            e.printStackTrace();
            res.put("code", 500);
            res.put("msg", "AI 服务暂时繁忙，请稍后再试");
        }

        return res;
    }
}