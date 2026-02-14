package com.fuyi.exam.controller;

import com.fuyi.exam.entity.Question;
import com.fuyi.exam.utils.WordExportUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/export")
@CrossOrigin
public class ExportController {

    @Autowired
    private WordExportUtil wordExportUtil;

    /**
     * 导出 Word 接口
     * 前端把生成的题目列表传过来，后端生成文件返回
     */
    @PostMapping("/word")
    public void exportWord(HttpServletResponse response, @RequestBody ExportRequest request) throws IOException {
        wordExportUtil.exportWord(response, request.title, request.questions);
    }

    public static class ExportRequest {
        public String title;
        public List<Question> questions;
    }
}