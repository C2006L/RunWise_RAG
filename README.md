# RunWise RAG

> 面向大学生跑者的训练与恢复知识问答助手

## 项目简介

RunWise RAG 是一个基于检索增强生成（RAG）技术的智能问答系统，专注于为大学生跑者提供专业的训练指导、伤病预防、恢复建议和比赛准备策略。

## 功能特性

- **智能问答**：基于知识库的精准问答，避免幻觉
- **知识检索**：使用向量数据库进行语义相似度检索
- **专业领域**：覆盖训练计划、伤病预防、恢复营养、比赛准备等核心主题
- **现代 UI**：采用 Vercel/Linear 风格的暗黑模式界面

## 技术栈

- **LangChain**：RAG 流程框架
- **ChromaDB**：向量数据库
- **HuggingFace Embeddings**：文本向量化（bge-small-zh-v1.5）
- **Streamlit**：Web 界面
- **OpenAI API**：大语言模型

## 项目结构

```
RunWise_RAG/
├── code/                    # 核心代码
│   ├── app.py              # Streamlit 应用主文件
│   ├── build_index.py      # 向量索引构建脚本
│   ├── .env.example        # 环境变量示例
│   └── README.md           # 详细使用说明
├── data/                    # 知识库文档
│   └── runwise/            # Markdown 知识文件
├── chroma_db/              # 向量数据库（自动生成）
├── models/                 # 本地模型缓存
├── .streamlit/             # Streamlit 配置
├── requirements.txt        # Python 依赖
└── README.md               # 本文件
```

## 快速开始

### 1. 克隆仓库

```bash
git clone https://github.com/YOUR_USERNAME/RunWise_RAG.git
cd RunWise_RAG
```

### 2. 创建虚拟环境

```bash
python -m venv venv
# Windows
venv\Scripts\activate
# Linux/Mac
source venv/bin/activate
```

### 3. 安装依赖

```bash
pip install -r requirements.txt
```

### 4. 配置环境变量

```bash
cp code/.env.example code/.env
# 编辑 .env 文件，填入你的 API Key
```

### 5. 构建向量索引

```bash
cd code
python build_index.py
```

### 6. 启动应用

```bash
streamlit run app.py
```

## 知识库内容

| 文件 | 描述 |
|------|------|
| 训练基础.md | 跑步基础知识和入门指导 |
| 训练计划.md | 不同阶段的训练计划安排 |
| 伤病预防.md | 常见运动损伤的预防和处理 |
| 恢复与营养.md | 跑后恢复和营养补充建议 |
| 比赛准备.md | 赛前准备和比赛策略 |
| 大学生跑步指南.md | 针对大学生的综合跑步指南 |

## 环境变量说明

| 变量名 | 说明 |
|--------|------|
| OPENAI_API_KEY | OpenAI API 密钥 |
| OPENAI_BASE_URL | API 基础 URL（支持兼容接口） |
| LLM_MODEL | 使用的模型名称 |

## 许可证

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request！
