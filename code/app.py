"""
RunWise RAG - Streamlit 聊天界面
面向大学生跑者的训练与恢复知识问答助手
赛博运动风 V5 版本
"""

import os
from pathlib import Path

os.environ.setdefault("HF_ENDPOINT", "https://hf-mirror.com")

import streamlit as st
from dotenv import load_dotenv
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_chroma import Chroma
from openai import OpenAI

load_dotenv()

EMBEDDING_MODEL = "BAAI/bge-small-zh-v1.5"
CHROMA_DIR = Path(__file__).resolve().parent / "../chroma_db/runwise"
DATA_DIR = Path(__file__).resolve().parent / "../data/runwise"
LOCAL_MODEL_DIR = (Path(__file__).resolve().parent / "../models/bge-small-zh-v1.5").resolve()

SYSTEM_PROMPT = """你是 RunWise RAG，一个面向大学生跑者的训练与恢复知识问答助手。

回答原则：
1. 优先基于知识库内容回答。
2. 不要编造知识库没有的信息。
3. 回答要具体、可执行，像理性跑步教练。
4. 涉及疼痛、损伤、恢复时要谨慎。
5. 不能替代医生诊断。
6. 如果用户提到持续疼痛、疼痛加重、影响走路、改变跑姿，应建议暂停训练并咨询医生或运动康复师。
7. 如果知识库没有依据，要明确说明。"""

