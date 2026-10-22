"""RunWise Chroma RAG HTTP service.

Expose the existing local knowledge base to Spring Boot. Retrieval and model
generation stay behind the backend, so Web and mini-program share one API.
"""

from __future__ import annotations

import json
import os
import re
import threading
from pathlib import Path
from typing import Any, Dict, List, Optional, Set
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException
from langchain_chroma import Chroma
from langchain_huggingface import HuggingFaceEmbeddings
from pydantic import BaseModel, Field
from rank_bm25 import BM25Okapi


ROOT_DIR = Path(__file__).resolve().parent.parent
CHROMA_DIR = ROOT_DIR / "chroma_db" / "runwise"
LOCAL_MODEL_DIR = ROOT_DIR / "models" / "bge-small-zh-v1.5"
EMBEDDING_MODEL = "BAAI/bge-small-zh-v1.5"
COLLECTION_NAME = "runwise_knowledge_v2"

load_dotenv(Path(__file__).resolve().parent / ".env")

OLLAMA_BASE_URL = os.getenv("OLLAMA_BASE_URL", "http://127.0.0.1:11434").rstrip("/")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "qwen2.5:3b")
DEFAULT_TOP_K = int(os.getenv("RAG_TOP_K", "3"))
RECALL_SIZE = int(os.getenv("RAG_RECALL_SIZE", "12"))
MAX_VECTOR_DISTANCE = float(os.getenv("RAG_MAX_VECTOR_DISTANCE", "0.82"))

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
SYSTEM_PROMPT = """你是 RunWise 跑步训练助手。请使用中文，先给结论，再给具体、可执行的建议。
知识库片段是专业依据，不是必须复述的答案：只采用与问题直接相关的内容，忽略无关片段。
对一般训练问题，可以在知识库基础上结合通用跑步训练原则解释，但不要捏造用户数据或来源。
如果知识库没有覆盖问题，可以基于通用知识回答，并明确说明“以下为通用建议”。
涉及疼痛和伤病时要保持保守，不做诊断，不建议带伤硬撑，并说明停止训练或就医的警示信号。
回答以解决用户问题为主，通常控制在 250 到 500 字；需要时使用简短分点，不要机械总结文档。
"""

CATEGORY_KEYWORDS = {
    "injury": ("疼", "痛", "膝", "脚踝", "小腿", "跟腱", "足底", "拉伤", "扭伤", "受伤", "肿"),
    "training": ("训练", "计划", "配速", "间歇", "节奏跑", "长距离", "心率", "5k", "10k", "半马", "减脂"),
    "equipment": ("跑鞋", "鞋", "装备", "鞋楦", "缓震", "碳板"),
    "nutrition": ("饮食", "营养", "补给", "吃", "喝", "碳水", "蛋白质"),
    "recovery": ("恢复", "睡眠", "疲劳", "休息", "酸痛"),
    "race": ("比赛", "赛事", "赛前", "赛中", "赛后", "冲刺"),
    "beginner": ("新手", "初学", "第一次", "入门", "开始跑"),
}
RUNNING_DOMAIN_TERMS = (
    "跑",
    "公里",
    "配速",
    "步频",
    "心率",
    "训练",
    "比赛",
    "跑鞋",
    "膝",
    "脚踝",
    "小腿",
    "跟腱",
    "足底",
    "恢复",
)
TRAINING_GOALS = {
    "5k": ("5k", "5公里", "五公里"),
    "10k": ("10k", "10公里", "十公里"),
    "half_marathon": ("半马", "半程马拉松"),
    "weight_loss": ("减脂", "减肥", "减重"),
    "fitness_test": ("体测", "体育测试"),
}
INJURY_TOPICS = {
    "iliotibial_band": ("髂胫束", "膝外", "外侧疼"),
    "runner_knee": ("跑者膝", "膝前", "髌骨"),
    "shin_splints": ("胫骨", "小腿前"),
    "achilles_tendinitis": ("跟腱",),
    "plantar_fasciitis": ("足底", "脚底"),
}


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
    category: str = "general"
    section: str = ""
    retrievalScore: float = 0.0


app = FastAPI(title="RunWise RAG API", version="1.0.0")

_load_lock = threading.Lock()
_vectorstore: Optional[Chroma] = None
_lexical_lock = threading.Lock()
_lexical_documents: List[Dict[str, Any]] = []
_bm25: Optional[BM25Okapi] = None


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
            collection_name=COLLECTION_NAME,
        )
        return _vectorstore


