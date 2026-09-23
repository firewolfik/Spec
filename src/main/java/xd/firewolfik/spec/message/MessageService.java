package xd.firewolfik.spec.message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        send(recipient, path, Collections.<String, String>emptyMap());
    }

    public void send(CommandSender recipient, String path, String key, String value) {
        send(recipient, path, Collections.singletonMap(key, value));
    }

    public void send(CommandSender recipient, String path, Map<String, String> placeholders) {
        List<String> lines = formatLines(path, placeholders);
        for (String line : lines) {
            recipient.sendMessage(line);
        }
    }

    public String format(String path) {
        return format(path, Collections.<String, String>emptyMap());
    }

    public String format(String path, String key, String value) {
        return format(path, Collections.singletonMap(key, value));
    }

    public String format(String path, Map<String, String> placeholders) {
        List<String> lines = formatLines(path, placeholders);
        if (lines.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            builder.append(lines.get(i));
            if (i < lines.size() - 1) {
                builder.append('\n');
            }
        }
        return builder.toString();
    }

    public List<String> formatLines(String path, Map<String, String> placeholders) {
        List<String> rawLines = config.getMessageLines(path);
        if (rawLines.isEmpty()) {
            return Collections.emptyList();
        }

        String prefix = config.getString("prefix");
        List<String> formatted = new ArrayList<>(rawLines.size());

        for (String raw : rawLines) {
            String message = raw.replace("%prefix%", prefix);
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("%" + entry.getKey() + "%", entry.getValue());
            }
            formatted.add(ColorUtil.colorize(message));
        }

        return formatted;
    }
}
