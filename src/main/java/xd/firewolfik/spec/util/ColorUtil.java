package xd.firewolfik.spec.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class ColorUtil {
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    private ColorUtil() {
    }

    public static Component colorize(String text) {
        return SERIALIZER.deserialize(text == null ? "" : text);
    }
}
