"""Local launcher and read-only preflight. Never executes migration SQL."""
import argparse
import json
import os
from pathlib import Path
import shutil
import socket
import subprocess
import sys
from urllib.request import urlopen
from dotenv import load_dotenv

ROOT = Path(__file__).resolve().parent.parent
REQUIRED_SCHEMA = {
    't_user': {'id'}, 't_checkin': {'active_checkin_date'},
    't_injury_article': {'tags', 'sections', 'status', 'revision', 'updated_by'},
    't_qa_record': {'conversation_id', 'plan_draft_id'},
    't_admin_account': {'password_hash'}, 't_admin_audit_log': {'action'},
    't_web_credential': {'password_hash'}, 't_training_plan_item': {'training_date', 'target_km'},
    't_ai_plan_draft': {'context_json', 'plan_json', 'expires_at'},
    't_badge': {'code'}, 't_user_badge': {'badge_id'},
}


def executable(name):
    found = shutil.which(name)
    if not found and name == 'mysql':
        candidate = Path(os.getenv('ProgramFiles', 'C:/Program Files')) / 'MySQL/MySQL Server 8.0/bin/mysql.exe'
        found = str(candidate) if candidate.exists() else None
    return found


def check():
    problems = []
    def report(label, ok, hint=''):
        print(f"[{'OK' if ok else 'CHECK'}] {label}" + (f' - {hint}' if not ok else ''))
        if not ok: problems.append(label)
    env = os.environ
    password = env.get('MYSQL_PASSWORD') or env.get('DB_PASSWORD')
    report('DB_PASSWORD / MYSQL_PASSWORD', bool(password), 'set in RunWise_Backend/.env or process environment')
    report('JWT_SECRET', len(env.get('JWT_SECRET','')) >= 32, 'requires at least 32 characters')
    for label, host, port in [('MySQL',env.get('DB_HOST','localhost'),env.get('DB_PORT','3306')),
                              ('Redis',env.get('REDIS_HOST','localhost'),env.get('REDIS_PORT','6379'))]:
        try:
            with socket.create_connection((host,int(port)),timeout=2): pass
            report(label+' TCP '+str(port),True)
        except (OSError, ValueError): report(label+' TCP '+str(port),False,'not reachable; check service and configured port')
    mysql = executable('mysql')
    if password and mysql and not env.get('MYSQL_URL'):
        command = [mysql,'--connect-timeout=3','--batch','--skip-column-names',
                   '-h',env.get('DB_HOST','localhost'),'-P',env.get('DB_PORT','3306'),
                   '-u',env.get('MYSQL_USERNAME') or env.get('DB_USERNAME','root'),
                   '-D',env.get('DB_NAME','runwise'),'-e',
                   'SELECT TABLE_NAME,COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()']
        child_env = dict(env, MYSQL_PWD=password)
        try:
            result = subprocess.run(command,env=child_env,capture_output=True,text=True,timeout=8)
            report('MySQL authenticated schema query',result.returncode==0,'check credentials/database; no raw error printed')
            if result.returncode == 0:
                schema = {}
                for line in result.stdout.splitlines():
                    table, column = line.split('\t'); schema.setdefault(table,set()).add(column)
                for table, columns in REQUIRED_SCHEMA.items():
                    missing = columns-schema.get(table,set())
                    report(table,not missing,'missing: '+', '.join(sorted(missing)))
        except (OSError, subprocess.TimeoutExpired): report('MySQL schema query',False,'client execution failed or timed out')
    else:
        report('MySQL schema query',False,'requires mysql client and password; MYSQL_URL overrides require manual inspection')
    base = env.get('RAG_BASE_URL','http://127.0.0.1:5000').rstrip('/')
    try:
        with urlopen(base+'/health',timeout=5) as response: health=json.load(response)
        report('RAG index/configuration',health.get('status')=='ok','start RAG/check index and model configuration')
        print('Model reachability is NOT tested; health does not run inference.')
    except Exception: report('RAG health',False,'service stopped, cold-starting or unavailable')
    print('No schema, accounts, passwords or records were changed.')
    return 1 if problems else 0


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('service',choices=['check','backend','rag','web','admin'])
    args=parser.parse_args()
    # Existing terminal/IDE environment wins. Files stay ignored by Git.
    load_dotenv(ROOT/'RunWise_Backend/.env',override=False)
    if args.service=='check': return check()
    if args.service=='backend':
        if not (os.getenv('DB_PASSWORD') or os.getenv('MYSQL_PASSWORD')) or len(os.getenv('JWT_SECRET',''))<32:
            print('Set database password and JWT_SECRET in RunWise_Backend/.env first.'); return 1
        program=executable('mvn'); command=[program,'spring-boot:run']; cwd=ROOT/'RunWise_Backend'
    elif args.service=='rag':
        command=[sys.executable,str(ROOT/'code/rag_api.py')]; cwd=ROOT
    else:
        program=executable('npm'); command=[program,'run','dev']; cwd=ROOT/('runwise-web' if args.service=='web' else 'runwise-admin')
    if command[0] is None:
        print('Required executable is missing from PATH.'); return 1
    try: return subprocess.call(command,cwd=cwd)
    except KeyboardInterrupt: return 130


if __name__=='__main__': sys.exit(main())
