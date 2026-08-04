package xd.firewolfik.spec.config;

import net.kyori.adventure.text.Component;
import xd.firewolfik.spec.Main;
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

    public boolean isActionBarEnabled() {
        return plugin.getConfig().getBoolean("action-bar.enabled", true);
    }

    public Component getActionBar() {
        return ColorUtil.colorize(getString("action-bar.message"));
    }
}
