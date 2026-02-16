package com.fuyi.exam.controller;

import com.fuyi.exam.common.Result;
import com.fuyi.exam.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin
public class ChatController {

    @Autowired private GeminiService geminiService;

    @PostMapping("/send")
    public Result<String> chat(@RequestBody Map<String, String> body) {
        String msg = body.get("message");
        if (msg == null || msg.trim().isEmpty()) return Result.error("内容不能为空");

        try {
            // 🔥 核心精装修：赋予 AI 专属的“系统助教”人设
            String systemPrompt = "你现在是【在线智能教务系统】的专属 AI 助教。你的任务是解答学生的疑问。\n" +
                    "要求：\n" +
                    "1. 语气温和、耐心、带有鼓励性。\n" +
                    "2. 如果学生问编程问题，必须使用 Markdown 代码块提供清晰的代码和注释。\n" +
                    "3. 绝对不要替学生直接完成考试，而是引导他们思考。";

            // 使用带有系统提示词的方法调用 AI
            String response = geminiService.callAi(systemPrompt, msg, false);
            return Result.success(response);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("AI 助教正在思考人生，请稍后再试...");
        }
    }
}