"""RunWise Chroma RAG HTTP service.

Expose the existing local knowledge base to Spring Boot. Retrieval and model
generation stay behind the backend, so Web and mini-program share one API.
"""

from __future__ import annotations

import json
import os
import threading
from pathlib import Path
from typing import List, Optional
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException
from langchain_chroma import Chroma
from langchain_huggingface import HuggingFaceEmbeddings
from pydantic import BaseModel, Field


ROOT_DIR = Path(__file__).resolve().parent.parent
CHROMA_DIR = ROOT_DIR / "chroma_db" / "runwise"
LOCAL_MODEL_DIR = ROOT_DIR / "models" / "bge-small-zh-v1.5"
EMBEDDING_MODEL = "BAAI/bge-small-zh-v1.5"

load_dotenv(Path(__file__).resolve().parent / ".env")

OLLAMA_BASE_URL = os.getenv("OLLAMA_BASE_URL", "http://127.0.0.1:11434").rstrip("/")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "qwen2.5:3b")
DEFAULT_TOP_K = int(os.getenv("RAG_TOP_K", "4"))

INJURY_TERMS = (
    "疼",
    "疼痛",
    "膝盖",
    "脚踝",
    "小腿",
    "跟腱",
    "足底",
    "拉伤",
    "扭伤",
    "肿胀",
    "刺痛",
    "伤病",
    "受伤",
)
SAFETY_TIP = (
    "以上建议仅供跑步训练参考；若疼痛持续、加重、伴随明显肿胀或影响行走，"
    "请停止训练并及时就医。"
)
SYSTEM_PROMPT = """你是 RunWise 跑步训练助手。请仅根据给出的知识库片段回答，并遵守：
1. 使用中文，先给结论，再给可执行建议；内容专业、克制，不编造知识库没有的信息。
2. 如果资料不足，明确说“当前知识库依据不足”，不要猜测。
3. 涉及疼痛和伤病时，说明停止训练或就医的警示信号，不替代医生诊断。
4. 回答控制在 200 到 500 字，必要时用简短分点。
"""


class HistoryItem(BaseModel):
    user: str = Field(min_length=1, max_length=500)
    assistant: str = Field(min_length=1, max_length=4000)


class AskRequest(BaseModel):
    question: str = Field(min_length=1, max_length=500)
    history: List[HistoryItem] = Field(default_factory=list)
    top_k: Optional[int] = Field(default=None, ge=1, le=8)


class AskResponse(BaseModel):
    answer: str
    sources: List[str]
    safetyTip: Optional[str] = None
    hitCount: int


class SearchHit(BaseModel):
    content: str
    source: str
    chunkId: str
    distance: float


app = FastAPI(title="RunWise RAG API", version="1.0.0")

_load_lock = threading.Lock()
_vectorstore: Optional[Chroma] = None


def get_vectorstore() -> Chroma:
    global _vectorstore
    if _vectorstore is not None:
        return _vectorstore

    with _load_lock:
        if _vectorstore is not None:
            return _vectorstore
        if not (CHROMA_DIR / "chroma.sqlite3").exists():
            raise RuntimeError(f"Chroma index not found: {CHROMA_DIR}")

        model_name = (
            str(LOCAL_MODEL_DIR)
            if (LOCAL_MODEL_DIR / "config.json").exists()
            else EMBEDDING_MODEL
        )
        embeddings = HuggingFaceEmbeddings(
            model_name=model_name,
            model_kwargs={"device": "cpu"},
            encode_kwargs={"normalize_embeddings": True},
        )
        _vectorstore = Chroma(
            persist_directory=str(CHROMA_DIR),
            embedding_function=embeddings,
        )
        return _vectorstore


def retrieve(question: str, top_k: int) -> List[SearchHit]:
    results = get_vectorstore().similarity_search_with_score(question, k=top_k)
    hits: List[SearchHit] = []
    for document, score in results:
        source = Path(str(document.metadata.get("source", "未知来源"))).name
        chunk_id = str(document.metadata.get("chunk_id", "?"))
        hits.append(
            SearchHit(
                content=document.page_content,
                source=source,
                chunkId=chunk_id,
                distance=float(score),
            )
        )
    return hits


def call_ollama(question: str, hits: List[SearchHit], history: List[HistoryItem]) -> str:
    context = "\n\n---\n\n".join(
        f"来源：{hit.source}（片段 {hit.chunkId}）\n{hit.content}" for hit in hits
    )
    history_text = "\n".join(
        f"用户：{item.user}\n助手：{item.assistant}" for item in history[-3:]
    )
    prompt = (
        f"{SYSTEM_PROMPT}\n"
        f"最近对话：\n{history_text or '无'}\n\n"
        f"知识库片段：\n{context or '未检索到相关片段'}\n\n"
        f"用户问题：{question}"
    )
    payload = json.dumps(
        {
            "model": OLLAMA_MODEL,
            "prompt": prompt,
            "stream": False,
            "options": {"temperature": 0.25, "num_predict": 700},
        },
        ensure_ascii=False,
    ).encode("utf-8")
    request = Request(
        f"{OLLAMA_BASE_URL}/api/generate",
        data=payload,
        headers={"Content-Type": "application/json; charset=utf-8"},
        method="POST",
    )
    try:
        with urlopen(request, timeout=120) as response:
            body = json.loads(response.read().decode("utf-8"))
    except (HTTPError, URLError, TimeoutError) as exc:
        raise RuntimeError(f"Ollama request failed: {exc}") from exc

    answer = str(body.get("response", "")).strip()
    if not answer:
        raise RuntimeError("Ollama returned an empty answer")
    return answer


@app.get("/health")
def health():
    try:
        vectorstore = get_vectorstore()
        count = vectorstore._collection.count()
        return {
            "status": "ok",
            "indexReady": True,
            "chunkCount": count,
            "model": OLLAMA_MODEL,
        }
    except Exception as exc:
        return {"status": "degraded", "indexReady": False, "detail": str(exc)}


@app.post("/api/rag/search", response_model=List[SearchHit])
def search(request: AskRequest):
    try:
        return retrieve(request.question.strip(), request.top_k or DEFAULT_TOP_K)
    except Exception as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc


@app.post("/api/rag/ask", response_model=AskResponse)
def ask(request: AskRequest):
    question = request.question.strip()
    try:
        hits = retrieve(question, request.top_k or DEFAULT_TOP_K)
        answer = call_ollama(question, hits, request.history)
    except Exception as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc

    sources = list(dict.fromkeys(f"{hit.source} · chunk {hit.chunkId}" for hit in hits))
    safety_tip = SAFETY_TIP if any(term in question for term in INJURY_TERMS) else None
    return AskResponse(
        answer=answer,
        sources=sources,
        safetyTip=safety_tip,
        hitCount=len(hits),
    )


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        app,
        host=os.getenv("RAG_HOST", "127.0.0.1"),
        port=int(os.getenv("RAG_PORT", "5000")),
    )
