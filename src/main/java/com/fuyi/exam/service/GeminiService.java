package com.fuyi.exam.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.*;

@Slf4j
@Service
public class GeminiService {

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Value("${deepseek.api.url:https://api.siliconflow.cn/v1/chat/completions}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        log.info("🚀 AI 核心服务已启动 | 状态: 就绪");
    }

    /**
     * ★★★ 核心修复：chat 方法 (全能助教专用) ★★★
     */
    public String chat(String userMessage) {
        return callAi("你是一个幽默、博学、耐心的全能助教老师。请用生动的语言回答学生的问题。", userMessage, false);
    }

    /**
     * ★★★ 核心修复：callAi 三参数方法 (智能出题/阅卷专用) ★★★
     * @param systemRole 系统人设
     * @param userPrompt 用户指令
     * @param jsonMode   是否强制返回 JSON
     */
    public String callAi(String systemRole, String userPrompt, boolean jsonMode) {
        if (apiKey == null || apiKey.length() < 5) {
            log.error("API Key 未配置");
            return jsonMode ? "{}" : "请先在配置文件中填入正确的 API Key";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "deepseek-ai/DeepSeek-V3");
        body.put("temperature", 0.7);
        body.put("max_tokens", 4000);

        List<Map<String, String>> messages = new ArrayList<>();

        // 1. 系统人设
        Map<String, String> sysMsg = new HashMap<>();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemRole);
        messages.add(sysMsg);

        // 2. 用户指令 (JSON模式加强制约束)
        if (jsonMode) {
            userPrompt += "\n\n【重要约束】请直接返回纯 JSON 格式字符串，不要使用 Markdown 代码块（如 ```json），不要有任何前缀后缀。";
        }
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        messages.add(userMsg);

        body.put("messages", messages);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // 重试机制
        for (int i = 0; i < 3; i++) {
            try {
                ResponseEntity<Map> resp = restTemplate.postForEntity(apiUrl, entity, Map.class);
                if (resp.getBody() != null) {
                    List choices = (List) resp.getBody().get("choices");
                    Map choice = (Map) choices.get(0);
                    Map message = (Map) choice.get("message");
                    String content = (String) message.get("content");

                    if (jsonMode) return cleanJson(content);
                    return content;
                }
            } catch (Exception e) {
                log.warn("AI 响应异常，第 {} 次重试...", i + 1);
                try { Thread.sleep(1000); } catch (Exception ex) {}
            }
        }
        return jsonMode ? "{}" : "AI 服务繁忙，请稍后再试。";
    }

    // 辅助清洗 JSON
    private String cleanJson(String raw) {
        if (raw == null) return "{}";
        String result = raw.trim();
        if (result.startsWith("```")) {
            result = result.replaceAll("^```[a-zA-Z]*", "").replaceAll("```$", "");
        }
        return result.trim();
    }
}