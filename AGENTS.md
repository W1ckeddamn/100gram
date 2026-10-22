# 100gram Agent Instructions

## 🔧 Setup
- **Windows**: Set `JAVA_HOME` to Android Studio's JBR:  
  `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"`
- **local.properties**: Required for SDK/NDK paths:  
  ```properties
  sdk.dir=C:\\Users\\<USER>\\AppData\\Local\\Android\\Sdk
  ndk.dir=C:\\Users\\<USER>\\AppData\\Local\\Android\\Sdk\\ndk\\26.1.10909125
  ```

## 🏗️ Building
- **Debug APK** (fast, skips lint/tests):  
  `.\gradlew.bat :TMessagesProj_AppStandalone:assembleAfatDebug -x lint -x test --daemon`
  Output: `TMessagesProj_AppStandalone\build\outputs\apk\afat\debug\app.apk`
- **Release APK**:  
  `.\gradlew.bat :TMessagesProj_AppStandalone:assembleAfatRelease -x lint -x test --daemon`
  Output: `TMessagesProj_AppStandalone\build\outputs\apk\afat\release\app.apk`
- **Signing**: Release build uses env vars or local.properties:  
  `RELEASE_KEY_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_STORE_PASSWORD`

## ⚠️ Critical Warnings & Upstream Sync
- **C++ NDK Submodule Trap (Immunity via `merge=ours`)**:  
  Upstream Telegram (`DrKLO/Telegram`) replaces `TMessagesProj/jni/` with submodules, which would delete all native C++ build sources if merged carelessly.  
  To protect local C++ sources automatically, `.gitattributes` configures:
  ```gitattributes
  TMessagesProj/jni/** merge=ours
  .gitmodules merge=ours
  ```
  Ensure the merge driver is enabled once per clone:
  `git config merge.ours.driver true`  
  When syncing with upstream (`git merge upstream/master`), Git keeps all local C++ files and updates all Java/Kotlin code, layouts, and assets without manual copying.
- **Duplicate resources**: Before building, check for conflicts:  
  `.\gradlew.bat :TMessagesProj:packageDebugResources --daemon`  
  Custom strings must be unique or exist in only one of `strings.xml`/`hundredgram_strings.xml`.

## 📂 Modules
- `:TMessagesProj`: Core library (Java/C++)
- `:TMessagesProj_AppStandalone`: Main app module used for APK builds (referenced in commands above)

## 🧪 Testing
- Tests are skipped in default build commands for speed. To run:  
  `.\gradlew.bat :TMessagesProj_AppStandalone:testAfatDebugUnitTest`