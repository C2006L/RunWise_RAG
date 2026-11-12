"""Provider contract tests; no paid requests, credentials or model downloads."""
import io
import json
import unittest
from unittest.mock import MagicMock, patch
from urllib.error import HTTPError, URLError

from llm_provider import ModelError, ModelSettings, generate_text, model_info


LOCAL = ModelSettings("ollama", "http://127.0.0.1:11434", "test-local")
CLOUD = ModelSettings("compatible", "https://provider.example/v1", "test-cloud", "test-only-secret")
SCHEMA = {"type": "object", "properties": {"candidateId": {"type": "string"}}, "required": ["candidateId"], "additionalProperties": False}


def response(body):
    resource = MagicMock()
    resource.__enter__.return_value.read.return_value = json.dumps(body).encode("utf-8")
    return resource


class ModelProviderTest(unittest.TestCase):
    def test_default_stays_local_even_if_cloud_credentials_exist(self):
        settings = ModelSettings.from_env({"OPENAI_API_KEY": "present", "LLM_MODEL": "cloud"})
        self.assertEqual("ollama", settings.provider)
        self.assertEqual("qwen2.5:3b", settings.model)

    def test_explicit_cloud_settings_and_key_redaction(self):
        settings = ModelSettings.from_env({"LLM_PROVIDER":"compatible", "OPENAI_BASE_URL":"https://provider.example/v1/", "OPENAI_API_KEY":"private-key", "LLM_MODEL":"cloud"})
        self.assertEqual(2400, settings.chat_tokens)
        self.assertEqual("https://provider.example/v1", settings.base_url)
        self.assertNotIn("private-key", repr(settings))

    def test_bad_configuration_never_sends_request_or_exposes_values(self):
        for env in ({"LLM_PROVIDER":"unknown"}, {"LLM_PROVIDER":"compatible"},
                    {"LLM_TIMEOUT_SECONDS":"90"}, {"LLM_CHAT_MAX_TOKENS":"NaN"},
                    {"OLLAMA_BASE_URL":"http://user:secret@host"}, {"OLLAMA_BASE_URL":"file:///tmp/file"},
                    {"LLM_JSON_MODE":"unknown"}):
            with self.subTest(env=env), patch("llm_provider.urlopen") as post:
                with self.assertRaises(ModelError) as caught:
                    ModelSettings.from_env(env)
                self.assertEqual("MODEL_CONFIG", caught.exception.code)
                self.assertNotIn("secret", str(caught.exception))
                post.assert_not_called()

    @patch("llm_provider.urlopen")
    def test_local_request_preserves_schema_and_markdown(self, post):
        post.return_value = response({"done": True, "done_reason":"stop", "response":" ## 建议 "})
        self.assertEqual("## 建议", generate_text("JSON test", 1100, SCHEMA, LOCAL))
        request = post.call_args.args[0]
        body = json.loads(request.data)
        self.assertEqual("http://127.0.0.1:11434/api/generate", request.full_url)
        self.assertEqual(SCHEMA, body["format"])
        self.assertFalse(body["stream"])
        self.assertEqual(1100, body["options"]["num_predict"])
        self.assertIsNone(request.get_header("Authorization"))

    @patch("llm_provider.urlopen")
    def test_compatible_chat_and_json_object_requests(self, post):
        post.return_value = response({"choices":[{"finish_reason":"stop", "message":{"content":"## 建议"}}]})
        self.assertEqual("## 建议", generate_text("上下文", 2400, settings=CLOUD))
        request = post.call_args.args[0]
        self.assertEqual("https://provider.example/v1/chat/completions", request.full_url)
        self.assertEqual("Bearer test-only-secret", request.get_header("Authorization"))
        self.assertEqual([{"role":"user", "content":"上下文"}], json.loads(request.data)["messages"])
        self.assertNotIn("response_format", json.loads(request.data))
        generate_text("JSON", 1800, SCHEMA, CLOUD)
        self.assertEqual({"type":"json_object"}, json.loads(post.call_args.args[0].data)["response_format"])

    @patch("llm_provider.urlopen")
    def test_strict_schema_is_opt_in(self, post):
        post.return_value = response({"choices":[{"finish_reason":"stop", "message":{"content":"{}"}}]})
        settings = ModelSettings("compatible", CLOUD.base_url, CLOUD.model, CLOUD.api_key, json_mode="json_schema")
        generate_text("JSON", 1800, SCHEMA, settings)
        sent = json.loads(post.call_args.args[0].data)["response_format"]
        self.assertTrue(sent["json_schema"]["strict"])
        self.assertEqual(SCHEMA, sent["json_schema"]["schema"])

    @patch("llm_provider.urlopen")
    def test_output_limit_is_never_accepted_as_complete(self, post):
        for settings, body in ((LOCAL, {"done":True,"done_reason":"length","response":"partial"}),
                               (CLOUD, {"choices":[{"finish_reason":"length","message":{"content":"partial"}}]})):
            post.return_value = response(body)
            with self.assertRaises(ModelError) as caught:
                generate_text("test", 1000, settings=settings)
            self.assertEqual("MODEL_TRUNCATED", caught.exception.code)

    @patch("llm_provider.urlopen")
    def test_empty_malformed_refused_and_incomplete_output_is_rejected(self, post):
        for body in ({}, [], {"choices":[]}, {"choices":[{"finish_reason":"stop","message":{"content":" "}}]},
                     {"choices":[{"finish_reason":"content_filter","message":{"content":"blocked"}}]},
                     {"choices":[{"finish_reason":"stop","message":{"content":"x","refusal":"refused"}}]}):
            post.return_value = response(body)
            with self.assertRaises(ModelError) as caught:
                generate_text("test", 1000, settings=CLOUD)
            self.assertEqual("MODEL_RESPONSE", caught.exception.code)
        post.return_value = response({"done":False,"response":"partial"})
        with self.assertRaises(ModelError):
            generate_text("test", 1000, settings=LOCAL)

    @patch("llm_provider.urlopen")
    def test_upstream_errors_are_safe_and_never_retried_on_local_model(self, post):
        for status, expected in ((401,"MODEL_AUTH"),(403,"MODEL_AUTH"),(429,"MODEL_BUSY"),(400,"MODEL_CONFIG"),(500,"MODEL_UNAVAILABLE")):
            post.reset_mock()
            post.side_effect = HTTPError("https://private?key=secret", status, "secret detail", {}, io.BytesIO(b"secret body"))
            with self.assertRaises(ModelError) as caught:
                generate_text("private question", 1000, settings=CLOUD)
            self.assertEqual(expected, caught.exception.code)
            self.assertNotIn("secret", str(caught.exception))
            post.assert_called_once()

    @patch("llm_provider.urlopen")
    def test_connection_timeout_and_invalid_json_have_clear_codes(self, post):
        for exc in (TimeoutError("secret"), URLError("private host")):
            post.side_effect = exc
            with self.assertRaises(ModelError) as caught:
                generate_text("test", 1000, settings=CLOUD)
            self.assertEqual("MODEL_TIMEOUT", caught.exception.code)
        post.side_effect = None
        post.return_value.__enter__.return_value.read.return_value = b"not json"
        with self.assertRaises(ModelError) as caught:
            generate_text("test", 1000, settings=CLOUD)
        self.assertEqual("MODEL_RESPONSE", caught.exception.code)

    @patch.dict("os.environ", {"LLM_PROVIDER":"compatible", "OPENAI_API_KEY":"private-key", "OPENAI_BASE_URL":"https://private/v1", "LLM_MODEL":"example"}, clear=True)
    def test_health_reports_configuration_without_key_or_host(self):
        info = model_info()
        self.assertEqual({"provider":"compatible","model":"example","configured":True}, info)
        self.assertNotIn("private", json.dumps(info))


if __name__ == "__main__":
    unittest.main()
