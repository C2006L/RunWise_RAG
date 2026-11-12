package com.runwise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.runwise.common.TrainingStats;
import com.runwise.entity.Badge;
import com.runwise.entity.Checkin;
import com.runwise.entity.UserBadge;
import com.runwise.mapper.BadgeMapper;
import com.runwise.mapper.CheckinMapper;
import com.runwise.mapper.UserBadgeMapper;
import com.runwise.service.BadgeService;
import com.runwise.vo.BadgeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 徽章服务实现。
 *
 * 判定数据源均为 t_checkin；解锁记录只新增不删除，
 * 因此编辑/删除打卡导致指标回落时，已解锁徽章仍然保留。
 */
@Slf4j
@Service
public class BadgeServiceImpl implements BadgeService {

    private static final String METRIC_COUNT = "count";
    private static final String METRIC_STREAK = "streak";
    private static final String METRIC_DISTANCE = "distance";

    private final BadgeMapper badgeMapper;
    private final UserBadgeMapper userBadgeMapper;
    private final CheckinMapper checkinMapper;

    public BadgeServiceImpl(BadgeMapper badgeMapper,
                            UserBadgeMapper userBadgeMapper,
                            CheckinMapper checkinMapper) {
        this.badgeMapper = badgeMapper;
        this.userBadgeMapper = userBadgeMapper;
        this.checkinMapper = checkinMapper;
    }

    @Override
    public List<BadgeVO> getUserBadges(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        return buildBadges(userId, true);
    }

    @Override
    public void recalculate(Long userId) {
        if (userId == null) {
            return;
        }
        // 打卡触发的重算只写解锁记录，不消费提示标记，
        // 等小程序下次拉取徽章列表时再提示一次。
        buildBadges(userId, false);
    }

    private List<BadgeVO> buildBadges(Long userId, boolean markNotified) {
        List<Badge> definitions = loadDefinitions();
        Metrics metrics = loadMetrics(userId);
        Map<Long, UserBadge> unlockedMap = loadUnlocked(userId);

        List<BadgeVO> result = new ArrayList<>();
        for (Badge badge : definitions) {
            double progress = progressValue(badge, metrics);
            UserBadge unlocked = badge.getId() == null ? null : unlockedMap.get(badge.getId());
            boolean newlyUnlocked = false;

            if (unlocked == null && badge.getId() != null && isReached(badge, metrics)) {
                try {
                    int inserted = userBadgeMapper.insertIgnore(userId, badge.getId());
                    if (inserted > 0) {
                        UserBadge record = userBadgeMapper.selectOne(new LambdaQueryWrapper<UserBadge>()
                                .eq(UserBadge::getUserId, userId)
                                .eq(UserBadge::getBadgeId, badge.getId()));
                        if (record == null) {
                            record = new UserBadge();
                            record.setUserId(userId);
                            record.setBadgeId(badge.getId());
                            record.setUnlockTime(LocalDateTime.now());
                        }
                        unlockedMap.put(badge.getId(), record);
                        unlocked = record;
                        newlyUnlocked = true;
                    }
                } catch (RuntimeException e) {
                    // 徽章表缺失或写入冲突都不能影响打卡主流程，按未解锁继续返回
                    log.warn("写入用户徽章失败 userId={}, badge={}: {}", userId, badge.getCode(), e.getMessage());
                }
            }

            if (unlocked != null && unlocked.getNotified() != null && unlocked.getNotified() == 0) {
                newlyUnlocked = true;
                if (markNotified) {
                    markNotified(unlocked);
                }
            }

            result.add(toVO(badge, progress, unlocked, newlyUnlocked));
        }
        return result;
    }

    /**
     * 读取徽章定义；表缺失或种子数据为空时退回内置定义，保证接口可用。
     */
    private List<Badge> loadDefinitions() {
        try {
            List<Badge> list = badgeMapper.selectList(new LambdaQueryWrapper<Badge>()
                    .orderByAsc(Badge::getSort)
                    .orderByAsc(Badge::getId));
            if (list != null && !list.isEmpty()) {
                return list;
            }
        } catch (RuntimeException e) {
            log.warn("读取徽章定义失败，使用内置定义: {}", e.getMessage());
        }
        return fallbackDefinitions();
    }

