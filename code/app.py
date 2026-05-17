"""
RunWise RAG - Streamlit 聊天界面
面向大学生跑者的训练与恢复知识问答助手
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
    --accent-glow: rgba(0,255,127,0.12);
    --accent-soft: rgba(0,255,127,0.06);
    --accent2: #818CF8;
    --gradient-brand: linear-gradient(135deg, #00FF7F 0%, #7C3AED 100%);
    --gradient-brand-text: linear-gradient(135deg, #4ADE80 0%, #22D3EE 50%, #818CF8 100%);
    --bg: #0A0A0A;
    --bg-surface: #181818;
    --bg-elevated: #2A2A2A;
    --bg-glass: rgba(255,255,255,0.025);
    --border: rgba(255,255,255,0.06);
    --border-hover: rgba(255,255,255,0.12);
    --text: #F5F5F5;
    --text-secondary: #A1A1AA;
    --text-muted: #52525B;
}

* { box-sizing: border-box; }

.stApp {
    background: var(--bg) !important;
    color: var(--text);
    font-family: 'Inter', 'Noto Sans SC', sans-serif;
}

.stApp > header { background-color: transparent !important; }

section[data-testid="stSidebar"] {
    background: rgba(10,10,10,0.98) !important;
    border-right: 1px solid var(--border) !important;
}

.hero-center {
    text-align: center;
    padding: 48px 0 16px;
}

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
    filter: drop-shadow(0 0 30px rgba(0,255,127,0.12));
}

.hero-sub {
    font-size: 0.76rem;
    color: var(--text-muted);
    margin-top: 14px;
    margin-bottom: 32px;
    letter-spacing: 1.5px;
    font-weight: 400;
    line-height: 1.6;
}

.hero-sub .hero-sub-sep {
    margin: 0 8px;
    opacity: 0.3;
}

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
    opacity: 0.3;
}

.status-bar {
    display: flex;
    justify-content: center;
    gap: 10px;
    padding: 0 0 36px;
}

.status-badge {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    background: rgba(20,20,20,0.8);
    border: 1px solid rgba(255,255,255,0.03);
    border-radius: 20px;
    padding: 3px 10px;
    font-size: 0.60rem;
    color: var(--text-muted);
    letter-spacing: 0.3px;
    transition: all 0.25s ease;
}

.status-badge:hover {
    border-color: rgba(0,255,127,0.10);
    background: rgba(0,255,127,0.02);
}

.status-badge svg {
    width: 10px;
    height: 10px;
    stroke: var(--accent);
    stroke-width: 2;
    fill: none;
    opacity: 0.5;
}

.status-badge .badge-val {
    font-family: 'JetBrains Mono', monospace;
    color: var(--accent);
    font-weight: 600;
    font-size: 0.60rem;
}

.quick-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    margin: 0 auto 40px;
    max-width: 640px;
}

.quick-card {
    background: rgba(30,30,30,0.5);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border: 1px solid rgba(255,255,255,0.06);
    border-radius: 14px;
    padding: 16px 18px;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    min-height: 60px;
    position: relative;
    overflow: hidden;
}

.quick-card::before {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: 14px;
    padding: 1px;
    background: linear-gradient(135deg, rgba(0,255,127,0.15) 0%, rgba(129,140,248,0.10) 50%, transparent 100%);
    -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
    -webkit-mask-composite: xor;
    mask-composite: exclude;
    opacity: 0;
    transition: opacity 0.3s ease;
}

.quick-card:hover {
    border-color: rgba(0,255,127,0.15);
    background: rgba(30,30,30,0.7);
    transform: translateY(-3px);
    box-shadow: 0 12px 32px rgba(0,0,0,0.35);
}

.quick-card:hover::before {
    opacity: 1;
}

.quick-card .q-label {
    font-size: 0.64rem;
    color: var(--accent);
    font-weight: 600;
    letter-spacing: 0.5px;
    margin-bottom: 3px;
    opacity: 0.6;
    transition: opacity 0.25s ease;
}

.quick-card:hover .q-label {
    opacity: 0.9;
}

.quick-card .q-text {
    font-size: 0.82rem;
    color: var(--text-secondary);
    line-height: 1.45;
    text-align: left;
    flex: 1;
}

.quick-card:hover .q-text {
    color: var(--text);
}

.quick-card .q-arrow {
    flex-shrink: 0;
    margin-left: 10px;
    opacity: 0;
    transform: translateX(-4px);
    transition: all 0.3s ease;
}

.quick-card:hover .q-arrow {
    opacity: 0.5;
    transform: translateX(0);
}

.quick-card .q-arrow svg {
    width: 14px;
    height: 14px;
    stroke: var(--accent);
    stroke-width: 2;
    fill: none;
}

.source-tag {
    display: inline-block;
    background: rgba(0,255,127,0.06);
    color: var(--accent);
    border: 1px solid rgba(0,255,127,0.12);
    border-radius: 4px;
    padding: 2px 8px;
    font-size: 0.72rem;
    font-family: 'JetBrains Mono', monospace;
    margin: 2px 3px 2px 0;
}

.warning-strip {
    background: rgba(251,191,36,0.06);
    border-left: 3px solid #FBBF24;
    border-radius: 0 8px 8px 0;
    padding: 10px 16px;
    color: #FCD34D;
    font-size: 0.88rem;
    margin-bottom: 12px;
}

div.stChatMessage {
    background: var(--bg-glass) !important;
    border-radius: 12px !important;
    border: none !important;
    margin-bottom: 6px;
    padding: 16px 20px !important;
}

div.stChatMessage[data-testid="user-chat-message"] {
    background: rgba(0,255,127,0.03) !important;
    border-left: 2px solid rgba(0,255,127,0.15) !important;
}

.input-disclaimer {
    text-align: center;
    font-size: 0.60rem;
    color: var(--text-muted);
    opacity: 0.35;
    margin-top: 8px;
    max-width: 760px;
    margin-left: auto;
    margin-right: auto;
}

div[data-testid="stBottomBlockContainer"] {
    background: rgba(10,10,10,0.96) !important;
    backdrop-filter: blur(28px) !important;
    -webkit-backdrop-filter: blur(28px) !important;
    border-top: 1px solid rgba(255,255,255,0.03) !important;
    padding: 14px 0 20px !important;
}

div.stChatInput {
    border-radius: 14px;
    margin: 0 auto;
    max-width: 720px;
    padding: 0 !important;
}

div.stChatInput > div > div {
    background: #181818 !important;
    border: 1px solid rgba(255,255,255,0.05) !important;
    border-radius: 14px;
    box-shadow: inset 0 2px 10px rgba(0,0,0,0.5), inset 0 1px 3px rgba(0,0,0,0.3);
    transition: all 0.3s ease;
    padding: 4px 4px !important;
}

div.stChatInput > div > div:focus-within {
    border-color: var(--accent) !important;
    box-shadow: inset 0 2px 10px rgba(0,0,0,0.5), inset 0 1px 3px rgba(0,0,0,0.3), 0 0 0 1px rgba(0,255,127,0.1);
}

div.stChatInput textarea {
    font-family: 'Inter', 'Noto Sans SC', sans-serif !important;
    font-size: 0.90rem !important;
    color: var(--text) !important;
    caret-color: var(--accent) !important;
    background: transparent !important;
    line-height: 1.5;
    min-height: 28px !important;
}

div.stChatInput textarea::placeholder {
    color: var(--text-muted) !important;
}

div.stChatInput button[data-testid="stChatInputSubmitButton"] {
    background: var(--bg-elevated) !important;
    border: 1px solid var(--border) !important;
    border-radius: 8px !important;
    width: 32px !important;
    height: 32px !important;
    min-width: 32px !important;
    min-height: 32px !important;
    padding: 0 !important;
    display: flex !important;
    align-items: center !important;
    justify-content: center !important;
    color: var(--text-muted) !important;
    transition: all 0.25s ease !important;
}

div.stChatInput button[data-testid="stChatInputSubmitButton"]:hover {
    background: var(--accent) !important;
    border-color: var(--accent) !important;
    color: #000 !important;
    box-shadow: 0 0 16px rgba(0,255,127,0.35) !important;
}

div.stChatInput button[data-testid="stChatInputSubmitButton"] svg {
    color: inherit !important;
}

.sidebar-brand {
    text-align: center;
    padding: 16px 0 20px;
}

.sidebar-brand-name {
    font-family: 'Inter', sans-serif;
    font-size: 1.1rem;
    font-weight: 900;
    font-style: italic;
    background: var(--gradient-brand-text);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
}

.sidebar-brand-sub {
    font-size: 0.55rem;
    color: var(--text-muted);
    margin-top: 3px;
    letter-spacing: 3px;
    text-transform: uppercase;
}

.sidebar-divider {
    height: 1px;
    background: var(--border);
    margin: 12px 0;
}

.kb-title {
    font-size: 9px;
    color: var(--accent);
    font-weight: 500;
    letter-spacing: 2px;
    margin-bottom: 8px;
    text-transform: uppercase;
    padding-left: 14px;
    opacity: 0.7;
}

section[data-testid="stSidebar"] .stButton > button {
    display: flex !important;
    align-items: center !important;
    gap: 10px !important;
    background: transparent !important;
    border: none !important;
    border-left: 2px solid transparent !important;
    border-radius: 0 8px 8px 0 !important;
    padding: 10px 12px 10px 14px !important;
    text-align: left !important;
    font-size: 0.80rem !important;
    font-weight: 500 !important;
    color: var(--text-muted) !important;
    transition: all 0.2s ease !important;
    box-shadow: none !important;
    letter-spacing: 0.2px !important;
    width: 100% !important;
    line-height: 1.4 !important;
}

section[data-testid="stSidebar"] .stButton > button:hover {
    color: var(--accent) !important;
    background: rgba(0,255,127,0.05) !important;
    border-left: 2px solid var(--accent) !important;
    box-shadow: none !important;
    transform: none !important;
}

section[data-testid="stSidebar"] .stButton > button:active {
    transform: none !important;
}

.api-dot {
    display: inline-block;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    margin-right: 6px;
}

.api-dot-on { background: var(--accent); box-shadow: 0 0 6px var(--accent); }
.api-dot-off { background: #EF4444; box-shadow: 0 0 6px rgba(239,68,68,0.4); }

.footer-line {
    text-align: center;
    color: var(--text-muted);
    font-size: 0.65rem;
    padding: 24px 0 8px;
    letter-spacing: 1px;
    opacity: 0.35;
}

div[data-testid="stExpander"] {
    background: var(--bg-glass);
    border: 1px solid var(--border);
    border-radius: 8px;
}

div[data-testid="stExpander"] summary p {
    color: var(--text-secondary) !important;
    font-size: 0.78rem !important;
}

.stSlider label { color: var(--text-muted) !important; font-size: 0.75rem !important; }
.stSlider [data-baseweb="slider"] { --slider-color: var(--accent) !important; }

h1, h2, h3, h4, h5, h6 { color: var(--text) !important; }
.stMarkdown { color: var(--text); }
.stAlert { border-radius: 8px; }

.stButton > button {
    background: var(--bg-glass) !important;
    border: 1px solid var(--border) !important;
    color: var(--text-secondary) !important;
    border-radius: 8px !important;
    font-size: 0.80rem !important;
    transition: all 0.2s ease !important;
}

.stButton > button:hover {
    border-color: rgba(0,255,127,0.3) !important;
    color: var(--text) !important;
    box-shadow: 0 0 8px var(--accent-glow) !important;
}

section[data-testid="stSidebar"] .streamlit-expanderHeader {
    background: transparent !important;
    border: none !important;
    font-size: 0.72rem !important;
}

@keyframes fadeInUp {
    from { opacity: 0; transform: translateY(8px); }
    to { opacity: 1; transform: translateY(0); }
}

.quick-card {
    animation: fadeInUp 0.4s ease both;
}

.quick-card:nth-child(1) { animation-delay: 0.05s; }
.quick-card:nth-child(2) { animation-delay: 0.10s; }
.quick-card:nth-child(3) { animation-delay: 0.15s; }
.quick-card:nth-child(4) { animation-delay: 0.20s; }
</style>
"""

