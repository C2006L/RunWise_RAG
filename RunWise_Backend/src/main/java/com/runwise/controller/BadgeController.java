package com.runwise.controller;

import com.runwise.common.Result;
import com.runwise.service.BadgeService;
import com.runwise.vo.BadgeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 徽章控制器
 */
@RestController
@RequestMapping("/api/badges")
@Tag(name = "徽章接口")
public class BadgeController {

    private final BadgeService badgeService;

    public BadgeController(BadgeService badgeService) {
        this.badgeService = badgeService;
    }

    @GetMapping
    @Operation(summary = "徽章列表(含解锁状态/进度/解锁时间)")
    public Result<List<BadgeVO>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(badgeService.getUserBadges(userId));
    }
}
