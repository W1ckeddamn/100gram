# OpexGram → 100gram feature parity

Source: `opexgram-13.1.1-beta10.apk` (`com.opexgram.messenger`, 72379), settings hub `kc.m2` / `kc.j1`.
Status is checked against the code in this repo, not against the settings UI alone.

## Implemented in this batch

| OpexGram setting | 100gram implementation |
|---|---|
| Weekday in date (`OpexgramWeekdayInDate`) | `HundredGramConfig.showWeekdayInDate` → `LocaleController.formatDate` uses `EEE, d MMM` |
| Forward time (`OpexgramForwardTime`) | `HundredGramConfig.forwardTime` → forwarded-name line in `ChatMessageCell` gets `· HH:mm` |
| Selection speed (`OpexgramSelectionSpeed`) | `HundredGramConfig.selectionSpeed` (2..8) → checkbox animation duration in `ChatMessageCell.animateCheckboxTranslation` |
| Selection no limit (`OpexgramSelectionNoLimit`) | `HundredGramConfig.selectionNoLimit` → skips the 100-message cap in `ChatActivity` |
| Media glow (`OpexgramMediaGlow`) | `HundredGramConfig.mediaGlowLevel` (off/low/medium/high) → halo in `ChatMessageCell.drawPhotoImage` |
| Delete effect (`OpexgramDeleteEffect`) | `HundredGramConfig.thanosDeleteEffect` → gates `ChatActivity.supportsThanosEffect` / `getChatThanosEffect` |

All six have rows in `HundredGramSubSettingsActivity` and entries in `HundredGramSettingsSearchIndex`.

## Batch 2

| OpexGram setting | 100gram implementation |
|---|---|
| Chat page-up button (`OpexgramChatPageUpButton`) | `HundredGramConfig.chatPageUpButton` → 8th side-control button (`BUTTON_PAGE_UP`), `ChatActivity.scrollPageUp()` jumps 100 messages back |
| Reset all settings | `HundredGramConfig.resetAllSettings()` + red row in every category + confirm dialog |
| Stories: hide in profiles (`OpexgramStoriesHideProfile`) | `HundredGramConfig.hideStoriesProfile` → `ProfileActivity.needInsetForStories()` |
| Stories: hide posting (`OpexgramStoriesHidePosting`) | `HundredGramConfig.hideStoriesPosting` → profile button, channel submenu, `DialogsActivity` FAB/hint/self-menu |

## Batch 3

| OpexGram setting | 100gram implementation |
|---|---|
| Delete my messages (`OpexgramDeleteMyMessages`) | `HundredGramDeleteMyMessages` — `messages.search` with `from_id = self`, batches of 100, progress + stop dialog, one chat (menu item) or many (`DialogsActivity` multi-select picker) |

## Batch 4

| OpexGram setting | 100gram implementation |
|---|---|
| Locks: archive (`OpexgramLockArchive`) | `HundredGramConfig.lockArchive` → `HundredGramLocks.guardArchive()` in `DialogsActivity.createView` when `folderId == 1` |
| Locks: secret chats (`OpexgramLockSecret`) | `lockSecret` → `HundredGramLocks.isChatLocked()` includes encrypted dialogs |
| Locks: saved messages (`OpexgramLockSaved`) | `lockSaved` → self dialog |
| Locks: delete confirmation (`OpexgramLockDelete`) | `lockDelete` → `DialogsActivity.performSelectedDialogsAction` wraps delete/clear in a credential prompt |
| Lock a single chat (`OpexgramLockChatLock`) | `HundredGramConfig.lockedChats` + "Lock chat / Unlock chat" in the chat-list selection menu; `ChatActivity.onResume` guard |
| Grace period (`OpexgramLockGrace`) | `lockGrace` seconds → `HundredGramLocks.shouldAsk()`; picker in Privacy |
| Prompt UI (`OpexgramLockUnlock`) | `HundredGramLocks` overlay (lock lottie + title + Unlock) + `BiometricPrompt` (biometric or device credential) |

## Batch 5

