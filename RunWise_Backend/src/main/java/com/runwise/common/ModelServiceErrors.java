package com.runwise.common;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

/** Only forward known error codes; never expose raw provider responses or credentials. */
public final class ModelServiceErrors {
    private ModelServiceErrors() {}

    public static BusinessException fromResponse(String body) {
        String code = "";
        try {
            JSONObject detail = JSONUtil.parseObj(body).getJSONObject("detail");
            if (detail != null) code = detail.getStr("code", "");
        } catch (RuntimeException ignored) { }
        String message;
        switch (code) {
            case "MODEL_AUTH": message = "模型服务认证失败，请检查服务端 API 密钥和权限。"; break;
            case "MODEL_BUSY": message = "模型服务限流或额度不足，请稍后重试或检查账户额度。"; break;
            case "MODEL_CONFIG": message = "模型配置不正确，请检查服务端模型名、接口地址、密钥和 JSON 模式。"; break;
            case "MODEL_TIMEOUT": message = "模型连接失败或回答超时，请稍后重试。"; break;
            case "MODEL_TRUNCATED": message = "回答达到输出上限，未保存不完整内容。请缩小问题范围，或由管理员提高输出预算。"; break;
            case "MODEL_RESPONSE": message = "模型没有返回完整可用的回答，请调整问题后重试。"; break;
            default: message = "AI 分析服务暂不可用，请稍后重试。";
        }
        return new BusinessException(ResultCode.RAG_SERVICE_ERROR, message);
    }
}
