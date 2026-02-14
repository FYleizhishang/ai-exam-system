package com.fuyi.exam.controller;

import com.fuyi.exam.common.Result;
import com.fuyi.exam.entity.Question;
import com.fuyi.exam.mapper.QuestionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/practice")
@CrossOrigin
public class PracticeController {

    @Autowired
    private QuestionMapper questionMapper;

    // 🔥 每次随机从真实题库里抽取 10 道题供学生刷题
    @GetMapping("/random")
    public Result<List<Question>> randomQuestions() {
        // 获取所有题目
        List<Question> allQuestions = questionMapper.findAll();

        if (allQuestions == null || allQuestions.isEmpty()) {
            return Result.error("题库为空，请先让老师生成一些题目");
        }

        // 打乱顺序，实现随机抽题
        Collections.shuffle(allQuestions);

        // 截取前 10 道（如果总题数不足 10 道，则取全部）
        int limit = Math.min(allQuestions.size(), 10);
        List<Question> randomQuestions = allQuestions.subList(0, limit);

        return Result.success(randomQuestions);
    }
}