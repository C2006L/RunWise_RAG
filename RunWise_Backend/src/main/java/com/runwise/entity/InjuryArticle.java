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

    /** 分类 key：knee / ankle / shin */
    private String category;

    private String title;

    /** 摘要（列表页展示） */
    private String summary;

    /** 标签，数据库中以英文逗号分隔 */
    private String tags;

    /** 正文分节，数据库中保存 JSON 数组字符串 */
    private String sections;

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
