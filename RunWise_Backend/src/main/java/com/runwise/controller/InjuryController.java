package com.runwise.controller;

import com.runwise.common.Result;
import com.runwise.entity.InjuryArticle;
import com.runwise.service.InjuryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 伤病预防控制器（公开只读知识库）
 */
@RestController
@RequestMapping("/api/injury")
@Tag(name = "伤病预防接口")
public class InjuryController {

    private final InjuryService injuryService;

    public InjuryController(InjuryService injuryService) {
        this.injuryService = injuryService;
    }

    @GetMapping("/articles")
    @Operation(summary = "文章列表(可按分类筛选)")
    public Result<List<InjuryArticle>> listArticles(
            @RequestParam(required = false) String category) {
        return Result.success(injuryService.listArticles(category));
    }

    @GetMapping("/articles/{id}")
    @Operation(summary = "文章详情")
    public Result<InjuryArticle> getArticle(@PathVariable Long id) {
        return Result.success(injuryService.getArticle(id));
    }
}