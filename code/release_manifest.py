"""Record release evidence without reading secrets, changing Git, or deploying."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parent.parent


def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT).decode('utf8').strip()


def sha(path):
    digest = hashlib.sha256()
    with path.open('rb') as source:
        for block in iter(lambda: source.read(1024 * 1024), b''):
            digest.update(block)
    return digest.hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, default=ROOT/'.cache/preflight/version-manifest.json')
    parser.add_argument('--require-clean', action='store_true')
    parser.add_argument('--include-mini', action='store_true', help='Optional separate mini-program artifact manifest')
    args = parser.parse_args()
    dirty = bool(git('status', '--porcelain'))
    tracked = subprocess.check_output(['git', 'ls-files', '-z', '--cached', '--others', '--exclude-standard'], cwd=ROOT)
    paths = sorted(set(tracked.decode('utf8').split('\0')) - {''})
    sources = {name: sha(ROOT/name) for name in paths if (ROOT/name).is_file()}
    artifacts = {}
    directories = ['runwise-web/dist', 'runwise-admin/dist']
    if args.include_mini:
        directories.append('RunWise_MiniProgram/dist/build/mp-weixin')
    for directory in directories:
        artifacts.update({str(p.relative_to(ROOT)): sha(p) for p in (ROOT/directory).rglob('*') if p.is_file()})
    jar = ROOT/'RunWise_Backend/target/runwise-backend-1.0.0.jar'
    if jar.is_file():
        artifacts[str(jar.relative_to(ROOT))] = sha(jar)
    stale_jar = not jar.is_file() or any(p.stat().st_mtime > jar.stat().st_mtime for p in (ROOT/'RunWise_Backend/src/main').rglob('*') if p.is_file())
    manifest = {'createdAt': datetime.now(timezone.utc).isoformat(), 'commit': git('rev-parse', 'HEAD'),
                'dirty': dirty, 'backendJarMayBeStale': stale_jar, 'sourceSha256': sources, 'artifactSha256': artifacts,
                'note': 'Hashes describe this workspace. They do not prove security, tests, server capacity or recovery.'}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(manifest, indent=2), encoding='utf8')
    print(json.dumps({'manifest': str(args.output), 'dirty': dirty, 'backendJarMayBeStale': stale_jar,
                      'sourceFiles': len(sources), 'artifacts': len(artifacts)}))
    if args.require_clean and (dirty or stale_jar):
        raise SystemExit('Release blocked: commit reviewed changes and rebuild artifacts from the same version.')


if __name__ == '__main__': main()
