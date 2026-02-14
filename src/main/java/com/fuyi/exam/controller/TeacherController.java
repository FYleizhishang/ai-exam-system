package com.fuyi.exam.controller;

import com.fuyi.exam.common.Result;
import com.fuyi.exam.entity.ExamRecord;
import com.fuyi.exam.entity.Paper;
import com.fuyi.exam.entity.Question;
import com.fuyi.exam.mapper.ExamRecordMapper;
import com.fuyi.exam.mapper.PaperMapper;
import com.fuyi.exam.mapper.QuestionMapper;
import com.fuyi.exam.service.ExamService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fuyi.exam.entity.User; // 补充导入
import com.fuyi.exam.mapper.UserMapper; // 补充导入
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/teacher")
@CrossOrigin
public class TeacherController {

    @Autowired private ExamService examService;
    @Autowired private PaperMapper paperMapper;
    @Autowired private QuestionMapper questionMapper;
    @Autowired private ExamRecordMapper examRecordMapper;
    @Autowired private UserMapper userMapper;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    // === 1. AI 流式出题 ===
    @GetMapping(value = "/paper/stream-gen", produces = "text/event-stream")
    public SseEmitter streamGeneratePaper(@RequestParam String topic, @RequestParam(defaultValue = "中等") String difficulty) {
        SseEmitter emitter = new SseEmitter(600000L);
        executor.execute(() -> {
            try {
                ExamService.LogCallback callback = (msg) -> { try { emitter.send(SseEmitter.event().name("log").data(msg)); } catch (Exception e) { } };
                Map<String, Object> result = examService.generatePaperStream(topic, difficulty, callback);
                emitter.send(SseEmitter.event().name("complete").data(result)); emitter.complete();
            } catch (Exception e) { try { emitter.send(SseEmitter.event().name("error").data("错误: " + e.getMessage())); } catch (Exception ex) {} emitter.completeWithError(e); }
        });
        return emitter;
    }

    // === 2. 试卷发布 ===
    @PostMapping("/paper/publish")
    public Result<String> publishPaper(@RequestBody PublishRequest request) {
        if (request.questions == null || request.questions.isEmpty()) return Result.error("题目不能为空");
        examService.publishPaper(request.title, request.questions);
        return Result.success("发布成功");
    }

    @PostMapping("/question/import")
    public Result<String> importQuestions(@RequestBody List<Question> questions) {
        if (questions == null || questions.isEmpty()) return Result.error("待入库题目为空");
        examService.importQuestions(questions);
        return Result.success("题目已成功存入题库");
    }

    // === 3. 基础管理接口 ===
    @GetMapping("/paper/list") public Result<List<Paper>> listPapers() { return Result.success(paperMapper.findAll()); }

    @GetMapping("/paper/{id}")
    public Result<Map<String, Object>> getPaperDetail(@PathVariable Integer id) {
        Map<String, Object> detail = examService.getPaperDetail(id);
        return detail != null ? Result.success(detail) : Result.error("试卷不存在");
    }

    @PostMapping("/paper/delete") public Result<String> deletePaper(@RequestParam Integer id) { paperMapper.deleteById(id); return Result.success("删除成功"); }
    @PostMapping("/subject/delete") public Result<String> deleteSubject(@RequestParam String subject) { examService.deleteSubject(subject); return Result.success("删除成功"); }
    @GetMapping("/subject/list") public Result<List<String>> getSubjects() { return Result.success(examService.getAllSubjects()); }
    @GetMapping("/question/list") public Result<List<Question>> getQuestions(@RequestParam(defaultValue = "全部") String subject) { return Result.success(examService.getQuestionsBySubject(subject)); }
    @PostMapping("/question/update") public Result<String> updateQuestion(@RequestBody Question question) { examService.updateQuestion(question); return Result.success("修改成功"); }
    @PostMapping("/question/delete") public Result<String> deleteQuestion(@RequestParam Integer id) { examService.deleteQuestion(id); return Result.success("删除成功"); }

    public static class PublishRequest { public String title; public List<Question> questions; }

    // === 4. 阅卷与分析接口 ===

    @GetMapping("/record/pending")
    public Result<List<Map<String, Object>>> getPendingList() {
        return Result.success(examService.getPendingList());
    }

    @GetMapping("/record/detail")
    public Result<Map<String, Object>> getRecordDetail(@RequestParam Integer id) {
        return Result.success(examService.getRecordDetail(id));
    }

    // 🔥🔥 修复：老师提交评分，强制更新状态为 2 🔥🔥
    @PostMapping("/record/grade")
    public Result<String> gradeRecord(@RequestBody Map<String, Object> body) {
        Integer id = Integer.parseInt(body.get("id").toString());
        List<Map<String, Object>> details = (List<Map<String, Object>>) body.get("details");

        int total = 0;
        for(Map<String, Object> d : details) {
            total += Integer.parseInt(d.get("getScore").toString());
        }

        // 1. 调用 Service 更新分数
        examService.updateRecordScore(id, total, "老师人工复核完成，成绩已发布。");

        // 2. 🔥 核心补丁：强制把状态改为 2 (已出分) 🔥
        // 防止 Service 层没改状态导致学生看不到分
        ExamRecord record = new ExamRecord();
        record.setId(id);
        record.setStatus(2);
        examRecordMapper.updateById(record);

        return Result.success("批改完成，成绩已对学生可见");
    }

    @GetMapping("/analysis/{paperId}")
    public Result<Map<String, Object>> analyzePaper(@PathVariable Integer paperId) {
        Paper paper = paperMapper.selectById(paperId);
        if(paper == null) return Result.error("试卷不存在");

        List<ExamRecord> records = examRecordMapper.findByPaperId(paperId);

        int totalStudents = records.size();
        int passCount = 0;
        int[] rangeCounts = new int[5];

        double fullMark = paper.getTotalScore() != null ? paper.getTotalScore() : 100.0;

        for (ExamRecord r : records) {
            if (r.getScore() == null) continue;
            double ratio = (double) r.getScore() / fullMark;

            if (ratio < 0.6) {
                rangeCounts[0]++;
            } else {
                passCount++;
                if (ratio < 0.7) rangeCounts[1]++;
                else if (ratio < 0.8) rangeCounts[2]++;
                else if (ratio < 0.9) rangeCounts[3]++;
                else rangeCounts[4]++;
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("paperTitle", paper.getTitle());
        data.put("totalScore", fullMark);
        data.put("totalExaminees", totalStudents);
        data.put("passCount", passCount);
        data.put("failCount", totalStudents - passCount);
        data.put("passRate", totalStudents == 0 ? 0 : (passCount * 100 / totalStudents));
        data.put("ranges", rangeCounts);

        return Result.success(data);
    }

    // 学生管理接口
    @GetMapping("/student/list")
    public Result<List<User>> listStudents() {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role", "student");
        return Result.success(userMapper.selectList(queryWrapper));
    }

    @PostMapping("/student/reset-pwd")
    public Result<String> resetStudentPwd(@RequestParam Integer id) {
        User user = new User();
        user.setId(id);
        user.setPassword("123456");
        userMapper.updateById(user);
        return Result.success("密码已重置为 123456");
    }

    @PostMapping("/student/delete")
    public Result<String> deleteStudent(@RequestParam Integer id) {
        userMapper.deleteById(id);
        return Result.success("账号已删除");
    }
}