package com.runwise.service;

import com.runwise.vo.BadgeVO;

import java.util.List;

/**
 * 徽章服务：判定、解锁与列表查询统一入口
 */
public interface BadgeService {

    /**
     * 查询当前用户的徽章列表（含已解锁状态、进度、解锁时间）
     */
    List<BadgeVO> getUserBadges(Long userId);

    /**
     * 打卡数据变更后重算徽章；已解锁的徽章不回收
     */
    void recalculate(Long userId);
}
