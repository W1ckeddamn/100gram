#!/usr/bin/env python3
"""
generate_state.py - Generates a compact, high-density PROJECT_STATE.md
by querying git log, git status, OpenCode SQLite DB, and Antigravity brain transcripts.
"""

import os
import sys
import subprocess
import sqlite3
import json
import re
from datetime import datetime, timezone

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "..", ".."))
OPENCODE_DB = os.path.expanduser(r"~\.local\share\opencode\opencode.db")
ANTIGRAVITY_BRAIN = os.path.expanduser(r"~\.gemini\antigravity-cli\brain")
OUTPUT_FILE = os.path.join(REPO_ROOT, "PROJECT_STATE.md")

def get_git_info(repo_path):
    info = {"branch": "unknown", "head": "unknown", "subject": "", "dirty": False, "recent_commits": []}
    try:
        branch = subprocess.check_output(["git", "-C", repo_path, "branch", "--show-current"], text=True).strip()
        info["branch"] = branch or "detached"
        
        log = subprocess.check_output(["git", "-C", repo_path, "log", "-n", "10", "--oneline"], text=True).strip()
        lines = log.splitlines()
        if lines:
            head_parts = lines[0].split(" ", 1)
            info["head"] = head_parts[0]
            info["subject"] = head_parts[1] if len(head_parts) > 1 else ""
            info["recent_commits"] = lines
            
        status = subprocess.check_output(["git", "-C", repo_path, "status", "-s"], text=True).strip()
        info["dirty"] = len(status) > 0
        info["status_lines"] = status.splitlines() if status else []
    except Exception as e:
        info["error"] = str(e)
    return info

def get_opencode_summary():
    if not os.path.exists(OPENCODE_DB):
        return None
    try:
        conn = sqlite3.connect(f"file:{OPENCODE_DB}?mode=ro", uri=True)
        conn.row_factory = sqlite3.Row
        c = conn.cursor()
        c.execute("""
            SELECT id, title, directory, time_updated 
            FROM session_v2 
            WHERE directory LIKE '%100gram%' OR directory LIKE '%exteragram%' OR directory LIKE '%opexgram%'
            ORDER BY time_updated DESC LIMIT 1
        """)
        row = c.fetchone()
        if not row:
            return None
        
        session_id = row["id"]
        title = row["title"]
        updated = datetime.fromtimestamp(row["time_updated"] / 1000.0).strftime("%Y-%m-%d %H:%M:%S")
        
        c.execute("""
            SELECT data FROM session_message 
            WHERE session_id = ? AND type = 'user' 
            ORDER BY seq DESC LIMIT 1
        """, (session_id,))
        last_user = c.fetchone()
        last_prompt = ""
        if last_user:
            d = json.loads(last_user["data"])
            last_prompt = d.get("text", "")
            
        return {
            "session_id": session_id,
            "title": title,
            "updated": updated,
            "last_prompt": last_prompt.strip()
        }
    except Exception as e:
        return {"error": str(e)}

def get_antigravity_summary():
    if not os.path.exists(ANTIGRAVITY_BRAIN):
        return []
    
    key_sessions = []
    # Relevant conversation IDs identified
    target_ids = {
        "fa0e7e88-df3f-4c38-88eb-1148e800ed67": "Foundational architecture, exteraGram/AyuGram parity, Vault, AI assistant & C++ JNI sync",
        "34a35936-5716-4850-97ce-a5a3742fbccc": "REA reverse engineering of exteraGram & AyuGram APKs into D:\\exteragram",
        "c62ff3ad-d3c3-41c1-99af-c5f825ca711a": "State inspection & synchronization between OpenCode and Antigravity"
    }
    
    for cid, desc in target_ids.items():
        cpath = os.path.join(ANTIGRAVITY_BRAIN, cid)
        if not os.path.exists(cpath):
            continue
        artifacts = [f for f in os.listdir(cpath) if f.endswith(".md")]
        key_sessions.append({
            "id": cid,
            "description": desc,
            "artifacts_count": len(artifacts),
            "artifacts": artifacts[:5]
        })
    return key_sessions

