package com.runwise.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.runwise.entity.UserBadge;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface UserBadgeMapper extends BaseMapper<UserBadge> {

    /**
     * 依赖唯一索引做幂等写入：已解锁时返回 0，不会覆盖首次解锁时间。
     */
    @Insert("INSERT IGNORE INTO t_user_badge (user_id, badge_id, unlock_time) VALUES (#{userId}, #{badgeId}, NOW())")
    int insertIgnore(@Param("userId") Long userId, @Param("badgeId") Long badgeId);
}
