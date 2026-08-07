-- RunWise 数据库建表脚本
-- 数据库: runwise
-- 字符集: utf8mb4

CREATE DATABASE IF NOT EXISTS runwise DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE runwise;

-- 用户表
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `openid`        VARCHAR(64)  NOT NULL COMMENT '微信openid',
    `nickname`      VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    `avatar_url`    VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    `gender`        TINYINT      DEFAULT 0 COMMENT '性别: 0未知 1男 2女',
    `status`        TINYINT      DEFAULT 1 COMMENT '状态: 0禁用 1正常',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      DEFAULT 0 COMMENT '逻辑删除: 0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 打卡记录表
DROP TABLE IF EXISTS `t_checkin`;
CREATE TABLE `t_checkin` (
    `id`            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT         NOT NULL COMMENT '用户ID',
    `checkin_date`  DATE           NOT NULL COMMENT '打卡日期',
    `distance`      DECIMAL(6,2)   NOT NULL COMMENT '跑步距离(公里)',
    `duration`      INT            NOT NULL COMMENT '跑步时长(秒)',
    `pace`          DECIMAL(5,2)   DEFAULT NULL COMMENT '平均配速(秒/公里)',
    `mood`          VARCHAR(16)    DEFAULT NULL COMMENT '心情标签',
    `remark`        VARCHAR(256)   DEFAULT NULL COMMENT '备注',
    `image_url`     VARCHAR(512)   DEFAULT NULL COMMENT '跑步截图URL',
    `image_audit`   TINYINT        DEFAULT 0 COMMENT '图片审核状态: 0待审 1通过 2违规',
    `create_time`   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT        DEFAULT 0 COMMENT '逻辑删除: 0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_date` (`user_id`, `checkin_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打卡记录表';

-- 问答记录表
DROP TABLE IF EXISTS `t_qa_record`;
CREATE TABLE `t_qa_record` (
    `id`            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT         NOT NULL COMMENT '用户ID',
    `question`      TEXT           NOT NULL COMMENT '用户问题',
    `answer`        TEXT           NOT NULL COMMENT 'RAG回答',
    `sources`       TEXT           DEFAULT NULL COMMENT '引用来源(JSON)',
    `feedback`      TINYINT        DEFAULT 0 COMMENT '反馈: 0未反馈 1有用 2无用',
    `create_time`   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT        DEFAULT 0 COMMENT '逻辑删除: 0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答记录表';
