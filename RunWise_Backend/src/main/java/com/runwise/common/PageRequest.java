package com.runwise.common;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页请求参数
 */
@Data
@NoArgsConstructor
public class PageRequest {

    private Integer pageNum = 1;
    private Integer pageSize = 10;

    public Integer getOffset() {
        return (pageNum - 1) * pageSize;
    }
}