CUSTOM_CSS = """
<style>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@200;300;400;500;600;700;800;900&family=JetBrains+Mono:wght@400;500;600&family=Noto+Sans+SC:wght@300;400;500;700;900&display=swap');

:root {
    --accent: #00FF7F;
    --accent-dim: rgba(0,255,127,0.5);
    --accent-glow: rgba(0,255,127,0.15);
    --accent-soft: rgba(0,255,127,0.06);
    --accent2: #818CF8;
    --gradient-brand: linear-gradient(135deg, #00FF7F 0%, #7C3AED 100%);
    --gradient-brand-text: linear-gradient(135deg, #4ADE80 0%, #22D3EE 50%, #818CF8 100%);
    --bg: #050505;
    --bg-surface: #181818;
    --bg-elevated: #2A2A2A;
    --bg-glass: rgba(30,30,30,0.6);
    --border: rgba(255,255,255,0.1);
    --border-hover: rgba(255,255,255,0.15);
    --text: #E2E8F0;
    --text-secondary: #A1A1AA;
    --text-muted: #52525B;
}

* { box-sizing: border-box; }

.stApp {
    background: var(--bg) !important;
    color: var(--text) !important;
    font-family: 'Inter', 'Noto Sans SC', sans-serif;
}

.stApp [data-testid="stAppViewContainer"] {
    background: var(--bg) !important;
}

.stApp > header {
    background-color: transparent !important;
    border-bottom: none !important;
}

.stApp > header [data-testid="stToolbar"] {
    position: fixed !important;
    top: 10px !important;
    left: 10px !important;
    z-index: 99999 !important;
    background: rgba(30,30,30,0.9) !important;
    backdrop-filter: blur(12px) !important;
    border: 1px solid rgba(255,255,255,0.1) !important;
    border-radius: 10px !important;
    padding: 4px !important;
}

.stApp > header [data-testid="stToolbar"] button {
    background: transparent !important;
    border: none !important;
    color: var(--text-muted) !important;
    width: 36px !important;
    height: 36px !important;
    border-radius: 8px !important;
}

.stApp > header [data-testid="stToolbar"] button:hover {
    background: rgba(0,255,127,0.08) !important;
    color: var(--accent) !important;
}

footer { visibility: hidden !important; height: 0 !important; }
div[data-testid="stDecoration"] { display: none !important; }
div[data-testid="stAppDeployButton"] { display: none !important; }
[data-testid="stAppView"] > div > div:last-child { visibility: hidden !important; height: 0 !important; overflow: hidden !important; }

section[data-testid="stSidebar"] {
    background: rgba(5,5,5,0.95) !important;
    backdrop-filter: blur(12px) !important;
    border-right: 1px solid var(--border) !important;
}

section[data-testid="stSidebar"] [data-testid="stSidebarNav"] { display: none !important; }

section[data-testid="stSidebar"] [data-testid="stSidebarUserContent"] {
    padding-top: 0 !important;
}

.sidebar-brand { text-align: center; padding: 16px 0 20px; }

.sidebar-brand-name {
    font-family: 'Inter', sans-serif; font-size: 1.1rem; font-weight: 900; font-style: italic;
    background: var(--gradient-brand-text); -webkit-background-clip: text;
    -webkit-text-fill-color: transparent; background-clip: text;
}

.sidebar-brand-sub { font-size: 0.55rem; color: var(--text-muted); margin-top: 3px; letter-spacing: 3px; text-transform: uppercase; }
.sidebar-divider { height: 1px; background: var(--border); margin: 12px 0; }

section[data-testid="stSidebar"] [data-testid="stButton"] {
    display: block !important;
    margin-bottom: 2px !important;
}

section[data-testid="stSidebar"] [data-testid="stButton"] > button {
    display: flex !important;
    align-items: center !important;
    justify-content: flex-start !important;
    gap: 10px !important;
    background: transparent !important;
    border: none !important;
    border-left: 3px solid transparent !important;
    border-radius: 0 10px 10px 0 !important;
    padding: 12px 16px 12px 18px !important;
    text-align: left !important;
    font-size: 0.86rem !important;
    font-weight: 500 !important;
    color: #71717A !important;
    transition: all 0.2s ease !important;
    box-shadow: none !important;
    letter-spacing: 0.3px !important;
    width: 100% !important;
    line-height: 1.4 !important;
    min-height: 44px !important;
}

section[data-testid="stSidebar"] [data-testid="stButton"] > button:hover {
    color: #A1A1AA !important;
    background: rgba(255,255,255,0.04) !important;
    border-left: 3px solid rgba(0,255,127,0.3) !important;
    box-shadow: none !important;
    transform: none !important;
}

section[data-testid="stSidebar"] [data-testid="stButton"] > button:active {
    transform: none !important;
}

.hero-center { text-align: center; padding: 48px 0 16px; }

.hero-title {
    font-family: 'Inter', sans-serif;
    font-size: 3.2rem;
    font-weight: 900;
    font-style: italic;
    letter-spacing: -2px;
    background: var(--gradient-brand-text);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    line-height: 1.1;
}

.hero-sub {
    font-size: 0.76rem;
    color: var(--text-muted);
    margin-top: 14px;
    margin-bottom: 32px;
    letter-spacing: 1.5px;
}

.hero-sub .hero-sub-sep { margin: 0 8px; opacity: 0.3; }

@keyframes scanLine {
    0% { background-position: -200% 0; }
    100% { background-position: 200% 0; }
}

.hero-line {
    height: 1px;
    margin: 0 auto 24px;
    max-width: 240px;
    background: linear-gradient(90deg, transparent 0%, transparent 40%, var(--accent) 50%, transparent 60%, transparent 100%);
    background-size: 200% 100%;
    animation: scanLine 8s linear infinite;
    opacity: 0.4;
}

.status-bar { display: flex; justify-content: center; gap: 10px; padding: 0 0 36px; }

.status-badge {
    display: inline-flex; align-items: center; gap: 5px;
    background: rgba(30,30,30,0.6); backdrop-filter: blur(12px);
    border: 1px solid rgba(255,255,255,0.08); border-radius: 20px;
    padding: 3px 10px; font-size: 0.60rem; color: var(--text-muted);
}

.status-badge svg { width: 10px; height: 10px; stroke: var(--accent); stroke-width: 2; fill: none; opacity: 0.6; }
.status-badge .badge-val { font-family: 'JetBrains Mono', monospace; color: var(--accent); font-weight: 600; font-size: 0.60rem; }

.source-tag {
    display: inline-block; background: rgba(0,255,127,0.06); color: var(--accent);
    border: 1px solid rgba(0,255,127,0.12); border-radius: 4px;
    padding: 2px 8px; font-size: 0.72rem; font-family: 'JetBrains Mono', monospace; margin: 2px 3px 2px 0;
}

.warning-strip {
    background: rgba(251,191,36,0.06); border-left: 3px solid #FBBF24;
    border-radius: 0 8px 8px 0; padding: 10px 16px; color: #FCD34D; font-size: 0.88rem; margin-bottom: 12px;
}

[data-testid="stChatMessage"], div.stChatMessage {
    background: rgba(30,30,30,0.4) !important; backdrop-filter: blur(12px) !important;
    border-radius: 12px !important; border: 1px solid rgba(255,255,255,0.06) !important;
    margin-bottom: 6px; padding: 16px 20px !important;
}

[data-testid="user-chat-message"], div.stChatMessage[data-testid="user-chat-message"] {
    background: rgba(0,255,127,0.04) !important; border-left: 2px solid rgba(0,255,127,0.2) !important;
}

.input-disclaimer {
    text-align: center; font-size: 0.58rem; color: var(--text-muted);
    opacity: 0.3; margin-top: 8px; max-width: 720px; margin-left: auto; margin-right: auto;
}

div[data-testid="stBottomBlockContainer"] {
    background: rgba(5,5,5,0.96) !important; backdrop-filter: blur(20px) !important;
    border-top: 1px solid rgba(255,255,255,0.04) !important; padding: 18px 0 24px !important;
}

[data-testid="stChatInput"] {
    border-radius: 24px; margin: 0 auto; max-width: 720px; padding: 0 !important;
}

[data-testid="stChatInput"] > div > div {
    background: #121212 !important;
    border: 1px solid rgba(255,255,255,0.1) !important;
    border-radius: 24px;
    box-shadow: 0 4px 20px rgba(0,0,0,0.4);
    transition: border-color 0.3s ease, box-shadow 0.3s ease;
    padding: 8px 8px !important;
}

[data-testid="stChatInput"] > div > div:focus-within {
    border: 1px solid #00FF7F !important;
    box-shadow: 0 0 10px rgba(0,255,127,0.3), 0 4px 20px rgba(0,0,0,0.4) !important;
}

[data-testid="stChatInput"] textarea {
    font-family: 'Inter', 'Noto Sans SC', sans-serif !important;
    font-size: 0.95rem !important; color: #E2E8F0 !important;
    caret-color: #00FF7F !important; background: transparent !important;
    line-height: 1.6; min-height: 32px !important;
}

[data-testid="stChatInput"] textarea::placeholder { color: var(--text-muted) !important; }

[data-testid="stChatInput"] button[data-testid="stChatInputSubmitButton"] {
    background: var(--bg-elevated) !important;
    border: 1px solid var(--border) !important;
    border-radius: 12px !important;
    width: 38px !important; height: 38px !important;
    min-width: 38px !important; min-height: 38px !important;
    padding: 0 !important;
    display: flex !important; align-items: center !important; justify-content: center !important;
    color: var(--text-muted) !important;
    transition: all 0.3s ease !important;
    opacity: 0.4 !important;
}

[data-testid="stChatInput"] button[data-testid="stChatInputSubmitButton"]:not(:disabled) {
    background: rgba(0,255,127,0.12) !important;
    border: 1px solid rgba(0,255,127,0.4) !important;
    color: #00FF7F !important;
    box-shadow: 0 0 14px rgba(0,255,127,0.2) !important;
    opacity: 1 !important;
}

[data-testid="stChatInput"] button[data-testid="stChatInputSubmitButton"]:not(:disabled):hover {
    background: #00FF7F !important;
    border-color: #00FF7F !important;
    color: #000 !important;
    box-shadow: 0 0 24px rgba(0,255,127,0.5) !important;
    transform: scale(1.08) !important;
}

[data-testid="stChatInput"] button[data-testid="stChatInputSubmitButton"] svg {
    color: inherit !important; fill: currentColor !important;
}

.api-dot { display: inline-block; width: 6px; height: 6px; border-radius: 50%; margin-right: 6px; }
.api-dot-on { background: var(--accent); box-shadow: 0 0 8px var(--accent); }
.api-dot-off { background: #EF4444; box-shadow: 0 0 8px rgba(239,68,68,0.5); }

.footer-line { text-align: center; color: var(--text-muted); font-size: 0.62rem; padding: 24px 0 8px; letter-spacing: 1px; opacity: 0.3; }

div[data-testid="stExpander"] { background: rgba(30,30,30,0.5); backdrop-filter: blur(12px); border: 1px solid var(--border); border-radius: 10px; }
div[data-testid="stExpander"] summary p { color: var(--text-secondary) !important; font-size: 0.78rem !important; }

.stSlider label, [data-testid="stSlider"] label { color: var(--text-muted) !important; font-size: 0.75rem !important; }
.stSlider [data-baseweb="slider"], [data-testid="stSlider"] [data-baseweb="slider"] { --slider-color: var(--accent) !important; }

h1, h2, h3, h4, h5, h6 { color: var(--text) !important; }
.stMarkdown, [data-testid="stMarkdown"] { color: var(--text); }
.stAlert, [data-testid="stAlert"] { border-radius: 8px; }

[data-testid="stButton"] > button {
    background: rgba(30,30,30,0.5) !important; backdrop-filter: blur(12px) !important;
    border: 1px solid var(--border) !important; color: var(--text-secondary) !important;
    border-radius: 8px !important; font-size: 0.80rem !important; transition: all 0.2s ease !important;
}

[data-testid="stButton"] > button:hover {
    border-color: rgba(0,255,127,0.3) !important; color: var(--text) !important;
    box-shadow: 0 0 12px var(--accent-glow) !important;
}

section[data-testid="stSidebar"] .streamlit-expanderHeader { background: transparent !important; border: none !important; font-size: 0.72rem !important; }

.stSpinner > div { border-color: var(--accent) transparent transparent transparent !important; }

.page-title { font-size: 1.4rem; font-weight: 700; color: var(--text); margin-bottom: 20px; padding-bottom: 12px; border-bottom: 1px solid var(--border); }
.page-subtitle { font-size: 0.75rem; color: var(--text-muted); margin-bottom: 24px; }

.file-list-item { background: rgba(30,30,30,0.4); border: 1px solid var(--border); border-radius: 8px; padding: 12px 16px; margin-bottom: 8px; display: flex; justify-content: space-between; align-items: center; }
.file-list-item:hover { border-color: rgba(0,255,127,0.2); background: rgba(30,30,30,0.6); }

.config-card { background: rgba(30,30,30,0.5); border: 1px solid var(--border); border-radius: 12px; padding: 20px; margin-bottom: 16px; }
.config-label { font-size: 0.72rem; color: var(--text-muted); text-transform: uppercase; letter-spacing: 1px; margin-bottom: 8px; }
.config-value { font-size: 0.90rem; color: var(--text); font-family: 'JetBrains Mono', monospace; }

[data-testid="stHorizontalBlock"] [data-testid="stButton"] button {
    background: rgba(30,30,30,0.4) !important;
    backdrop-filter: blur(12px) !important;
    -webkit-backdrop-filter: blur(12px) !important;
    border: 1px solid rgba(0,255,127,0.2) !important;
    border-radius: 16px !important;
    padding: 20px 24px !important;
    min-height: 120px !important;
    text-align: left !important;
    justify-content: flex-start !important;
    align-items: flex-start !important;
    white-space: pre-line !important;
    word-break: break-word !important;
    line-height: 1.5 !important;
    font-size: 0.82rem !important;
    color: var(--text-secondary) !important;
    transition: all 0.3s ease !important;
    box-shadow: 0 0 0 rgba(0,255,127,0) !important;
    display: flex !important;
    flex-direction: column !important;
}

[data-testid="stHorizontalBlock"] [data-testid="stButton"] button:hover {
    background: rgba(40,40,40,0.6) !important;
    border: 1px solid rgba(0,255,127,0.5) !important;
    box-shadow: 0 4px 20px rgba(0,255,127,0.12) !important;
    transform: translateY(-3px) !important;
    color: var(--text) !important;
}

[data-testid="stHorizontalBlock"] [data-testid="stButton"] button p {
    margin: 0 !important;
    padding: 0 !important;
}

[data-testid="stHorizontalBlock"] [data-testid="stButton"] button p:first-child {
    display: block !important;
}

.quick-card-title {
    color: #00FF7F !important;
    font-weight: 700 !important;
    font-size: 0.78rem !important;
    letter-spacing: 0.5px !important;
    margin-bottom: 6px !important;
    opacity: 0.9 !important;
}

.quick-card-desc {
    color: #A1A1AA !important;
    font-weight: 400 !important;
    font-size: 0.82rem !important;
    line-height: 1.5 !important;
}
</style>
"""

