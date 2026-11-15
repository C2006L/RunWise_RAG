package com.runwise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.runwise.common.BusinessException;
import com.runwise.common.PageResult;
import com.runwise.common.ResultCode;
import com.runwise.common.TrainingStats;
import com.runwise.dto.CheckinDTO;
import com.runwise.entity.Checkin;
import com.runwise.mapper.CheckinMapper;
import com.runwise.service.BadgeService;
import com.runwise.service.CheckinService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    // 字段注入保持无参构造，兼容现有单元测试的 new CheckinServiceImpl()
    @Autowired
    private BadgeService badgeService;
    @org.springframework.beans.factory.annotation.Value("${runwise.upload-dir:uploads}")
    private String uploadDir = "uploads";

    @Override
    public Long createCheckin(Long userId, CheckinDTO dto) {
        // 1. 检查今日是否已打卡
        LocalDate checkinDate = validDate(dto.getCheckinDate());
        com.runwise.common.UploadedImages.requireOwned(dto.getImageUrl(), null, userId, uploadDir);
        Checkin existing = getOne(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId)
                .eq(Checkin::getCheckinDate, checkinDate));
        if (existing != null) {
            throw new BusinessException(ResultCode.CHECKIN_ALREADY_EXISTS);
        }

        // 2. 计算配速（分钟/公里，与两端既有字段单位一致）
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
        boolean hasImage = dto.getImageUrl() != null && !dto.getImageUrl().trim().isEmpty();
        checkin.setImageAudit(hasImage ? 0 : 1); // 有图片待审核，无图片直接通过
        save(checkin);
        refreshBadges(userId);

        log.info("用户{}打卡成功, date={}, distance={}km", userId, checkinDate, dto.getDistance());
        return checkin.getId();
    }

    @Override
    public Checkin updateCheckin(Long userId, Long id, CheckinDTO dto) {
        Checkin checkin = getById(id);
        if (checkin == null || !checkin.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.CHECKIN_NOT_FOUND);
        }

        LocalDate checkinDate = validDate(dto.getCheckinDate());
        com.runwise.common.UploadedImages.requireOwned(dto.getImageUrl(), checkin.getImageUrl(), userId, uploadDir);
        Checkin sameDay = getOne(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId)
                .eq(Checkin::getCheckinDate, checkinDate)
                .ne(Checkin::getId, id));
        if (sameDay != null) {
            throw new BusinessException(ResultCode.CHECKIN_ALREADY_EXISTS);
        }

        BigDecimal pace = new BigDecimal(dto.getDuration())
                .divide(dto.getDistance(), 2, RoundingMode.HALF_UP);
        boolean imageChanged = !Objects.equals(checkin.getImageUrl(), dto.getImageUrl());

        checkin.setCheckinDate(checkinDate);
        checkin.setDistance(dto.getDistance());
        checkin.setDuration(dto.getDuration());
        checkin.setPace(pace);
        checkin.setMood(dto.getMood());
        checkin.setRemark(dto.getRemark());
        checkin.setImageUrl(dto.getImageUrl());
        if (imageChanged) {
            checkin.setImageAudit(dto.getImageUrl() == null || dto.getImageUrl().trim().isEmpty() ? 1 : 0);
        }
        updateById(checkin);
        refreshBadges(userId);

        log.info("用户{}修改打卡记录{}, date={}", userId, id, checkinDate);
        return checkin;
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
                .between(Checkin::getCheckinDate, weekStart, LocalDate.now()));
        stats.put("weekCount", weekCount);

        // 连续打卡天数
        int streak = calculateStreak(userId);
        stats.put("streak", streak);

        // 总公里数
        List<Checkin> all = list(new LambdaQueryWrapper<Checkin>().eq(Checkin::getUserId, userId)
                .le(Checkin::getCheckinDate, LocalDate.now()));
        stats.put("totalCount",(long)all.size());
        stats.put("longestStreak",TrainingStats.longest(all.stream().map(Checkin::getCheckinDate).collect(java.util.stream.Collectors.toList()),LocalDate.now()));
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
        refreshBadges(userId);
        log.info("用户{}删除打卡记录{}", userId, id);
    }

    /**
     * 打卡数据变更后同步重算徽章。徽章属于激励功能，
     * 任何异常都只记录日志，不能让打卡主流程失败。
     */
    private void refreshBadges(Long userId) {
        if (badgeService == null) {
            return;
        }
        try {
            badgeService.recalculate(userId);
        } catch (RuntimeException e) {
            log.warn("重算用户{}徽章失败: {}", userId, e.getMessage());
        }
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

        return TrainingStats.streak(list.stream().map(Checkin::getCheckinDate)
                .collect(java.util.stream.Collectors.toList()),LocalDate.now());
    }

    private LocalDate validDate(String raw) {
        LocalDate date=LocalDate.parse(raw);
        if(date.isAfter(LocalDate.now()) || date.isBefore(LocalDate.of(2000,1,1)))
            throw new BusinessException(ResultCode.PARAM_ERROR,"不允许记录未来日期或2000年以前的训练");
        return date;
    }
}
