"""Validate an index; apply schema migration only with a cold backup and explicit confirmation."""
import argparse
from pathlib import Path
import shutil

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--index',type=Path,default=Path(__file__).resolve().parent.parent/'chroma_db/runwise')
    parser.add_argument('--apply',action='store_true')
    parser.add_argument('--backup-dir',type=Path)
    parser.add_argument('--confirm-service-stopped',action='store_true')
    args=parser.parse_args()
    index=args.index.resolve()
    if not (index/'chroma.sqlite3').is_file(): parser.error('Existing index not found; not creating a new database')
    if args.apply:
        if not args.confirm_service_stopped or args.backup_dir is None:
            parser.error('Stop RAG first, then specify --confirm-service-stopped and --backup-dir')
        backup=args.backup_dir.resolve()
        if backup==index or index in backup.parents or backup in index.parents:
            parser.error('Backup must be separate from index and must not contain it')
        if backup.exists(): parser.error('Backup destination already exists; refusing overwrite')
        if any(p.is_symlink() for p in index.rglob('*')): parser.error('Index contains symlinks; review manually')
        shutil.copytree(index,backup)
        print('Cold backup created:',backup)
    import chromadb
    from chromadb.config import Settings
    client=chromadb.PersistentClient(path=str(index),settings=Settings(migrations='apply' if args.apply else 'validate'))
    collection=client.get_collection('runwise_knowledge_v2')
    count=collection.count()
    if count<1: raise SystemExit('Index collection is empty; not ready')
    print('Index schema validated; chunks:',count)

if __name__=='__main__': main()
