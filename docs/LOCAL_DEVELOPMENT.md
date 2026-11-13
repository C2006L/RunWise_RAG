# RunWise 本地开发配置

更新于 2026-11-12。先做只读检查，不要执行 `injury_init.sql`（会删除文章表）。

## 推荐启动入口（IDEA 也可直接运行）

将 `RunWise_Backend/.env.example` 复制为同目录 `.env`，填写实际数据库专用账号、密码、JWT_SECRET 和 Redis 端口（本机常用6380，须与实际映射一致）。不要覆盖已有 `.env`。该文件被 Git 忽略。启动器自动读取它；已设置的终端／IDEA 环境变量优先。

在项目根目录分别运行以下命令，每个服务使用独立终端：

```powershell
.\venv\Scripts\python.exe code\dev.py check
.\venv\Scripts\python.exe code\dev.py backend
.\venv\Scripts\python.exe code\dev.py rag
.\venv\Scripts\python.exe code\dev.py web
.\venv\Scripts\python.exe code\dev.py admin
```

`check` 检查变量是否齐备、MySQL/Redis TCP、用当前账号只读查询表字段和 RAG 健康状态。不会运行迁移或轮换密码，不输出密码和数据库错误原文。Redis TCP 成功不等同于认证成功；RAG health 也不等同于模型实际推理成功。MySQL JDBC URL 覆盖模式需人工核查连接和结构。

IDEA：现在可以直接运行 `RunWiseApplication`。工作目录设为项目根目录或 `RunWise_Backend`；启动前会读取 `RunWise_Backend/.env`（模块目录启动时读取 `.env`）。已有终端／IDEA 环境变量优先，如果里面保留了旧密码或旧端口，请删除旧值或同步更新。该读取器面向单行本机配置，不执行命令或变量插值。原有 Python 启动器仍可使用。缺少本机配置文件时不会自动生成密码或放宽校验。

## 数据库版本核对与验收顺序

已有数据库先备份，再按缺失结构决定迁移，禁止全量重跑。顺序为 `admin_v1.sql` → `business_v2.sql` → `ai_planning_v3.sql` → `badge_v4.sql`。前三份含非幂等 DDL；部分执行失败时须检查已完成的字段／索引，不要从头重跑。当前先提供只读核查，尚未引入自动版本化迁移工具。

伤病模块代码已经包含 tags/sections、VO、分类列表。启动后验收：

```powershell
Invoke-RestMethod http://127.0.0.1:8081/api/injury/categories
Invoke-RestMethod http://127.0.0.1:8081/api/injury/articles?category=knee
# 将下面的1替换成列表返回的真实已发布文章ID
Invoke-RestMethod http://127.0.0.1:8081/api/injury/articles/1
```

再验证管理端发布后用户端可见、下架后不可见，以及空分类状态。自动化 MVC 测试不代替真实 MySQL 和三端操作验收。

真机小程序地址在 `RunWise_MiniProgram/.env.development.local` 中设置 `VITE_API_BASE_URL=http://电脑当前局域网IP:8081`，然后重新编译；不修改公共 request.ts。

### 训练计划进度验收

计划列表现在按当前用户同日未删除打卡的距离汇总判断里程进度：无有效里程为待执行，有里程但未达目标为已记录，达到目标为里程达标。未来打卡不参与，休息日保持休息。编辑、删除打卡或修改目标后，再次读取计划时重新计算，不持久化第二份完成状态。

目前打卡未记录训练课型与间歇执行情况，因此“里程达标”不代表节奏／间歇等任务已完成；暂不显示 MISSION COMPLETE。真实联调时应验证同日其他用户记录不影响状态，删除／降低打卡距离后状态回退。后续若需要完整任务确认，应先明确课型、配速与任务关联规则。

## 1. 为什么不能把密码写进 application.yml

`application.yml` 会进入 Git 历史，因此数据库密码、JWT 密钥、微信 Secret 和 OSS 密钥都必须通过环境变量注入。仓库中的 `RunWise_Backend/.env.example` 只用于说明需要哪些变量，Spring Boot 不会自动读取该文件。

已经提交过的密码即使从最新代码删除，仍可能存在于 Git 历史中。请先在 MySQL 中轮换旧密码，再使用新密码启动项目。

## 2. PowerShell 启动后端

以下变量只对当前 PowerShell 窗口生效：

```powershell
$env:SERVER_PORT = "8081"
$env:DB_HOST = "localhost"
$env:DB_PORT = "3306"
$env:DB_NAME = "runwise"
$env:DB_USERNAME = "runwise_app"
$env:DB_PASSWORD = "你的专用账号密码"
$env:JWT_SECRET = "至少32个字符的随机开发密钥"
$env:REDIS_HOST = "127.0.0.1"
$env:REDIS_PORT = "6380"
$env:RAG_BASE_URL = "http://127.0.0.1:5000"

cd D:\RunWise\RunWise_Backend
mvn spring-boot:run
```

`MYSQL_URL` / `MYSQL_USERNAME` / `MYSQL_PASSWORD` 仍可作为高级覆盖项；日常开发统一使用上面的 `DB_*`，避免变量名混淆。RunWise 使用 8081，是为了避开本机另一个项目占用的 8080。

请勿把包含真实值的 PowerShell 脚本提交到仓库。

## 3. 启动 Python Chroma RAG

首次运行先安装依赖：

```powershell
cd D:\RunWise
.\venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

随后启动 RAG 服务：

```powershell
python code\rag_api.py
```

服务启动后访问 `http://127.0.0.1:5000/health`，应看到 `status: ok`、Chroma 文本块数量和当前 Ollama 模型。RAG 服务使用已有的 `chroma_db/runwise` 与本机 Ollama，不需要把云端 API Key 交给 Spring Boot。

## 4. 启动 Web

```powershell
cd D:\RunWise\runwise-web
npm install
npm run dev
```

当前 Web 开发环境已设置 `VITE_USE_MOCK=false`，使用真实密码登录与业务接口。GitHub Pages 的静态演示另外由工作流开启 Mock，不能作为真实后端验收依据。

## 5. 当前认证差异

| 能力 | Web 期望 | Spring Boot 当前实现 |
| --- | --- | --- |
| 登录 | 用户名 + 密码 | 已有密码注册/登录；微信 code 登录另走微信接口 |
| 登录路径 | `/api/auth/login-password` | 已实现 |
| 刷新 Token | Authorization Header | 已对齐 |
| 退出 | `/api/auth/logout` | 已实现 |
| 用户凭证 | username/password | 独立凭证表、PBKDF2 哈希，微信通过 openid 关联 |

上述为代码能力；真实微信配置、当前数据库迁移状态及双端完整回归仍须在运行环境核验。

## 6. 当前阶段验收

- `application.yml` 不包含真实密码或固定密钥。
- 未设置 `DB_PASSWORD`（或 `MYSQL_PASSWORD`）、`JWT_SECRET` 时，后端不会以不安全默认值启动。
- 设置环境变量后，后端能够正常编译和启动。
- 使用真实账号完成登录、刷新、退出及双端数据回归；不以 Mock 登录通过代替验收。
