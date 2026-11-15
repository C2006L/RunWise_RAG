FROM python:3.11-slim-bookworm
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1 HF_HOME=/app/models/.cache \
    RAG_HOST=0.0.0.0 RAG_PORT=5000 OMP_NUM_THREADS=2 TOKENIZERS_PARALLELISM=false
WORKDIR /app
COPY deploy/requirements-rag.txt ./requirements.txt
# CPU inference avoids installing the CUDA runtime on a server without a GPU.
RUN pip install --no-cache-dir torch==2.13.0 --index-url https://download.pytorch.org/whl/cpu
RUN pip install --no-cache-dir -r requirements.txt \
    && pip check \
    && groupadd -g 10001 app && useradd -u 10001 -g app -m app \
    && mkdir -p models chroma_db && chown -R app:app /app
COPY --chown=app:app code ./code
COPY --chown=app:app data/runwise ./data/runwise
USER app
EXPOSE 5000
CMD ["python", "code/rag_api.py"]
