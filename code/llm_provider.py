"""生成模型适配层。密钥只在 Python 服务端使用，不返回给客户端。"""
from __future__ import annotations

import json
import os
import logging
from dataclasses import dataclass, field
from typing import Mapping
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit
from urllib.request import Request, urlopen


class ModelError(RuntimeError):
    def __init__(self, code: str, message: str, provider_code: str | None = None):
        super().__init__(message)
        self.code = code
        self.provider_code = provider_code

    def detail(self):
        detail = {"code": self.code, "message": str(self)}
        if self.provider_code: detail["providerCode"] = self.provider_code
        return detail


@dataclass(frozen=True)
class ModelSettings:
    provider: str
    base_url: str
    model: str
    api_key: str = field(default="", repr=False)
    timeout: float = 70
    chat_tokens: int = 1000
    plan_tokens: int = 1100
    json_mode: str = "json_object"
    thinking: str = "default"

    @classmethod
    def from_env(cls, env: Mapping[str, str] | None = None):
        env = os.environ if env is None else env
        provider = env.get("LLM_PROVIDER", "ollama").strip().lower()
        if provider not in ("ollama", "compatible"):
            raise ModelError("MODEL_CONFIG", "LLM_PROVIDER 只能为 ollama 或 compatible，请检查服务端配置。")
        local = provider == "ollama"
        base = env.get("OLLAMA_BASE_URL", "http://127.0.0.1:11434") if local else env.get("OPENAI_BASE_URL", "")
        model = env.get("OLLAMA_MODEL", "qwen2.5:3b") if local else env.get("LLM_MODEL", "")
        key = "" if local else env.get("OPENAI_API_KEY", "")
        try:
            parsed = urlsplit(base)
            small_vision = not local and parsed.hostname == 'open.bigmodel.cn' and model.strip().lower() == 'glm-4v-flash'
            valid_url = parsed.scheme in ("http", "https") and parsed.hostname and not (
                parsed.username or parsed.password or parsed.query or parsed.fragment)
            timeout = float(env.get("LLM_TIMEOUT_SECONDS", "70"))
            chat_tokens = int(env.get("LLM_CHAT_MAX_TOKENS", "900" if small_vision else "1000" if local else "2400"))
            plan_tokens = int(env.get("LLM_PLAN_MAX_TOKENS", "900" if small_vision else "1100" if local else "1800"))
            valid_numbers = 1 <= timeout <= 70 and 128 <= chat_tokens <= 8000 and 128 <= plan_tokens <= 8000
        except (ValueError, TypeError):
            valid_url = valid_numbers = False
        mode = env.get("LLM_JSON_MODE", "json_object").strip()
        thinking = env.get("LLM_THINKING", "default").strip().lower() if not local else "default"
        if thinking not in ("default", "enabled", "disabled"):
            raise ModelError("MODEL_CONFIG", "LLM_THINKING 只能为 default、enabled 或 disabled。")
        if not local and valid_url and valid_numbers and model.strip().lower()=='glm-4v-flash' and urlsplit(base).hostname=='open.bigmodel.cn':
            if chat_tokens>1024 or plan_tokens>1024 or thinking!='default':
                raise ModelError('MODEL_CONFIG','GLM-4V-Flash 输出预算不能超过1024，LLM_THINKING 请设为 default。')
        if not valid_url or not valid_numbers or mode not in ("json_object", "json_schema") or not model.strip() or (not local and not key.strip()):
            raise ModelError("MODEL_CONFIG", "模型配置不完整或不合法，请检查服务端地址、模型名、密钥、输出预算和超时配置。")
        return cls(provider, base.rstrip("/"), model.strip(), key.strip(), timeout, chat_tokens, plan_tokens, mode, thinking)


def model_info():
    """仅报告配置，不声称已连接供应商；不暴露密钥或私有地址。"""
    try:
        settings = ModelSettings.from_env()
        return {"provider": settings.provider, "model": settings.model, "configured": True}
    except ModelError:
        return {"provider": os.getenv("LLM_PROVIDER", "ollama"), "configured": False}


