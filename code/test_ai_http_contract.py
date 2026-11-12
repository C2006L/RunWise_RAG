"""RAG 路由到兼容模型的真实本地 HTTP 契约；不调用供应商、不写数据库。"""
import json
import os
import threading
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from unittest.mock import patch

from fastapi.testclient import TestClient
import rag_api as api


class AiHttpContractTest(unittest.TestCase):
    def test_chat_plan_and_errors_use_the_same_configured_provider(self):
        requests = []

        class Handler(BaseHTTPRequestHandler):
            def log_message(self, *_):
                pass

            def do_POST(self):
                body = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
                requests.append((self.path, self.headers.get("Authorization"), body))
                content = json.dumps({"candidateId":"p1","explanation":"## 测试说明\n根据已记录跑量安排基础周"}) if "response_format" in body else "## 测试建议\n- 保留恢复日"
                result = {"choices":[{"finish_reason":"stop","message":{"content":content}}]}
                if len(requests) == 3:
                    result = {"error":{"message":"must not expose private provider body"}}
                payload = json.dumps(result).encode("utf-8")
                self.send_response(429 if len(requests)==3 else 200)
                self.send_header("Content-Type","application/json")
                self.send_header("Content-Length",str(len(payload)))
                self.end_headers()
                self.wfile.write(payload)

        server = ThreadingHTTPServer(("127.0.0.1",0), Handler)
        worker = threading.Thread(target=server.serve_forever, daemon=True)
        worker.start()
        env = {"LLM_PROVIDER":"compatible", "OPENAI_BASE_URL":f"http://127.0.0.1:{server.server_port}/v1",
               "OPENAI_API_KEY":"test-only-key", "LLM_MODEL":"test-cloud", "LLM_JSON_MODE":"json_object"}
        try:
            with patch.dict(os.environ,env), patch.object(api,"retrieve",return_value=[]), TestClient(api.app) as client:
                answer = client.post("/api/rag/ask",json={"question":"测试：如何安排恢复？","trainingContext":{"recordCount":8}})
                self.assertEqual(200, answer.status_code)
                self.assertIn("##",answer.json()["answer"])
                self.assertEqual([],answer.json()["sources"])
                plan = client.post("/api/rag/plan",json={"request":{"startDate":"2026-10-29","sessionsPerWeek":3},"trainingContext":{"recordCount":8,"weeklyAverageKm":10,"longestRunKm":5}})
                self.assertEqual(200,plan.status_code)
                self.assertEqual("test-cloud",plan.json()["model"])
                self.assertEqual(7,len(plan.json()["days"]))
                error = client.post("/api/rag/ask",json={"question":"测试限流"})
                self.assertEqual(503,error.status_code)
                self.assertEqual("MODEL_BUSY",error.json()["detail"]["code"])
                self.assertNotIn("private provider",error.text)
            self.assertEqual(3,len(requests))
            for path, auth, body in requests:
                self.assertEqual("/v1/chat/completions",path)
                self.assertEqual("Bearer test-only-key",auth)
                self.assertEqual("test-cloud",body["model"])
            self.assertNotIn("response_format",requests[0][2])
            self.assertEqual({"type":"json_object"},requests[1][2]["response_format"])
        finally:
            server.shutdown()
            server.server_close()
            worker.join(timeout=2)

    @patch.dict(os.environ,{"LLM_PROVIDER":"compatible","OPENAI_API_KEY":"","LLM_MODEL":"test"})
    @patch.object(api,"get_vectorstore")
    def test_health_does_not_claim_model_connectivity_or_expose_credentials(self, store):
        store.return_value._collection.count.return_value = 10
        with TestClient(api.app) as client:
            body = client.get("/health").json()
        self.assertEqual("degraded",body["status"])
        self.assertTrue(body["indexReady"])
        self.assertFalse(body["configured"])
        self.assertEqual("configuration_only",body["modelCheck"])
        self.assertNotIn("api_key",body)


if __name__ == "__main__":
    unittest.main()