LUCIDE_ICONS = {
    "message-circle": '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M7.9 20A9 9 0 1 0 4 16.1L2 22Z"/></svg>',
    "database": '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M3 5V19A9 3 0 0 0 21 19V5"/><path d="M3 12A9 3 0 0 0 21 12"/></svg>',
    "file-text": '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z"/><path d="M14 2v4a2 2 0 0 0 2 2h4"/><path d="M10 9H8"/><path d="M16 13H8"/><path d="M16 17H8"/></svg>',
    "settings": '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 0-1 1.73V10a2 2 0 0 0 1 1.73l.43.25a2 2 0 0 1 1 1.73v.18a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 0 1-1.73V6.56a2 2 0 0 0-1-1.73l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"/><circle cx="12" cy="12" r="3"/></svg>',
    "file-code": '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/><path d="m10 13-2 2 2 2"/><path d="m14 17 2-2-2-2"/></svg>',
    "history": '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5"/><path d="M12 7v5l4 2"/></svg>',
    "wrench": '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 0-7.94-7.94l-3.77 3.77a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0Z"/><path d="M8.56 2.75a4.5 4.5 0 0 0-5.81 5.81l1.87 1.87a1 1 0 0 0 1.4 0l5.2-5.2a1 1 0 0 0 0-1.4Z"/><path d="M3.5 15a1 1 0 0 0 0 1.4l5.2 5.2a1 1 0 0 0 1.4 0l1.87-1.87a4.5 4.5 0 0 0-5.81-5.81Z"/></svg>',
    "file-text-2": '<svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z"/><path d="M14 2v4a2 2 0 0 0 2 2h4"/><path d="M10 9H8"/><path d="M16 13H8"/><path d="M16 17H8"/></svg>',
    "layers": '<svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m12.83 2.18a2 2 0 0 0-1.66 0L2.6 6.08a1 1 0 0 0 0 1.83l8.58 3.91a2 2 0 0 0 1.66 0l8.58-3.9a1 1 0 0 0 0-1.83Z"/><path d="m22.4 12.08-8.58 3.91a2 2 0 0 1-1.66 0l-8.58-3.9"/><path d="m22.4 17.08-8.58 3.91a2 2 0 0 1-1.66 0l-8.58-3.9"/></svg>',
    "brain": '<svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5a3 3 0 1 0-5.997.125 4 4 0 0 0-2.526 5.77 4 4 0 0 0 .556 6.588A4 4 0 1 0 12 18Z"/><path d="M12 5a3 3 0 1 1 5.997.125 4 4 0 0 1 2.526 5.77 4 4 0 0 1-.556 6.588A4 4 0 1 1 12 18Z"/><path d="M15 13a4.5 4.5 0 0 1-3-4 4.5 4.5 0 0 1-3 4"/></svg>',
    "database-2": '<svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M3 5V19A9 3 0 0 0 21 19V5"/><path d="M3 12A9 3 0 0 0 21 12"/></svg>',
    "arrow-up-right": '<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M7 7h10v10"/><path d="M7 17 17 7"/></svg>',
    "calendar-days": '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 2v4"/><path d="M16 2v4"/><rect width="18" height="18" x="3" y="4" rx="2"/><path d="M3 10h18"/><path d="M8 14h.01"/><path d="M12 14h.01"/><path d="M16 14h.01"/><path d="M8 18h.01"/><path d="M12 18h.01"/><path d="M16 18h.01"/></svg>',
    "activity": '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 12h-2.48a2 2 0 0 0-1.93 1.46l-2.35 8.36a.25.25 0 0 1-.48 0L9.24 2.18a.25.25 0 0 0-.48 0l-2.35 8.36A2 2 0 0 1 4.49 12H2"/></svg>',
    "gauge": '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 12l-3-3"/><path d="M16.24 7.76a6 6 0 0 1 0 8.49m-8.48-.01a6 6 0 0 1 0-8.49m11.31-2.82a10 10 0 0 1 0 14.14m-14.14 0a10 10 0 0 1 0-14.14"/></svg>',
    "footprints": '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 16v-2.38C4 11.5 2.97 10.5 3 8c.03-2.72 1.49-6 4.5-6C9.37 2 10 3.8 10 5.5c0 3.11-2 5.66-2 8.68V16h4v-2.38c0-2.12-1.03-3.12-1-5.62.03-2.72 1.49-6 4.5-6 1.87 0 2.5 1.8 2.5 3.5 0 3.11-2 5.66-2 8.68V16h4"/></svg>',
}