    private Metrics loadMetrics(Long userId) {
        Metrics metrics = new Metrics();
        try {
            List<Checkin> records = checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                    .eq(Checkin::getUserId, userId));
            List<LocalDate> dates = new ArrayList<>();
            for (Checkin record : records) {
                if (record.getCheckinDate() != null) {
                    dates.add(record.getCheckinDate());
                }
                if (record.getDistance() != null) {
                    metrics.totalDistance = metrics.totalDistance.add(record.getDistance());
                }
            }
            metrics.count = records.size();
            metrics.longestStreak = TrainingStats.longest(dates, LocalDate.now());
        } catch (RuntimeException e) {
            log.warn("读取打卡指标失败 userId={}: {}", userId, e.getMessage());
        }
        return metrics;
    }

    private Map<Long, UserBadge> loadUnlocked(Long userId) {
        Map<Long, UserBadge> map = new HashMap<>();
        try {
            List<UserBadge> list = userBadgeMapper.selectList(new LambdaQueryWrapper<UserBadge>()
                    .eq(UserBadge::getUserId, userId));
            for (UserBadge item : list) {
                if (item.getBadgeId() != null) {
                    map.put(item.getBadgeId(), item);
                }
            }
        } catch (RuntimeException e) {
            log.warn("读取用户徽章失败 userId={}: {}", userId, e.getMessage());
        }
        return map;
    }

    /**
     * 标记已向用户展示过解锁提示，避免小程序反复弹窗。
     * 写入失败不影响列表返回，下次拉取时会再提示一次。
     */
    private void markNotified(UserBadge record) {
        if (record == null || record.getId() == null) {
            return;
        }
        try {
            UserBadge update = new UserBadge();
            update.setId(record.getId());
            update.setNotified(1);
            userBadgeMapper.updateById(update);
            record.setNotified(1);
        } catch (RuntimeException e) {
            log.warn("更新徽章提示状态失败 userId={}, badge={}: {}",
                    record.getUserId(), record.getBadgeId(), e.getMessage());
        }
    }

    private boolean isReached(Badge badge, Metrics metrics) {
        if (badge.getMetric() == null || badge.getTarget() == null) {
            return false;
        }
        if (METRIC_COUNT.equals(badge.getMetric())) {
            return BigDecimal.valueOf(metrics.count).compareTo(badge.getTarget()) >= 0;
        }
        if (METRIC_STREAK.equals(badge.getMetric())) {
            return BigDecimal.valueOf(metrics.longestStreak).compareTo(badge.getTarget()) >= 0;
        }
        if (METRIC_DISTANCE.equals(badge.getMetric())) {
            return metrics.totalDistance.compareTo(badge.getTarget()) >= 0;
        }
        return false;
    }

    private double progressValue(Badge badge, Metrics metrics) {
        double target = badge.getTarget() == null ? 0D : badge.getTarget().doubleValue();
        double raw = 0D;
        if (badge.getMetric() != null) {
            if (METRIC_COUNT.equals(badge.getMetric())) {
                raw = metrics.count;
            } else if (METRIC_STREAK.equals(badge.getMetric())) {
                raw = metrics.longestStreak;
            } else if (METRIC_DISTANCE.equals(badge.getMetric())) {
                raw = metrics.totalDistance.doubleValue();
            }
        }
        if (target > 0D && raw > target) {
            raw = target;
        }
        return BigDecimal.valueOf(raw).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private BadgeVO toVO(Badge badge, double progress, UserBadge unlocked, boolean newlyUnlocked) {
        BadgeVO vo = new BadgeVO();
        vo.setCode(badge.getCode());
        vo.setName(badge.getName());
        vo.setDescription(badge.getDescription());
        vo.setIcon(badge.getIcon());
        vo.setSort(badge.getSort());
        vo.setMetric(badge.getMetric());
        vo.setUnit(badge.getUnit());
        vo.setTarget(badge.getTarget() == null ? 0D : badge.getTarget().doubleValue());
        vo.setProgress(progress);
        vo.setUnlocked(unlocked != null);
        vo.setUnlockTime(unlocked == null ? null : unlocked.getUnlockTime());
        vo.setNewlyUnlocked(newlyUnlocked);
        vo.setProgressText(formatNumber(vo.getProgress()) + " / " + formatNumber(vo.getTarget())
                + (badge.getUnit() == null || badge.getUnit().isEmpty() ? "" : " " + badge.getUnit()));
        return vo;
    }

    private String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.001D) {
            return String.valueOf((long) Math.rint(value));
        }
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
    }

    /**
     * 种子数据缺失时的兜底定义，与 badge_v4.sql 保持一致。
     */
    private List<Badge> fallbackDefinitions() {
        List<Badge> list = new ArrayList<>();
        list.add(define("first_checkin", "首次打卡", "完成第一条训练记录", "flag", METRIC_COUNT, 1, "次", 1));
        list.add(define("streak_7", "连续 7 天", "最长连续打卡达到 7 天", "fire", METRIC_STREAK, 7, "天", 2));
        list.add(define("distance_100", "累计 100km", "累计跑量达到 100 公里", "map-pin", METRIC_DISTANCE, 100, "km", 3));
        list.add(define("streak_30", "连续 30 天", "最长连续打卡达到 30 天", "calendar", METRIC_STREAK, 30, "天", 4));
        list.add(define("distance_500", "累计 500km", "累计跑量达到 500 公里", "star", METRIC_DISTANCE, 500, "km", 5));
        list.add(define("distance_1000", "累计 1000km", "累计跑量达到 1000 公里", "trophy", METRIC_DISTANCE, 1000, "km", 6));
        return list;
    }

    private Badge define(String code, String name, String description, String icon,
                         String metric, double target, String unit, int sort) {
        Badge badge = new Badge();
        badge.setCode(code);
        badge.setName(name);
        badge.setDescription(description);
        badge.setIcon(icon);
        badge.setMetric(metric);
        badge.setTarget(BigDecimal.valueOf(target));
        badge.setUnit(unit);
        badge.setSort(sort);
        return badge;
    }

    private static class Metrics {
        private int count;
        private int longestStreak;
        private BigDecimal totalDistance = BigDecimal.ZERO;
    }
}
