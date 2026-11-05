"""Check searchable 100gram rows whose string keys identify a settings row.

Run from any directory: python Tools/check_settings_search.py
This intentionally checks only direct UItem rows with literal localization keys.
"""
from pathlib import Path
import re
import sys

root = Path(__file__).resolve().parents[1]
base = root / 'TMessagesProj/src/main/java/org/telegram'
ui = (base / 'ui/HundredGramSubSettingsActivity.java').read_text(encoding='utf-8')
index = (base / 'messenger/HundredGramSettingsSearchIndex.java').read_text(encoding='utf-8')
constants = {key: int(value) for key, value in re.findall(
    r'private static final int (ITEM_\w+)\s*=\s*(\d+)', ui)}
rows = {}
for match in re.finditer(r'UItem\.as(?:Check|Button)\((ITEM_\w+),.{0,100}?getString\("([^"]+)"', ui, re.S):
    rows[match.group(2)] = constants.get(match.group(1))

checked = 0
errors = []
pattern = (r'addEntry\(id\+\+,\s*"([^"]+)"(?:(?!addEntry\(id\+\+).){0,1000}?'
           r'HundredGramSubSettingsActivity\.CATEGORY_(\w+),\s*(\d+),\s*HundredGramSubSettingsActivity\.class')
for match in re.finditer(pattern, index, re.S):
    key, category, item_id = match.group(1), match.group(2), int(match.group(3))
    if key not in rows or rows[key] is None:
        continue
    checked += 1
    if rows[key] != item_id:
        errors.append(f'{key} ({category}): index={item_id}, row={rows[key]}')

print(f'Checked {checked} direct settings search entries')
for error in errors:
    print(error, file=sys.stderr)
sys.exit(bool(errors) or checked == 0)
