package com.runwise.common;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

/** Only forward known error codes; never expose raw provider responses or credentials. */
public final class ModelServiceErrors {
    private ModelServiceErrors() {}

    public static BusinessException fromResponse(String body) {
        String code = "";
        String providerCode = "";
        try {
            JSONObject detail = JSONUtil.parseObj(body).getJSONObject("detail");
            if (detail != null) {code = detail.getStr("code", "");providerCode=detail.getStr("providerCode", "");}
        } catch (RuntimeException ignored) { }
        String message;
        switch (code) {
            case "MODEL_AUTH": message = "模型服务认证失败，请检查服务端 API 密钥和权限。"; break;
            case "MODEL_BUSY": message = "平台暂时限制此请求，请稍后重试，并查看平台错误码确认原因。"; break;
            case "MODEL_OVERLOADED": message = "当前模型访问量过大，请稍后重试。此错误不表示余额不足。"; break;
            case "MODEL_RATE_LIMIT": message = "请求触发平台速率限制，请稍后重试。"; break;
            case "MODEL_QUOTA": message = "已达到平台使用或调用次数上限，请查看平台限额与重置时间。"; break;
            case "MODEL_BALANCE": message = "平台报告账户余额不足，请核对账户状态和当前模型权限。"; break;
            case "MODEL_PERMISSION": message = "当前 Key 类型、模型权限或套餐状态不匹配，请检查平台配置。"; break;
            case "MODEL_CONFIG": message = "模型配置不正确，请检查服务端模型名、接口地址、密钥和 JSON 模式。"; break;
            case "MODEL_TIMEOUT": message = "模型连接失败或回答超时，请稍后重试。"; break;
            case "MODEL_TRUNCATED": message = "回答达到输出上限，未保存不完整内容。请缩小问题范围，或由管理员提高输出预算。"; break;
            case "MODEL_RESPONSE": message = "模型没有返回完整可用的回答，请调整问题后重试。"; break;
            default: message = "AI 分析服务暂不可用，请稍后重试。";
        }
        if(providerCode.matches("[0-9]{4}"))message+="（平台错误码："+providerCode+"）";
        return new BusinessException(ResultCode.RAG_SERVICE_ERROR, message);
    }
}