def tokenize(text: str) -> List[str]:
    """无需额外分词服务的中英文检索 token；中文使用二元组减少单字噪声。"""
    tokens: List[str] = []
    for part in re.findall(r"[a-z0-9]+|[\u4e00-\u9fff]+", text.lower()):
        if re.fullmatch(r"[\u4e00-\u9fff]+", part):
            if len(part) == 1:
                tokens.append(part)
            else:
                tokens.extend(part[index : index + 2] for index in range(len(part) - 1))
        else:
            tokens.append(part)
    return tokens


def infer_categories(question: str) -> Set[str]:
    lowered = question.lower()
    return {
        category
        for category, keywords in CATEGORY_KEYWORDS.items()
        if any(keyword in lowered for keyword in keywords)
    }


def infer_training_goal(text: str) -> Optional[str]:
    lowered = text.lower().replace(" ", "")
    for goal, keywords in TRAINING_GOALS.items():
        if any(keyword in lowered for keyword in keywords):
            return goal
    return None


def infer_injury_topic(text: str) -> Optional[str]:
    for topic, keywords in INJURY_TOPICS.items():
        if any(keyword in text for keyword in keywords):
            return topic
    return None


def expand_retrieval_query(question: str, categories: Set[str], goal: Optional[str]) -> str:
    """为不同问题补充检索意图，只影响召回，不改变交给模型的原始问题。"""
    additions: List[str] = []
    if goal:
        additions.append("具体训练安排 周计划 训练课表 距离 配速 休息日 注意事项")
    if "injury" in categories:
        additions.append("是否停跑 急性处理 恢复跑步 红旗信号 就医条件")
    if "equipment" in categories:
        additions.append("选择标准 足型 使用场景 注意事项")
    if "nutrition" in categories:
        additions.append("摄入时间 摄入量 食物选择 注意事项")
    if "recovery" in categories:
        additions.append("恢复方案 训练调整 风险信号")
    return f"{question} {' '.join(additions)}".strip()


def get_lexical_index() -> tuple[List[Dict[str, Any]], BM25Okapi]:
    global _bm25, _lexical_documents
    if _bm25 is not None:
        return _lexical_documents, _bm25

    with _lexical_lock:
        if _bm25 is not None:
            return _lexical_documents, _bm25
        raw = get_vectorstore().get(include=["documents", "metadatas"])
        ids = raw.get("ids", [])
        documents = raw.get("documents", [])
        metadatas = raw.get("metadatas", [])
        _lexical_documents = [
            {"id": item_id, "content": content, "metadata": metadata or {}}
            for item_id, content, metadata in zip(ids, documents, metadatas)
        ]
        corpus = [tokenize(item["content"]) for item in _lexical_documents]
        _bm25 = BM25Okapi(corpus)
        return _lexical_documents, _bm25


