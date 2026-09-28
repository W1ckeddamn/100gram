# Устранение проблемы с темами и сборкой на Windows в Telegram / Telegraher

В этом документе описана причина критического бага, из-за которого при сборке клиента Telegram (или его форков) на **Windows** перестают работать все стандартные темы оформления, кроме белой («Классической»), а также описано решение проблемы.

---

## 1. Симптомы проблемы

* При сборке на компьютере под управлением Windows (как в `Debug`, так и в `Release`) в приложении **не загружаются и не применяются стандартные темы** («День», «Ночь», «Арктика», «Тёмно-синяя», «Monet» и др.).
* Единственная тема, которая работает корректно — **белая («Классическая»)**.
* При скачивании готовых APK (собранных автором репозитория или через GitHub Actions) все темы работают без нареканий.

---

## 2. Причина возникновения бага

Проблема вызвана цепочкой из трёх факторов, специфичных для Windows-окружения:

### А. Автоматическая замена окончаний строк в Git (`core.autocrlf = true`)
По умолчанию Git для Windows при клонировании репозитория конвертирует символы окончания строк Unix (`LF`, `\n`, байт `0x0A`) в формат Windows (`CRLF`, `\r\n`, байты `0x0D 0x0A`).
Все встроенные темы Telegram хранятся в виде текстовых файлов в папке:
`TMessagesProj/src/main/assets/*.attheme` (`day.attheme`, `night.attheme` и др.).
В результате клонирования все эти файлы на диске получают окончания строк `\r\n`.

### Б. Особенность парсера строк в `Theme.java`
В методе `Theme.getThemeFileValues()` чтение файла темы происходит буфером байтов с поиском символа `\n`:
```java
if (bytes[a] == '\n') {
    int len = a - start + 1;
    String line = new String(bytes, start, len - 1);
    ...
```
Код рассчитан исключительно на Unix-окончания строк: вычитание `len - 1` отрезает только символ `\n`. На Windows в конце каждой прочитанной строки остаётся «невидимый» символ возврата каретки `\r` (CR).

### В. Ошибка индексации в `Utilities.parseInt()`
Строка с цветом имеет вид `windowBackgroundWhite=-14054705\r`. Значение цвета передаётся в парсер чисел:
```java
for (end = 0; end < value.length(); ++end) {
    char character = value.charAt(end);
    boolean allowedChar = character == '-' || character >= '0' && character <= '9';
    if (allowedChar && start < 0) {
        start = end;
    } else if (!allowedChar && start >= 0) {
        end++; // <-- ОШИБКА: захватывает следующий недопустимый символ ('\r')
        break;
    }
}
if (start >= 0) {
    String str = value.subSequence(start, end).toString(); // Получается "-14054705\r"
    val = Integer.parseInt(str);                           // Вызывает NumberFormatException!
}
...
catch (Exception ignore) {}
return val; // Возвращает 0
```
Из-за `end++` в подстроку попадал символ `\r`, вызов `Integer.parseInt` падал с исключением `NumberFormatException`, которое тихо перехватывалось блоком `catch`, и функция возвращала **`0`**.

В итоге **каждый цвет темы парсился как `0` (полностью прозрачный/чёрный)**.

### Почему белая тема работала?
Белая («Классическая») тема Telegram не использует файлы `.attheme` из ассетов — её цвета жестко прописаны в виде Java-констант прямо в коде `Theme.java` в массиве `defaultColors`.

---

## 3. Внесённые исправления

### 1. Конвертация файлов тем в формат LF
Все файлы `*.attheme` в каталоге `TMessagesProj/src/main/assets/` приведены к Unix-окончаниям строк (`LF`, `\n`).

### 2. Добавление файла `.gitattributes`
В корень репозитория добавлен файл `.gitattributes`, запрещающий Git на Windows изменять окончания строк для файлов тем и бинарных данных:
```gitattributes
# Ensure all .attheme files always maintain Unix LF line endings
*.attheme text eol=lf
*.dat binary
*.gzip binary
*.json text eol=lf
*.txt text eol=lf
*.html text eol=lf
```

### 3. Защита в `Theme.java`
В метод `Theme.getThemeFileValues()` добавлена проверка и удаление `\r`:
```java
int len = a - start + 1;
String line = new String(bytes, start, len - 1);
if (line.endsWith("\r")) {
    line = line.substring(0, line.length() - 1);
}
```

### 4. Исправление бага в `Utilities.java`
В методе `Utilities.parseInt()` убран лишний инкремент `end++`:
```java
} else if (!allowedChar && start >= 0) {
    break; // end уже указывает на первый недопустимый символ
}
```

---

## 4. Дополнительно: Ошибка с `ccache` на Windows

При первой сборке на Windows процесс C++ CMake падал с ошибкой:
`CreateProcess failed: The system cannot find the file specified` при вызове `ccache`.

**Причина:** в `TMessagesProj/build.gradle` были жестко заданы параметры компилятора:
```groovy
'-DCMAKE_C_COMPILER_LAUNCHER=ccache',
'-DCMAKE_CXX_COMPILER_LAUNCHER=ccache'
```
Утилита `ccache` используется в Linux CI/CD GitHub Actions, но отсутствует в стандартной системе Windows. 

**Решение:** эти две строки удалены из `TMessagesProj/build.gradle`, после чего Android NDK компилирует C++ файлы напрямую через встроенный Clang.
