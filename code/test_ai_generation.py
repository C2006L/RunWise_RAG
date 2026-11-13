"""生成层契约测试，不调用真实模型，不写业务数据库。"""
import unittest
import os
from unittest.mock import patch
from fastapi import HTTPException
import rag_api as api

class GenerationContractTest(unittest.TestCase):
    def setUp(self):
        # Tests must not depend on a developer's real .env provider/key.
        self.environment = patch.dict(os.environ, {"LLM_PROVIDER": "ollama"})
        self.environment.start()
        self.addCleanup(self.environment.stop)

    @patch.object(api, "generate_text", return_value="## 建议\n- 根据真实记录分析")
    def test_chat_prompt_includes_real_context_and_markdown(self, generate):
        result = api.generate_answer("如何训练", [], [], {"recordCount": 8, "totalKm": 40})
        self.assertIn("##", result)
        self.assertIn('"recordCount": 8', generate.call_args.args[0])
        self.assertIn("Markdown", generate.call_args.args[0])

    @patch.object(api, "retrieve", return_value=[])
    @patch.object(api, "generate_text", return_value='{"candidateId":"p1","explanation":"## 说明","sources":["fabricated"]}')
    def test_sources_are_owned_by_retrieval_not_invented_by_model(self, generate, retrieve):
        result = api.generate_plan(api.PlanRequest(request={"goal":"maintain","startDate":"2026-10-28","sessionsPerWeek":3}, trainingContext={"recordCount":8,"weeklyAverageKm":10,"longestRunKm":5}))
        self.assertEqual([], result["sources"])
        self.assertTrue(generate.call_args.kwargs["structured"])

    @patch.object(api, "retrieve", return_value=[])
    @patch.object(api, "generate_text", return_value="invalid incomplete json")
    def test_truncated_json_is_not_returned_as_a_valid_plan(self, generate, retrieve):
        with self.assertRaises(HTTPException) as caught:
            api.generate_plan(api.PlanRequest(request={"startDate":"2026-10-28","sessionsPerWeek":3}, trainingContext={"recordCount":8,"weeklyAverageKm":10,"longestRunKm":5}))
        self.assertEqual(503, caught.exception.status_code)

    def test_every_candidate_respects_recorded_volume_and_frequency(self):
        from decimal import Decimal
        candidates=api.build_plan_candidates({"startDate":"2026-10-28","sessionsPerWeek":3},{"recordCount":12,"weeklyAverageKm":20,"longestRunKm":8})
        for candidate in candidates:
            self.assertEqual(7,len(candidate["days"]))
            self.assertLessEqual(sum(day["type"]!="rest" for day in candidate["days"]),3)
            self.assertLessEqual(sum(Decimal(str(day["targetKm"])) for day in candidate["days"]),Decimal("20"))
            self.assertLessEqual(max(day["targetKm"] for day in candidate["days"]),8)

    @patch.object(api, "retrieve", return_value=[])
    @patch.object(api, "generate_text", return_value='{"candidateId":"needs_input","explanation":"请补充能训练的日期"}')
    def test_no_matching_schedule_asks_for_input_instead_of_inventing_days(self,generate,retrieve):
        result=api.generate_plan(api.PlanRequest(request={"startDate":"2026-10-28","sessionsPerWeek":3},trainingContext={"recordCount":8,"weeklyAverageKm":10,"longestRunKm":5}))
        self.assertEqual("NEEDS_INPUT",result["status"])
        self.assertNotIn("days",result)

    @patch.object(api, "generate_text", return_value="## 建议")
    def test_all_three_recent_rounds_are_available_without_600_char_cutoff(self, generate):
        history = [api.HistoryItem(user=f"第{i}轮", assistant="内容"*500+f"末尾条件{i}") for i in range(3)]
        api.generate_answer("继续", [], history)
        prompt = generate.call_args.args[0]
        for i in range(3):
            self.assertIn(f"末尾条件{i}", prompt)

    @patch.object(api, "retrieve", return_value=[])
    @patch.object(api, "generate_text", side_effect=api.ModelError("MODEL_TRUNCATED", "未保存不完整内容"))
    def test_model_failure_keeps_its_safe_code_for_chat_and_plan(self, generate, retrieve):
        for call in (lambda: api.ask(api.AskRequest(question="继续")),
                     lambda: api.generate_plan(api.PlanRequest(request={"startDate":"2026-10-28","sessionsPerWeek":3},trainingContext={"recordCount":8,"weeklyAverageKm":10,"longestRunKm":5}))):
            with self.assertRaises(HTTPException) as caught:
                call()
            self.assertEqual("MODEL_TRUNCATED", caught.exception.detail["code"])

if __name__ == "__main__":
    unittest.main()
