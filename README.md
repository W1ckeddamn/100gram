> Yeah, well... I'm gonna go build my own theme park, with blackjack and hookers.

(c) Bender Bending Rodríguez

![Telegraher](/TMessagesProj/src/main/res/mipmap-xhdpi/ic_launcher.png)

## Telegraher

> 3rd generation

[TOP 3](https://github.com/DrKLO/Telegram/forks?include=active&page=1&period=&sort_by=stargazer_counts) Telegram Android
fork! ([screenshot](top3_tg_forks_2026-08-29.jpg)'2026-08-29)

![Telegraher](/telegraher.512px.png)

* **No one gets to decide** what i run on my device
* **No one gets to decide** where i run my app
* **No one gets to decide** what must be deleted

> This is my device so i control it 😎

> This app have nothing with privacy, it's remotely controlled. It's pissing me off, so i changed
> that.

> I took an original Telegram client from ["official" repo](https://github.com/DrKLO/Telegram) and
> made my own theme park with blackjack and hoookers.

**Special thanks**:

* my wife and my dog, love them 🍑
* mr Rodríguez for the inspiration
* some anonymous folks over the telegram for the great ideas (can't share their names here, cause
  they are anonymous)
* "Telegram🦄magic🦄team" for their "magic🦄updates" including ~~private~~ chats and "magic🦄ads"

### WTF?! / is it legit?

Follow the ~~white rabbit~~ the git flow:

* i took and forked the original client
* i cloned the latest `master` branch (with a.b.c patch) into `master_a.b.c`
* i made another branch `graher_a.bc` from `master_a.b.c`, it contain the changes
    * actual master branch for repo is `graher_12.92.1` (YES it have to be `graher_12.92`)

It gives us `telegram` -> `master` -> `master_a.b.c` -> `graher_a.bc`

So **all the code changes** are in `graher_a.bc`

### Summary

This is a 3rd generation of Graher, a grand-kid whatever

ReadMe history:

* old README from [8.3](README_old_8.3.1.md)
* 2023th [9.33](README_old_9.33.md).
* 2026 pre-graher3 [12.92-old](README-pre-12.92.md).

### Latest release (12.10.5)

**100gram 12.10.5 (Build 7105010)** — based on upstream [DrKLO/Telegram 12.10.5](https://github.com/DrKLO/Telegram).

> 📘 **Полная техническая документация и руководство по передаче проекта**: [HANDOVER.md](HANDOVER.md)

#### ✨ Новые возможности 100gram:
* **🚀 Network Boost (Сетевой ускоритель)**: параллельное скачивание и выгрузка до 8 потоков MTProto, встроенный мониторинг скорости и пинг-тест DC.
* **🎥 Advanced Camera & Video Notes Engine**:
  * Поддержка движков: **Jetpack CameraX**, **Camera2**, **Camera1 Legacy** и системного Telegram.
  * **⬛ Квадратные видеосообщения («Квадратные кружочки»)**: запись и отображение с аккуратным скруглением (16dp).
  * **🌫️ Отключение размытия фона**: переписка остается видимой и читаемой во время записи кружка.
  * **🎚️ Ступенчатый битрейт**: 750 Кбит/с, 1.2 Мбит/с, 2.0 Мбит/с, 3.0 Мбит/с Ultra.
  * **📐 Поддержка старых сенсоров**: переключение соотношения 16:9 vs 4:3, разрешение 240p/360p/480p/720p.
  * **60 FPS** и ультраширокоугольный объектив (0.6x).
* **🗄️ Сейф медиа (Media Vault)**:
  * Фоновый архиватор медиа из 100% чатов (включая архивные папки).
  * Авторотация по сроку (TTL) и квоте диска с защитой избранных чатов.
  * Чистый интерфейс с поиском, чипами-фильтрами и контекстными действиями.
* **🎭 Режим стримера**:
  * Защита окна `FLAG_SECURE` (полное затемнение в OBS, Discord, захвате экрана, блокировка скриншотов).
  * Мгновенное маскирование текста сообщений на `••••••••••`.
  * Маскирование профиля в шапке настроек (фейковое имя, скрытые номера и юзернеймы).
* **👻 Режим невидимки (Ghost Mode)**: нечиталка сообщений, анонимный просмотр Stories, скрытие статуса набора текста.
* **🤖 Правила автоматизации (Automation Engine)**: фильтры по ключевым словам/медиа с автосохранением в Избранное/Галерею и пересылкой.
* **🛰️ Keep-Alive Foreground Service**: защита от убийства фоновых процессов системой.

---

### Сборка (Build)

Требования: **JDK 17**, **Android SDK 35**, **NDK 26.1.10909125**, **CMake 3.22.1**.

* **Отладочная сборка (Debug APK)**:
  ```powershell
  .\gradlew.bat :TMessagesProj_AppStandalone:assembleAfatDebug -x lint -x test --daemon
  ```
  Выходной файл: `TMessagesProj_AppStandalone/build/outputs/apk/afat/debug/app.apk`

* **Релизная сборка (Release APK)**:
  ```powershell
  .\gradlew.bat :TMessagesProj_AppStandalone:assembleAfatRelease -x lint -x test --daemon
  ```
  Выходной файл: `TMessagesProj_AppStandalone/build/outputs/apk/afat/release/app.apk`

**If you're a donut**

1) Hey buddy, how are you?
2) Nice to see you!
3) You're so smart and clever if you're interrested in graher ❤️
4) You need to create a github account
5) Then you need to fork graher (graher it's a short of telegraher) using big white (or black) fork button
6) Then you can go to Actions where you click on `sudo bring me a beer`
7) Once you clicked, cause NO BEER = NO GRAHER, you have to click on `Run workflow`
8) `Graher version bruh` - any version, `Tell me bruh` - the text you wanna share on release, `Multipass?` here you pick
   `Multipass?`. If you pick `Korben Dallas` you won't board that spaceship!
