"""Create a source-only PEA submission; excludes build outputs and local IDE files."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED

home = Path(__file__).resolve().parents[1]
label = 'Media' if (home / 'DE190530UserService').exists() else 'Hotel'
output = home / 'submission' / ('DE190530_' + label + '.zip')
output.parent.mkdir(exist_ok=True)
excluded = {'target', 'submission', '.idea', '.git', '__pycache__'}
files = [p for p in home.rglob('*') if p.is_file()
         and not any(part in excluded for part in p.relative_to(home).parts)
         and p.suffix not in {'.iml', '.log', '.pyc', '.zip'}]
with ZipFile(output, 'w', ZIP_DEFLATED) as archive:
    for path in sorted(files):
        archive.write(path, Path('DE190530') / path.relative_to(home))
with ZipFile(output) as archive:
    assert archive.testzip() is None
    assert not any('/target/' in name for name in archive.namelist())
print(str(output) + ' (' + str(len(files)) + ' files, ' + str(output.stat().st_size) + ' bytes)')
