# AI 模型切换与排错

## GLM-4V-Flash 试用配置

本机已按请求试切换到 `glm-4v-flash`，原 API Key 保留。配置模板为 `code/.env.glm4v.example`，问答／计划预算均900，`LLM_THINKING=default` 表示不发送思考参数，JSON仍用 `json_object`。适配层会拒绝超过1024的输出预算及残留的思考参数，避免沿用4.7配置而请求失败。

官方[模型概览](https://docs.bigmodel.cn/cn/guide/start/model-overview)标注上下文16K、最大输出1K。本次最小文本和JSON请求均HTTP200；仅证明当时接口可用，不代表长回答质量、长上下文和复杂计划均已通过。输出达到上限时仍拒绝保存残缺内容，不静默截断或切换其他模型。当前页面仍是文本问答，换视觉模型不会自动新增图片提问功能。

补充实测：`smoke_ai_plan.py` 使用标注为测试的摘要完成7天计划，日期、次数、跑量边界检查通过；未写业务库。总耗时71.5秒，包含本地索引／嵌入模型冷加载，不能视为纯GLM接口耗时或持续性能保证。Chroma遥测兼容性警告未阻止本次检索与生成。

## 智谱 429 排查

HTTP 429 不能直接判断余额耗尽。服务现读取智谱响应中经过校验的4位数字业务码，并使用固定文案，不展示供应商原始正文：1305/1312为模型繁忙，1302/1303为限流，1308/1310为使用上限，1113为平台报告余额不足，1311/1315为模型或Key权限问题。未知429仅提示请求受限，不再推断充值需求。Java问答／计划接口会附带数字平台码便于排查；Python日志仅记录HTTP状态与数字码，不记录密钥或原始错误内容。当前没有新增自动重试，也不会切换到收费模型。

错误含义以[智谱官方错误码说明](https://docs.bigmodel.cn/cn/api/api-code)为准。2026-11-13最小诊断请求实际收到1305，属于当时模型繁忙；这不保证后续每次失败都是相同原因。更新后需重启Python及Spring Boot才能显示新提示。

## GLM 本机接入（2026-11-13）

配置模板见 `code/.env.glm.example`。当前本机 `code/.env` 已切换 `compatible`、地址 `https://open.bigmodel.cn/api/paas/v4`、模型 `glm-4.7-flash`、JSON 模式 `json_object`。请自行填写重新生成的 `OPENAI_API_KEY`，不要将密钥提交到 Git 或发到聊天。

本配置显式使用 `LLM_THINKING=disabled`，普通问答和计划生成均发送 `thinking: {type: disabled}`；其他供应商保持 `default` 时不发送该字段。此参数依据 [GLM 思考模式说明](https://docs.bigmodel.cn/cn/guide/capabilities/thinking-mode)，更换模型需重新核对支持情况。

填好后重启 Python：`venv\Scripts\python.exe code\dev.py rag`。若终端或 IDEA 已设置同名环境变量，请同步修改，因为进程环境优先。Spring 保持 `RAG_ENABLED=true`；仅更换模型时无需重启前端或迁移数据库。Ollama 不再参与当前问答生成，知识检索仍使用本地 BGE/Chroma。未填写 Key 时模型配置检查失败属于预期；真实网络连通和回答质量须填 Key 后验证。

Web / 小程序 → Spring Boot（身份、真实训练摘要、历史、草稿保存）→ Python（知识检索与生成模型）。两端 UI、历史表、计划确认流程不因更换模型而变化。

## 本地调试

在 `code/.env` 中保留以下配置。不要用示例文件覆盖已有的真实配置；只编辑对应变量。

```dotenv
LLM_PROVIDER=ollama
OLLAMA_BASE_URL=http://127.0.0.1:11434
OLLAMA_MODEL=qwen2.5:3b
LLM_TIMEOUT_SECONDS=70
```

问答默认输出预算1000 token，计划1100。本地仍需运行 Ollama 和已有模型。无需云端密钥。

## 切换上线用的 API

当前支持 Chat Completions 兼容协议；不是所有标有“AI API”的服务都支持同一协议。先向选定供应商确认基础地址、模型名称和 JSON 模式。

```dotenv
LLM_PROVIDER=compatible
OPENAI_BASE_URL=https://你的供应商地址/v1
OPENAI_API_KEY=填入你自己的服务端密钥
LLM_MODEL=填入供应商的真实模型名称
LLM_JSON_MODE=json_object
LLM_TIMEOUT_SECONDS=70
LLM_CHAT_MAX_TOKENS=2400
LLM_PLAN_MAX_TOKENS=1800
```

地址填写到基础路径，程序会追加 `/chat/completions`，不要填两次。示例占位值不能直接用于请求。密钥只放 Python 的本地配置或部署环境，不放前端、截图或 GitHub。

普通问答使用 `messages` / `max_tokens`，计划使用 JSON 输出。默认 `json_object`；只有供应商支持严格 JSON Schema 时才用 `json_schema`。不支持 JSON 输出的接口暂不适用于可保存计划。输出仍须通过候选检查和 Java 课表校验，供应商的 JSON 模式不能代替业务校验。

保持 Spring Boot 的 `RAG_ENABLED=true`，否则会走旧的 Java 本地 Ollama 直连路径。云模型通过 Python 使用，Java 不需要重复配置云密钥。正常模式下 Ollama 可以不启动；BGE 与 Chroma 仍负责本地检索。

修改后只需重启 Python（首次使用本轮 Java 修改也需重启 Spring Boot）：

```powershell
cd D:\RunWise
.\venv\Scripts\python.exe code\rag_api.py
```

Python 会自动读取 `code/.env`。若同名环境变量已在终端或 IDEA 运行配置中设置，环境变量优先，需要同步调整。后端仍可使用原来的 IDEA 配置启动，无须增加新的必填变量。

## 超时、历史与失败行为

- 模型连接/读取超时配置为1～70秒，Java 请求85秒，前端100秒。此处是各 HTTP 层的超时，不是整个含冷启动/检索的请求时限保证；索引首次加载仍可能耗时。
- 问答与计划共享模型配置，不会因云端认证、限流、超时或不支持格式而偷偷换成本地模型，也不自动重试产生额外请求。
- Java 的自动本地降级默认关闭。仅在本地开发确有需要时设置 `LLM_LOCAL_FALLBACK_ENABLED=true`；只对快速连接故障生效，模型服务返回的明确错误不降级。
- 认证失败、额度/限流、配置不匹配、网络超时和输出截断有独立错误代码，页面可显示具体原因。模型正文和供应商原始错误不会直接暴露给用户。
- 完成原因是 `length` 的回答或计划会拒绝保存；不会把半截答案当成功。缩小问题范围或在供应商支持范围内提高输出预算。仅加大超时时间不能解决输出预算不足。
- 最近三轮真实会话保持用户/会话隔离。同一时间戳按 ID 稳定排序；超长回答仅在发给模型时节选到4000字符，历史库中的原文保持完整。Python 不再把每轮回答二次截成600字。
- `/health` 会显示 `provider`、`model`、`configured`。`modelCheck=configuration_only` 表示只验证配置与索引，不代表模型已完成真实推理，也不自动消耗云 API 额度。

## 验证

### Web 运动截图识别

打卡页上传新图片后，点击「识别距离与配速」，核对结果再「确认填入表单」。此操作将截图发送给当前服务端模型，建议先裁掉姓名、位置等隐私信息。不会自动创建或修改打卡，不会写入问答历史。

链路：`POST /api/files/recognize-checkin`（使用现有登录认证）→ Python `/api/rag/image-metrics` → 当前兼容 API 多模态模型。密钥保留在 Python 服务端。只传图片字节，不接受任意图片 URL；真实解码校验 JPG/PNG/WebP，限制5MB、2000万像素，缩放并去除元数据后调用模型。本地纯文本 Ollama 配置不支持此入口。

提取距离、运动时长、平均配速；字段无法确认时为空，不生成训练数据。仅缺时长且距离、配速均识别时才估算时长，并标注。截图值有差异时提醒人工核对。沿用现有打卡存储规则：距离两位小数、时长整分钟，最终配速由这两项重新计算，可能与截图显示略有差异。识别支持预览/取消，切换日期或图片后的旧结果不会填回新记录。

更新后需重启 Spring Boot 与 Python RAG，刷新 Web。Pillow 已加入根目录 requirements.txt；如缺失可执行 `venv\Scripts\python.exe -m pip install "Pillow>=10,<13"`。当前入口接在 Web 打卡表单，小程序尚未添加识图按钮。

模型识图存在误读：一次人工合成图测试中正确读取5.2km、369秒/公里，但将32分钟误读成33分钟；一致性校验会提示核对。不可将此能力作为训练数据真实性验证。

补充实测：明确HH:MM:SS逐位读取规则后，同一合成图复测成功返回5.2km、32分钟、369秒/公里。不发送真实用户图片，不写业务库；该样例成功不代表所有截图都能正确识别。

不连接真实模型、不消耗 API 额度的契约测试：

```powershell
.\venv\Scripts\python.exe code\test_llm_provider.py -v
.\venv\Scripts\python.exe code\test_ai_generation.py -v
.\venv\Scripts\python.exe code\test_ai_http_contract.py -v
```

可选 `code/smoke_ai_plan.py` 会调用当前选定的真实模型，云模式下会消耗供应商额度。只使用标记为测试的摘要，不写业务库。更换模型后仍要评估回答质量、时间限制遵循、计划说明与真实记录是否一致；接口通过不能代表建议质量。

协议参考：[DeepSeek Chat Completions](https://api-docs.deepseek.com/api/create-chat-completion/)、[Ollama generate](https://docs.ollama.com/api/generate)。兼容层按这两类请求/响应设计，其他供应商需单独联调。
