package xd.firewolfik.spec.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;

public final class ColorUtil {

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private ColorUtil() {
    }

    public static String colorize(String text) {
        if (text == null) {
            return "";
        }

        Matcher matcher = HEX_COLOR_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder bungeecordHex = new StringBuilder("&x");
            for (char ch : hex.toCharArray()) {
                bungeecordHex.append('&').append(ch);
            }
            matcher.appendReplacement(buffer, bungeecordHex.toString());
        }
        matcher.appendTail(buffer);

        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
}
