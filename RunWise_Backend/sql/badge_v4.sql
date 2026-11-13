-- 在 business_v2.sql 之后执行一次。备份后再运行；本脚本可重复执行，不会删除已有数据。
-- 徽章定义表 + 用户解锁记录表 + 6 枚徽章种子数据
-- 文件为 UTF-8；Windows mysql 客户端的默认会话编码可能不同。
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS t_badge (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(32)  NOT NULL UNIQUE,
    name        VARCHAR(64)  NOT NULL,
    description VARCHAR(255) NOT NULL DEFAULT '',
    icon        VARCHAR(32)  NOT NULL DEFAULT '',
    metric      VARCHAR(16)  NOT NULL COMMENT 'count=打卡次数 / streak=最长连续天数 / distance=累计公里',
    target      DECIMAL(10,2) NOT NULL DEFAULT 0,
    unit        VARCHAR(16)  NOT NULL DEFAULT '',
    sort        INT          NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_badge_sort (sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_user_badge (
    id          BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT   NOT NULL,
    badge_id    BIGINT   NOT NULL,
    unlock_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notified    TINYINT  NOT NULL DEFAULT 0 COMMENT '0=待提示 1=已提示',
    UNIQUE KEY uk_user_badge (user_id,badge_id),
    KEY idx_user_badge_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO t_badge (code, name, description, icon, metric, target, unit, sort)
VALUES ('first_checkin', '首次打卡', '完成第一条训练记录', 'flag', 'count', 1, '次', 1),
       ('streak_7', '连续 7 天', '最长连续打卡达到 7 天', 'fire', 'streak', 7, '天', 2),
       ('distance_100', '累计 100km', '累计跑量达到 100 公里', 'map-pin', 'distance', 100, 'km', 3),
       ('streak_30', '连续 30 天', '最长连续打卡达到 30 天', 'calendar', 'streak', 30, '天', 4),
       ('distance_500', '累计 500km', '累计跑量达到 500 公里', 'star', 'distance', 500, 'km', 5),
       ('distance_1000', '累计 1000km', '累计跑量达到 1000 公里', 'trophy', 'distance', 1000, 'km', 6)
ON DUPLICATE KEY UPDATE name = VALUES(name),
                        description = VALUES(description),
                        icon = VALUES(icon),
                        metric = VALUES(metric),
                        target = VALUES(target),
                        unit = VALUES(unit),
                        sort = VALUES(sort);
