package com.runwise.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 问答请求参数
 */
@Data
public class AskDTO {

    @NotBlank(message = "问题不能为空")
    private String question;
}
