package xd.firewolfik.spec.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import xd.firewolfik.spec.Main;
import xd.firewolfik.spec.service.VisibilityMode;
import xd.firewolfik.spec.util.ColorUtil;

/**
 * Accessor service for configuration values stored in config.yml.
 */
public final class ConfigService {

    private final Main plugin;

    public ConfigService(Main plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
    }

    public String getString(String path) {
        return plugin.getConfig().getString(path, "");
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        return plugin.getConfig().getBoolean(path, defaultValue);
    }

    public int getInt(String path, int defaultValue) {
        return plugin.getConfig().getInt(path, defaultValue);
    }

    public double getDouble(String path, double defaultValue) {
        return plugin.getConfig().getDouble(path, defaultValue);
    }

    public List<String> getStringList(String path) {
        return plugin.getConfig().getStringList(path);
    }

    public List<String> getMessageLines(String path) {
        if (plugin.getConfig().isList(path)) {
            return plugin.getConfig().getStringList(path);
        }
        String single = plugin.getConfig().getString(path, "");
        if (single.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(single);
    }

    public String getButtonHover() {
        if (plugin.getConfig().isList("requests.button.hover")) {
            List<String> lines = plugin.getConfig().getStringList("requests.button.hover");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                sb.append(lines.get(i));
                if (i < lines.size() - 1) {
                    sb.append('\n');
                }
            }
            return sb.toString();
        }
        return plugin.getConfig().getString("requests.button.hover", "");
    }

    public List<String> getKeywords() {
        List<String> raw = getStringList("requests.keywords");
        List<String> normalized = new ArrayList<>();
        for (String word : raw) {
            String trimmed = word.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }
        return normalized;
    }

    public VisibilityMode getVisibilityMode() {
        String rawMode = plugin.getConfig().getString("visibility.mode", "");
        return VisibilityMode.fromConfig(rawMode);
    }

    public boolean isActionBarEnabled() {
        return getBoolean("action-bar.enabled", true);
    }

    public String getActionBar() {
        return ColorUtil.colorize(getString("action-bar.message"));
    }
}
