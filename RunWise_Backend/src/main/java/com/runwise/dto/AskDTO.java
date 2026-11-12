package com.runwise.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 问答请求参数
 */
@Data
public class AskDTO {

    @NotBlank(message = "问题不能为空")
    @javax.validation.constraints.Size(max=500)
    private String question;

    @javax.validation.constraints.Pattern(regexp="[A-Za-z0-9_-]{1,80}")
    private String conversationId;
}
