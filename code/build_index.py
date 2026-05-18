"""
RunWise RAG - 向量索引构建脚本
读取知识库文档，分块后使用 ChromaDB 持久化向量库
"""

import os
import sys
from pathlib import Path

os.environ.setdefault("HF_ENDPOINT", "https://hf-mirror.com")
os.environ.setdefault("HF_HUB_ENABLE_HF_TRANSFER", "0")
os.environ.setdefault("HF_HUB_DISABLE_SYMLINKS_WARNING", "1")

from huggingface_hub import snapshot_download

from langchain_community.document_loaders import TextLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_chroma import Chroma

DATA_DIR = Path(__file__).resolve().parent / "../data/runwise"
CHROMA_DIR = Path(__file__).resolve().parent / "../chroma_db/runwise"
EMBEDDING_MODEL = "BAAI/bge-small-zh-v1.5"

CHUNK_SIZE = 800
CHUNK_OVERLAY = 120


def main():
    data_path = DATA_DIR.resolve()
    chroma_path = CHROMA_DIR.resolve()

    if not data_path.exists() or not any(data_path.iterdir()):
        print(f"[错误] 知识库目录为空或不存在: {data_path}")
        print("请先在 data/runwise/ 下放置 .md 或 .txt 知识文件")
        sys.exit(1)

    print(f"知识库目录: {data_path}")
    print("正在加载文档...")

    docs = []
    for f in sorted(data_path.rglob("*")):
        if f.suffix.lower() in (".md", ".txt") and f.is_file():
            try:
                loader = TextLoader(str(f), encoding="utf-8")
                file_docs = loader.load()
                docs.extend(file_docs)
                print(f"  已加载: {f.name}")
            except Exception as e:
                print(f"  加载失败 {f.name}: {e}")
    print(f"加载了 {len(docs)} 个文件")

    if not docs:
        print("[错误] 未加载到任何文档，请检查文件格式")
        sys.exit(1)

    for doc in docs:
        source = doc.metadata.get("source", "")
        doc.metadata["source"] = Path(source).name

    print(f"正在分块 (chunk_size={CHUNK_SIZE}, chunk_overlap={CHUNK_OVERLAY})...")
    splitter = RecursiveCharacterTextSplitter(
        chunk_size=CHUNK_SIZE,
        chunk_overlap=CHUNK_OVERLAY,
        separators=["\n## ", "\n### ", "\n\n", "\n", "。", "，", " ", ""],
        length_function=len,
    )
    chunks = splitter.split_documents(docs)

    for i, chunk in enumerate(chunks):
        chunk.metadata["chunk_id"] = i

    print(f"切分出 {len(chunks)} 个文本块")

    print(f"正在加载 Embedding 模型: {EMBEDDING_MODEL} ...")
    local_model_dir = Path(__file__).resolve().parent / "../models/bge-small-zh-v1.5"
    local_model_dir = local_model_dir.resolve()
    if not (local_model_dir / "config.json").exists():
        print(f"首次运行，正在从镜像下载模型到: {local_model_dir}")
        local_model_dir.mkdir(parents=True, exist_ok=True)
        snapshot_download(
            repo_id=EMBEDDING_MODEL,
            local_dir=str(local_model_dir),
        )
        print("模型下载完成")
    else:
        print(f"使用本地缓存模型: {local_model_dir}")

    embeddings = HuggingFaceEmbeddings(
        model_name=str(local_model_dir),
        model_kwargs={"device": "cpu"},
        encode_kwargs={"normalize_embeddings": True},
    )

    print("正在构建向量索引并存入 ChromaDB ...")
    chroma_path.mkdir(parents=True, exist_ok=True)
    vectorstore = Chroma.from_documents(
        documents=chunks,
        embedding=embeddings,
        persist_directory=str(chroma_path),
    )

    print("=" * 50)
    print("索引构建完成!")
    print(f"  加载文件数: {len(docs)}")
    print(f"  文本块数:   {len(chunks)}")
    print(f"  保存路径:   {chroma_path}")
    print("=" * 50)


if __name__ == "__main__":
    main()
