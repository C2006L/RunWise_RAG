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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

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
        requireUser(userId);
        return Result.success(qaService.ask(userId, dto));
    }

    @GetMapping("/categories")
    @Operation(summary = "问题分类")
    public Result<List<Map<String, Object>>> categories() {
        return Result.success(qaService.getCategories());
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
        requireUser(userId);
        if (pageNum < 1 || pageSize < 1 || pageSize > 100)
            throw new com.runwise.common.BusinessException(com.runwise.common.ResultCode.PARAM_ERROR);
        return Result.success(qaService.getHistory(userId, pageNum, pageSize));
    }

    @PostMapping("/feedback")
    @Operation(summary = "回答反馈")
    public Result<Void> feedback(
            HttpServletRequest request,
            @RequestParam Long id,
            @RequestParam Integer feedback) {
        Long userId = (Long) request.getAttribute("userId");
        requireUser(userId);
        if (feedback < -1 || feedback > 1)
            throw new com.runwise.common.BusinessException(com.runwise.common.ResultCode.PARAM_ERROR);
        qaService.feedback(userId, id, feedback);
        return Result.success();
    }

    @GetMapping("/conversation/{id}")
    public Result<PageResult<QaRecord>> conversation(@RequestAttribute("userId") Long userId,@PathVariable String id,
            @RequestParam(defaultValue="1") int pageNum) {
        requireUser(userId);
        if(!id.matches("[A-Za-z0-9_-]{1,80}") || pageNum<1 || pageNum>10000)
            throw new com.runwise.common.BusinessException(com.runwise.common.ResultCode.PARAM_ERROR);
        Page<QaRecord> page=qaService.page(new Page<>(pageNum,100),new LambdaQueryWrapper<QaRecord>()
                .eq(QaRecord::getUserId,userId).eq(QaRecord::getConversationId,id)
                .orderByAsc(QaRecord::getCreateTime,QaRecord::getId));
        return Result.success(new PageResult<>(page.getRecords(),page.getTotal(),pageNum,100));
    }

    private void requireUser(Long id) {
        if (id == null) throw new com.runwise.common.BusinessException(com.runwise.common.ResultCode.UNAUTHORIZED);
    }
}
