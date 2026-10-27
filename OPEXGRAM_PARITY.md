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

## Missing — feasible, next batches

| Feature | Why it is not done yet | Rough touch points |
|---|---|---|
| Chat page-up button | Needs a floating control in `ChatActivity` that tracks the enter-view height | `ChatActivity.createView`, `ChatActivityEnterView` |
| Haptics level + per-action tune | Telegram calls `performHapticFeedback` in ~270 places; OpexGram wraps its own vibrator helper | `AndroidUtilities.vibrate`, action call sites |
| Chat box animations / motion / contrast | Needs bubble color/alpha math and enter-view animation changes | `ChatMessageCell`, `ChatActivityEnterView`, `Theme` |
| Glass message bubbles + transparency + border + chat edge blur | Big renderer change; upstream `LiquidGlassEffect` exists but is not wired to bubbles | `ChatMessageCell.drawBackground`, blur3 |
| Quotes | Needs a message-menu action and quote rendering | `ChatActivity` menu, `ChatMessageCell` |
| Message menu customization | Needs a reorderable list and menu builder changes | `ChatActivity`, message menu |
| Stories: hide profile / hide posting | Only the stories bar toggle exists | `ProfileActivity`, story entry points |
| Profile name switch | Action-bar title source change | `ProfileActivity`, `ChatAvatarContainer` |
| Folder menu style / segments | Partially covered by tab title style | `DialogsActivity`, `MainTabsActivity` |
| Icon color source (monet / rainbow) | Icon pack engine exists, color source does not | `IconPacksActivity` |
| AI FX glow / rainbow | Visual-only AI chrome | AI chat UI |
| Locks: archive / secret / saved / delete + grace | Needs a passcode prompt and per-surface guards | `DialogsActivity`, `ChatActivity`, passcode |
| Delete my messages | Bulk delete; must keep the existing confirm dialog | `ChatActivity`, `MessagesController` |
| Bulk gifts / scheduled gifts | Needs new TL flows and gift picker UI | `StarGiftSheet`, `ApiWrap` |
| Reset all settings | Needs a destructive-action confirm and config reset | `HundredGramConfig`, backup activity |

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
