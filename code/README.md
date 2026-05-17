# RunWise RAG：面向大学生跑者的训练与恢复知识问答助手

## 项目简介

RunWise RAG 是一个基于 RAG（检索增强生成）技术的问答助手，面向大学生跑者群体，提供关于训练安排、恢复与营养、伤病预防、比赛准备等方面的专业建议。系统基于本地 Markdown 知识库进行检索，确保回答有据可依、来源可追溯。

## 技术栈

| 组件 | 技术选型 |
|------|---------|
| 框架 | LangChain |
| Embedding 模型 | BAAI/bge-small-zh-v1.5 |
| 向量数据库 | ChromaDB |
| 文本分割 | RecursiveCharacterTextSplitter |
| LLM | OpenAI-compatible API（DeepSeek / Kimi / 其他） |
| 前端界面 | Streamlit |
| 运行环境 | Python 3.11 |

## 知识库目录

```
data/runwise/
├── 训练基础.md
├── 训练计划.md
├── 恢复与营养.md
├── 伤病预防.md
├── 比赛准备.md
└── 大学生跑步指南.md
```

## 构建索引命令

```bash
cd code/runwise_rag
python build_index.py
```

构建完成后，向量库将保存到 `chroma_db/runwise/` 目录。

## 启动网页命令

```bash
cd code/runwise_rag
streamlit run app.py
```

首次运行前，请复制 `.env.example` 为 `.env` 并填入 API 配置：

```bash
cp .env.example .env
# 编辑 .env 填入你的 API Key
```

## 示例问题

- 我是跑步新手，想开始跑步，应该怎么安排训练？
- 跑完步后应该怎么拉伸和恢复？
- 跑步时膝盖外侧疼痛是怎么回事？该怎么处理？
- 我准备跑半程马拉松，赛前一周应该怎么调整？
- 考试周怎么平衡跑步和学习？
- 跑步后30分钟内应该吃什么？
- 什么是80/20训练原则？

## 实验亮点

1. **领域垂直**：知识库专门针对大学生跑者群体设计，涵盖训练、恢复、伤病、比赛等核心场景
2. **ChromaDB 持久化**：向量索引持久化存储，无需每次启动重建
3. **来源可追溯**：每个回答附带参考来源文件名和 chunk_id，体现 RAG 的可追溯性优势
4. **安全提示机制**：当知识库无足够依据时给出警告；涉及伤病时提示就医
5. **Streamlit 交互界面**：提供友好的网页聊天界面，支持多轮对话和参数调节
6. **中文 Embedding 模型**：使用 bge-small-zh-v1.5，对中文语义理解更准确