NAV_MENU = [
    ("message-circle", "对话"),
    ("database", "现有知识库"),
    ("file-text", "知识库文件"),
    ("settings", "模型配置"),
    ("file-code", "提示词模板"),
    ("history", "对话历史"),
    ("wrench", "开发者工具"),
]

QUICK_QUESTIONS = [
    ("calendar-days", "训练计划", "帮我制定本周的排酸跑计划？"),
    ("activity", "伤病恢复", "刚跑完膝盖外侧疼，可能是什么原因？"),
    ("gauge", "配速建议", "半马目标1h45m，前5公里该怎么跑？"),
    ("footprints", "跑者装备", "新手怎么选缓震跑鞋和竞速鞋？"),
]


@st.cache_resource
def init_embeddings():
    model_path = str(LOCAL_MODEL_DIR) if LOCAL_MODEL_DIR.exists() else EMBEDDING_MODEL
    return HuggingFaceEmbeddings(
        model_name=model_path,
        model_kwargs={"device": "cpu"},
        encode_kwargs={"normalize_embeddings": True},
    )


@st.cache_resource
def init_vectorstore(_embeddings):
    chroma_path = CHROMA_DIR.resolve()
    if not chroma_path.exists():
        return None
    return Chroma(
        persist_directory=str(chroma_path),
        embedding_function=_embeddings,
    )


