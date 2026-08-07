package com.runwise.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或token已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    SERVER_ERROR(500, "服务器内部错误"),

    // 业务错误码 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_DISABLED(1002, "用户已被禁用"),
    LOGIN_FAILED(1003, "登录失败"),
    CHECKIN_ALREADY_EXISTS(1004, "今日已打卡"),
    CHECKIN_NOT_FOUND(1005, "打卡记录不存在"),
    RAG_SERVICE_ERROR(1006, "问答服务暂时不可用"),
    WECHAT_API_ERROR(1007, "微信接口调用失败");

    private final Integer code;
    private final String message;
}
