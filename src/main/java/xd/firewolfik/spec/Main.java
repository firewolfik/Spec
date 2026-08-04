package xd.firewolfik.spec;

import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;
import xd.firewolfik.spec.command.SpecCommand;
import xd.firewolfik.spec.command.SpecTabCompleter;
import xd.firewolfik.spec.config.ConfigService;
import xd.firewolfik.spec.listener.PlayerConnectionListener;
import xd.firewolfik.spec.listener.SpectatorProtectionListener;
import xd.firewolfik.spec.manager.SpecSessionManager;
import xd.firewolfik.spec.message.MessageService;
import xd.firewolfik.spec.repository.SessionRepository;
import xd.firewolfik.spec.service.ActionBarService;
import xd.firewolfik.spec.service.PlayerStateService;
import xd.firewolfik.spec.service.SpectatorService;
import xd.firewolfik.spec.service.VisibilityService;

public final class Main extends JavaPlugin {
    private SpectatorService spectatorService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        ConfigService config = new ConfigService(this);
        MessageService messages = new MessageService(config);
        SpecSessionManager sessions = new SpecSessionManager(new SessionRepository(this));
        sessions.load();
        VisibilityService visibility = new VisibilityService(this, sessions);
        ActionBarService actionBar = new ActionBarService(this, config, sessions);
        spectatorService = new SpectatorService(
                sessions,
                new PlayerStateService(this),
                visibility,
                actionBar,
                messages
        );

        SpecCommand specCommand = new SpecCommand(this, config, messages, spectatorService, actionBar);
        Objects.requireNonNull(getCommand("spec"), "Command spec is missing from plugin.yml")
                .setExecutor(specCommand);
        Objects.requireNonNull(getCommand("spec")).setTabCompleter(new SpecTabCompleter());
        getServer().getPluginManager().registerEvents(
                new PlayerConnectionListener(this, sessions, spectatorService, visibility),
                this
        );
        getServer().getPluginManager().registerEvents(new SpectatorProtectionListener(sessions), this);

        actionBar.refresh();
    }

    @Override
    public void onDisable() {
        if (spectatorService != null) {
            spectatorService.shutdown();
        }
    }
}