| OpexGram setting | 100gram implementation |
|---|---|
| Haptics master + strength (`OpexgramHapticsEnable`) | `HundredGramHaptics` engine; `HundredGramConfig.hapticsEnabled` / `hapticStrength` (20–200%) |
| Per-action effect (`OpexgramHapticsActive`) | 12 effects (selection, tick, soft, click, rigid, heavy, double, rise, fall, success, warning, error) picked per action; vibration primitives on Android 11+, waveform below |
| Wired actions | all 20 actions: send message, send media, attach picker, set reaction, select message, tab switch, archive pull, account switch, photo dismiss, message action, menu selection, swipe to reply, photo swipe, zoom edge, slider step, player track, player speed, error, success, incoming message |
| System hooks | `AndroidUtilities.vibrate/vibrateCursor`, `ActionBarMenuSubItem.performClick`, `MediaController`, `ChatActivity` (swipe reply, message actions, incoming), `PhotoViewer` (swipe, zoom edge), `Bulletin` (success/error) |

## Batch 6 — folder swipe animation

| OpexGram setting | 100gram implementation |
|---|---|
| Folder swipe (`OpexgramFolderSwipe`) | `HundredGramConfig.folderSwipe` → `HundredGramFolderSwipe.apply()` from `DialogsActivity.ViewPage.setTranslationX`; scale `1 - shrink * sin(pi * progress)`, dp(28) rounded outline clipped to the tabs |
| Shrink percent (`OpexgramFolderSwipeShrink`) | `folderSwipeShrink` 2–20 % (default 8), picker in Interface |
| Spring (`OpexgramFolderSwipeSpring`) | `folderSwipeSpring` 0/1/2 → `HundredGramFolderSwipe.Spring` drives the page-snap animator (`DialogsActivity`) and the tab indicator (`FilterTabsView.animationRunnable`) |

## Batch 8 — message menu customization

| OpexGram setting | 100gram implementation |
|---|---|
| Message menu order & visibility | `HundredGramMessageMenu` config (`order` + `hidden` prefs); 23 entries across 6 groups (reply/forward, copy, manage, translation, save, other) with icons; `apply()` filter/sort pass at the end of `ChatActivity.fillMessageMenu` reorders/hides items; `HundredGramMessageMenuActivity` drag-and-drop settings with per-item tap-to-hide and Reset |
| Settings entry | Row "Пункты меню" in `HundredGramSubSettingsActivity` (Actions category) + `HundredGramSettingsSearchIndex` entry |

## Batch 9 — Profile real-name cache & name switch

| OpexGram setting | 100gram implementation |
|---|---|
| Profile name switch (`OpexgramProfileNameSwitch`) | `HundredGramRealNameCache` (per-account real name storage, strangers-only cache, 3000->2000 LRU trim, contact switch); toggle in settings and UI hook in `ProfileActivity` |

## Batch 10 — UI tweaks, autoplay & folders

| OpexGram setting | 100gram implementation |
|---|---|
| Bubble tails (`OpexgramBubbleTails`) | Toggle in `HundredGramConfig` wired to `ChatMessageCell` bubble drawing |
| Pinned downloads (`OpexgramPinnedDownloads`) | Pinned downloads bar in cache / media management |
| Hide "All Chats" folder (`OpexgramHideAllChats`) | Toggle to omit the default all-chats tab when custom folders exist |
| Autoplay toggles | Independent toggles for voice messages, round videos, and music playback |
| Remember last folder | Persists and restores selected folder ID across app restarts |
| Profile join date | Displays registration date / account age in `ProfileActivity` |
| Poll results peek | `showResultsBeforeVoting` + `pollPeekWarned` to preview votes without submitting |
| Big reactions & transcript quotes | Enlarge reactions on tap; direct quoting of speech-to-text transcriptions |
| Swipe hides keyboard | `hideKeyboardOnScroll` in `ChatActivity` |

## Already present before this batch

| OpexGram feature | Existing 100gram feature |
|---|---|
| Media size (sticker/GIF) | `stickerSize` / `gifSize` |
| Swipe hides keyboard | `hideKeyboardOnScroll` |
| Swipe actions | `swipeActions` / `swipeActionsLoop` / `swipeActionsReversed` |
| Double tap incoming/outgoing | `doubleTapAction` / `doubleTapActionOutOwner` |
| Time seconds | `showSecondsInTime` |
| Poll results preview | `showResultsBeforeVoting` |
| Forwards counter | `showForwardsCount` |
| Hide account phone | `hidePhoneNumber` |
| Show peer ID | `showIdAndDc` |
| Join date / account age | `HundredGramRegDateController` in `ProfileActivity` |
| Open archive on pull | `archiveOnPull` |
| Hide stories bar | `hideStoriesBar` |
| Folder tabs icon/text | `tabTitleStyle` |
| Chat title modes | `actionBarTitleMode` / `centerActionBarTitle` |
| Backup import/export | `HundredGramBackupManager` + `HundredGramBackupActivity` |
| AI config / fact check | `HundredGramAiClient` / `FactCheckController` |
| HTML/JSON/MD/TXT chat export | `HundredGramChatExporter` |

