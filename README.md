# 🎙️ Voice Typer (Kotlin)

## 📋 Описание

Voice Typer — легковесный, полностью локальный и приватный ассистент голосового ввода для Windows.
Написан на Kotlin, работает на базе whisper.cpp (CLI), записывает речь по удержанию горячей клавиши,
распознаёт её без отправки в интернет и автоматически вставляет готовый текст в любое активное поле ввода.

## ✨ Возможности

- **Push-to-Talk (Удержание F9):** Запись голоса, отпускание — обработка и вставка текста
- **Глобальные хоткеи:** Работает в любом приложении, даже когда свернуто
- **Переключение языка (F10):** RU/EN для транскрипции
- **Быстрое скрытие (F8):** Свернуть/развернуть окно
- **Кастомный UI:** Плавающее окно без рамок, перетаскивание мышкой
- **Системный трей:** Иконка с индикатором записи
- **Абсолютная приватность:** Всё работает локально

## 🛠️ Установка

### Требования

1. **Java 17+** (JDK)
2. **whisper.exe** — бинарник whisper.cpp (прилагается в `resources/win/` или скачайте с [GitHub](https://github.com/ggerganov/whisper.cpp))

### Запуск

```bash
# Сборка проекта
./gradlew build

# Запуск
./gradlew run
```

Или через IDE (IntelliJ IDEA):
- Откройте проект
- Нажмите Run (Shift+F10)

## ⌨️ Горячие клавиши

| Клавиша | Действие |
|---------|----------|
| **F9 (удержание)** | Начать запись | Отпустить — обработка и вставка |
| **F10** | Переключить язык (RU/EN) |
| **F8** | Показать/скрыть окно |

## ⚙️ Конфигурация

Файл настроек: `%USERPROFILE%\.config\voice-typer\config.json`

```json
{
  "show_window_hotkey": "f8",
  "record_key": "f9",
  "lang_toggle_hotkey": "f10",
  "sample_rate": 16000,
  "channels": 1,
  "model_size": "base",
  "device": "auto",
  "beam_size": 5,
  "language": "ru",
  "app_language": "ru",
  "window_size": "260x160"
}
```

## 📁 Структура проекта

```
src/main/kotlin/com/voicetyper/
├── AppConfig.kt       # Модель конфигурации
├── TranscriptionResult.kt # Результат транскрипции
├── ConfigManager.kt   # Загрузка/сохранение конфига
├── Locales.kt         # Локализация (RU/EN)
├── Logger.kt          # Логирование
├── AudioRecorder.kt   # Запись с микрофона
├── WavFileWriter.kt   # Запись PCM в WAV
├── WhisperEngine.kt   # Вызов whisper.exe
├── HotkeyListener.kt  # Глобальные хоткеи
├── TrayIconManager.kt # Иконка в трее
├── ClipboardManager.kt # Вставка текста
├── UiCanvas.kt        # Кастомный UI
├── AppProcessor.kt    # Центральный процессор
└── Main.kt            # Точка входа
```

## 📄 Лицензия

MIT License
