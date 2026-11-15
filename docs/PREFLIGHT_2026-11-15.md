# 发布前检查执行记录（2026-11-15）

结论：**未放行生产部署**。本轮只在开发机修改、构建、审计和验证，没有连接服务器、清理数据卷或改动真实账号/训练记录。已有工作区修改保留，尚未创建发布commit或推送GitHub。

范围更新：按用户2026-11-15要求，仅部署Web（含现有Web管理端）及Java/MySQL/Redis/Python RAG。小程序不部署、不再继续修改，也不作为Web门禁；已拆为手动维护工作流，保留已有改动和历史检查结果。

## 已执行及修复

1. 正式RAG改用独立的 `deploy/requirements-rag.txt`，不再安装Streamlit、FAISS等HTTP服务不需要的直接依赖。CPU PyTorch与CI统一；按审计对同主版本可修复项更新依赖，不强制升级Chroma/LangChain到另一代。
2. 上传接口在5MB限制之外真实解码JPEG/PNG/WebP，限制2000万像素，重新编码而非直接保存声明为图片的任意字节；清除原始元数据。新URL按用户目录分区，创建/换图时检查本人的文件及路径；本人旧记录不换图时兼容旧URL。不新增数据库表。
3. WebP保留解码支持，并新增PNG/JPEG/WebP、伪造文件、跨用户绑定、外链及路径穿越回归。
4. Web升级到ECharts6.1与Vite6.4系列，修复官方npm审计发现；新增离散数据的SVG图表渲染回归。管理端与小程序只执行兼容范围内的audit fix，不使用force。
5. 增加Ruff正确性门禁，不做全仓格式重排；CI增加依赖审计，风险未消除时应失败，不通过continue-on-error掩盖。
6. 新增 `code/release_manifest.py`：记录commit、dirty状态、源码与产物SHA256，识别旧JAR；显式 `--require-clean` 可阻止脏工作区或旧JAR作为发布版本。清单不证明安全或服务器容量。
7. Nginx新增普通图片上传频率及并发限制；保留此前AI与登录限制。

## 已获得的验证证据

- Java全套回归通过（另有真实Redis集成用例因未启用条件跳过，不能当作真实Redis已验证）。上传改动另有专项回归。
- Web构建/21项测试、管理端构建、小程序7项测试/类型检查/微信构建通过。ECharts渲染回归不等于浏览器交互或截图验收。
- Python已有生成/HTTP契约/识图测试、备份校验测试通过；Ruff正确性检查通过。备份校验不等于实际恢复演练。
- 官方npm审计：Web与管理端修复后为0；小程序由79项（含1项严重）降为75项（27高、20中、28低），无严重项，作为独立维护事项，不阻塞本次Web发布。uni-app框架主版本保持原样。
- 本机Python开发环境审计：26个包含审计发现，包含未部署的开发工具；不能直接等同于正式镜像风险。首版正式RAG镜像单独审计发现9个包/117条记录，其中存在重复漏洞ID，不能解释为117个独立可利用漏洞。原始报告在被Git忽略的 `.cache/preflight/`；更新后必须对最终镜像重新审计。
- 安全补丁版正式RAG镜像已构建，使用模型/索引副本的离线检索及30项AI相关测试通过。补丁后复审仍有5个包/64条审计记录（含重复ID）；需逐项分析，不能宣称风险清零。实际版本清单位于.cache/preflight/rag-patched-freeze.txt。
- 公开文章列表本机短测：30次请求、3并发、0错误、P95约325ms。不是10分钟稳定性测试，不代表目标服务器性能。
- 网关镜像构建在拉取Node基础镜像时因Docker Hub认证地址连接超时失败。之后单独成功拉取Nginx镜像并通过实际 `nginx -t`，但不等于网关完整构建或全Compose运行通过。
- 对本机当前3个有效凭证值进行Git历史精确匹配，检查706个不超过2MiB的blob，未匹配；不输出凭证值。该检查不覆盖已轮换密钥或所有模式，不能代替完整Gitleaks扫描。

## 剩余阻塞项与风险分级

| 项目 | 必须完成的验收 |
|---|---|
| Python剩余依赖风险 | 扫描最终镜像并去重；判断实际调用路径，尤其Chroma服务端鉴权、LangChain模板/反序列化、Starlette文件/表单接口。本项目不公开Chroma HTTP服务，不接受客户端序列化配置，Python端只接收内部JSON；这些限制只能辅助评估，不能自动豁免全部发现。框架升级需用索引副本测试并保留迁移/回退方案 |
| 图片私密性 | 当前/uploads仍是公开静态链接。归属校验解决绑定问题，不解决图片私密访问；需要落实签名/认证读取或明确的公开照片产品策略 |
| 上传资源管理 | 孤立文件、总容量配额、EXIF方向视觉兼容尚需专项验收；重新编码可能改变输出大小 |
| 真实全链路 | Web双账号/多标签页、计划并发与AI故障演练尚未在浏览器逐一验收；小程序和微信绑定排除在本次范围之外 |
| 发布环境 | Linux正式后端/网关镜像构建、Nginx检查、Compose完整启动、HTTPS及重启验证 |
| 5GB容量 | 必须测全栈及更新峰值，保留运行余量；单个精简镜像成功不等于全栈装得下 |
| 数据库与恢复 | 完整基线/增量顺序、异机真实恢复、回滚兼容、定时备份与告警 |
| 扫描与规范 | Java依赖/镜像漏洞扫描、Gitleaks历史密钥扫描、完整前端Lint/Java静态规则仍需完成；Ruff基础检查不能替代 |
| 发布版本 | 审查所有累积差异，固定commit及镜像摘要；不得直接发布.cache实验镜像或只复制旧dist |

## 可重复执行的命令

在各自子目录使用现有npm脚本；仓库根目录没有npm项目，不在根目录执行npm test。

```powershell
cd D:\RunWise\RunWise_Backend
mvn test
mvn -DskipTests package
cd D:\RunWise\runwise-web
npm test
npm run build
npm audit --package-lock-only --registry=https://registry.npmjs.org
cd D:\RunWise\runwise-admin
npm run build -- --base=/admin/
cd D:\RunWise\RunWise_MiniProgram
npm test
npm run type-check
npm run build:mp-weixin
cd D:\RunWise
.\venv\Scripts\python.exe code\release_manifest.py
```

正式发布前从已审核commit干净检出，重新构建、跑门禁并执行 `release_manifest.py --require-clean`。不要为了让报告变绿删除测试、跳过安全门禁或清理业务数据。
