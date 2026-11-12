package com.runwise.controller;

import com.runwise.common.*;
import com.runwise.entity.Checkin;
import com.runwise.service.CheckinService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import javax.validation.constraints.*;
import java.time.*;
import java.util.*;

@Validated
@RestController
@RequestMapping("/api/stats")
public class StatsController {
    private final CheckinService checkins;
    public StatsController(CheckinService checkins) {this.checkins=checkins;}
    private List<Checkin> records(Long user) {
        return checkins.list(new LambdaQueryWrapper<Checkin>().eq(Checkin::getUserId,user).le(Checkin::getCheckinDate,LocalDate.now()));
    }
    @GetMapping("/weekly")
    public Result<Map<String,Object>> weekly(@RequestAttribute("userId") Long user,
            @RequestParam(defaultValue="0") @Min(0) @Max(520) int offset) {
        LocalDate end=LocalDate.now().minusWeeks(offset);
        return Result.success(TrainingStats.period(records(user),end.minusDays(6),end,"weekDates"));
    }
    @GetMapping("/monthly")
    public Result<Map<String,Object>> monthly(@RequestAttribute("userId") Long user,@RequestParam(required=false) String month) {
        YearMonth ym=month==null?YearMonth.now():YearMonth.parse(month);
        if(ym.getYear()<2000||ym.getYear()>2100) throw new BusinessException(ResultCode.PARAM_ERROR);
        return Result.success(TrainingStats.period(records(user),ym.atDay(1),ym.atEndOfMonth(),"monthDates"));
    }
}