LUCIDE_ICONS = {
    "file-text": '<svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z"/><path d="M14 2v4a2 2 0 0 0 2 2h4"/><path d="M10 9H8"/><path d="M16 13H8"/><path d="M16 17H8"/></svg>',
    "layers": '<svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m12.83 2.18a2 2 0 0 0-1.66 0L2.6 6.08a1 1 0 0 0 0 1.83l8.58 3.91a2 2 0 0 0 1.66 0l8.58-3.9a1 1 0 0 0 0-1.83Z"/><path d="m22.4 12.08-8.58 3.91a2 2 0 0 1-1.66 0l-8.58-3.9"/><path d="m22.4 17.08-8.58 3.91a2 2 0 0 1-1.66 0l-8.58-3.9"/></svg>',
    "brain": '<svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5a3 3 0 1 0-5.997.125 4 4 0 0 0-2.526 5.77 4 4 0 0 0 .556 6.588A4 4 0 1 0 12 18Z"/><path d="M12 5a3 3 0 1 1 5.997.125 4 4 0 0 1 2.526 5.77 4 4 0 0 1-.556 6.588A4 4 0 1 1 12 18Z"/><path d="M15 13a4.5 4.5 0 0 1-3-4 4.5 4.5 0 0 1-3 4"/></svg>',
    "database": '<svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M3 5V19A9 3 0 0 0 21 19V5"/><path d="M3 12A9 3 0 0 0 21 12"/></svg>',
    "arrow-up-right": '<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M7 7h10v10"/><path d="M7 17 17 7"/></svg>',
    "shield": '<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z"/></svg>',
    "medal": '<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M7.21 15 2.66 7.14a2 2 0 0 1 .13-2.2L4.4 2.3A2 2 0 0 1 6.05 2h11.9a2 2 0 0 1 1.66.89l1.6 2.63a2 2 0 0 1 .14 2.2L16.79 15"/><circle cx="12" cy="17" r="5"/><path d="M12 14v2l1.5.5"/></svg>',
    "graduation-cap": '<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21.42 10.922a1 1 0 0 0-.019-1.838L12.83 5.18a2 2 0 0 0-1.66 0L2.6 9.08a1 1 0 0 0 0 1.832l8.57 3.908a2 2 0 0 0 1.66 0z"/><path d="M22 10v6"/><path d="M6 12.5V16a6 3 0 0 0 12 0v-3.5"/></svg>',
    "heart-pulse": '<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/><path d="M3.22 12H9.5l.5-1 2 4.5 2-7 1.5 3.5h5.27"/></svg>',
    "apple": '<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3c-1.2 0-2.4-.6-3-1.5A6.5 6.5 0 0 0 5.5 6 5.5 5.5 0 0 0 3 10c0 4 2.5 8 5.5 11 1 1 2.5 2 3.5 2s2.5-1 3.5-2c3-3 5.5-7 5.5-11a5.5 5.5 0 0 0-2.5-4A6.5 6.5 0 0 0 15 1.5c-.6.9-1.8 1.5-3 1.5Z"/></svg>',
    "target": '<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="6"/><circle cx="12" cy="12" r="2"/></svg>',
    "bandage": '<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 9a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 0 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2Z"/><path d="M13 5v2"/><path d="M13 17v2"/><path d="M13 11v2"/></svg>',
    "trophy": '<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6"/><path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18"/><path d="M4 22h16"/><path d="M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22"/><path d="M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22"/><path d="M18 2H6v7a6 6 0 0 0 12 0V2Z"/></svg>',
}

