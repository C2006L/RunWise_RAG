package com.runwise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户徽章解锁记录（只新增，不回收）
 */
@Data
@TableName("t_user_badge")
public class UserBadge {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long badgeId;

    private LocalDateTime unlockTime;

    /** 0=待提示 1=已提示，用于小程序只弹一次解锁提示 */
    private Integer notified;
}
