package com.runwise.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 打卡请求参数
 */
@Data
public class CheckinDTO {

    @NotBlank(message = "日期不能为空")
    @javax.validation.constraints.Pattern(regexp="\\d{4}-\\d{2}-\\d{2}",message="日期需为yyyy-MM-dd")
    private String checkinDate;

    @NotNull(message = "距离不能为空")
    @DecimalMin(value = "0.01", message = "距离必须大于0")
    @javax.validation.constraints.DecimalMax("300")
    @javax.validation.constraints.Digits(integer=3,fraction=2)
    private BigDecimal distance;

    @NotNull(message = "时长不能为空")
    @Min(value = 1, message = "时长必须大于0")
    @javax.validation.constraints.Max(10080)
    private Integer duration;

    @javax.validation.constraints.Size(max=32) private String mood;

    @javax.validation.constraints.Size(max=1000) private String remark;

    @javax.validation.constraints.Size(max=512) private String imageUrl;
}