def generate_text(prompt: str, max_tokens: int, structured=False, settings: ModelSettings | None = None, image_data: str | None = None) -> str:
    settings = settings or ModelSettings.from_env()
    if image_data and settings.provider != 'compatible':
        raise ModelError('MODEL_CONFIG', '图片识别需要配置支持视觉的兼容 API 模型。')
    headers = {"Content-Type": "application/json; charset=utf-8"}
    if settings.provider == "ollama":
        endpoint = settings.base_url + "/api/generate"
        body = {"model": settings.model, "prompt": prompt, "stream": False,
                "options": {"temperature": 0.15, "num_predict": max_tokens}}
        if structured:
            body["format"] = structured if isinstance(structured, dict) else "json"
    else:
        endpoint = settings.base_url + "/chat/completions"
        headers["Authorization"] = "Bearer " + settings.api_key
        body = {"model": settings.model, "messages": [{"role": "user", "content": prompt}],
                "stream": False, "temperature": 0.15, "max_tokens": max_tokens}
        if image_data:
            body['messages'][0]['content'] = [
                {'type': 'text', 'text': prompt},
                {'type': 'image_url', 'image_url': {'url': image_data}},
            ]
        if settings.thinking != "default":
            body["thinking"] = {"type": settings.thinking}
        if structured:
            if settings.json_mode == "json_schema" and isinstance(structured, dict):
                body["response_format"] = {"type": "json_schema", "json_schema": {
                    "name": "runwise_plan_choice", "strict": True, "schema": structured}}
            else:
                body["response_format"] = {"type": "json_object"}
    request = Request(endpoint, data=json.dumps(body, ensure_ascii=False).encode("utf-8"), headers=headers, method="POST")
    try:
        with urlopen(request, timeout=settings.timeout) as response:
            payload = response.read(2_000_001)
        if len(payload) > 2_000_000:
            raise ModelError("MODEL_RESPONSE", "模型返回内容过大，请缩小问题范围。")
        data = json.loads(payload.decode("utf-8"))
    except HTTPError as exc:
        # Only extract a numeric vendor code. Never forward the raw error text.
        vendor_code = None
        if urlsplit(settings.base_url).hostname == 'open.bigmodel.cn':
            try:
                candidate = str(json.loads(exc.read(16384).decode('utf-8'))['error']['code'])
                if len(candidate)==4 and candidate.isascii() and candidate.isdigit(): vendor_code=candidate
            except (ValueError, KeyError, TypeError, UnicodeError, OSError): pass
        known = {
            '1113': ('MODEL_BALANCE', '平台报告账户余额不足，请核对账户状态和当前模型权限。'),
            '1302': ('MODEL_RATE_LIMIT', '请求触发平台速率限制，请稍后重试。'),
            '1303': ('MODEL_RATE_LIMIT', '请求触发平台速率限制，请稍后重试。'),
            '1304': ('MODEL_QUOTA', '已达到平台调用次数上限，请查看限额与重置时间。'),
            '1305': ('MODEL_OVERLOADED', '当前 GLM 模型访问量过大，请稍后重试。此错误不表示余额不足。'),
            '1312': ('MODEL_OVERLOADED', '当前 GLM 模型访问量过大，请稍后重试。此错误不表示余额不足。'),
            '1308': ('MODEL_QUOTA', '已达到平台使用上限，请查看限额与重置时间。'),
            '1310': ('MODEL_QUOTA', '已达到平台使用上限，请查看限额与重置时间。'),
            '1309': ('MODEL_PERMISSION', '平台套餐状态异常，请核对当前 Key 对应的产品与权限。'),
            '1311': ('MODEL_PERMISSION', '当前 Key 或套餐不支持此模型，请核对产品类型与模型权限。'),
            '1315': ('MODEL_PERMISSION', '当前 Key 类型不适用于此接口，请使用模型标准 API Key。'),
        }
        if vendor_code in known:
            error = ModelError(*known[vendor_code], provider_code=vendor_code)
        elif exc.code in (401, 403):
            error = ModelError("MODEL_AUTH", "模型服务认证失败，请检查服务端 API 密钥和权限。")
        elif exc.code == 429:
            error = ModelError("MODEL_BUSY", "平台暂时限制此请求，请稍后重试，并查看平台错误码确认原因。", vendor_code)
        elif exc.code in (400, 404, 422):
            error = ModelError("MODEL_CONFIG", "模型服务不接受当前请求，请检查模型名、接口地址和 JSON 模式。")
        else:
            error = ModelError("MODEL_UNAVAILABLE", "模型服务暂不可用，请稍后重试。")
        logging.getLogger(__name__).warning('Model request rejected: HTTP=%s provider_code=%s', exc.code, vendor_code or 'unknown')
        exc.close()
        raise error from None
    except (TimeoutError, URLError, OSError):
        raise ModelError("MODEL_TIMEOUT", "模型连接失败或回答超时，请稍后重试；本次没有保存回答或课表。") from None
    except (ValueError, UnicodeError):
        raise ModelError("MODEL_RESPONSE", "模型返回格式异常，请稍后重试。") from None
    try:
        if settings.provider == "ollama":
            content, reason = data.get("response"), data.get("done_reason")
            complete = data.get("done") is True
        else:
            choice = data["choices"][0]
            content, reason = choice["message"].get("content"), choice.get("finish_reason")
            complete = reason == "stop" and not choice["message"].get("refusal")
    except (KeyError, IndexError, TypeError, AttributeError):
        raise ModelError("MODEL_RESPONSE", "模型没有返回可用回答，请稍后重试。") from None
    if reason == "length":
        raise ModelError("MODEL_TRUNCATED", "回答达到输出上限，未保存不完整内容。请缩小问题范围，或由管理员提高输出预算。")
    if not complete or not isinstance(content, str) or not content.strip():
        raise ModelError("MODEL_RESPONSE", "模型没有返回完整回答，请调整问题后重试。")
    return content.strip()