## Batch 11 — Visuals (Phase 4)

| OpexGram setting | 100gram implementation | Commit |
|---|---|:---:|
| Expressive chat input | `HundredGramConfig.chatBoxStyle` (0 = Classic, 1 = Floating Capsule, 2 = Floating with separate Send button) in `ChatActivityEnterView` | `a5e541068` |
| Glass message bubbles | `HundredGramConfig.glassMessages`, `glassMessagesTransparency` (10..90%), `glassMessagesBorder` in `MessageDrawable` | `a5e541068` |
| Sticker quote generator | `HundredGramConfig.stickerQuotes` + `HundredGramQuoteStickerAlert` (export to 512px WEBP sticker) in `ChatActivity` | `a5e541068` |

## Batch 12 — MD3 Suite & Sliders (Phases 1 & 2)

| OpexGram setting | 100gram implementation | Commit |
|---|---|:---:|
| Material Design 3 Suite | `HundredGramConfig.md3MasterEnabled`, `md3ChatList`, `md3Sections`, `md3Controls`, `md3Menus`, `md3NavigationStyle`, `md3MediaLoader` | `0459f1b50` |
| MD3 Sliders Engine | `HundredGramConfig.newSliderStyle` in `SeekBarView` (10dp track, capsule thumb, gap, snap points) | `0459f1b50` |
| Sticker pack owner | `HundredGramConfig.stickerPackOwner` (author ID calculation, clickable author profile in `StickersAlert`) | `0459f1b50` |
| Secret view TTL | `HundredGramConfig.secretViewTTL` (burn-timer & read receipt bypass + FLAG_SECURE unlock) | `0459f1b50` |
| Social link fixers | `HundredGramConfig.socialLinkFixer` (fxtwitter, fixupx, ddinstagram, vxtiktok, phixiv) | `0459f1b50` |

## Batch 13 — Media & Player (Phase 5)

| OpexGram setting | 100gram implementation | Commit |
|---|---|:---:|
| Song lyrics in player | `HundredGramConfig.lyricsInPlayer` + `HundredGramLyricsHelper` (ID3 + LRCLIB) + `HundredGramLyricsBottomSheet` | `4821a0375` |
| MD3 Audio Player | `HundredGramConfig.md3AudioPlayer` (rounded artwork, accent circle play/pause) in `AudioPlayerAlert` | `4821a0375` |
| Slowmode queue | `HundredGramConfig.slowmodeQueue` + `HundredGramSlowmodeController` (FIFO queue + countdown + auto-dispatch) | `4821a0375` |

## Batch 14 — Custom User Badges & Remote Sync

| Feature | 100gram implementation | Commit |
|---|---|:---:|
| Custom User Badges | `HundredGramConfig.customBadgesEnabled` + `HundredGramBadgesController` + `HundredGramBadgeEditorBottomSheet` + `HundredGramBadgeAlert` + UI hooks | `06605a865` |
| Remote Badges Sync | GitHub raw sync (`badgesSyncUrl`, `badgesAutoSync`), Lead Dev owner role (`1211607237L`), admin management, export to clipboard | `69f03668a` |

## Missing — feasible, next batches

| Feature | Why it is not done yet | Rough touch points |
|---|---|---|
| 12 Vector Badges Icon Pack | In progress (vector assets catalog for badges) | `TMessagesProj/src/main/res/drawable/badge_*.xml` |
| Icon color source (monet / rainbow) | Icon pack engine exists, dynamic color source does not | `IconPacksActivity` |
| AI FX glow / rainbow | OpexGram's glow sits on its "Ask AI" chat message; this fork answers in bottom sheets | AI chat UI |
| Bulk gifts / scheduled gifts | Needs new TL flows and gift picker UI | `StarGiftSheet`, `ApiWrap` |

## Not implementable client-side

| Feature | Reason |
|---|---|
| Supporter badge (paid, via bot/xRocket) | Server-side entitlement and payment flow; a local toggle would be a fake badge |
| Remote account-age table refresh | Depends on OpexGram's `ageClientKey` backend; the bundled table already works offline |

## Build / verification

```
.\gradlew.bat :TMessagesProj_AppStandalone:assembleAfatDebug -x lint -x test
```

Use the Gradle daemon when free RAM is above ~8 GB; otherwise `--no-daemon -Dorg.gradle.parallel=false`
to avoid the native ABI builds OOM-killing the daemon.
