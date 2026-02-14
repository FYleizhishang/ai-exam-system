package com.fuyi.exam.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.*;

@Service
public class DeepSeekService {

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Value("${deepseek.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DeepSeekService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(60000); // 60s 连接超时
        factory.setReadTimeout(120000);    // 120s 读取超时
        this.restTemplate = new RestTemplate(factory);
    }

    // 兼容旧调用
    public String callAi(String systemPrompt, String userPrompt, boolean jsonMode) throws Exception {
        return callDeepSeek(userPrompt);
    }

    public String callDeepSeek(String prompt) throws Exception {
        return callDeepSeekInternal(prompt, "deepseek-chat");
    }

    private String callDeepSeekInternal(String prompt, String model) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> body = new HashMap<>();
        // 🔥 关键修改：确保模型名称正确 🔥
        body.put("model", model);
        body.put("messages", Collections.singletonList(
                new HashMap<String, String>() {{
                    put("role", "user");
                    put("content", prompt);
                }}
        ));
        // 强制 JSON 模式
        body.put("response_format", new HashMap<String, String>() {{ put("type", "json_object"); }});
        body.put("stream", false);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        System.out.println("🚀 [AI] 发送请求 (模型: " + model + ")...");
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String content = root.path("choices").get(0).path("message").path("content").asText();
                return content.replaceAll("```json", "").replaceAll("```", "").trim();
            } else {
                throw new RuntimeException("API 响应异常: " + response.getStatusCode());
            }
        } catch (Exception e) {
            // 如果是 400 错误，可能是模型名字不对，尝试备用模型
            if (e.getMessage().contains("400") && model.equals("deepseek-chat")) {
                System.out.println("⚠️ deepseek-chat 模型不可用，尝试 deepseek-reasoner...");
                return callDeepSeekInternal(prompt, "deepseek-reasoner");
            }
            throw e;
        }
    }
}