def init_llm_client():
    api_key = os.getenv("OPENAI_API_KEY", "")
    base_url = os.getenv("OPENAI_BASE_URL", "")
    model = os.getenv("LLM_MODEL", "")
    if not api_key:
        return None, model
    client = OpenAI(api_key=api_key, base_url=base_url)
    return client, model


def retrieve_context(vectorstore, query, top_k):
    results = vectorstore.similarity_search_with_score(query, k=top_k)
    if not results:
        return [], []
    contexts = []
    sources = []
    for doc, score in results:
        contexts.append(doc.page_content)
        source_name = doc.metadata.get("source", "未知")
        chunk_id = doc.metadata.get("chunk_id", "?")
        sources.append(f"{source_name} (chunk {chunk_id})")
    return contexts, sources


def generate_answer(client, model, query, contexts, history):
    context_text = "\n\n---\n\n".join(contexts)
    messages = [{"role": "system", "content": SYSTEM_PROMPT}]
    for msg in history[-6:]:
        messages.append({"role": "user", "content": msg["user"]})
        messages.append({"role": "assistant", "content": msg["assistant"]})
    user_content = f"参考知识库内容：\n{context_text}\n\n用户问题：{query}"
    messages.append({"role": "user", "content": user_content})
    response = client.chat.completions.create(
        model=model,
        messages=messages,
        temperature=0.3,
        max_tokens=1024,
    )
    return response.choices[0].message.content


def get_knowledge_files():
    data_path = DATA_DIR.resolve()
    if not data_path.exists():
        return []
    files = []
    for f in sorted(data_path.rglob("*")):
        if f.suffix.lower() in (".md", ".txt") and f.is_file():
            files.append(f.relative_to(data_path))
    return files


def render_sidebar_nav():
    with st.sidebar:
        st.markdown(
            """
            <div class="sidebar-brand">
                <div class="sidebar-brand-name">RUNWISE</div>
                <div class="sidebar-brand-sub">Knowledge Runner</div>
            </div>
            """,
            unsafe_allow_html=True,
        )
        st.markdown('<div class="sidebar-divider"></div>', unsafe_allow_html=True)

        if "nav_page" not in st.session_state:
            st.session_state.nav_page = "对话"

        for icon_key, label in NAV_MENU:
            if st.button(
                label,
                key=f"nav_{label}",
                use_container_width=True,
            ):
                st.session_state.nav_page = label
                st.rerun()

        active_page = st.session_state.nav_page

        sidebar_highlight_js = f"""
        <script>
        (function() {{
            try {{
                var doc = parent.document || window.top.document;

                var navButtons = doc.querySelectorAll('section[data-testid="stSidebar"] [data-testid="stButton"] button');
                navButtons.forEach(function(btn) {{
                    btn.style.color = '';
                    btn.style.background = '';
                    btn.style.borderLeft = '';
                    if (btn.textContent.trim() === '{active_page}') {{
                        btn.style.color = '#00FF7F';
                        btn.style.background = 'rgba(0,255,127,0.06)';
                        btn.style.borderLeft = '3px solid #00FF7F';
                    }}
                }});

                var quickBtns = doc.querySelectorAll('[data-testid="stHorizontalBlock"] [data-testid="stButton"] button p');
                quickBtns.forEach(function(p) {{
                    var text = p.textContent || '';
                    if (text.indexOf('\\n') > -1) {{
                        var parts = text.split('\\n');
                        p.innerHTML = '<span style="color:#00FF7F;font-weight:700;font-size:0.78rem;letter-spacing:0.5px;opacity:0.9;">' + parts[0].trim() + '</span><br><span style="color:#A1A1AA;font-weight:400;font-size:0.82rem;line-height:1.5;">' + parts.slice(1).join('\\n').trim() + '</span>';
                    }}
                }});
            }} catch(e) {{}}
        }})();
        </script>
        """
        st.iframe(sidebar_highlight_js, height=1)

        return st.session_state.nav_page


