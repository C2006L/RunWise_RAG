import copy
from datetime import date
import json
from pathlib import Path
import unittest
import yaml
from check_audit_report import evaluate

ROOT=Path(__file__).resolve().parent.parent

class AuditPolicyTest(unittest.TestCase):
    def setUp(self):
        self.policy=json.loads((ROOT/'deploy/audit-policy.json').read_text())
        self.compose=yaml.safe_load((ROOT/'compose.yaml').read_text())
        self.source=(ROOT/'code/rag_api.py').read_text(encoding='utf8')
        self.docker=(ROOT/'deploy/rag.Dockerfile').read_text()
        self.report={'dependencies':[{'name':'chromadb','version':'1.5.9','vulns':[{'id':'PYSEC-2026-311'}]}]}
    def check(self,report=None,today=None):
        return evaluate(report or self.report,self.policy,self.compose,self.source,self.docker,today or date(2026,11,15))
    def test_only_reviewed_server_advisory_is_scoped_out(self):
        self.assertFalse(self.check()['blocking'])
        data=copy.deepcopy(self.report);data['dependencies'][0]['vulns'].append({'id':'UNKNOWN'})
        self.assertEqual(1,len(self.check(data)['blocking']))
    def test_expiry_and_public_ports_fail_closed(self):
        self.assertTrue(self.check(today=date(2026,12,16))['blocking'])
        self.compose['services']['rag']['ports']=['5000:5000']
        self.assertTrue(self.check()['blocking'])
    def test_missing_and_skipped_audits_are_not_clean(self):
        with self.assertRaises(ValueError):self.check({'dependencies':[]})
        data=copy.deepcopy(self.report);data['dependencies'][0]['skip_reason']='not resolved'
        self.assertTrue(self.check(data)['blocking'])

if __name__=='__main__':unittest.main()
