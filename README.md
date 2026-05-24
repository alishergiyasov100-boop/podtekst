# Подтекст · Cyberpunk Subtext Decoder

4 быстрых тапа по любому сообщению в любом мессенджере → поверх экрана появляется
киберпанк-плашка с расшифровкой подтекста (синий неон, сканлайны, шум, RGB-split,
glitch-in).

## Архитектура

- **AccessibilityService** ловит TYPE_VIEW_CLICKED, считает 4 тапа в окне 1.5 сек
  на одной ноде, вытаскивает текст + до 12 соседних сообщений как контекст.
- **OverlayService** (SYSTEM_ALERT_WINDOW + FOREGROUND_SERVICE) рисует Compose-плашку
  поверх любого приложения.
- **AnalysisPipeline** — 3 паса через PocketQwal Relay (OpenAI-совместимый API):
  1. факты + эмоции + таймлайн (T=0.2)
  2. три гипотезы интерпретации (T=0.6, включая «доброжелательную» и «тёмную»)
  3. JSON-вердикт: subtext, confidence, 3 интерпретации, red flags, counter-script (T=0.3)
- **Словарь манипуляций**: гаслайт, DARVO, love-bombing, breadcrumbing,
  stonewalling, guilt-trip, проекция, future-faking, triangulation, negging,
  weaponized incompetence.

## Использование

1. Установи APK, открой приложение.
2. Выдай Accessibility-разрешение (для перехвата тапов).
3. Выдай Overlay-разрешение (Draw over other apps).
4. Укажи URL твоего PocketQwal Relay (или любого OpenAI-совместимого endpoint).
5. В любом мессенджере 4 раза подряд тапни по сообщению.

Никакого облака без твоего ведома: текст уходит только на указанный тобой URL.
