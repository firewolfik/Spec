package xd.firewolfik.spec.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public final class ChatComponentUtil {

    private static final String BUTTON_TOKEN = "%button%";

    private ChatComponentUtil() {
    }

    public static BaseComponent[] buildLineWithButton(
            String template,
            String buttonText,
            String buttonHover,
            String buttonCommand,
            Map<String, String> placeholders
    ) {
        String renderedTemplate = applyPlaceholders(template, placeholders);
        String renderedButtonText = ColorUtil.colorize(applyPlaceholders(buttonText, placeholders));
        String renderedButtonHover = ColorUtil.colorize(applyPlaceholders(buttonHover, placeholders));
        String renderedButtonCommand = applyPlaceholders(buttonCommand, placeholders);

        if (!renderedTemplate.contains(BUTTON_TOKEN)) {
            return TextComponent.fromLegacyText(ColorUtil.colorize(renderedTemplate));
        }

        String[] parts = renderedTemplate.split(Pattern.quote(BUTTON_TOKEN), -1);
        List<BaseComponent> lineComponents = new ArrayList<>();

        BaseComponent[] hoverComponents = TextComponent.fromLegacyText(renderedButtonHover);
        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponents);
        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, renderedButtonCommand);

        BaseComponent[] buttonParts = TextComponent.fromLegacyText(renderedButtonText);
        for (BaseComponent part : buttonParts) {
            part.setClickEvent(clickEvent);
            part.setHoverEvent(hoverEvent);
        }

        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                BaseComponent[] textParts = TextComponent.fromLegacyText(ColorUtil.colorize(parts[i]));
                for (BaseComponent component : textParts) {
                    lineComponents.add(component);
                }
            }

            if (i < parts.length - 1) {
                for (BaseComponent part : buttonParts) {
                    lineComponents.add(part.duplicate());
                }
            }
        }

        return lineComponents.toArray(new BaseComponent[0]);
    }

    private static String applyPlaceholders(String text, Map<String, String> placeholders) {
        if (text == null) {
            return "";
        }
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return result;
    }
}