def render_hero():
    st.markdown(
        """
        <div class="hero-center">
            <div class="hero-title">RUNWISE RAG</div>
            <div class="hero-sub">
                您的全能型大学生运动训练与恢复顾问
                <span class="hero-sub-sep">|</span>
                Run with Intelligence
            </div>
            <div class="hero-line"></div>
        </div>
        """,
        unsafe_allow_html=True,
    )


def render_status_bar(vectorstore):
    kb_files = get_knowledge_files()
    try:
        doc_count = vectorstore._collection.count()
    except Exception:
        doc_count = 0

    badges = [
        ("file-text-2", "文件", f"{len(kb_files)}"),
        ("layers", "块", f"{doc_count}"),
        ("brain", "模型", "bge-zh"),
        ("database-2", "库", "Chroma"),
    ]
    badges_html = "".join(
        f'<span class="status-badge">'
        f'{LUCIDE_ICONS.get(icon, "")} '
        f'{label} <span class="badge-val">{val}</span>'
        f'</span>'
        for icon, label, val in badges
    )
    st.markdown(
        f'<div class="status-bar">{badges_html}</div>',
        unsafe_allow_html=True,
    )


def render_quick_questions():
    cols = st.columns(2)

    for i, (icon_key, label, question) in enumerate(QUICK_QUESTIONS):
        with cols[i % 2]:
            btn_label = f"{label}\n{question}"
            if st.button(
                btn_label,
                key=f"quick_btn_{i}",
                use_container_width=True,
            ):
                st.session_state.quick_question = question
                st.rerun()


def render_source_badges(sources):
    if not sources:
        return
    badges = " ".join(f'<span class="source-tag">{s}</span>' for s in sources)
    st.markdown(f'<div style="margin-top:8px;">{badges}</div>', unsafe_allow_html=True)


def render_input_disclaimer():
    st.markdown(
        '<div class="input-disclaimer">RunWise 可能产生不准确的信息，请注意甄别</div>',
        unsafe_allow_html=True,
    )


def render_page_chat(vectorstore, top_k):
    render_hero()
    render_status_bar(vectorstore)

    if "messages" not in st.session_state:
        st.session_state.messages = []

    show_quick = len(st.session_state.messages) == 0

    if show_quick:
        render_quick_questions()

    for msg in st.session_state.messages:
        with st.chat_message(msg["role"]):
            st.markdown(msg["content"])
            if msg["role"] == "assistant" and "sources" in msg:
                with st.expander("查看参考来源"):
                    render_source_badges(msg["sources"])

    prompt = st.chat_input("输入你的跑步问题...")

    if "quick_question" not in st.session_state:
        st.session_state.quick_question = None

    if st.session_state.quick_question:
        prompt = st.session_state.quick_question
        st.session_state.quick_question = None

    if prompt:
        st.chat_message("user").markdown(prompt)
        st.session_state.messages.append({"role": "user", "content": prompt})

        with st.chat_message("assistant"):
            with st.spinner("检索中..."):
                contexts, sources = retrieve_context(vectorstore, prompt, top_k)

            if not contexts:
                warning = "知识库中没有找到足够依据，以下回答可能不可靠。"
                st.markdown(f'<div class="warning-strip">{warning}</div>', unsafe_allow_html=True)
                answer_prefix = warning + "\n\n"
                contexts_for_llm = []
            else:
                answer_prefix = ""
                contexts_for_llm = contexts

            client, model = init_llm_client()

            if client is None:
                answer = "API Key 未配置，无法调用大模型。请在 `.env` 文件中设置 `OPENAI_API_KEY`。"
            else:
                with st.spinner("生成回答..."):
                    history = []
                    for m in st.session_state.messages[:-1]:
                        if m["role"] == "user":
                            history.append({"user": m["content"], "assistant": ""})
                        elif m["role"] == "assistant" and history:
                            history[-1]["assistant"] = m["content"]
                    answer = generate_answer(client, model, prompt, contexts_for_llm, history)

            full_answer = answer_prefix + answer
            st.markdown(full_answer)

            if sources:
                with st.expander("查看参考来源"):
                    render_source_badges(sources)

        msg_to_save = {"role": "assistant", "content": full_answer}
        if sources:
            msg_to_save["sources"] = sources
        st.session_state.messages.append(msg_to_save)

    render_input_disclaimer()

    st.markdown(
        '<div class="footer-line">RUNWISE RAG · KNOWLEDGE DRIVES EVERY STEP</div>',
        unsafe_allow_html=True,
    )