KB_FILE_ICONS = {
    "伤病预防": "bandage",
    "大学生跑步指南": "graduation-cap",
    "恢复与营养": "apple",
    "比赛准备": "trophy",
    "训练基础": "heart-pulse",
    "训练计划": "target",
}


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
    return sorted([f.name for f in data_path.iterdir() if f.suffix in (".md", ".txt")])


QUICK_QUESTIONS = [
    ("训练计划", "帮我制定本周的排酸跑计划？"),
    ("伤病恢复", "刚跑完膝盖外侧疼，可能是什么原因？"),
    ("配速建议", "半马目标1h45m，前5公里该怎么跑？"),
    ("跑者装备", "新手怎么选缓震跑鞋和竞速鞋？"),
]


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
        ("file-text", "文件", f"{len(kb_files)}"),
        ("layers", "块", f"{doc_count}"),
        ("brain", "模型", "bge-zh"),
        ("database", "库", "Chroma"),
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
    cards_html = ""
    arrow_svg = LUCIDE_ICONS["arrow-up-right"]
    for i, (label, question) in enumerate(QUICK_QUESTIONS):
        cards_html += (
            f'<div class="quick-card" data-idx="{i}">'
            f'<div style="flex:1;min-width:0;">'
            f'<div class="q-label">{label}</div>'
            f'<div class="q-text">{question}</div>'
            f'</div>'
            f'<span class="q-arrow">{arrow_svg}</span>'
            f'</div>'
        )

    st.markdown(f'<div class="quick-grid">{cards_html}</div>', unsafe_allow_html=True)


