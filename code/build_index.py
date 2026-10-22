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

import chromadb
from langchain_text_splitters import MarkdownHeaderTextSplitter, RecursiveCharacterTextSplitter
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_chroma import Chroma

DATA_DIR = Path(__file__).resolve().parent / "../data/runwise"
CHROMA_DIR = Path(__file__).resolve().parent / "../chroma_db/runwise"
EMBEDDING_MODEL = "BAAI/bge-small-zh-v1.5"
COLLECTION_NAME = "runwise_knowledge_v2"

CHUNK_SIZE = 520
CHUNK_OVERLAP = 80

HEADER_SPLITTER = MarkdownHeaderTextSplitter(
    headers_to_split_on=[
        ("#", "h1"),
        ("##", "h2"),
        ("###", "h3"),
        ("####", "h4"),
    ],
    strip_headers=False,
)


def load_markdown_sections(file_path: Path, data_path: Path):
    """按 Markdown 标题保留章节语境，并补充可用于检索过滤的元数据。"""
    text = file_path.read_text(encoding="utf-8")
    relative_path = file_path.relative_to(data_path)
    category = relative_path.parts[0] if len(relative_path.parts) > 1 else "general"
    sections = HEADER_SPLITTER.split_text(text)

    for section in sections:
        headings = [
            section.metadata.get(key, "").strip()
            for key in ("h1", "h2", "h3", "h4")
            if section.metadata.get(key, "").strip()
        ]
        document_title = headings[0] if headings else file_path.stem
        section_path = " / ".join(headings) if headings else document_title
        section.metadata.update(
            {
                "source": file_path.name,
                "source_path": relative_path.as_posix(),
                "category": category,
                "document_title": document_title,
                "section": section_path,
            }
        )
    return sections


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
    loaded_file_count = 0
    for f in sorted(data_path.rglob("*")):
        if f.suffix.lower() in (".md", ".txt") and f.is_file():
            try:
                if f.suffix.lower() == ".md":
                    file_docs = load_markdown_sections(f, data_path)
                else:
                    # 当前知识库以 Markdown 为主；txt 文件作为一个带基础元数据的章节处理。
                    from langchain_core.documents import Document

                    relative_path = f.relative_to(data_path)
                    file_docs = [
                        Document(
                            page_content=f.read_text(encoding="utf-8"),
                            metadata={
                                "source": f.name,
                                "source_path": relative_path.as_posix(),
                                "category": relative_path.parts[0]
                                if len(relative_path.parts) > 1
                                else "general",
                                "document_title": f.stem,
                                "section": f.stem,
                            },
                        )
                    ]
                docs.extend(file_docs)
                loaded_file_count += 1
                print(f"  已加载: {f.name}")
            except Exception as e:
                print(f"  加载失败 {f.name}: {e}")
    print(f"加载了 {loaded_file_count} 个文件，解析出 {len(docs)} 个标题章节")

    if not docs:
        print("[错误] 未加载到任何文档，请检查文件格式")
        sys.exit(1)

    print(f"正在分块 (chunk_size={CHUNK_SIZE}, chunk_overlap={CHUNK_OVERLAP})...")
    splitter = RecursiveCharacterTextSplitter(
        chunk_size=CHUNK_SIZE,
        chunk_overlap=CHUNK_OVERLAP,
        separators=["\n\n", "\n", "。", "；", "，", " ", ""],
        length_function=len,
    )
    chunks = splitter.split_documents(docs)

    for i, chunk in enumerate(chunks):
        section = chunk.metadata.get("section", chunk.metadata.get("document_title", ""))
        chunk.page_content = f"章节：{section}\n\n{chunk.page_content.strip()}"
        chunk.metadata["chunk_id"] = f"{chunk.metadata.get('source_path', 'unknown')}#{i}"

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
    client = chromadb.PersistentClient(path=str(chroma_path))
    try:
        client.delete_collection(COLLECTION_NAME)
        print(f"已移除旧的 {COLLECTION_NAME} collection")
    except Exception:
        pass

    Chroma.from_documents(
        documents=chunks,
        embedding=embeddings,
        persist_directory=str(chroma_path),
        collection_name=COLLECTION_NAME,
    )

    print("=" * 50)
    print("索引构建完成!")
    print(f"  加载文件数: {loaded_file_count}")
    print(f"  标题章节数: {len(docs)}")
    print(f"  文本块数:   {len(chunks)}")
    print(f"  Collection: {COLLECTION_NAME}")
    print(f"  保存路径:   {chroma_path}")
    print("=" * 50)


if __name__ == "__main__":
    main()