def retrieve(question: str, top_k: int) -> List[SearchHit]:
    """融合语义与关键词召回，并通过类别和相关度过滤掉噪声片段。"""
    vectorstore = get_vectorstore()
    recall_size = max(RECALL_SIZE, top_k * 4)
    inferred_categories = infer_categories(question)
    requested_goal = infer_training_goal(question)
    requested_injury_topic = infer_injury_topic(question)
    retrieval_query = expand_retrieval_query(question, inferred_categories, requested_goal)
    vector_results = vectorstore.similarity_search_with_score(retrieval_query, k=recall_size)
    if not inferred_categories and not any(term in question.lower() for term in RUNNING_DOMAIN_TERMS):
        return []
    candidates: Dict[str, Dict[str, Any]] = {}

    for rank, (document, distance) in enumerate(vector_results, start=1):
        chunk_id = str(document.metadata.get("chunk_id", f"vector-{rank}"))
        candidates[chunk_id] = {
            "content": document.page_content,
            "metadata": document.metadata,
            "distance": float(distance),
            "semantic": max(0.0, 1.0 - float(distance)),
            "lexical": 0.0,
        }

    lexical_documents, bm25 = get_lexical_index()
    query_tokens = tokenize(retrieval_query)
    if query_tokens and lexical_documents:
        lexical_scores = bm25.get_scores(query_tokens)
        ranked_indexes = sorted(
            range(len(lexical_scores)), key=lambda index: lexical_scores[index], reverse=True
        )[:recall_size]
        best_lexical_score = max((float(lexical_scores[index]) for index in ranked_indexes), default=0.0)
        for index in ranked_indexes:
            raw_score = float(lexical_scores[index])
            if raw_score <= 0:
                continue
            item = lexical_documents[index]
            metadata = item["metadata"]
            chunk_id = str(metadata.get("chunk_id", item["id"]))
            candidate = candidates.setdefault(
                chunk_id,
                {
                    "content": item["content"],
                    "metadata": metadata,
                    "distance": 999.0,
                    "semantic": 0.0,
                    "lexical": 0.0,
                    "lexical_raw": 0.0,
                },
            )
            candidate["lexical"] = raw_score / best_lexical_score if best_lexical_score else 0.0
            candidate["lexical_raw"] = raw_score

    ranked: List[Dict[str, Any]] = []
    for chunk_id, candidate in candidates.items():
        metadata = candidate["metadata"]
        category = str(metadata.get("category", "general"))
        document_goal = infer_training_goal(
            f"{metadata.get('source_path', '')} {metadata.get('document_title', '')}"
        )
        if requested_goal and document_goal and requested_goal != document_goal:
            continue
        category_boost = 0.12 if category in inferred_categories else 0.0
        # 明确提出 5K/10K/半马等目标时，专项计划应优先于通用训练文章。
        goal_boost = 0.25 if requested_goal and document_goal == requested_goal else 0.0
        source_path = str(metadata.get("source_path", ""))
        injury_topic_boost = 0.0
        if requested_injury_topic and category == "injury":
            if requested_injury_topic in source_path:
                injury_topic_boost = 0.18
            elif any(topic in source_path for topic in INJURY_TOPICS):
                injury_topic_boost = -0.08
        candidate["chunk_id"] = chunk_id
        candidate["document_goal"] = document_goal
        candidate["retrieval_score"] = (
            candidate["semantic"] * 0.68
            + candidate["lexical"] * 0.32
            + category_boost
            + goal_boost
            + injury_topic_boost
        )
        # 语义距离过远且没有实际关键词命中的候选不进入生成上下文。
        category_matches = category in inferred_categories
        if candidate["distance"] <= MAX_VECTOR_DISTANCE or (
            category_matches and candidate.get("lexical_raw", 0.0) >= 2.0
        ):
            ranked.append(candidate)

    if requested_goal:
        goal_specific = [item for item in ranked if item.get("document_goal") == requested_goal]
        if goal_specific:
            ranked = goal_specific

    ranked.sort(key=lambda item: item["retrieval_score"], reverse=True)
    if ranked:
        # 不为了凑满 top_k 强塞明显弱于最佳命中的片段。
        score_floor = max(0.2, ranked[0]["retrieval_score"] - 0.2)
        ranked = [item for item in ranked if item["retrieval_score"] >= score_floor]
    hits: List[SearchHit] = []
    for candidate in ranked[:top_k]:
        metadata = candidate["metadata"]
        hits.append(
            SearchHit(
                content=candidate["content"],
                source=str(metadata.get("document_title") or metadata.get("source", "未知来源")),
                chunkId=candidate["chunk_id"],
                distance=round(candidate["distance"], 6),
                category=str(metadata.get("category", "general")),
                section=str(metadata.get("section", "")),
                retrievalScore=round(candidate["retrieval_score"], 6),
            )
        )
    return hits


def call_ollama(question: str, hits: List[SearchHit], history: List[HistoryItem]) -> str:
    context = "\n\n---\n\n".join(
        f"来源：{hit.source}\n章节：{hit.section or '未标注'}\n{hit.content}" for hit in hits
    )
    history_text = "\n".join(
        f"用户：{item.user[:300]}\n助手：{item.assistant[:600]}" for item in history[-2:]
    )
    prompt = (
        f"{SYSTEM_PROMPT}\n"
        "重要约束：知识片段中的案例人物、疼痛评分、跑量、配速和训练周数都不是用户数据。"
        "除非用户在问题中明确提供，否则不得写成‘你的数据’或‘根据你的评分’。\n"
        "不得为了说明方法而假设用户每周跑量、疼痛分数或其他数值，也不要编造数值示例。"
        "缺少这些信息时，先给保守建议，再直接询问需要补充的信息。\n"
        "如果用户要求具体计划，而片段没有提供足够的周次或训练量，不得自行编造精确数值；"
        "应给出原则、可确认的内容，并说明还需要哪些信息。长课表只概括阶段重点，"
        "不要逐行复述或把多行表格拼成一次训练。\n"
        f"最近对话：\n{history_text or '无'}\n\n"
        f"知识库片段：\n{context or '未检索到足够相关的片段，请给出通用建议并明确其性质。'}\n\n"
        f"用户问题：{question}"
    )
    payload = json.dumps(
        {
            "model": OLLAMA_MODEL,
            "prompt": prompt,
            "stream": False,
            "options": {"temperature": 0.15, "num_predict": 500},
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
            "collection": COLLECTION_NAME,
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

    sources = list(
        dict.fromkeys(
            f"{hit.source} · {hit.section}" if hit.section else hit.source for hit in hits
        )
    )
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