9) It will work somehow around 40min or an 1h and build you your own babygraher 🎉

You can still use docker TG offer for official build, just build a **Standalone** app

### APKs

* for android >=5
* Realeases are
  here: [https://github.com/nikitasius/Telegraher/releases](https://github.com/nikitasius/Telegraher/releases)
    * if it contain `beta` it mean it's BETA
* To get help in chat write `!graher`

### Issues/Wishlist

Feel free to use the "issues section". I'm not an Android programmer, i'm a Java developper.
Probably it's a good thing 😃

### [Changes](README_CHANGES.md)

### Big thanks 🍻 :

* [xMAXIMx ](https://t.me/xMAXIMx) - chat bot maker, angry mod and support from 1-st days
* [fru2nyanya](https://t.me/fru2nyanya) - FAQ maker, user helper
* [redditOwner](https://t.me/redditOwner) - phone number supporter and extreme multiacc tester
* [Wolfsschanze44](https://t.me/Wolfsschanze44) - graher gen-2 notification icon designer
* [Scarlet4th](https://t.me/Scarlet4th) - chat keeper, english corrector
* [kolomviano](https://t.me/kolomviano) - making features and pushing PRs
* [scrizza](https://t.me/scrizza) - long time tester, extra fan (Humpee told that)
* ping [me](https://t.me/nikitasius) if i forgot someone

### Code mirrors

* Github: [https://github.com/nikitasius/Telegraher](https://github.com/nikitasius/Telegraher)
* Gitlab (**OUTDATED**): [https://gitlab.com/nikitasius/Telegraher](https://gitlab.com/nikitasius/Telegraher)
    * autosync from github is for prem only, have to build a CI/CD dammit!
* HTTPS: [https://git.evildayz.com/Telegraher/](https://git.evildayz.com/Telegraher/)
    * manually sync (add a script later 😀)
    * `releases` w/ actual releases and cloned `Telegraher` & `Telegraher.git` in `.tar.gz`
    * `betas` w/ actual betas
* TG channel: [https://t.me/telegraher](https://t.me/telegraher)
* XDA thread: https://xdaforums.com/t/blackjack-and-hookers.4492879/

### Coffee
 
* Here is my [PayPal](https://paypal.me/nikitasius) `https://paypal.me/nikitasius`
* Cryptos
    * [BTC](bitcoin:bc1qhdxuuud5cy494m7s2gg3gy9pgz052wjtvrgu6m?message=github_telegraher)
      `bc1qhdxuuud5cy494m7s2gg3gy9pgz052wjtvrgu6m`
    * [XMR](monero:85jHELdPDbZSsJAUccud3aRmoF3oJszxCCJ3zEETM85AHeRfpEbQKnRFAkwHtdxfWhX5DaZf4v8NEGz6Wa6WHgQBGQXnYhr?message=github_telegraher)
      `85jHELdPDbZSsJAUccud3aRmoF3oJszxCCJ3zEETM85AHeRfpEbQKnRFAkwHtdxfWhX5DaZf4v8NEGz6Wa6WHgQBGQXnYhr` (best crypto
      ever, back in [old days](https://github.com/monero-project/monero/issues/6151))
    * [ETH](eth:0x00008672BC75707d897264a3dBB85036BCdE4CB3?message=github_telegraher)
      `0x00008672BC75707d897264a3dBB85036BCdE4CB3` and `USDT` on Ethereum
    * [SOL](solana:7ds9H5jFqnrZ1CemzDhqQnPvwiEuFVbW153bZihnF1dB?message=github_telegraher)
      `7ds9H5jFqnrZ1CemzDhqQnPvwiEuFVbW153bZihnF1dB` and `USDT` on Solana
* Here is
  my [Yoomoney](https://yoomoney.ru/to/410015481871381) `https://yoomoney.ru/to/410015481871381`

> In fact, forget the park!
