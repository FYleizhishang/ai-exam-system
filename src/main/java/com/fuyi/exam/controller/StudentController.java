package com.fuyi.exam.controller;

import com.fuyi.exam.common.Result;
import com.fuyi.exam.entity.ExamRecord; // 🔥 新增引用
import com.fuyi.exam.entity.Paper;
import com.fuyi.exam.entity.Question;
import com.fuyi.exam.mapper.ExamRecordMapper; // 🔥 新增引用
import com.fuyi.exam.mapper.PaperMapper;
import com.fuyi.exam.mapper.QuestionMapper;
import com.fuyi.exam.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/student")
@CrossOrigin
public class StudentController {

    @Autowired private PaperMapper paperMapper;
    @Autowired private QuestionMapper questionMapper;
    @Autowired private ExamRecordMapper examRecordMapper; // 🔥 新增：注入成绩Mapper
    @Autowired private ExamService examService;

    @GetMapping("/paper/list")
    public Result<List<Paper>> listPapers() {
        // 1. 获取所有试卷
        List<Paper> list = paperMapper.findAll();

        // 🛡️【强力修复】防止前端卡死：检查并修复空时间的数据
        if (list != null) {
            Iterator<Paper> iterator = list.iterator();
            while (iterator.hasNext()) {
                Paper p = iterator.next();
                if (p.getCreateTime() == null) {
                    p.setCreateTime(new Date());
                }
            }
            list.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));
        }

        return Result.success(list);
    }

    @GetMapping("/paper/{paperId}")
    public Result<Map<String, Object>> getExamPaper(@PathVariable Integer paperId) {
        Paper paper = paperMapper.selectById(paperId);
        if (paper == null) return Result.error("试卷不存在");

        String idsStr = paper.getQuestionIds() != null ? paper.getQuestionIds().replace("[", "").replace("]", "").replace(" ", "") : "";
        List<Question> questions = new ArrayList<>();
        if (!idsStr.isEmpty()) {
            List<Integer> ids = new ArrayList<>();
            for (String s : idsStr.split(",")) if (!s.isEmpty()) try { ids.add(Integer.parseInt(s.trim())); } catch (Exception e) {}
            if (!ids.isEmpty()) questions = questionMapper.selectBatchIds(ids);
        }

        // 屏蔽答案和解析（防作弊）
        if (questions != null) { for (Question q : questions) { q.setAnswer(null); q.setAnalysis(null); } }

        Map<String, Object> map = new HashMap<>();
        map.put("paper", paper);
        map.put("questions", questions);
        return Result.success(map);
    }

    @PostMapping("/exam/submit")
    public Result<Integer> submitExam(@RequestBody Map<String, Object> body) {
        Integer userId = 1;
        Integer paperId = Integer.parseInt(body.get("paperId").toString());
        Map<String, String> answers = (Map<String, String>) body.get("answers");

        // 🔥🔥 [升级] 接收防作弊数据 🔥🔥
        Integer status = body.get("status") != null ? Integer.parseInt(body.get("status").toString()) : 0;
        Integer switchCount = body.get("switchCount") != null ? Integer.parseInt(body.get("switchCount").toString()) : 0;

        // 调用升级后的 Service 方法
        Integer recordId = examService.gradeExam(userId, paperId, answers, status, switchCount);
        return Result.success(recordId);
    }

    // 🔥🔥 [新增] 获取考试结果接口 (只增不减) 🔥🔥
    @GetMapping("/exam/result/{recordId}")
    public Result<ExamRecord> getExamResult(@PathVariable Integer recordId) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null) {
            return Result.error("未找到成绩单");
        }
        return Result.success(record);
    }
}