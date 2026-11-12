package com.runwise.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 徽章定义实体
 */
@Data
@TableName("t_badge")
public class Badge {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 徽章唯一编码 */
    private String code;

    private String name;

    private String description;

    /** 前端图标标识 */
    private String icon;

    /** 判定指标：count / streak / distance */
    private String metric;

    private BigDecimal target;

    private String unit;

    private Integer sort;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
