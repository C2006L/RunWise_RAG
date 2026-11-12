# RunWise 管理端（第一批）

独立 Vue 3 管理站点。包含管理员认证、文章列表/筛选、分节编辑、预览、发布/下架、超级管理员操作日志。用户管理、RAG 文档管理和评测属于后续批次，当前没有放置占位菜单或假数据。

## 1. 数据库安装（必须在启动新版后端之前执行）

先备份现有 `runwise` 数据库。在 IDEA Database、MySQL Workbench 或 MySQL 命令行中，以有建表/改表权限的数据库账号执行：

```sql
USE runwise;
SOURCE D:/RunWise/RunWise_Backend/sql/admin_v1.sql;
```

图形工具中可直接打开 SQL 文件运行，不需要执行 `SOURCE`。

脚本创建 `t_admin_account`、`t_admin_audit_log`，给文章表增加 `status`、`revision`、`updated_by`。旧文章默认保持 `PUBLISHED`。此迁移只能执行一次；遇到 duplicate column 时应先检查现有列，不要删除文章表。**不要执行旧的 `injury_init.sql`，它会删除并重建文章表。**

应用账号需要对新增表具有 SELECT/INSERT/UPDATE 权限；DDL 使用单独的数据库管理账号。新版后端文章查询依赖新增列，未迁移时文章接口会报错。

## 2. 首次创建管理员（仅执行一次）

继续在 IDEA 的 `RunWiseApplication` 运行配置中保留原有 MySQL、Redis、JWT 环境变量，另外临时添加：

- `ADMIN_USERNAME`：自己设置的管理员登录名，3–64 位英文/数字/下划线/点/横线。
- `ADMIN_PASSWORD`：自己设置的 12–128 位密码。

在同一运行配置的“程序参数 / Program arguments”中临时添加：

```text
--runwise.admin.bootstrap=true
```

运行后端。启动成功后，初始超级管理员已创建。随后停止后端，**删除这个程序参数和 ADMIN_USERNAME / ADMIN_PASSWORD 两个环境变量**，再正常启动。初始化不会覆盖已有账户；如果忘记关闭参数，下一次启动会明确拒绝初始化。

管理员密码采用独立随机盐和 PBKDF2 存储，不存在默认管理员密码。管理员 Token 与跑者 Token 隔离，Redis 会话有效期沿用配置中的 access-token-expire；退出或会话过期后重新登录。同账号重新登录会使旧会话失效。

## 3. 启动管理端

```powershell
cd D:\RunWise\runwise-admin
npm ci
npm run dev
```

打开 `http://127.0.0.1:5274`。默认开发代理转发到后端 `http://127.0.0.1:8081`；如后端端口不同，请调整 `vite.config.js` 的 proxy target。

IDEA 中也可新增 npm 运行配置，package.json 选择 `runwise-admin/package.json`，脚本选 `dev`。用户端原有 npm 运行配置继续独立使用。

构建：`npm run build`。部署时由 Web 服务器将 `/api/admin` 反向代理至 Spring Boot；也可在构建前通过 `VITE_API_BASE_URL` 指定完整 API 地址。前端使用 Hash 路由。

## 4. 文章与日志规则

- 新建默认是草稿，仅后台可见。
- 编辑保存后转为草稿；这会暂时隐藏原先已发布的文章，审核后再点击发布。第一版不保留线上/草稿双副本，操作前页面会提示此规则。
- 发布后用户端文章列表和详情均可读取；下架后列表与详情均不再开放。
- 保存和发布使用 revision 并发检查，过期版本会被拒绝，需重新加载再编辑。
- 文章变更和操作日志位于同一数据库事务中；日志失败时文章操作回滚。
- 仅 SUPER_ADMIN 可查看操作日志；CONTENT_EDITOR 可维护文章。第一版通过初始化创建超级管理员，暂未提供管理员增删与角色配置界面。
- 预览以纯文本渲染，不执行文章 HTML；延续现有分节正文数据格式。

## 5. 验收步骤

1. 登录后新建一篇测试草稿，确认用户端列表和详情都不可见。
2. 编辑各字段、增加/移除小节、预览，再保存并发布；确认 Web 和小程序显示同一内容。
3. 下架该文章，确认用户端列表和详情都不可见。
4. 两个浏览器标签打开同一篇文章，分别编辑，确认第二次保存会提示版本冲突。
5. 查看操作档案，核对创建、编辑、发布、下架记录。
6. 退出后访问后台接口，确认需要重新登录；用跑者 Token 访问 `/api/admin/articles` 应返回 401。
7. 分别在 1366、1440、1920 宽度检查列表、编辑器、预览与键盘焦点；窄屏表格允许独立横向滚动。

自动检查：在 `RunWise_Backend` 执行 `mvn test`，在 `runwise-admin` 执行 `npm run build`。单元测试不连接真实 MySQL/Redis，不代替上述实际数据库联调。
