# Web发布检查续办记录

范围仅Web、现有Web管理端及Java/MySQL/Redis/Python服务，小程序不部署。本轮未连接服务器、未修改homestock/Caddy、未清理任何现有容器或数据。

## 本轮完成

- RAG候选运行依赖升级：FastAPI0.163.1、Starlette1.10.0、Pillow13、LangChain core1.8.0/Chroma适配1.1.0/HuggingFace适配1.2.3、Chroma1.5.9。PyTorch保持上一批修复后的CPU2.13，镜像实测约2.51GB。
- 候选镜像构建通过；在无外网临时容器内运行10项生成/HTTP契约、6项识图、15项模型适配、4项检索测试，共35项通过。没有调用真实模型API。
- 索引副本冷备与迁移验证通过，前后649条的ID/文本/metadata指纹一致。真实索引未挂载或改写。
- 正式配置启用只校验索引结构，空索引健康状态为degraded。新增migrate_index.py，需显式确认服务停机、建立不覆盖旧备份的冷备后才能迁移。
- 候选镜像pip-audit剩5条记录，去重为4条Chroma HTTP服务端漏洞；其余之前发现的运行依赖记录不再出现。RunWise使用嵌入式PersistentClient，不运行Chroma HTTP服务，不接受请求指定模型仓库、trust_remote_code或租户配置。已在deploy/audit-policy.json记录限定版本/限定ID/部署条件及2026-12-15复核期限；不是声称Chroma无漏洞。
- 新增check_audit_report.py：未知漏洞、跳过审计、公开RAG端口或例外过期仍阻断。3项专项测试通过。保留原始审计JSON，不静默吞掉扫描结果。
- Ruff正确性检查通过。正式后端镜像构建仍因Docker Hub的Maven镜像认证地址连接超时失败；没有宣称全栈Compose通过。

## 服务器只读信息与结论

用户提供：x86_64、磁盘根分区50G/剩5.3G；RAM1.9GiB/available807MiB；Swap5.9GiB/已用775MiB；运行homestock应用和Caddy，Caddy已绑定80及3004。已用Swap不等于正在频繁换页，需要真实负载观察。

- 单个RAG副本测试峰值约594MiB，叠加Java、MySQL与现有应用后存在显著内存/Swap压力。它不是完整服务资源上限，不据此承诺2GB机器稳定承载全部服务。
- 磁盘不能按压缩包大小核算，必须包含数据库、日志、上传文件、新旧版本和导入临时空间。当前尚未完成全栈与更新峰值实测，不直接部署。
- 不启动Compose独立tls profile与现有Caddy抢80/443。未来如复用容器内Caddy，需要审核现有配置及共享网络；容器内127.0.0.1不代表宿主机，不直接把它代理到宿主机loopback8088。
- 不把docker system df中的“可回收”当作删除授权；停止的容器/镜像可能属于其他项目。Docker显示可回收构建缓存约634MB，尚未清理。

## 仍未完成的放行条件

正式后端/网关镜像完整构建、隔离全栈运行与故障测试、浏览器全链路、图片私密读取策略、完整备份恢复演练、资源峰值与更新回退实测、最终commit及镜像摘要固定。保留前述发布阻塞，不宣布已上线或已满足稳定上线标准。
