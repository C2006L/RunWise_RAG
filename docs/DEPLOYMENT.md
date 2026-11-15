# RunWise 单服务器部署与验收

本阶段交付部署文件，不代表生产环境已经上线。不会自动导入 SQL、建立管理员或重建索引；不读取开发机密钥到服务器。当前是单实例方案，不适合直接扩为多副本。

本次发布范围为Web跑者端、Web管理端及共享后端服务，不部署小程序。微信AppID/Secret、小程序真机与跨端回归不作为本次Web上线条件。小程序检查已拆到手动工作流，不阻塞Web流水线。

## 组件与入口

`compose.yaml` 管理 MySQL、Redis、Spring Boot、Python RAG、Nginx。可选 `tls` profile 启动 Caddy，自动申请 HTTPS 证书。Web 在 `/`，管理端在 `/admin/`。构建强制真实 API，GitHub Pages 的 Mock 演示站与这套部署分开。

默认只有 `127.0.0.1:8088` 提供 HTTP，供本机或 SSH 隧道检查。公网使用 tls profile 的80/443；MySQL、Redis、Java、Python均无宿主机端口。RAG异常不影响基础业务启动。后端就绪检查要求数据库、Redis和11张核心表存在，**不代替所有列与约束的迁移验证**。RAG健康检查验证索引及模型配置，不调用 GLM，不能证明供应商实时可用。

宿主机需要 Linux、Docker Engine、Compose v2；执行备份脚本另需Python3标准库。运行时Java、Python和Nginx在镜像内，无需IDEA、Node、Maven或Ollama。需预留BGE模型、PyTorch镜像、索引及图片空间。实际内存/CPU容量需要在目标服务器测量，不把当前单元测试视为容量结论。

## 1. 配置与构建

以下命令从仓库根目录执行：

```sh
cp deploy/.env.example deploy/.env
chmod 600 deploy/.env
# 编辑 deploy/.env，填入服务器独立密码、至少32字符随机JWT密钥和GLM标准API Key。
docker compose --env-file deploy/.env config --quiet
docker compose --env-file deploy/.env build
```

不要将真实 `.env` 提交Git。不要把开发机数据库密码、旧截图泄露的API Key用于生产；如果实际使用过已泄露密钥，先在对应平台更换。密码建议使用足够长的随机字母数字，避免手动转义错误。`docker compose config`不加`--quiet`会展开敏感环境变量，勿截图公开。

`RUNWISE_ALLOWED_ORIGINS`填写真实HTTPS域名（多个以逗号分隔），`DOMAIN`只填域名。微信端真实登录需填AppID、Secret及微信后台合法HTTPS域名。IP测试并不等于小程序上线验收。

网络默认使用 `172.30.41.0/24`。若与服务器已有网络冲突，需同时修改Compose子网和nginx.conf里的受信代理网段。不要不加限制地信任公网X-Forwarded-For。

## 2. 数据库：先导入经过验证的完整数据库

```sh
docker compose --env-file deploy/.env up -d mysql redis
```

此时只创建空库及专用账号，不导入业务表。已有开发库应在本机完成迁移检查，再导出完整SQL到受保护文件；其中包含账号哈希和用户数据，勿提交Git。使用MySQL8兼容的mysqldump，单库导出不使用`--databases`，不要携带`CREATE DATABASE`、`USE`或`DROP DATABASE`，以免导入错误库。

Linux服务器导入经审核的单库SQL：

```sh
docker compose --env-file deploy/.env exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot "$MYSQL_DATABASE"' < /secure/runwise.sql
```

不要在已有生产库运行 `injury_init.sql`，该文件包含破坏性重建。仓库增量脚本也不等于可从零开始的完整基线。目前还没有版本化自动迁移体系；全新业务库需先准备完整基线或审核后的完整导出。修改 `.env` 不会修改已经初始化的MySQL用户密码，必须通过数据库管理流程另行更换。

管理员使用现有初始化工具建立，勿保留默认测试密码。应用始终使用 `runwise_app`，不使用root。

## 3. RAG资源：选择迁移或首次构建

2026-11-15候选环境已升级至Chroma1.5.9及维护版本依赖。Compose开启`RAG_VALIDATE_INDEX_SCHEMA=true`，只校验现有结构，不在API启动时自动迁移。旧索引升级前必须停止RAG，并使用候选镜像显式备份迁移：

```sh
docker compose --env-file deploy/.env stop rag
docker compose --env-file deploy/.env run --rm --no-deps rag python code/migrate_index.py \
  --apply --confirm-service-stopped --backup-dir /app/chroma_db/schema-backups/before-upgrade
```

该备份目标应首次使用且保留，不能位于runwise索引子目录中。先把冷备复制到另一台机器，再处理正式索引；不要对仍在使用的索引执行。迁移失败停止发布，使用保留的旧镜像和冷备回退，不能让旧版Chroma继续打开已升级的索引。本机只对独立副本验证，649个文本块的ID、内容、metadata指纹升级前后相同，不能把该结果当作真实服务器已迁移。

已有完整备份时优先使用下文恢复流程。首次构建索引可运行：

