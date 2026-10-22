# RunWise RAG 服务

RunWise RAG 为 Web 端和小程序端的 AI 答疑提供跑步领域知识检索。它以本地 Markdown 知识库为依据，先召回相关章节，再把证据交给大模型生成回答。

## 当前检索流程

1. 按 Markdown 标题切分文档，保留文章标题、章节路径、分类和来源文件。
2. 使用 `BAAI/bge-small-zh-v1.5` 做中文语义召回。
3. 使用字符 bigram BM25 补充关键词召回。
4. 根据问题类型、训练目标和伤病部位进行路由与重排。
5. 通过相关性阈值和领域判断过滤弱相关内容；非跑步问题不会强行拼接知识库。
6. 将最多 3 段高质量证据发送给生成模型，并返回可追溯的来源信息。

当前 Chroma 集合名为 `runwise_knowledge_v2`。旧集合不会在重建时被删除，但服务只读取 v2 集合。

## 目录

```text
data/runwise/              Markdown 知识库，按领域分目录
chroma_db/runwise/         Chroma 持久化目录（由脚本生成）
code/build_index.py        索引构建脚本
code/rag_api.py            提供给 Spring Boot 的 FastAPI 服务
code/test_rag_retrieval.py 检索回归测试
code/app.py                独立 Streamlit 调试界面
```

## 环境准备

在项目根目录执行：

```powershell
.\venv\Scripts\Activate.ps1
Copy-Item .\code\.env.example .\code\.env
```

`.env` 中与检索有关的配置：

```dotenv
OLLAMA_BASE_URL=http://127.0.0.1:11434
OLLAMA_MODEL=qwen2.5:3b
RAG_HOST=127.0.0.1
RAG_PORT=5000
RAG_TOP_K=3
RAG_RECALL_SIZE=12
RAG_MAX_VECTOR_DISTANCE=0.82
```

- `RAG_TOP_K`：最终交给模型的证据数量。
- `RAG_RECALL_SIZE`：语义检索和关键词检索的候选规模。
- `RAG_MAX_VECTOR_DISTANCE`：Chroma cosine distance 上限，越小越严格。

Ollama 和 `qwen2.5:3b` 仅适合本地联调。上线时可以替换生成模型供应商，检索与知识库部分无需随之重写。

## 重建向量索引

修改 `data/runwise/` 中的知识文档后，在项目根目录执行：

```powershell
.\venv\Scripts\python.exe .\code\build_index.py
```

脚本会重新创建 `runwise_knowledge_v2`，并保留其他 Chroma 集合。目前知识库包含 21 个 Markdown 文件，构建结果约 649 个分块；文档变化后数量可能不同。

## 启动 RAG API

先确保 Ollama 已运行并已安装配置中的模型，然后执行：

```powershell
.\venv\Scripts\python.exe .\code\rag_api.py
```

健康检查：

```powershell
Invoke-RestMethod http://127.0.0.1:5000/health
```

正常响应会包含：

```json
{
  "status": "ok",
  "indexReady": true,
  "collection": "runwise_knowledge_v2"
}
```

Windows 首次加载 BGE 模型可能需要几十秒；服务启动完成后再测试问答。

## 运行检索回归测试

```powershell
.\venv\Scripts\python.exe .\code\test_rag_retrieval.py -v
```

测试覆盖：

- 5K 训练问题不会混入 10K 计划。
- 膝外侧疼痛优先召回髂胫束相关章节。
- 扁平足跑鞋问题召回装备知识。
- Spring Boot 等非跑步问题不返回伪相关上下文。

注意：不要使用 `python -m unittest code.test_rag_retrieval`，因为项目目录名 `code` 会与 Python 标准库模块 `code` 冲突。

## 调试建议

如果检索来源正确但回答仍然编造用户里程、疼痛评分或比例，通常是本地小模型的指令遵循能力不足，而不是向量库没有命中。排查时应分开验证：

1. 先查看检索结果或运行测试脚本。
2. 再检查最终生成回答。
3. 生产环境更换模型后，使用同一组问题重新做回答质量评测。

伤病回答仅用于一般跑步知识参考，出现持续或剧烈疼痛时应建议用户停止训练并寻求专业医疗评估。