def render_sidebar():
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

        st.markdown(
            '<div class="kb-title">Knowledge Base</div>',
            unsafe_allow_html=True,
        )
        kb_files = get_knowledge_files()
        if "selected_kb_file" not in st.session_state:
            st.session_state.selected_kb_file = None

        if kb_files:
            for f in kb_files:
                base_name = f.replace(".md", "").replace(".txt", "")
                is_active = st.session_state.selected_kb_file == f

                if st.button(
                    base_name,
                    key=f"kb_btn_{base_name}",
                    use_container_width=True,
                ):
                    st.session_state.selected_kb_file = f
                    st.rerun()

                if is_active:
                    st.markdown(
                        f"""<style>
                        section[data-testid="stSidebar"] .stButton > button[data-testid="baseButton-secondary-kb_btn_{base_name}"] {{
                            color: var(--accent) !important;
                            background: rgba(0,255,127,0.06) !important;
                            border-left: 2px solid var(--accent) !important;
                        }}
                        </style>""",
                        unsafe_allow_html=True,
                    )

            if st.session_state.selected_kb_file:
                file_path = DATA_DIR.resolve() / st.session_state.selected_kb_file
                if file_path.exists():
                    preview_name = st.session_state.selected_kb_file.replace('.md', '').replace('.txt', '')
                    with st.expander(f"Preview: {preview_name}", expanded=True):
                        try:
                            content = file_path.read_text(encoding="utf-8")
                            st.markdown(
                                f"""
                                <div style="
                                    background: rgba(255,255,255,0.02);
                                    border:1px solid rgba(255,255,255,0.05);
                                    border-radius:8px;
                                    padding:14px;
                                    max-height:350px;
                                    overflow-y:auto;
                                    font-size:0.82rem;
                                    line-height:1.8;
                                    color:#999;
                                    white-space:pre-wrap;
                                ">{content}</div>
                                """,
                                unsafe_allow_html=True,
                            )
                        except Exception as e:
                            st.error(f"读取失败: {e}")
                    if st.button("Close", key="close_kb_preview"):
                        st.session_state.selected_kb_file = None
                        st.rerun()
        else:
            st.warning("未找到知识库文件")

        st.markdown('<div class="sidebar-divider"></div>', unsafe_allow_html=True)

        with st.expander("Developer Settings"):
            top_k = st.slider("Top K", 1, 10, 3, key="top_k")

            api_key = os.getenv("OPENAI_API_KEY", "")
            llm_model = os.getenv("LLM_MODEL", "")

            dot_class = "api-dot-on" if api_key else "api-dot-off"
            key_text = "Connected" if api_key else "Not Set"
            model_text = llm_model if llm_model else "Not Set"

            st.markdown(
                f"""
                <div style="font-size:0.72rem; line-height:2; color:#888;">
                <span class="api-dot {dot_class}"></span>API: {key_text}<br/>
                Model: <span style="color:var(--accent);">{model_text}</span>
                </div>
                """,
                unsafe_allow_html=True,
            )

        return top_k


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


def main():
    st.set_page_config(
        page_title="RunWise RAG",
        page_icon="🏃",
        layout="wide",
        initial_sidebar_state="collapsed",
    )
    st.markdown(CUSTOM_CSS, unsafe_allow_html=True)

    top_k = render_sidebar()

    render_hero()

    embeddings = init_embeddings()
    vectorstore = init_vectorstore(embeddings)

    if vectorstore is None:
        st.error("向量库未找到！请先运行 `python build_index.py` 构建索引。")
        st.stop()

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


if __name__ == "__main__":
    main()
