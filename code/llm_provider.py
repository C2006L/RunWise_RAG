"""生成模型适配层。密钥只在 Python 服务端使用，不返回给客户端。"""
from __future__ import annotations

import json
import os
from dataclasses import dataclass, field
from typing import Mapping
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit
from urllib.request import Request, urlopen


class ModelError(RuntimeError):
    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code

    def detail(self):
        return {"code": self.code, "message": str(self)}


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
            valid_url = parsed.scheme in ("http", "https") and parsed.hostname and not (
                parsed.username or parsed.password or parsed.query or parsed.fragment)
            timeout = float(env.get("LLM_TIMEOUT_SECONDS", "70"))
            chat_tokens = int(env.get("LLM_CHAT_MAX_TOKENS", "1000" if local else "2400"))
            plan_tokens = int(env.get("LLM_PLAN_MAX_TOKENS", "1100" if local else "1800"))
            valid_numbers = 1 <= timeout <= 70 and 128 <= chat_tokens <= 8000 and 128 <= plan_tokens <= 8000
        except (ValueError, TypeError):
            valid_url = valid_numbers = False
        mode = env.get("LLM_JSON_MODE", "json_object").strip()
        if not valid_url or not valid_numbers or mode not in ("json_object", "json_schema") or not model.strip() or (not local and not key.strip()):
            raise ModelError("MODEL_CONFIG", "模型配置不完整或不合法，请检查服务端地址、模型名、密钥、输出预算和超时配置。")
        return cls(provider, base.rstrip("/"), model.strip(), key.strip(), timeout, chat_tokens, plan_tokens, mode)


def model_info():
    """仅报告配置，不声称已连接供应商；不暴露密钥或私有地址。"""
    try:
        settings = ModelSettings.from_env()
        return {"provider": settings.provider, "model": settings.model, "configured": True}
    except ModelError:
        return {"provider": os.getenv("LLM_PROVIDER", "ollama"), "configured": False}


def generate_text(prompt: str, max_tokens: int, structured=False, settings: ModelSettings | None = None) -> str:
    settings = settings or ModelSettings.from_env()
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
        # 不读取或传播供应商错误正文，防止错误内容含密钥、地址或用户文本。
        if exc.code in (401, 403):
            error = ModelError("MODEL_AUTH", "模型服务认证失败，请检查服务端 API 密钥和权限。")
        elif exc.code == 429:
            error = ModelError("MODEL_BUSY", "模型服务限流或额度不足，请稍后重试或检查账户额度。")
        elif exc.code in (400, 404, 422):
            error = ModelError("MODEL_CONFIG", "模型服务不接受当前请求，请检查模型名、接口地址和 JSON 模式。")
        else:
            error = ModelError("MODEL_UNAVAILABLE", "模型服务暂不可用，请稍后重试。")
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
