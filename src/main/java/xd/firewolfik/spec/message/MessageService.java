package xd.firewolfik.spec.message;

import java.util.Map;
import org.bukkit.command.CommandSender;
import xd.firewolfik.spec.config.ConfigService;
import xd.firewolfik.spec.util.ColorUtil;

public final class MessageService {
    private final ConfigService config;

    public MessageService(ConfigService config) {
        this.config = config;
    }

    public void send(CommandSender recipient, String path) {
        send(recipient, path, Map.of());
    }

    public void send(CommandSender recipient, String path, Map<String, String> placeholders) {
        String message = config.getString(path);
        if (message.isEmpty()) {
            return;
        }

        message = message.replace("%prefix%", config.getString("prefix"));
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace('%' + entry.getKey() + '%', entry.getValue());
        }
        recipient.sendMessage(ColorUtil.colorize(message));
    }
}
