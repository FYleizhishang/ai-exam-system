package com.fuyi.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fuyi.exam.common.Result;
import com.fuyi.exam.entity.ExamRecord;
import com.fuyi.exam.entity.Paper;
import com.fuyi.exam.entity.Question;
import com.fuyi.exam.mapper.ExamRecordMapper;
import com.fuyi.exam.mapper.PaperMapper;
import com.fuyi.exam.mapper.QuestionMapper;
import com.fuyi.exam.service.ExamService;
import com.fuyi.exam.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/api/student")
@CrossOrigin
public class StudentController {

    @Autowired private PaperMapper paperMapper;
    @Autowired private QuestionMapper questionMapper;
    @Autowired private ExamRecordMapper examRecordMapper;
    @Autowired private ExamService examService;
    @Autowired private HttpServletRequest request;

    private Integer getCurrentUserId() {
        String token = request.getHeader("token");
        if (token == null || token.isEmpty()) {
            token = request.getHeader("Authorization");
        }
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        try {
            String userIdStr = JwtUtil.getClaimsByToken(token).getSubject();
            return Integer.parseInt(userIdStr);
        } catch (Exception e) {
            return null; // 如果没登录，返回null
        }
    }

    @GetMapping("/paper/list")
    public Result<List<Paper>> listPapers() {
        List<Paper> list = paperMapper.findAll();
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

        if (questions != null) { for (Question q : questions) { q.setAnswer(null); q.setAnalysis(null); } }

        Map<String, Object> map = new HashMap<>();
        map.put("paper", paper);
        map.put("questions", questions);
        return Result.success(map);
    }

    @PostMapping("/exam/submit")
    public Result<Integer> submitExam(@RequestBody Map<String, Object> body) {
        Integer userId = getCurrentUserId();
        if (userId == null) return Result.error("请先登录");

        Integer paperId = Integer.parseInt(body.get("paperId").toString());
        Map<String, String> answers = (Map<String, String>) body.get("answers");

        Integer status = body.get("status") != null ? Integer.parseInt(body.get("status").toString()) : 0;
        Integer switchCount = body.get("switchCount") != null ? Integer.parseInt(body.get("switchCount").toString()) : 0;

        Integer recordId = examService.gradeExam(userId, paperId, answers, status, switchCount);
        return Result.success(recordId);
    }

    @GetMapping("/exam/result/{recordId}")
    public Result<ExamRecord> getExamResult(@PathVariable Integer recordId) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        return record != null ? Result.success(record) : Result.error("未找到成绩单");
    }

    // 🔥🔥🔥 核心修复点：orderByDesc 必须用数据库字段名 submit_time 🔥🔥🔥
    @GetMapping("/history/list")
    public Result<List<Map<String, Object>>> getHistoryList() {
        Integer userId = getCurrentUserId();
        if (userId == null) return Result.error("请先登录");

        QueryWrapper<ExamRecord> query = new QueryWrapper<>();
        // ❌ 之前写错的代码: query.eq("user_id", userId).orderByDesc("exam_time");
        // ✅ 修正后的代码: 使用数据库真实列名 submit_time
        query.eq("user_id", userId).orderByDesc("submit_time");

        List<ExamRecord> records = examRecordMapper.selectList(query);

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (ExamRecord r : records) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("score", r.getScore());
            map.put("examTime", r.getExamTime()); // 这里是实体类字段，对应 DB 的 submit_time
            map.put("status", r.getStatus());

            Paper p = paperMapper.selectById(r.getPaperId());
            map.put("paperTitle", p != null ? p.getTitle() : "未知试卷");

            resultList.add(map);
        }

        return Result.success(resultList);
    }

    @GetMapping("/exam/review/{recordId}")
    public Result<Map<String, Object>> getExamReview(@PathVariable Integer recordId) {
        Map<String, Object> detail = examService.getRecordDetail(recordId);
        return Result.success(detail);
    }
}