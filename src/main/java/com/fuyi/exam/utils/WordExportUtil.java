package com.fuyi.exam.utils;

import com.fuyi.exam.entity.Question;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

@Component
public class WordExportUtil {

    public void exportWord(HttpServletResponse response, String title, List<Question> questions) throws IOException {
        // 1. 创建文档
        XWPFDocument document = new XWPFDocument();

        // 2. 试卷主标题
        addTitle(document, title);

        // 3. 试卷密封线信息 (商用级细节)
        addSubTitle(document, "班级：__________  姓名：__________  学号：__________");
        addSeparator(document);

        // 4. 遍历题目生成试卷内容
        int index = 1;
        int currentType = -1;
        String[] typeNames = {"", "一、单选题", "二、多选题", "三、判断题", "四、填空题", "五、简答题"};

        for (Question q : questions) {
            // 智能分题型添加大标题
            if (q.getType() != null && q.getType() != currentType) {
                currentType = q.getType();
                if (currentType >= 1 && currentType <= 5) {
                    addSectionTitle(document, typeNames[currentType]);
                } else {
                    addSectionTitle(document, "六、其他题目");
                }
            }

            // 添加题目
            addQuestion(document, index++, q);
        }

        // 5. 换页，生成参考答案
        document.createParagraph().setPageBreak(true);
        addTitle(document, "参考答案与解析");

        index = 1;
        for (Question q : questions) {
            addAnswer(document, index++, q);
        }

        // 6. 输出流下载
        String fileName = URLEncoder.encode(title + ".docx", "UTF-8").replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename*=utf-8''" + fileName);

        try (ServletOutputStream out = response.getOutputStream()) {
            document.write(out);
        }
        document.close();
    }

    // --- 辅助方法 (排版细节) ---

    private void addTitle(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(18);
        r.setFontFamily("宋体");
        r.addBreak();
    }

    private void addSubTitle(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setFontSize(12);
        r.setFontFamily("宋体");
        r.addBreak();
    }

    private void addSeparator(XWPFDocument doc) {
        XWPFParagraph p = doc.createParagraph();
        p.setBorderBottom(Borders.SINGLE);
    }

    private void addSectionTitle(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(14);
        r.setFontFamily("黑体");
        r.addBreak();
    }

    private void addQuestion(XWPFDocument doc, int index, Question q) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        // 题目文本
        String scoreStr = q.getScore() != null ? " (" + q.getScore() + "分)" : "";
        r.setText(index + ". " + q.getTitle() + scoreStr);
        r.setFontSize(12);
        r.addBreak();

        // 简单处理选项 (不管是JSON还是字符串，都直接显示)
        if (q.getOptions() != null && !q.getOptions().toString().isEmpty()) {
            r.setText("   " + q.getOptions().toString());
            r.addBreak();
        }

        // 简答题留空行给学生写
        if (q.getType() == 5) {
            for (int i = 0; i < 4; i++) r.addBreak();
        }
        r.addBreak();
    }

    private void addAnswer(XWPFDocument doc, int index, Question q) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        r.setText(index + ". 【答案】 " + q.getAnswer());
        r.addBreak();
        r.setText("   【解析】 " + (q.getAnalysis() != null ? q.getAnalysis() : "无"));
        r.setFontSize(10);
        r.addBreak();
    }
}