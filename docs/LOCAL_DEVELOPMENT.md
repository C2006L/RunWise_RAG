# RunWise 本地开发配置

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

当前 `runwise-web/.env.development` 仍设置为 `VITE_USE_MOCK=true`，但伤病预防与 AI 问答通过各自开关连接真实后端。在密码登录接口完成前不要直接关闭全局 Mock，否则 Web 的 `/auth/login-password` 与当前微信登录接口不兼容。

## 5. 当前认证差异

| 能力 | Web 期望 | Spring Boot 当前实现 |
| --- | --- | --- |
| 登录 | 用户名 + 密码 | 微信 code 换 openid |
| 登录路径 | `/api/auth/login-password` | `/api/auth/login` |
| 刷新 Token | JSON 请求体 | Authorization Header |
| 退出 | `/api/auth/logout` | 尚未实现 |
| 用户凭证 | username/password | openid |

下一阶段需要增加经过 BCrypt 哈希的密码凭证、统一刷新 Token 契约，并为登录、刷新、退出编写集成测试；不能把微信 code 当作 Web 密码，也不能保存明文密码。

## 6. 当前阶段验收

- `application.yml` 不包含真实密码或固定密钥。
- 未设置 `DB_PASSWORD`（或 `MYSQL_PASSWORD`）、`JWT_SECRET` 时，后端不会以不安全默认值启动。
- 设置环境变量后，后端能够正常编译和启动。
- Web 暂时保留 Mock 登录，直到真实密码认证完成。