def render_page_knowledge_base():
    st.markdown('<div class="page-title">现有知识库</div>', unsafe_allow_html=True)
    st.markdown('<div class="page-subtitle">查看向量数据库中的知识块统计</div>', unsafe_allow_html=True)

    embeddings = init_embeddings()
    vectorstore = init_vectorstore(embeddings)

    if vectorstore is None:
        st.error("向量库未找到！请先运行 `python build_index.py` 构建索引。")
        return

    try:
        doc_count = vectorstore._collection.count()
    except Exception:
        doc_count = 0

    kb_files = get_knowledge_files()

    col1, col2 = st.columns(2)
    with col1:
        st.markdown(
            f"""
            <div class="config-card">
                <div class="config-label">知识库文件</div>
                <div class="config-value">{len(kb_files)} 个</div>
            </div>
            """,
            unsafe_allow_html=True,
        )
    with col2:
        st.markdown(
            f"""
            <div class="config-card">
                <div class="config-label">向量块数</div>
                <div class="config-value">{doc_count} 块</div>
            </div>
            """,
            unsafe_allow_html=True,
        )

    st.markdown('<div class="page-title" style="margin-top:24px;">文件列表</div>', unsafe_allow_html=True)

    for f in kb_files:
        st.markdown(
            f"""
            <div class="file-list-item">
                <span>{f}</span>
                <span style="color:var(--accent);font-size:0.7rem;">.md</span>
            </div>
            """,
            unsafe_allow_html=True,
        )


def render_page_kb_files():
    st.markdown('<div class="page-title">知识库文件</div>', unsafe_allow_html=True)
    st.markdown('<div class="page-subtitle">浏览和预览知识库中的 Markdown 文件</div>', unsafe_allow_html=True)

    kb_files = get_knowledge_files()

    if not kb_files:
        st.warning("未找到知识库文件")
        return

    file_names = [str(f) for f in kb_files]
    selected = st.selectbox("选择文件", file_names, index=0, key="kb_file_select")

    if selected:
        file_path = DATA_DIR.resolve() / selected
        if file_path.exists():
            try:
                content = file_path.read_text(encoding="utf-8")
                lines = content.split("\n")
                st.markdown(
                    f"""
                    <div class="config-card" style="margin-bottom:12px;">
                        <div class="config-label">文件信息</div>
                        <div class="config-value" style="font-size:0.75rem;">
                            路径: {selected}<br>
                            行数: {len(lines)} 行<br>
                            字符: {len(content)} 字符
                        </div>
                    </div>
                    """,
                    unsafe_allow_html=True,
                )
                st.markdown(
                    f"""
                    <div style="
                        background: rgba(30,30,30,0.5);
                        border: 1px solid var(--border);
                        border-radius: 8px;
                        padding: 16px;
                        max-height: 500px;
                        overflow-y: auto;
                        font-size: 0.82rem;
                        line-height: 1.8;
                        color: #999;
                        white-space: pre-wrap;
                    ">{content}</div>
                    """,
                    unsafe_allow_html=True,
                )
            except Exception as e:
                st.error(f"读取失败: {e}")


def render_page_model_config():
    st.markdown('<div class="page-title">模型配置</div>', unsafe_allow_html=True)
    st.markdown('<div class="page-subtitle">查看和修改 LLM 与 Embedding 模型配置</div>', unsafe_allow_html=True)

    api_key = os.getenv("OPENAI_API_KEY", "")
    base_url = os.getenv("OPENAI_BASE_URL", "")
    llm_model = os.getenv("LLM_MODEL", "")

    dot_class = "api-dot-on" if api_key else "api-dot-off"
    key_status = "已配置" if api_key else "未配置"
    key_preview = api_key[:8] + "..." + api_key[-4:] if len(api_key) > 12 else api_key

    st.markdown(
        f"""
        <div class="config-card">
            <div class="config-label">API Key</div>
            <div class="config-value">
                <span class="api-dot {dot_class}"></span>
                {key_status}
                <span style="color:var(--text-muted);font-size:0.7rem;margin-left:8px;">{key_preview}</span>
            </div>
        </div>
        """,
        unsafe_allow_html=True,
    )

    st.markdown(
        f"""
        <div class="config-card">
            <div class="config-label">Base URL</div>
            <div class="config-value" style="font-size:0.75rem;">{base_url or "未配置"}</div>
        </div>
        """,
        unsafe_allow_html=True,
    )

    st.markdown(
        f"""
        <div class="config-card">
            <div class="config-label">LLM Model</div>
            <div class="config-value">{llm_model or "未配置"}</div>
        </div>
        """,
        unsafe_allow_html=True,
    )

    st.markdown(
        f"""
        <div class="config-card">
            <div class="config-label">Embedding Model</div>
            <div class="config-value">{EMBEDDING_MODEL}</div>
        </div>
        """,
        unsafe_allow_html=True,
    )

    st.markdown(
        f"""
        <div class="config-card">
            <div class="config-label">向量数据库路径</div>
            <div class="config-value" style="font-size:0.75rem;">{CHROMA_DIR.resolve()}</div>
        </div>
        """,
        unsafe_allow_html=True,
    )


