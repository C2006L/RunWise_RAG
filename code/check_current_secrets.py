"""Check current local credentials against Git history without printing their values.

This exact-match check is not a replacement for a general secret scanner.
"""
from pathlib import Path
import json
import subprocess
from dotenv import dotenv_values

ROOT = Path(__file__).resolve().parent.parent
NAMES = {'DB_PASSWORD','MYSQL_PASSWORD','REDIS_PASSWORD','JWT_SECRET','OPENAI_API_KEY','WECHAT_SECRET'}


def main():
    credentials = []
    for file in (ROOT/'RunWise_Backend/.env', ROOT/'code/.env', ROOT/'deploy/.env'):
        if file.is_file():
            for key, value in dotenv_values(file).items():
                if key in NAMES and value and len(value) >= 8:
                    credentials.append((key, value.encode('utf8')))
    if not credentials:
        raise SystemExit('No current credentials available to check; this is not a successful scan.')
    objects = subprocess.check_output(['git','rev-list','--objects','--all'],cwd=ROOT)
    ids = sorted({line.split(b' ',1)[0] for line in objects.splitlines()})
    info = subprocess.check_output(['git','cat-file','--batch-check=%(objectname) %(objecttype) %(objectsize)'],
                                   input=b'\n'.join(ids)+b'\n',cwd=ROOT)
    blobs = [row.split()[0] for row in info.splitlines() if len(row.split())==3
             and row.split()[1]==b'blob' and int(row.split()[2]) <= 2*1024*1024]
    data = subprocess.check_output(['git','cat-file','--batch'], input=b'\n'.join(blobs)+b'\n',cwd=ROOT)
    matches = []
    offset = 0
    for _ in blobs:
        end = data.index(b'\n',offset)
        oid, _, size = data[offset:end].split()
        start = end+1
        content = data[start:start+int(size)]
        offset = start+int(size)+1
        for key, value in credentials:
            if value in content:
                matches.append({'credential':key,'gitBlob':oid.decode('ascii')})
    report = {'checkedCredentialValues':len(credentials),'historicalBlobsUnder2MiB':len(blobs),
              'matches':matches,'scope':'Current local secret values only; old/rotated secrets and large blobs not covered'}
    output=ROOT/'.cache/preflight/current-secret-history.json'
    output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps(report,indent=2),encoding='utf8')
    print(json.dumps(report))
    if matches:
        raise SystemExit(1)


if __name__=='__main__': main()
