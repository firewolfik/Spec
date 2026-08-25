package xd.firewolfik.spec.config;

import xd.firewolfik.spec.Main;
import xd.firewolfik.spec.service.VisibilityMode;
import xd.firewolfik.spec.util.ColorUtil;

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

    public VisibilityMode getVisibilityMode() {
        String raw = plugin.getConfig().getString("visibility.mode", "");
        return VisibilityMode.fromConfig(raw);
    }

    public double getDouble(String path, double defaultValue) {
        return plugin.getConfig().getDouble(path, defaultValue);
    }

    public boolean isActionBarEnabled() {
        return getBoolean("action-bar.enabled", true);
    }

    public String getActionBar() {
        return ColorUtil.colorize(getString("action-bar.message"));
    }
}