def render_page_prompt_template():
    st.markdown('<div class="page-title">提示词模板</div>', unsafe_allow_html=True)
    st.markdown('<div class="page-subtitle">查看和编辑 System Prompt</div>', unsafe_allow_html=True)

    st.markdown(
        f"""
        <div style="
            background: rgba(30,30,30,0.5);
            border: 1px solid var(--border);
            border-radius: 8px;
            padding: 16px;
            font-size: 0.82rem;
            line-height: 1.8;
            color: #999;
            white-space: pre-wrap;
        ">{SYSTEM_PROMPT}</div>
        """,
        unsafe_allow_html=True,
    )


def render_page_chat_history():
    st.markdown('<div class="page-title">对话历史</div>', unsafe_allow_html=True)
    st.markdown('<div class="page-subtitle">查看和管理当前会话的对话记录</div>', unsafe_allow_html=True)

    if "messages" not in st.session_state:
        st.session_state.messages = []

    messages = st.session_state.messages

    if not messages:
        st.info("暂无对话记录")
        return

    st.markdown(
        f"""
        <div class="config-card" style="margin-bottom:16px;">
            <div class="config-label">对话统计</div>
            <div class="config-value">共 {len(messages)} 条消息</div>
        </div>
        """,
        unsafe_allow_html=True,
    )

    for i, msg in enumerate(messages):
        role_label = "用户" if msg["role"] == "user" else "助手"
        role_color = "var(--accent)" if msg["role"] == "user" else "var(--accent2)"
        st.markdown(
            f"""
            <div style="
                background: rgba(30,30,30,0.4);
                border: 1px solid var(--border);
                border-radius: 8px;
                padding: 12px 16px;
                margin-bottom: 8px;
            ">
                <div style="font-size:0.7rem;color:{role_color};margin-bottom:6px;">{role_label} #{i+1}</div>
                <div style="font-size:0.82rem;color:var(--text);">{msg['content'][:200]}{'...' if len(msg['content']) > 200 else ''}</div>
            </div>
            """,
            unsafe_allow_html=True,
        )

    if st.button("清空对话历史", key="clear_history_btn"):
        st.session_state.messages = []
        st.success("对话历史已清空")
        st.rerun()


def render_page_dev_tools():
    st.markdown('<div class="page-title">开发者工具</div>', unsafe_allow_html=True)
    st.markdown('<div class="page-subtitle">调试参数和系统信息</div>', unsafe_allow_html=True)

    top_k = st.slider("检索 Top K", 1, 10, 3, key="dev_top_k")

    st.markdown(
        f"""
        <div class="config-card">
            <div class="config-label">当前 Top K</div>
            <div class="config-value">{top_k}</div>
        </div>
        """,
        unsafe_allow_html=True,
    )

    st.markdown(
        f"""
        <div class="config-card">
            <div class="config-label">数据目录</div>
            <div class="config-value" style="font-size:0.75rem;">{DATA_DIR.resolve()}</div>
        </div>
        """,
        unsafe_allow_html=True,
    )

    st.markdown(
        f"""
        <div class="config-card">
            <div class="config-label">Chroma 目录</div>
            <div class="config-value" style="font-size:0.75rem;">{CHROMA_DIR.resolve()}</div>
        </div>
        """,
        unsafe_allow_html=True,
    )

    st.markdown(
        f"""
        <div class="config-card">
            <div class="config-label">本地模型目录</div>
            <div class="config-value" style="font-size:0.75rem;">{LOCAL_MODEL_DIR}</div>
        </div>
        """,
        unsafe_allow_html=True,
    )

    return top_k


def main():
    st.set_page_config(
        page_title="RunWise RAG",
        page_icon="🏃",
        layout="wide",
        initial_sidebar_state="expanded",
    )
    st.markdown(CUSTOM_CSS, unsafe_allow_html=True)

    current_page = render_sidebar_nav()

    top_k = st.session_state.get("dev_top_k", 3)

    if current_page == "对话":
        with st.spinner("正在加载 Embedding 模型，首次加载约需 1-2 分钟..."):
            embeddings = init_embeddings()
            vectorstore = init_vectorstore(embeddings)
        if vectorstore is None:
            st.error("向量库未找到！请先运行 `python build_index.py` 构建索引。")
            st.stop()
        render_page_chat(vectorstore, top_k)
    elif current_page == "现有知识库":
        render_page_knowledge_base()
    elif current_page == "知识库文件":
        render_page_kb_files()
    elif current_page == "模型配置":
        render_page_model_config()
    elif current_page == "提示词模板":
        render_page_prompt_template()
    elif current_page == "对话历史":
        render_page_chat_history()
    elif current_page == "开发者工具":
        render_page_dev_tools()


if __name__ == "__main__":
    main()

