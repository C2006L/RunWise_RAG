package com.runwise.controller;

import com.runwise.common.PageResult;
import com.runwise.common.Result;
import com.runwise.dto.CheckinDTO;
import com.runwise.entity.Checkin;
import com.runwise.service.CheckinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import javax.validation.constraints.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;

/**
 * 打卡控制器
 */
@RestController
@Validated
@RequestMapping("/api/checkin")
@Tag(name = "打卡接口")
public class CheckinController {

    private final CheckinService checkinService;

    public CheckinController(CheckinService checkinService) {
        this.checkinService = checkinService;
    }

    @GetMapping("/today")
    @Operation(summary = "今日打卡状态")
    public Result<Checkin> getToday(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(checkinService.getTodayCheckin(userId));
    }

    @GetMapping("/stats")
    @Operation(summary = "统计数据")
    public Result<Map<String, Object>> getStats(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(checkinService.getStats(userId));
    }

    @PostMapping
    @Operation(summary = "提交打卡")
    public Result<Long> create(HttpServletRequest request, @Valid @RequestBody CheckinDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(checkinService.createCheckin(userId, dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改打卡记录")
    public Result<Checkin> update(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody CheckinDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(checkinService.updateCheckin(userId, id, dto));
    }

    @GetMapping("/list")
    @Operation(summary = "打卡列表(分页)")
    public Result<PageResult<Checkin>> getList(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(500) Integer pageSize,
            @RequestParam(required=false) String startDate,
            @RequestParam(required=false) String endDate) {
        Long userId = (Long) request.getAttribute("userId");
        LambdaQueryWrapper<Checkin> query=new LambdaQueryWrapper<Checkin>().eq(Checkin::getUserId,userId);
        if(startDate!=null) query.ge(Checkin::getCheckinDate,LocalDate.parse(startDate));
        if(endDate!=null) query.le(Checkin::getCheckinDate,LocalDate.parse(endDate));
        if(startDate!=null&&endDate!=null&&LocalDate.parse(startDate).isAfter(LocalDate.parse(endDate)))
            throw new com.runwise.common.BusinessException(com.runwise.common.ResultCode.PARAM_ERROR);
        Page<Checkin> page=checkinService.page(new Page<>(pageNum,pageSize),query.orderByDesc(Checkin::getCheckinDate));
        return Result.success(new PageResult<>(page.getRecords(),page.getTotal(),pageNum,pageSize));
    }

    @GetMapping("/calendar")
    @Operation(summary = "月历打卡数据")
    public Result<List<Map<String, Object>>> getCalendar(
            HttpServletRequest request,
            @RequestParam String month) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(checkinService.getCalendarData(userId, month));
    }

    @GetMapping("/{id}")
    @Operation(summary = "打卡详情")
    public Result<Checkin> getDetail(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        Checkin checkin = checkinService.getById(id);
        if (checkin == null || !checkin.getUserId().equals(userId)) {
            return Result.success(null);
        }
        return Result.success(checkin);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除打卡")
    public Result<Void> delete(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        checkinService.deleteCheckin(userId, id);
        return Result.success();
    }
}
