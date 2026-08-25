# 👁️ Spec

<div align="center">

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen)
![Paper](https://img.shields.io/badge/Paper-Compatible-blue)
![Java](https://img.shields.io/badge/Java-21+-red)

**Скрытый режим наблюдения за игроками для модераторов**

</div>

---

## ⚙️ Конфигурация

Сообщения, HEX-цвета, Action Bar и звуки настраиваются в [`config.yml`](https://github.com/firewolfik/Spec/blob/main/src/main/resources/config.yml).

## ⭐ Возможности

- Скрытое наблюдение за выбранным игроком.
- Два режима скрытия (переключаются в конфиге):
  - `VANISH` — при начале спека модератору выдаётся vanish через Essentials API (скрыт из таба и мира).
  - `SILENT` — модератор виден в табе как обычный игрок в выживании (гм0), без серого выделения и сортировки в конец списка.
- Переключение между целями без завершения режима.
- Возврат на первоначальную позицию после `/spec`.
- Скрытие модератора от игроков без специального права.
- Защита от урона, эффектов, огня, голода и отбрасывания.
- Восстановление состояния после выхода или перезапуска сервера.
- Настраиваемый Action Bar.
- Настраиваемые звуки действий.
- Хранение активных сессий в SQLite (`plugins/Spec/sessions.db`).

## 🔧 Команды и права

### Команды

| Команда | Описание | Право |
|---------|----------|-------|
| `/spec <игрок>` | Начать наблюдение или сменить цель | `spec.use` |
| `/spec` | Завершить наблюдение | `spec.use` |
| `/spec reload` | Перезагрузить конфигурацию | `spec.reload` |

### Права доступа

| Право | Описание |
|-------|----------|
| `spec.use` | Использование режима наблюдения |
| `spec.reload` | Перезагрузка конфигурации |

> Для режима `VANISH` на сервере должен быть установлен Essentials.

## 📦 Установка

1. Для Paper 1.21.11 соберите проект командой `mvn clean package` (Java 21).
2. Для Spigot/Paper 1.16.5 используйте `mvn clean package -Pmc-1.16.5` (Java 8+).
3. Переместите соответствующий `spec-1.21.11.jar` или `spec-1.16.5.jar` из папки `target` в папку `plugins` сервера.

## 📞 Поддержка

- **Telegram**: [тык](https://t.me/oooSwagParty)
- **Issues**: [тык](https://github.com/firewolfik/Spec/issues)

---

<div align="center">

**⭐ Поставьте звезду, если проект вам помог!**

Made with ❤️ by [firewolfik](https://github.com/firewolfik)

</div>
