import importlib.util
import io
import json
from pathlib import Path
import tarfile
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('backup', Path(__file__).resolve().parents[1] / 'backup.py')
backup = importlib.util.module_from_spec(spec)
spec.loader.exec_module(backup)


class BackupTest(unittest.TestCase):
    def fixture(self, directory, unsafe=False):
        (directory / 'database.sql').write_text('SELECT 1;', encoding='utf-8')
        for name, root in [('uploads.tar.gz', 'uploads'), ('rag.tar.gz', 'models')]:
            with tarfile.open(directory / name, 'w:gz') as archive:
                entry = tarfile.TarInfo(root + ('/../../outside' if unsafe else '/test'))
                entry.size = 4
                archive.addfile(entry, io.BytesIO(b'test'))
        (directory / 'manifest.json').write_text(json.dumps({'version': 1, 'sha256': {
            name: backup.digest(directory / name) for name in backup.FILES}}), encoding='utf-8')

    def test_complete_backup_and_corruption(self):
        with tempfile.TemporaryDirectory() as temporary:
            folder = Path(temporary)
            self.fixture(folder)
            backup.verify(folder)
            (folder / 'database.sql').write_text('corrupted', encoding='utf-8')
            with self.assertRaises(ValueError): backup.verify(folder)

    def test_traversal_is_rejected_even_when_checksum_matches(self):
        with tempfile.TemporaryDirectory() as temporary:
            folder = Path(temporary)
            self.fixture(folder, unsafe=True)
            with self.assertRaises(ValueError): backup.verify(folder)


if __name__ == '__main__': unittest.main()