def build_markdown(git_info, opencode_info, antigravity_info):
    now_str = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    lines = [
        "# 100gram Project State & Handoff",
        "",
        f"> **Generated:** {now_str}  ",
        f"> **Branch:** `{git_info.get('branch', 'main')}`  ",
        f"> **Head Commit:** `{git_info.get('head', 'HEAD')}` ({git_info.get('subject', '')})  ",
        f"> **Working Tree:** `{'DIRTY (' + str(len(git_info.get('status_lines', []))) + ' changes)' if git_info.get('dirty') else 'Clean'}`",
        "",
        "---",
        "",
        "## 1. Multi-Environment Ecosystem",
        "",
        "| Source | Role | Key Function / Content |",
        "|---|---|---|",
        f"| **`D:\\100gram`** | Primary Git Repo | Active Telegram fork (`main`), C++ JNI (`merge=ours`), standalone APK target |",
        "| **`D:\\opexgram`** | Feature Donor (13.1.1) | Decompiled OpexGram beta10 APK (`opexgram_src`), strings index, feature maps |",
        "| **`D:\\exteragram`** | Donor Reference (extera/Ayu) | Extracted classes (`AiController`, `AppearancePreferencesActivity`, etc.) |",
        "| **OpenCode DB** | Session Logs | `~/.local/share/opencode/opencode.db` (OpexGram parity batches 1–10) |",
        "| **Antigravity Brain** | Architecture & Core Specs | `~/.gemini/antigravity-cli/brain/` (AyuGram/extera modules, Vault, 21 plans) |",
        "",
        "---",
        "",
        "## 2. Core Philosophy: User Sovereignty & Total Customizability",
        "",
        "> **Cardinal Rule:** Nothing is forced. Every feature, visual tweak, animation, or behavior modification **MUST** be optional and controlled by a user setting.",
        "",
        "- **No Unconditional Mutations:** Never alter Telegram's UI or behavior without a toggle.",
        "- **Vanilla Baseline:** If all 100gram toggles are off, the app must behave identically to vanilla official Telegram.",
        "- **Granularity over Monoliths:** Provide fine-grained toggles rather than monolithic bundles (e.g. separate MD3 toggles for Chat List, Sections, Controls, Menus, Navigation, and Media Loaders).",
        "- **The 6-Point Feature Contract:** Every feature must implement: (1) `HundredGramConfig` prefs, (2) `HundredGramSubSettingsActivity` UI, (3) EN/RU strings, (4) `HundredGramSettingsSearchIndex`, (5) `HundredGramBackupManager` JSON support, and (6) `resetAllSettings()` handling.",
        "",
        "---",
        "",
        "## 3. Antigravity Historical Foundations",
        "",
        "Antigravity sessions established the core architectural foundations of 100gram:",
        ""
    ]
    
    for s in antigravity_info:
        lines.append(f"- **`{s['id']}`** — {s['description']}")
        if s['artifacts']:
            lines.append(f"  *Key artifacts ({s['artifacts_count']}):* `{', '.join(s['artifacts'])}`")
            
    lines.extend([
        "",
        "---",
        "",
        "## 4. Recent Commits (Last 10)",
        "",
        "```text"
    ])
    for c in git_info.get("recent_commits", []):
        lines.append(c)
    lines.extend([
        "```",
        ""
    ])
    lines.extend([
        "---",
        "",
        "## 5. Current Focus & Handoff Point",
        ""
    ])
    
    if opencode_info and "session_id" in opencode_info:
        lines.extend([
            f"- **Active OpenCode Session:** `{opencode_info['session_id']}` ({opencode_info['title']})",
            f"- **Last Updated:** {opencode_info['updated']}",
            f"- **Last Request:** \"{opencode_info['last_prompt']}\"",
            "- **Active Focus:** Custom User Badges (`HundredGramBadgesController`), remote GitHub JSON sync (`badges.json`), Lead Dev role (ID `1211607237L`), and 12-icon VectorDrawable expansion.",
            ""
        ])
    else:
        lines.extend([
            "- **Active Focus:** Custom User Badges (`HundredGramBadgesController`), remote GitHub JSON sync (`badges.json`), Lead Dev role (ID `1211607237L`), and 12-icon VectorDrawable expansion.",
            ""
        ])

    all_commits = subprocess.check_output(["git", "-C", REPO_ROOT, "log", "--oneline"], text=True).splitlines()
    tracked_features = [
        {"name": "MD3 Theme & Toggles", "pattern": r"md3 suite|md3 theme", "keys": "`HundredGramConfig.md3*`, `Theme.java`", "category": "UI"},
        {"name": "Sliders Engine (MD3 SeekBar)", "pattern": r"slider", "keys": "`HundredGramConfig.newSliderStyle`, `SeekBarView`", "category": "UI"},
        {"name": "Sticker Owner & TTL Secret View", "pattern": r"ttl secret|sticker owner", "keys": "`HundredGramConfig.secretViewTTL`, `StickersAlert`", "category": "Chats"},
        {"name": "Social Link Fixers", "pattern": r"social link", "keys": "`HundredGramConfig.socialLinkFixer`, `ChatActivity`", "category": "Chats"},
        {"name": "Glass Message Bubbles", "pattern": r"glass bubbles", "keys": "`HundredGramConfig.glassMessages*`, `MessageDrawable`", "category": "Visuals"},
        {"name": "Expressive Chat Input", "pattern": r"expressive input", "keys": "`HundredGramConfig.chatBoxStyle`, `ChatActivityEnterView`", "category": "Visuals"},
        {"name": "Sticker Quote Generator", "pattern": r"sticker quotes", "keys": "`HundredGramConfig.stickerQuotes`, `HundredGramQuoteStickerAlert`", "category": "Visuals"},
        {"name": "Song Lyrics (ID3 + LRCLIB)", "pattern": r"song lyrics", "keys": "`HundredGramConfig.lyricsInPlayer`, `HundredGramLyricsHelper`", "category": "Media"},
        {"name": "MD3 Audio Player", "pattern": r"md3 audio player", "keys": "`HundredGramConfig.md3AudioPlayer`, `AudioPlayerAlert`", "category": "Media"},
        {"name": "Slowmode Queue", "pattern": r"slowmode queue", "keys": "`HundredGramConfig.slowmodeQueue`, `HundredGramSlowmodeController`", "category": "Chats"},
        {"name": "Custom User Badges & Remote Sync", "pattern": r"custom (user )?badges|badges sync", "keys": "`HundredGramConfig.customBadgesEnabled`, `HundredGramBadgesController`", "category": "Badges"},
        {"name": "12 Vector Badges Icon Pack", "pattern": r"vector badge", "keys": "12 VectorDrawable XML assets (`badge_*.xml`)", "category": "Assets", "status": "🟡 In Progress"},
        {"name": "Scheduled / Bulk Gifts", "pattern": r"bulk gift", "keys": "`StarGiftSheet.java`, TL send flows", "category": "Gifts", "status": "⏳ Backlog"},
        {"name": "Icon Color Source (Monet / Rainbow)", "pattern": r"icon color source", "keys": "`IconPacksActivity`", "category": "Theming", "status": "⏳ Backlog"},
        {"name": "AI Chat FX Glow", "pattern": r"ai glow|ai fx", "keys": "Visual glow shader on AI responses", "category": "AI", "status": "⏳ Backlog"},
    ]

    lines.extend([
        "---",
        "",
        "## 6. Feature Status & Roadmap Parity (Dynamic from Git)",
        "",
        "### 🟢 Implemented Features",
        "",
        "| Feature | Category | Commit | Key References |",
        "|---|---|:---:|---|"
    ])

    backlog_items = []
    for f in tracked_features:
        matched_commit = None
        for c in all_commits:
            if re.search(f["pattern"], c, re.IGNORECASE):
                matched_commit = c.split(" ", 1)[0]
                break
        if matched_commit:
            lines.append(f"| **{f['name']}** | {f['category']} | `{matched_commit}` | {f['keys']} |")
        else:
            status = f.get("status", "⏳ Backlog")
            backlog_items.append(f"| **{f['name']}** | {f['category']} | {status} | {f['keys']} |")

    lines.extend([
        "",
        "### ⏳ Remaining Backlog & Active Work",
        "",
        "| Feature | Category | Status / Priority | Key References |",
        "|---|---|---|---|"
    ])
    lines.extend(backlog_items)
    lines.extend([
        "",
        "> Full parity matrix available in [`OPEXGRAM_PARITY.md`](file:///D:/100gram/OPEXGRAM_PARITY.md) and [`ROADMAP_PLAN.md`](file:///C:/Users/wicked/.gemini/antigravity-cli/brain/c62ff3ad-d3c3-41c1-99af-c5f825ca711a/ROADMAP_PLAN.md).",
        "",
        "---",
        "",
        "## 7. Key Architectural Touchpoints",
        "",
        "- **Configs:** `HundredGramConfig.java`",
        "- **Settings Activities:** `HundredGramSubSettingsActivity.java`, `HundredGramMessageMenuActivity.java`, `HundredGramSettingsActivity.java`",
        "- **Custom Engines:** `HundredGramHaptics.java`, `HundredGramLocks.java`, `HundredGramDeleteMyMessages.java`, `HundredGramRealNameCache.java`, `HundredGramIconManager.java`, `HundredGramAiClient.java`",
        "- **Strings:** `hundredgram_strings.xml`, `hundredgram_strings_ru.xml`",
        "",
        "---",
        "",
        "## 8. Verification Command",
        "",
        "```powershell",
        ".\\gradlew.bat :TMessagesProj_AppStandalone:assembleAfatDebug -x lint -x test --daemon",
        "```"
    ])
    return "\n".join(lines) + "\n"

def main():
    git_info = get_git_info(REPO_ROOT)
    opencode_info = get_opencode_summary()
    antigravity_info = get_antigravity_summary()
    content = build_markdown(git_info, opencode_info, antigravity_info)
    
    target_path = sys.argv[1] if len(sys.argv) > 1 else OUTPUT_FILE
    with open(target_path, "w", encoding="utf-8") as f:
        f.write(content)
    print(f"Project state written successfully to {target_path}")

if __name__ == "__main__":
    main()
