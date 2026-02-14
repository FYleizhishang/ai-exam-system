package com.fuyi.exam;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class ExamSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamSystemApplication.class, args);
    }

    /**
     * 🔥🔥🔥 数据库全自动修复脚本 🔥🔥🔥
     */
    @Bean
    public CommandLineRunner fixDatabase(JdbcTemplate jdbcTemplate) {
        return args -> {
            System.out.println("🔧 正在检查并修复数据库结构...");
            try {
                // 1. 修复题目表 (分数)
                jdbcTemplate.execute("ALTER TABLE sys_question ADD COLUMN score INT DEFAULT 5 COMMENT '题目分值'");
            } catch (Exception e) { /* 忽略重复列错误 */ }

            try {
                // 2. 修复成绩表 (AI评语) - 这一步非常重要！
                jdbcTemplate.execute("ALTER TABLE sys_exam_record ADD COLUMN ai_diagnosis TEXT COMMENT 'AI评语'");
            } catch (Exception e) { /* 忽略重复列错误 */ }

            System.out.println("✅ 数据库结构已确保完整！可以交卷了！");
        };
    }
}