# 👁️ Spec

<div align="center">

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11%20%7C%201.16.5-brightgreen)
![Paper](https://img.shields.io/badge/Paper%2FSpigot-Compatible-blue)
![Java](https://img.shields.io/badge/Java-21+%20%7C%208+-red)

**Скрытый режим наблюдения за игроками для модераторов**

</div>

---

## ⚙️ Конфигурация

Полная конфигурация доступна по ссылке: [тык](https://github.com/firewolfik/Spec/blob/main/src/main/resources/config.yml)

## ⭐ Возможности

- Скрытое наблюдение за игроком с возвратом на исходную точку.
- Два режима скрытия:
  - `VANISH` — скрытие через Essentials (из таба и мира).
  - `SILENT` — модератор виден в табе как обычный игрок в выживании (гм0).
- Запросы на спек по ключевым словам из чата (`spec`, `spek`, `спек` и др.).
- Кликабельная кнопка в чате для быстрого перехода в спек.
- Команда `/spec alerts` для вкл/выкл оповещений о запросах.
- Разделение прав: спек только по запросам или за кем угодно (`spec.any`).
- Полная защита модератора в спеке (урон, эффекты, мобы, отбрасывание).
- Сохранение сессий и настроек в SQLite (`sessions.db`).
- Поддержка многострочных сообщений и HEX-цветов.

## 🔧 Команды и права

### Команды

| Команда | Описание | Права |
|---|---|---|
| `/spec <игрок>` | Начать наблюдение за игроком | `spec.use` |
| `/spec` | Завершить наблюдение | `spec.use` |
| `/spec alerts` | Вкл/Выкл оповещения о запросах | `spec.use` |
| `/spec reload` | Перезагрузить конфигурацию | `spec.reload` |

### Права доступа

| Право | Описание |
|---|---|
| `spec.use` | Использование режима наблюдения |
| `spec.any` | Наблюдение за любым игроком без запроса |
| `spec.reload` | Перезагрузка конфигурации |

## 📦 Установка

1. Скачайте `spec-1.21.11.jar` или `spec-1.16.5.jar` из [релизов](https://github.com/firewolfik/Spec/releases).
2. Закиньте в папку `plugins` и перезапустите сервер.

## 📞 Поддержка

- **Telegram**: [тык](https://t.me/oooSwagParty)
- **Issues**: [тык](https://github.com/firewolfik/Spec/issues)

---

<div align="center">

**⭐ Поставьте звезду, если проект вам помог!**

Made with ❤️ by [firewolfik](https://github.com/firewolfik)

</div>
