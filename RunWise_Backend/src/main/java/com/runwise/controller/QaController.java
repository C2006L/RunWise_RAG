package com.runwise.controller;

import com.runwise.common.PageResult;
import com.runwise.common.Result;
import com.runwise.dto.AskDTO;
import com.runwise.entity.QaRecord;
import com.runwise.service.QaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 问答控制器
 */
@RestController
@RequestMapping("/api/qa")
@Tag(name = "问答接口")
public class QaController {

    private final QaService qaService;

    public QaController(QaService qaService) {
        this.qaService = qaService;
    }

    @PostMapping("/ask")
    @Operation(summary = "提问")
    public Result<Map<String, Object>> ask(HttpServletRequest request, @Valid @RequestBody AskDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(qaService.ask(userId, dto));
    }

    @GetMapping("/hot")
    @Operation(summary = "热门问题")
    public Result<List<Map<String, Object>>> hot() {
        return Result.success(qaService.hotQuestions());
    }

    @GetMapping("/history")
    @Operation(summary = "问答历史(分页)")
    public Result<PageResult<QaRecord>> getHistory(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(qaService.getHistory(userId, pageNum, pageSize));
    }

    @PostMapping("/feedback")
    @Operation(summary = "回答反馈")
    public Result<Void> feedback(
            HttpServletRequest request,
            @RequestParam Long id,
            @RequestParam Integer feedback) {
        Long userId = (Long) request.getAttribute("userId");
        qaService.feedback(userId, id, feedback);
        return Result.success();
    }
}
