package com.fuyi.exam.service;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fuyi.exam.entity.ExamRecord;
import com.fuyi.exam.entity.Paper;
import com.fuyi.exam.entity.Question;
import com.fuyi.exam.mapper.ExamRecordMapper;
import com.fuyi.exam.mapper.PaperMapper;
import com.fuyi.exam.mapper.QuestionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class ExamService {

    @Autowired private GeminiService geminiService;
    @Autowired private QuestionMapper questionMapper;
    @Autowired private PaperMapper paperMapper;
    @Autowired private ExamRecordMapper examRecordMapper;

    private final ObjectMapper objectMapper;
    // 异步线程池，用于阅卷不卡顿
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(5);

    public interface LogCallback {
        void log(String message);
    }

    public ExamService() {
        this.objectMapper = JsonMapper.builder()
                .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                .build();
    }

    @PostConstruct
    public void init() {
        System.out.println("⚡⚡⚡ iStudy AI [指令听话版 - 终极全量] 已启动 ⚡⚡⚡");
        System.out.println("✅ 已加载：Smart Grading | Anti-Cheat Logic | Full Mode");
    }

    // ================= 1. 核心数据结构 (全量) =================

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuestionDTO {
        @JsonAlias({"Title", "question", "content", "q_title", "questionBody", "stem"})
        public String title;

        @JsonAlias({"Type", "type", "questionType", "kind"})
        public Object type; // 1=单选, 2=多选, 3=判断, 4=填空, 5=大题

        @JsonAlias({"Options", "choices", "items", "selection"})
        public Object options;

        @JsonAlias({"Answer", "key", "correct", "rightAnswer", "referenceAnswer", "standardAnswer", "solution"})
        public Object answer;

        @JsonAlias({"Analysis", "explain", "explanation", "desc", "description", "reasoning", "detail"})
        public String analysis;

        @JsonAlias({"Score", "point", "value", "points"})
        public Object score;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ExamPlan {
        public String reasoning;    // AI 的思考逻辑
        public int smallCount;      // 小题总数
        public int bigCount;        // 大题总数
        public String paperTitle;   // 试卷标题
        public int singleChoiceCount; // 单选
        public int multiChoiceCount;  // 多选
        public int judgeCount;        // 判断
        public int fillBlankCount;    // 填空

        // 🔥 核心控制位：是否开启严格模式（开启后禁止系统自动填充题目）
        public boolean strictMode;
    }

    // ================= 2. 智能出题流程 (听话逻辑修复) =================

    public Map<String, Object> generatePaperStream(String userRequest, String difficultyLevel, LogCallback callback) {
        List<Question> finalQuestions = new ArrayList<>();

        callback.log("🧠 [AI大脑] 正在深度理解您的指令: \"" + userRequest + "\"...");

        // Step 1: 意图分析 (使用升级后的强力 Prompt)
        ExamPlan plan = analyzeUserIntent(userRequest);

        // 🔥🔥 核心修复：只有在 非严格模式 下，且数量全为0时，才允许兜底 🔥🔥
        if (!plan.strictMode && plan.smallCount <= 0 && plan.bigCount <= 0) {
            plan.smallCount = 20;
            plan.bigCount = 4;
            plan.reasoning = "用户未指定数量，启用【标准期末考试】模板配置。";

            // 自动拆分
            plan.singleChoiceCount = 8;
            plan.multiChoiceCount = 4;
            plan.judgeCount = 4;
            plan.fillBlankCount = 4;
            callback.log("💡 [方案确认] 未检测到具体数量，系统自动启用默认模板 (20小题+4大题)。");
        } else {
            callback.log("⚖️ [精准执行] 严格遵照指令：小题 " + plan.smallCount + " 道，大题 " + plan.bigCount + " 道。");
        }

        // 智能分配小题类型
        if (plan.smallCount > 0 && !plan.strictMode) {
            int splitSum = plan.singleChoiceCount + plan.multiChoiceCount + plan.judgeCount + plan.fillBlankCount;
            if (splitSum != plan.smallCount) {
                plan.singleChoiceCount = (int)(plan.smallCount * 0.4);
                plan.multiChoiceCount = (int)(plan.smallCount * 0.2);
                plan.judgeCount = (int)(plan.smallCount * 0.2);
                plan.fillBlankCount = plan.smallCount - plan.singleChoiceCount - plan.multiChoiceCount - plan.judgeCount;
                callback.log("⚖️ [智能分配] 将 " + plan.smallCount + " 道小题自动拆分为不同题型。");
            }
        }

        callback.log("💡 [方案确认] " + (plan.reasoning != null ? plan.reasoning : "") + " | 标题：《" + plan.paperTitle + "》");

        // Step 2: 并行生成各类型题目
        if (plan.singleChoiceCount > 0) generateBatch(plan.paperTitle, plan.singleChoiceCount, 1, "单项选择题", userRequest, difficultyLevel, finalQuestions, callback);
        if (plan.multiChoiceCount > 0) generateBatch(plan.paperTitle, plan.multiChoiceCount, 2, "多项选择题", userRequest, difficultyLevel, finalQuestions, callback);
        if (plan.judgeCount > 0) generateBatch(plan.paperTitle, plan.judgeCount, 3, "判断题", userRequest, difficultyLevel, finalQuestions, callback);
        if (plan.fillBlankCount > 0) generateBatch(plan.paperTitle, plan.fillBlankCount, 4, "填空题", userRequest, difficultyLevel, finalQuestions, callback);

        // Step 3: 生成大题 (Type=5)
        if (plan.bigCount > 0) {
            callback.log("🚀 [生成中] 正在创作 " + plan.bigCount + " 道综合大题 (含详细解析)...");
            String bigPrompt = String.format(
                    "任务：为《%s》出卷。\n数量：严格生成 %d 道【综合问答/编程/计算题】。\n背景要求：\"%s\"\n难度：%s\n" +
                            "★要求★：\n" +
                            "- 'title': 题目描述要完整。\n" +
                            "- 'answer': 必须提供标准参考答案。\n" +
                            "- 'analysis': 必须包含不少于50字的详细解析。\n" +
                            "- 'type': 固定为 5。\n" +
                            "★格式★：纯 JSON 数组。",
                    plan.paperTitle, plan.bigCount, userRequest, difficultyLevel
            );
            List<Question> bigQs = callAiSafely(bigPrompt, callback);
            for(Question q : bigQs) q.setType(5);
            finalQuestions.addAll(bigQs);
        }

        // Step 4: 整理与赋分
        callback.log("✨ [后期处理] 正在进行排版和智能赋分...");

        // 排序
        finalQuestions.sort(Comparator.comparingInt((Question q) -> q.getType() == null ? 1 : q.getType()));

        // 重新编号
        for (int i = 0; i < finalQuestions.size(); i++) {
            Question q = finalQuestions.get(i);
            if (q.getTitle() == null) q.setTitle("题目缺失");
            String cleanTitle = q.getTitle().replaceAll("^(\\d+[\\.\\、\\s]*|一、|二、|三、|四、|五、)", "").trim();
            q.setTitle(cleanTitle);
        }

        // 智能赋分
        distributeScores(finalQuestions);

        // 此时不自动入库，等待老师点击“仅入库”或“发布”
        callback.log("🎉 生成完成！共 " + finalQuestions.size() + " 道题，请在上方确认入库。");

        Map<String, Object> result = new HashMap<>();
        result.put("questions", finalQuestions);
        result.put("aiTitle", plan.paperTitle);
        return result;
    }

    private void generateBatch(String title, int count, int type, String typeName, String userReq, String diff, List<Question> resultList, LogCallback callback) {
        int batchSize = 5; // 每次最多生成5道，防止AI超时
        int batches = (int) Math.ceil((double) count / batchSize);

        for (int i = 1; i <= batches; i++) {
            int currentSize = 5;
            if (i == batches && count % 5 != 0) currentSize = count % 5;

            if (currentSize <= 0) break;

            callback.log(String.format("🚀 [生成中] 正在生成 %s (%d/%d)...", typeName, (resultList.stream().filter(q->q.getType()==type).count() + currentSize), count));

            String prompt = String.format(
                    "任务：为《%s》出卷。\n题型：严格生成 %d 道【%s】。\n背景要求：\"%s\"\n难度：%s\n" +
                            "★要求★：\n" +
                            "- 'type': 固定为 %d。\n" +
                            "- 必须包含 'answer' (答案) 和 'analysis' (解析)。\n" +
                            "- 选项(Options)如果是选择题请给4个选项。\n" +
                            "★格式★：纯 JSON 数组。",
                    title, currentSize, typeName, userReq, diff, type
            );

            List<Question> qs = callAiSafely(prompt, callback);
            for(Question q : qs) q.setType(type);
            resultList.addAll(qs);
        }
    }

    private ExamPlan analyzeUserIntent(String text) {
        String prompt = String.format(
                "你是一个资深教务主任。请根据用户口语化的指令，规划出一份试卷结构。返回JSON。\n" +
                        "用户指令：\"%s\"\n\n" +
                        "★逻辑规则 (严格执行)★：\n" +
                        "1. **精准抓取**：如果用户说了具体数量（如'只要3道大题'），请将 bigCount=3，且 **strictMode=true**。\n" +
                        "2. **否定排除**：如果用户说“不要选择题”，请将 singleChoiceCount=0, multiChoiceCount=0，且 **strictMode=true**。\n" +
                        "3. **自动模式**：如果用户没提数量，只是说'来套计算机卷子'，则 strictMode=false。\n" +
                        "4. **模糊推演**：如果用户只说'10道题'没说类型，请自行分配，但 strictMode=true。\n" +
                        "\n" +
                        "返回格式：\n" +
                        "{\n" +
                        "  \"reasoning\": \"分析思路\",\n" +
                        "  \"paperTitle\": \"试卷标题\",\n" +
                        "  \"smallCount\": int (小题总数),\n" +
                        "  \"bigCount\": int (大题总数),\n" +
                        "  \"singleChoiceCount\": int,\n" +
                        "  \"multiChoiceCount\": int,\n" +
                        "  \"judgeCount\": int,\n" +
                        "  \"fillBlankCount\": int,\n" +
                        "  \"strictMode\": boolean\n" +
                        "}",
                text
        );

        try {
            String json = geminiService.callAi("你是一个教务规划系统。", prompt, true);
            System.out.println("🔍 [意图规划] " + json);
            if (json.trim().startsWith("[")) {
                List<ExamPlan> plans = objectMapper.readValue(json, new TypeReference<List<ExamPlan>>(){});
                return plans.isEmpty() ? new ExamPlan() : plans.get(0);
            } else {
                return objectMapper.readValue(json, ExamPlan.class);
            }
        } catch (Exception e) {
            System.err.println("❌ 意图解析失败: " + e.getMessage());
            return new ExamPlan(){{paperTitle="智能试卷"; smallCount=0; bigCount=0; strictMode=false;}};
        }
    }

    private List<Question> callAiSafely(String prompt, LogCallback callback) {
        for(int i=0; i<3; i++) {
            try {
                String json = geminiService.callAi("你是一个出题专家。请直接返回 JSON 数组。", prompt, true);
                if (json.trim().startsWith("{")) {
                    QuestionDTO single = objectMapper.readValue(json, QuestionDTO.class);
                    return convertToQuestions(Collections.singletonList(single));
                }
                List<QuestionDTO> dtos = objectMapper.readValue(json, new TypeReference<List<QuestionDTO>>(){});
                return convertToQuestions(dtos);
            } catch(Exception e) {
                try{Thread.sleep(800);}catch(Exception ex){}
            }
        }
        return new ArrayList<>();
    }

    private List<Question> convertToQuestions(List<QuestionDTO> dtos) throws Exception {
        List<Question> questions = new ArrayList<>();
        for (QuestionDTO dto : dtos) {
            Question q = new Question();
            q.setTitle(dto.title != null ? dto.title : "题目缺失");
            q.setType(transType(dto.type));
            q.setAnalysis(dto.analysis != null && !dto.analysis.isEmpty() ? dto.analysis : "略");
            q.setDifficulty(3);

            if (dto.answer != null) {
                if(dto.answer instanceof String) q.setAnswer((String)dto.answer);
                else q.setAnswer(objectMapper.writeValueAsString(dto.answer).replace("[","").replace("]","").replace("\"",""));
            } else q.setAnswer("略");

            if (dto.options != null) {
                if(dto.options instanceof String) q.setOptions((String)dto.options);
                else q.setOptions(objectMapper.writeValueAsString(dto.options));
            } else q.setOptions(null);

            q.setScore(2);
            questions.add(q);
        }
        return questions;
    }

    private Integer transType(Object typeObj) {
        if (typeObj == null) return 1;
        String s = typeObj.toString().trim().toLowerCase();
        try { return Integer.parseInt(s); } catch (Exception e) {}
        if (s.contains("单选") || s.contains("single")) return 1;
        if (s.contains("多选") || s.contains("multiple")) return 2;
        if (s.contains("判断") || s.contains("bool") || s.contains("true")) return 3;
        if (s.contains("填空") || s.contains("fill")) return 4;
        if (s.contains("简答") || s.contains("大题")) return 5;
        return 1;
    }

    private void distributeScores(List<Question> list) {
        if (list.isEmpty()) return;
        int totalScore = 100;

        List<Question> bigs = list.stream().filter(q -> q.getType() == 5).collect(Collectors.toList());
        List<Question> smalls = list.stream().filter(q -> q.getType() != 5).collect(Collectors.toList());

        if (bigs.isEmpty()) {
            int avg = totalScore / (smalls.isEmpty() ? 1 : smalls.size());
            for(Question q : smalls) q.setScore(avg > 0 ? avg : 2);
            return;
        }

        if (smalls.isEmpty()) {
            int avg = totalScore / bigs.size();
            for(Question q : bigs) q.setScore(avg > 0 ? avg : 10);
            return;
        }

        int bigTotal = 40;
        if (bigs.size() >= 4) bigTotal = 50;
        if (bigs.size() >= 6) bigTotal = 60;

        int smallTotal = totalScore - bigTotal;

        int bigItemScore = bigTotal / bigs.size();
        int smallItemScore = smallTotal / smalls.size();

        if (smallItemScore < 1) smallItemScore = 1;

        for (Question q : bigs) q.setScore(bigItemScore);
        for (Question q : smalls) q.setScore(smallItemScore);
    }

    private String smartSubject(String title) {
        if (title == null) return "综合科目";
        String t = title.toLowerCase();
        if (t.contains("java") || t.contains("spring") || t.contains("jvm")) return "Java程序设计";
        if (t.contains("数据结构") || t.contains("算法") || t.contains("链表")) return "数据结构";
        if (t.contains("数据库") || t.contains("mysql") || t.contains("sql")) return "数据库系统";
        if (t.contains("网络") || t.contains("http") || t.contains("tcp")) return "计算机网络";
        if (t.contains("操作系统") || t.contains("linux")) return "操作系统";
        if (t.contains("组成原理")) return "计算机组成原理";
        if (t.contains("高数") || t.contains("数学")) return "高等数学";
        return title;
    }

    // ================= 3. 业务功能 (入库 vs 发布) =================

    public void publishPaper(String title, List<Question> qs) {
        String cleanSubject = smartSubject(title);
        List<Integer> ids = new ArrayList<>();
        for (Question q : qs) {
            q.setKnowledgePoint(cleanSubject);
            if(q.getId() == null) questionMapper.insert(q); else questionMapper.update(q);
            ids.add(q.getId());
        }
        Paper p = new Paper();
        p.setTitle(title);
        p.setTotalScore(100);
        p.setQuestionIds(ids.toString().replace(" ", ""));
        p.setStatus(1);
        p.setDuration(90);
        p.setCreateTime(new Date());
        paperMapper.insert(p);
    }

    public void importQuestions(List<Question> qs) {
        String cleanSubject = "综合题库";
        if (!qs.isEmpty() && qs.get(0).getTitle() != null) {
            cleanSubject = smartSubject(qs.get(0).getTitle());
        }
        for (Question q : qs) {
            if(q.getKnowledgePoint() == null || q.getKnowledgePoint().isEmpty()) {
                q.setKnowledgePoint(cleanSubject);
            }
            if(q.getId() == null) questionMapper.insert(q); else questionMapper.update(q);
        }
    }

    public Map<String, Object> getPaperDetail(Integer paperId) {
        Paper paper = paperMapper.selectById(paperId);
        if (paper == null) return null;
        String idsStr = paper.getQuestionIds().replace("[", "").replace("]", "").replace(" ", "");
        List<Integer> ids = new ArrayList<>();
        for(String s : idsStr.split(",")) if(!s.isEmpty()) ids.add(Integer.parseInt(s));

        List<Question> questions = new ArrayList<>();
        if (!ids.isEmpty()) questions = questionMapper.selectBatchIds(ids);

        Map<String, Object> map = new HashMap<>();
        map.put("paper", paper);
        map.put("questions", questions);
        return map;
    }

    // ================= 4. 阅卷服务 =================

    public Integer gradeExam(Integer userId, Integer paperId, Map<String, String> userAnswers, Integer status, Integer switchCount) {
        Paper paper = paperMapper.selectById(paperId);
        if (paper == null) return 0;

        String idsStr = paper.getQuestionIds().replace("[", "").replace("]", "").replace(" ", "");
        List<Integer> ids = new ArrayList<>();
        for(String s : idsStr.split(",")) if(!s.isEmpty()) ids.add(Integer.parseInt(s));

        List<Question> questions = questionMapper.selectBatchIds(ids);
        int finalScore = 0;
        List<Map<String, String>> subjectiveQs = new ArrayList<>();
        List<Map<String, String>> allErrors = new ArrayList<>();

        for (Question q : questions) {
            String qId = String.valueOf(q.getId());
            Object ansObj = userAnswers.get(qId);
            String myAns = ansObj != null ? ansObj.toString().trim() : "";
            String rightAns = q.getAnswer() != null ? q.getAnswer().trim() : "";

            if (q.getType() != 5) {
                if (myAns.equalsIgnoreCase(rightAns)) {
                    finalScore += q.getScore();
                } else {
                    Map<String, String> err = new HashMap<>();
                    err.put("题目", q.getTitle());
                    err.put("我的答案", myAns);
                    err.put("正确答案", rightAns);
                    allErrors.add(err);
                }
            } else {
                Map<String, String> map = new HashMap<>();
                map.put("q", q.getTitle());
                map.put("ans", myAns);
                map.put("right", rightAns);
                map.put("score", String.valueOf(q.getScore()));
                subjectiveQs.add(map);
            }
        }

        ExamRecord record = new ExamRecord();
        record.setUserId(userId);
        record.setPaperId(paperId);
        record.setScore(finalScore);
        record.setExamTime(new Date());

        try { record.setAnswers(objectMapper.writeValueAsString(userAnswers)); } catch (Exception e) { record.setAnswers("{}"); }

        record.setStatus(status == 1 ? 1 : 0);
        record.setSwitchCount(switchCount);

        record.setAiDiagnosis("AI 正在初审试卷，请等待老师最终归档...");

        examRecordMapper.insert(record);

        final Integer recordId = record.getId();
        final int objectiveScore = finalScore;

        asyncExecutor.submit(() -> {
            try {
                int subjScore = 0;
                if (!subjectiveQs.isEmpty()) {
                    subjScore = aiGradeSubjective(subjectiveQs);
                }

                int totalScore = objectiveScore + subjScore;

                StringBuilder sb = new StringBuilder();
                sb.append(String.format("我是阅卷老师。学生客观题得分：%d，主观题建议得分：%d (暂定总分%d)。\n",
                        objectiveScore, subjScore, totalScore));

                if (subjectiveQs.isEmpty()) {
                    sb.append("注意：本试卷没有主观题。请不要点评主观题缺失的情况。\n");
                }

                sb.append("错题列表：").append(objectMapper.writeValueAsString(allErrors));
                sb.append("\n请给出一段简短、鼓励性的学习诊断建议（100字以内）。");

                String diagnosis = geminiService.chat(sb.toString());

                examRecordMapper.updateScoreAndDiagnosis(recordId, totalScore, diagnosis);

            } catch (Exception e) {
                e.printStackTrace();
                examRecordMapper.updateScoreAndDiagnosis(recordId, objectiveScore, "AI 服务繁忙，暂无法提供详细诊断。");
            }
        });

        return recordId;
    }

    public Map<String, Object> getRecordDetail(Integer recordId) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if(record == null) return null;

        Paper paper = paperMapper.selectById(record.getPaperId());
        String idsStr = paper.getQuestionIds().replace("[", "").replace("]", "").replace(" ", "");
        List<Integer> ids = new ArrayList<>();
        for(String s : idsStr.split(",")) if(!s.isEmpty()) ids.add(Integer.parseInt(s));

        List<Question> qs = questionMapper.selectBatchIds(ids);

        Map<String, String> ansMap = new HashMap<>();
        try { if (record.getAnswers() != null) ansMap = objectMapper.readValue(record.getAnswers(), new TypeReference<Map<String, String>>(){}); } catch (Exception e) {}

        List<Map<String, Object>> details = new ArrayList<>();
        for(Question q : qs) {
            Map<String, Object> d = new HashMap<>();
            d.put("title", q.getTitle());
            d.put("fullScore", q.getScore());
            d.put("stuAns", ansMap.getOrDefault(String.valueOf(q.getId()), ""));
            d.put("rightAns", q.getAnswer());

            String myAns = d.get("stuAns").toString().trim();
            String rightAns = q.getAnswer() != null ? q.getAnswer().trim() : "";
            boolean isCorrect = myAns.equalsIgnoreCase(rightAns);

            d.put("getScore", q.getType()!=5 && isCorrect ? q.getScore() : 0);

            if (q.getType() == 5) {
                d.put("aiHint", "⚠️ 此为简答大题，目前预设为 0 分，请查阅上方【AI 阅卷参考意见】并手动打分。");
            } else {
                d.put("aiHint", isCorrect ? "✅ 客观题：系统比对正确" : "❌ 客观题：系统比对错误");
            }

            details.add(d);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("record", record);
        try { res.put("details", objectMapper.writeValueAsString(details)); } catch (Exception e) { res.put("details", "[]"); }
        return res;
    }

    public void updateRecordScore(Integer id, Integer score, String diagnosis) {
        ExamRecord r = new ExamRecord();
        r.setId(id);
        r.setScore(score);
        r.setAiDiagnosis(diagnosis);
        r.setStatus(2);
        examRecordMapper.updateById(r);
    }

    private int aiGradeSubjective(List<Map<String, String>> list) {
        try {
            String prompt = "请批改以下主观题，返回总得分(纯数字)。\n数据：" + objectMapper.writeValueAsString(list);
            String res = geminiService.callAi("你是一个阅卷老师。", prompt, false);
            return Integer.parseInt(res.replaceAll("[^0-9]", ""));
        } catch(Exception e) { return 0; }
    }

    public List<Map<String, Object>> getPendingList() {
        return examRecordMapper.findAllRecords().stream().map(r -> {
            Map<String, Object> map = new HashMap<>(r);
            map.put("studentName", "学生用户" + r.get("user_id"));
            return map;
        }).collect(Collectors.toList());
    }

    public List<Question> getAllQuestions() { return questionMapper.findAll(); }
    public List<String> getAllSubjects() { return questionMapper.findAllSubjects(); }
    public List<Question> getQuestionsBySubject(String s) { return "全部".equals(s) ? questionMapper.findAll() : questionMapper.findBySubject(s); }
    public void updateQuestion(Question q) { questionMapper.update(q); }
    public void deleteQuestion(Integer id) { questionMapper.deleteById(id); }

    public void deleteSubject(String subject) {
        List<Question> qs = questionMapper.findBySubject(subject);
        for(Question q: qs) questionMapper.deleteById(q.getId());
    }

    // ================= 以下为新增：用于获取学生的历史成绩单 =================

    public List<Map<String, Object>> getStudentHistory(Integer userId) {
        // 调用 Mapper 查询该学生所有状态为 2 (已归档发布) 的成绩单
        return examRecordMapper.findHistoryByUserId(userId);
    }
}