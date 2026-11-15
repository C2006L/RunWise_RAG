"""Evaluate raw pip-audit output with narrow, expiring deployment scope exceptions."""
import argparse
from datetime import date
import json
from pathlib import Path
import yaml

ROOT=Path(__file__).resolve().parent.parent

def evaluate(report, policy, compose, source, dockerfile, today=None):
    today=today or date.today()
    scoped=(today <= date.fromisoformat(policy['expires'])
            and not compose['services']['rag'].get('ports')
            and str(compose['services']['rag']['environment'].get('RAG_VALIDATE_INDEX_SCHEMA')).lower()=='true'
            and 'persist_directory=str(CHROMA_DIR)' in source
            and 'HttpClient(' not in source and 'trust_remote_code=True' not in source
            and 'CMD ["python", "code/rag_api.py"]' in dockerfile)
    blocked=[]; scoped_out=[]; seen=set()
    for package in report.get('dependencies',[]):
        # Missing advisories for skipped/unresolved packages are not a clean scan.
        if package.get('skip_reason'):
            blocked.append({'package':package['name'],'reason':'audit skipped package'})
        for issue in package.get('vulns',[]):
            key=(package['name'],issue['id'])
            if key in seen: continue
            seen.add(key)
            item={'package':package['name'],'id':issue['id']}
            if scoped and package['name']==policy['package'] and package['version']==policy['version'] and issue['id'] in policy['ids']:
                scoped_out.append(item)
            else: blocked.append(item)
    if not report.get('dependencies'): raise ValueError('Missing audit dependencies; refusing empty report')
    return {'blocking':blocked,'scopedServerOnlyFindings':scoped_out,'exceptionScopeValid':scoped}

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('report',type=Path)
    args=parser.parse_args()
    result=evaluate(json.loads(args.report.read_text(encoding='utf8')),
        json.loads((ROOT/'deploy/audit-policy.json').read_text(encoding='utf8')),
        yaml.safe_load((ROOT/'compose.yaml').read_text(encoding='utf8')),
        (ROOT/'code/rag_api.py').read_text(encoding='utf8'),
        (ROOT/'deploy/rag.Dockerfile').read_text(encoding='utf8'))
    print(json.dumps(result,indent=2))
    if result['blocking']: raise SystemExit(1)

if __name__=='__main__': main()
