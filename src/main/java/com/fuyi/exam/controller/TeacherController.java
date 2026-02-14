package com.fuyi.exam.controller;

import com.fuyi.exam.common.Result;
import com.fuyi.exam.entity.Paper;
import com.fuyi.exam.entity.Question;
import com.fuyi.exam.mapper.PaperMapper;
import com.fuyi.exam.mapper.QuestionMapper;
import com.fuyi.exam.service.ExamService;
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

    private final ExecutorService executor = Executors.newCachedThreadPool();

    // === 1. AI 流式出题 (完全保留原有逻辑，确保听得懂指令) ===
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

    // === 2. 试卷发布 (正式发布，学生可见) ===
    @PostMapping("/paper/publish")
    public Result<String> publishPaper(@RequestBody PublishRequest request) {
        if (request.questions == null || request.questions.isEmpty()) return Result.error("题目不能为空");
        examService.publishPaper(request.title, request.questions);
        return Result.success("发布成功");
    }

    // 🔥🔥 [核心修复] 仅入库接口：只存题目，不建卷子，不给学生看 🔥🔥
    @PostMapping("/question/import")
    public Result<String> importQuestions(@RequestBody List<Question> questions) {
        if (questions == null || questions.isEmpty()) return Result.error("待入库题目为空");
        examService.importQuestions(questions);
        return Result.success("题目已成功存入题库");
    }

    // === 3. 基础管理接口 (全量保留) ===
    @GetMapping("/paper/list") public Result<List<Paper>> listPapers() { return Result.success(paperMapper.findAll()); }

    // 🔥🔥 [新增] 查看试卷详情接口 🔥🔥
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

    // === 4. 阅卷相关接口 ===
    @GetMapping("/record/pending") public List<Map<String, Object>> getPendingList() { return examService.getPendingList(); }
    @GetMapping("/record/detail") public Map<String, Object> getRecordDetail(@RequestParam Integer id) { return examService.getRecordDetail(id); }
    @PostMapping("/record/grade") public Result<String> gradeRecord(@RequestBody Map<String, Object> body) {
        Integer id = Integer.parseInt(body.get("id").toString());
        List<Map<String, Object>> details = (List<Map<String, Object>>) body.get("details");
        int total = 0;
        for(Map<String, Object> d : details) { total += Integer.parseInt(d.get("getScore").toString()); }
        examService.updateRecordScore(id, total, "老师人工复核完成");
        return Result.success("批改完成");
    }
}