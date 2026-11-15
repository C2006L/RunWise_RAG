"""Add reviewed educational articles. Default is dry-run; never replace existing titles."""
import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
from datetime import datetime
from dotenv import load_dotenv

ROOT = Path(__file__).resolve().parent.parent

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    articles = json.loads((ROOT/'data/injury-library/articles.json').read_text(encoding='utf8'))
    assert len({a['title'] for a in articles}) == len(articles)
    for article in articles:
        assert article['category'] in ('knee','ankle','shin') and len(article['sections']) >= 4
        assert len(article['title']) <= 128 and len(article['summary']) <= 255
        assert 'https://' in article['sections'][-1]['body']
    print('Validated articles:',len(articles))
    if not args.apply:
        print('Dry-run only. Use --apply to back up and insert missing titles.')
        return
    load_dotenv(ROOT/'RunWise_Backend/.env', override=False)
    mysql = shutil.which('mysql')
    if not mysql: raise SystemExit('mysql client not found')
    env = dict(os.environ,MYSQL_PWD=os.getenv('MYSQL_PASSWORD') or os.environ['DB_PASSWORD'])
    command=[mysql,'--default-character-set=utf8mb4','--batch','--raw','--skip-column-names',
             '-h',os.getenv('DB_HOST','localhost'),'-P',os.getenv('DB_PORT','3306'),
             '-u',os.getenv('MYSQL_USERNAME') or os.getenv('DB_USERNAME','runwise_app'),'-D',os.getenv('DB_NAME','runwise')]
    def execute(sql):
        response=subprocess.run(command,input=sql.encode('utf8'),env=env,capture_output=True,timeout=30)
        if response.returncode: raise RuntimeError('MySQL operation failed; no credentials or SQL printed')
        return response.stdout
    backup=ROOT/'.cache/db-backups'/('injury-content-'+datetime.now().strftime('%Y%m%d-%H%M%S')+'.jsonl')
    backup.parent.mkdir(parents=True,exist_ok=True)
    backup.write_bytes(execute("SELECT JSON_OBJECT('id',id,'category',category,'title',title,'summary',summary,'tags',tags,'sections',sections,'status',status,'revision',revision,'deleted',deleted) FROM t_injury_article;"))
    def text(value): return "CONVERT(0x"+value.encode('utf8').hex()+" USING utf8mb4)"
    statements=['SET NAMES utf8mb4;','START TRANSACTION;']
    for a in articles:
        values=[text(a[k]) for k in ('category','title','summary','tags')]
        values += [text(json.dumps(a['sections'],ensure_ascii=False)),str(a['read_minutes']),str(a['sort']),"'PUBLISHED'",'1']
        statements.append('INSERT INTO t_injury_article(category,title,summary,tags,sections,read_minutes,sort,status,revision) SELECT '+','.join(values)+' WHERE NOT EXISTS (SELECT 1 FROM t_injury_article WHERE title='+text(a['title'])+');')
    execute('\n'.join(statements+['COMMIT;']))
    print('Missing articles inserted; existing articles unchanged. Backup:',backup)

if __name__=='__main__': main()