```sh
docker compose --env-file deploy/.env run --rm --no-deps rag python code/build_index.py
```

此脚本会下载BGE嵌入模型，并**删除/重建命名collection**，只能首次初始化或在维护窗口、有备份时显式执行。不要在RAG运行时重建。默认知识来源是镜像内 `data/runwise`；管理员发布到MySQL的文章尚不会自动进入索引。

模型存在独立 `rag_models` 数据卷、索引存在 `rag_index` 数据卷，启动不自动重建；更新知识时需要管理版本、备份并重新验收。初次构建需要访问模型下载站点，GLM调用需要服务器能访问配置的API域名。

## 4. 启动与HTTPS

```sh
docker compose --env-file deploy/.env up -d
docker compose --env-file deploy/.env ps
curl -f http://127.0.0.1:8088/healthz
```

将域名DNS解析到服务器，防火墙允许80/443，确认域名和端口归当前服务使用，再运行：

```sh
docker compose --env-file deploy/.env --profile tls up -d
```

已有Nginx/Caddy入口时可不启用tls profile，由已有入口转发本机8088，且需将读取超时设为至少110秒。不要启动两套服务抢占80/443。不要将Python5000或数据库端口直接开放公网；Python内部接口没有单独的用户鉴权。

Nginx目前提供按IP的登录限流和AI频率/并发限制，**不等于按用户配额或分布式限流**，共享出口用户可能共同触发限制。应按小范围试用实测调整。

## 5. 持久化、备份与恢复

命名卷包含MySQL、Redis AOF、上传图片、RAG模型及索引、TLS证书。容器重建保留卷，但不要运行 `down -v`、盲目volume prune；数据卷不是异机备份。

```sh
python3 deploy/backup.py backup
python3 deploy/backup.py verify deploy/backups/实际备份目录
```

备份是短维护窗口：暂停正在运行的gateway/backend/rag，导出MySQL，打包图片、模型和索引，生成SHA256，最后恢复原本运行的这几个服务；MySQL/Redis保持运行。中断或报错可能留下不完整目录，只有含manifest且验证通过的备份可用。人工确认停写期间没有其他程序直连数据库；不要并发执行备份。备份目录应定期复制到另一台机器或受控存储，设置保留周期并监控失败。脚本本身尚未配置定时任务。

Redis登录会话不包含在业务备份中：灾难恢复后要求用户重新登录；TLS证书可由Caddy重新申请，需考虑平台签发限额。

**只恢复到新服务器/隔离的新实例**：准备同一应用版本及新 `.env`，启动空MySQL/Redis；不要启动backend/gateway/rag。使用同一DB_USERNAME或在审核SQL后调整应用账号。执行：

```sh
python3 deploy/backup.py restore /secure/备份目录 --confirm-new-empty-instance
docker compose --env-file deploy/.env up -d
```

脚本会拒绝非空数据库、非空图片/模型/索引目录，验证校验和及压缩包路径；恢复失败不会自动回滚部分导入，应保留失败实例排查，重新准备空实例。只接受你自己保管的可信备份，校验和不是数字签名。恢复后依次核对账号、记录数量、图片、问答和统计。**校验和验证不等于已经完成恢复演练。**

## 6. 发布、故障与回退

先在测试服务器运行同版本SQL与真实业务回归，再发布。发布前记录commit、镜像ID、数据库结构和知识库版本，并生成备份。当前Dockerfile使用版本tag及部分范围依赖，尚未达到digest/完整依赖锁定，最终发布需保存构建产物及版本清单。

```sh
docker compose --env-file deploy/.env ps
docker compose --env-file deploy/.env logs --tail=100 backend
docker compose --env-file deploy/.env logs --tail=100 rag
```

日志做容器级10MB×3轮转。后端prod关闭SQL参数输出及DEBUG、禁用API文档、限制跨域；网关拒绝公开内部健康接口。健康变为unhealthy本身不会触发Docker自动重启；`unless-stopped`负责进程退出及宿主机重启后的恢复。需另行接入告警和处理流程。

应用回退使用上一版已保留镜像；如果修改了数据库结构，不可只回退Java镜像就认为恢复完成。不要自动覆盖生产库。正式开放前还需图片归属/私密访问、用户级AI额度、账号恢复及管理端知识索引同步等业务加固。

## 本阶段验收清单

- [ ] 本机/CI Docker Compose配置校验、三镜像构建、nginx -t通过。
- [ ] 全新环境导入数据库后，所有服务健康；空库被标记未就绪。
- [ ] Web和管理端使用真实API，刷新子路由不404。
- [ ] 登录、双端同号、打卡编辑、图片大图、问答历史、计划确认、统计通过。
- [ ] RAG停止时普通打卡可用；Redis停止时就绪失败，恢复后可重新登录。
- [ ] 容器/服务器重启后图片、记录、索引保留。
- [ ] 对外仅开放预定入口，文档和/internal不公开。
- [ ] 在另一套空环境完成备份恢复演练；确认回退路径。
- [ ] 真实微信域名、HTTPS、绑定与跨端回归完成。

未勾选项目不应解释为已经验证。
