# 👁️ Spec

<div align="center">

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen)
![Paper](https://img.shields.io/badge/Paper-Compatible-blue)
![Java](https://img.shields.io/badge/Java-21+-red)

**Скрытый режим наблюдения за игроками для модераторов**

</div>

---

## ⚙️ Конфигурация

Сообщения, HEX-цвета и Action Bar настраиваются в [`config.yml`](https://github.com/firewolfik/Spec/blob/main/src/main/resources/config.yml).

## ⭐ Возможности

- Скрытое наблюдение за выбранным игроком.
- Переключение между целями без завершения режима.
- Возврат на первоначальную позицию после `/spec`.
- Скрытие модератора от игроков без специального права.
- Защита от урона, эффектов, огня, голода и отбрасывания.
- Восстановление состояния после выхода или перезапуска сервера.
- Настраиваемый Action Bar.

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
| `spec.see` | Возможность видеть скрытых модераторов |

## 📦 Установка

1. Соберите проект командой `mvn clean package`.
2. Переместите JAR из папки `target` в папку `plugins` сервера.
3. Запустите Paper 1.21.11 на Java 21.

## 📞 Поддержка

- **Telegram**: [тык](https://t.me/oooSwagParty)
- **Issues**: [тык](https://github.com/firewolfik/Spec/issues)

---

<div align="center">

**⭐ Поставьте звезду, если проект вам помог!**

Made with ❤️ by [firewolfik](https://github.com/firewolfik)

</div>
