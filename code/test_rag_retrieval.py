"""RunWise RAG 检索质量回归测试。

只测试本地向量检索，不调用 Ollama，也不会写入业务问答历史。
"""

import unittest

from rag_api import retrieve


class RetrievalQualityTest(unittest.TestCase):
    def test_5k_plan_does_not_mix_10k_plan(self):
        hits = retrieve("每周跑三次怎么制定5K训练计划？", 3)
        self.assertTrue(hits)
        self.assertIn("5 公里", hits[0].source)
        self.assertFalse(any("10 公里" in hit.source for hit in hits))

    def test_lateral_knee_pain_prefers_iliotibial_band(self):
        hits = retrieve("跑到三公里时膝盖外侧疼怎么办？", 3)
        self.assertTrue(hits)
        self.assertIn("髂胫束", hits[0].source)

    def test_flat_feet_shoe_query_hits_specific_case(self):
        hits = retrieve("扁平足怎么选择跑鞋？", 3)
        self.assertTrue(hits)
        self.assertIn("跑鞋选择", hits[0].source)
        self.assertTrue(any("扁平足" in hit.section for hit in hits))

    def test_out_of_domain_query_returns_no_context(self):
        self.assertEqual(retrieve("Spring Boot 怎么连接数据库？", 3), [])


if __name__ == "__main__":
    unittest.main()
