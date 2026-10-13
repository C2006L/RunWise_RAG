package com.runwise.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 伤病预防文章实体
 */
@Data
@TableName("t_injury_article")
public class InjuryArticle {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类：急性损伤 / 慢性劳损 / 预防训练 */
    private String category;

    private String title;

    /** 摘要（列表页展示） */
    private String summary;

    /** 常见表现 */
    private String symptoms;

    /** 成因分析 */
    private String causes;

    /** 预防要点 */
    private String prevention;

    /** 处理建议 */
    private String treatment;

    /** 预计阅读时长（分钟） */
    private Integer readMinutes;

    /** 排序号，越小越靠前 */
    private Integer sort;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}