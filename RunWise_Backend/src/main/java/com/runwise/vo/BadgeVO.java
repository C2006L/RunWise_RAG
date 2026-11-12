package com.runwise.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 返回给 Web / 小程序的徽章结构。
 *
 * 数据库只存定义与解锁记录，进度、进度文案、是否本次新解锁都在服务端计算后输出，
 * 避免两端各自实现一套判定逻辑。
 */
@Data
public class BadgeVO {

    private String code;
    private String name;
    private String description;
    private String icon;
    private Integer sort;

    /** count / streak / distance */
    private String metric;

    private double target;
    private String unit;

    /** 当前进度，已按 target 封顶 */
    private double progress;

    /** 前端直接展示的进度文案，如「86 / 100 km」 */
    private String progressText;

    private boolean unlocked;

    private LocalDateTime unlockTime;

    /** 本次请求才写入 t_user_badge 的徽章，供小程序弹解锁提示 */
    private boolean newlyUnlocked;
}
