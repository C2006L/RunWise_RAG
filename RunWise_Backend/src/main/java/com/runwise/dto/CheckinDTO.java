package com.runwise.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 打卡请求参数
 */
@Data
public class CheckinDTO {

    @NotNull(message = "日期不能为空")
    private String checkinDate;

    @NotNull(message = "距离不能为空")
    private BigDecimal distance;

    @NotNull(message = "时长不能为空")
    private Integer duration;

    private String mood;

    private String remark;

    private String imageUrl;
}
