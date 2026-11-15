"""Offline-consistent backup and restore to a NEW EMPTY Compose instance only.

Run from any directory: python3 deploy/backup.py backup|verify|restore [directory]
No credentials are read or printed by this script; Compose injects them.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tarfile
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parent.parent
FILES = ('database.sql', 'uploads.tar.gz', 'rag.tar.gz')


def digest(path):
    result = hashlib.sha256()
    with path.open('rb') as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b''):
            result.update(chunk)
    return result.hexdigest()


def verify(folder):
    manifest = json.loads((folder / 'manifest.json').read_text(encoding='utf-8'))
    if manifest.get('version') != 1:
        raise ValueError('Unknown backup format')
    for name in FILES:
        if digest(folder / name) != manifest['sha256'].get(name):
            raise ValueError('Checksum mismatch: ' + name)
    for name, roots in [('uploads.tar.gz', {'uploads'}), ('rag.tar.gz', {'models', 'chroma_db'})]:
        with tarfile.open(folder / name, 'r:gz') as archive:
            for member in archive:
                parts = Path(member.name).parts
                if not parts or parts[0] not in roots or '..' in parts or member.name.startswith('/') or '\\' in member.name:
                    raise ValueError('Unsafe archive path')
                if not member.isfile() and not member.isdir():
                    raise ValueError('Backup may contain only regular files and directories')
    print('Backup checksums and archive paths verified. This is NOT a restore drill.')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['backup', 'verify', 'restore'])
    parser.add_argument('directory', nargs='?', type=Path)
    parser.add_argument('--env-file', type=Path, default=ROOT / 'deploy/.env')
    parser.add_argument('--confirm-new-empty-instance', action='store_true')
    args = parser.parse_args()
    if args.action != 'backup' and args.directory is None:
        parser.error('Specify a backup directory')
    folder = (args.directory or ROOT / 'deploy/backups' / datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')).resolve()
    if args.action == 'verify':
        verify(folder)
        return
    if not args.env_file.is_file():
        parser.error('Missing server environment file')
    compose = ['docker', 'compose', '--env-file', str(args.env_file.resolve()), '-f', str(ROOT / 'compose.yaml')]

    def run(*command, **kwargs):
        return subprocess.run(compose + list(command), cwd=ROOT, check=True, **kwargs)

    running = set(run('ps', '--status', 'running', '--services', capture_output=True, text=True).stdout.split())
    if 'mysql' not in running:
        parser.error('Start mysql before backup/restore')
    if args.action == 'restore':
        if not args.confirm_new_empty_instance:
            parser.error('Restore requires --confirm-new-empty-instance and an empty target')
        verify(folder)
        if running & {'backend', 'gateway', 'rag'}:
            parser.error('Restore target must have backend, gateway and rag stopped')
        count = run('exec', '-T', 'mysql', 'sh', '-c',
                    'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()" "$MYSQL_DATABASE"',
                    capture_output=True, text=True).stdout.strip()
        if count != '0':
            parser.error('Refusing to restore into a non-empty database; existing data was not changed')
        run('run', '--rm', '--no-deps', '--entrypoint', 'sh', 'backend', '-c', 'test -z "$(ls -A /app/uploads)"')
        run('run', '--rm', '--no-deps', '--entrypoint', 'sh', 'rag', '-c',
            'test -z "$(ls -A /app/models)" && test -z "$(ls -A /app/chroma_db)"')
        with (folder / 'database.sql').open('rb') as sql:
            run('exec', '-T', 'mysql', 'sh', '-c', 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot "$MYSQL_DATABASE"', stdin=sql)
        for service, name in [('backend', 'uploads.tar.gz'), ('rag', 'rag.tar.gz')]:
            with (folder / name).open('rb') as archive:
                run('run', '--rm', '-T', '--no-deps', '--entrypoint', 'tar', service, '-C', '/app', '-xzf', '-', stdin=archive)
        print('Restore complete. Services remain stopped. Start and run acceptance tests manually.')
        return

    os.umask(0o077)
    folder.mkdir(parents=True, exist_ok=False, mode=0o700)
    paused = sorted(running & {'gateway', 'backend', 'rag'})
    try:
        if paused:
            run('stop', *paused)
        with (folder / 'database.sql').open('wb') as sql:
            run('exec', '-T', 'mysql', 'sh', '-c',
                'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -uroot --single-transaction --no-tablespaces --set-gtid-purged=OFF --routines --triggers --events "$MYSQL_DATABASE"', stdout=sql)
        for service, name, paths in [('backend', 'uploads.tar.gz', ['uploads']), ('rag', 'rag.tar.gz', ['models', 'chroma_db'])]:
            with (folder / name).open('wb') as archive:
                run('run', '--rm', '-T', '--no-deps', '--entrypoint', 'tar', service, '-C', '/app', '-chzf', '-', *paths, stdout=archive)
        manifest = {'version': 1, 'createdAt': datetime.now(timezone.utc).isoformat(),
                    'sha256': {name: digest(folder / name) for name in FILES},
                    'redisSessionsIncluded': False}
        (folder / 'manifest.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')
        verify(folder)
        print('Backup complete:', folder)
    finally:
        if paused:
            run('start', *paused)


if __name__ == '__main__':
    main()
