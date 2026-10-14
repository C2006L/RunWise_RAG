package com.runwise.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 返回给 Web / 小程序的伤病文章结构。
 *
 * 数据库实体保留 tags、sections 的原始字符串；VO 将它们转换成前端可直接遍历的数组，
 * 避免把数据库存储格式泄漏到页面层。
 */
@Data
public class InjuryArticleVO {

    private Long id;
    private String category;
    private String categoryLabel;
    private String title;
    private String summary;
    private List<String> tags;
    private List<Section> sections;
    private Integer readMinutes;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Section {
        private String heading;
        private String body;
    }
}
