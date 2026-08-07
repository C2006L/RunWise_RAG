package com.runwise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.runwise.common.BusinessException;
import com.runwise.common.PageResult;
import com.runwise.common.ResultCode;
import com.runwise.dto.CheckinDTO;
import com.runwise.entity.Checkin;
import com.runwise.mapper.CheckinMapper;
import com.runwise.service.CheckinService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 打卡服务实现
 */
@Slf4j
@Service
public class CheckinServiceImpl extends ServiceImpl<CheckinMapper, Checkin> implements CheckinService {

    @Override
    public Long createCheckin(Long userId, CheckinDTO dto) {
        // 1. 检查今日是否已打卡
        LocalDate checkinDate = LocalDate.parse(dto.getCheckinDate());
        Checkin existing = getOne(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId)
                .eq(Checkin::getCheckinDate, checkinDate));
        if (existing != null) {
            throw new BusinessException(ResultCode.CHECKIN_ALREADY_EXISTS);
        }

        // 2. 计算配速 (秒/公里)
        BigDecimal pace = null;
        if (dto.getDistance().compareTo(BigDecimal.ZERO) > 0) {
            pace = new BigDecimal(dto.getDuration())
                    .divide(dto.getDistance(), 2, RoundingMode.HALF_UP);
        }

        // 3. 保存打卡记录
        Checkin checkin = new Checkin();
        checkin.setUserId(userId);
        checkin.setCheckinDate(checkinDate);
        checkin.setDistance(dto.getDistance());
        checkin.setDuration(dto.getDuration());
        checkin.setPace(pace);
        checkin.setMood(dto.getMood());
        checkin.setRemark(dto.getRemark());
        checkin.setImageUrl(dto.getImageUrl());
        checkin.setImageAudit(dto.getImageUrl() != null ? 0 : 1); // 有图片待审核，无图片直接通过
        save(checkin);

        log.info("用户{}打卡成功, date={}, distance={}km", userId, checkinDate, dto.getDistance());
        return checkin.getId();
    }

    @Override
    public Checkin getTodayCheckin(Long userId) {
        return getOne(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId)
                .eq(Checkin::getCheckinDate, LocalDate.now()));
    }

    @Override
    public PageResult<Checkin> getCheckinList(Long userId, Integer pageNum, Integer pageSize) {
        Page<Checkin> page = new Page<>(pageNum, pageSize);
        page(page, new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId)
                .orderByDesc(Checkin::getCheckinDate));
        return new PageResult<>(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    @Override
    public List<Map<String, Object>> getCalendarData(Long userId, String month) {
        YearMonth ym = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<Checkin> list = list(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId)
                .between(Checkin::getCheckinDate, start, end)
                .orderByAsc(Checkin::getCheckinDate));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Checkin c : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", c.getCheckinDate().toString());
            item.put("distance", c.getDistance());
            item.put("duration", c.getDuration());
            item.put("mood", c.getMood());
            result.add(item);
        }
        return result;
    }

    @Override
    public Map<String, Object> getStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        // 总打卡次数
        long totalCount = count(new LambdaQueryWrapper<Checkin>().eq(Checkin::getUserId, userId));
        stats.put("totalCount", totalCount);

        // 本周打卡次数
        LocalDate weekStart = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
        long weekCount = count(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId)
                .ge(Checkin::getCheckinDate, weekStart));
        stats.put("weekCount", weekCount);

        // 连续打卡天数
        int streak = calculateStreak(userId);
        stats.put("streak", streak);

        // 总公里数
        List<Checkin> all = list(new LambdaQueryWrapper<Checkin>().eq(Checkin::getUserId, userId));
        double totalDistance = all.stream()
                .mapToDouble(c -> c.getDistance().doubleValue())
                .sum();
        stats.put("totalDistance", Math.round(totalDistance * 100) / 100.0);

        return stats;
    }

    @Override
    public void deleteCheckin(Long userId, Long id) {
        Checkin checkin = getById(id);
        if (checkin == null || !checkin.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.CHECKIN_NOT_FOUND);
        }
        removeById(id);
        log.info("用户{}删除打卡记录{}", userId, id);
    }

    /**
     * 计算连续打卡天数
     */
    private int calculateStreak(Long userId) {
        List<Checkin> list = list(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId)
                .orderByDesc(Checkin::getCheckinDate));
        if (list.isEmpty()) {
            return 0;
        }

        int streak = 0;
        LocalDate expected = LocalDate.now();
        for (Checkin c : list) {
            if (c.getCheckinDate().equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else if (c.getCheckinDate().equals(expected.minusDays(1))) {
                // 今天还没打卡但昨天打了，也算连续
                expected = expected.minusDays(1);
                streak++;
                expected = expected.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }
}
