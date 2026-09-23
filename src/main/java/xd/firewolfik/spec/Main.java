package xd.firewolfik.spec;

import java.util.Objects;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import xd.firewolfik.spec.command.SpecCommand;
import xd.firewolfik.spec.command.SpecTabCompleter;
import xd.firewolfik.spec.config.ConfigService;
import xd.firewolfik.spec.listener.PlayerChatListener;
import xd.firewolfik.spec.listener.PlayerConnectionListener;
import xd.firewolfik.spec.listener.SpectatorProtectionListener;
import xd.firewolfik.spec.manager.SpecSessionManager;
import xd.firewolfik.spec.message.MessageService;
import xd.firewolfik.spec.repository.SessionRepository;
import xd.firewolfik.spec.service.ActionBarService;
import xd.firewolfik.spec.service.GamemodeMaskService;
import xd.firewolfik.spec.service.PermissionTrackerService;
import xd.firewolfik.spec.service.PlayerStateService;
import xd.firewolfik.spec.service.SoundService;
import xd.firewolfik.spec.service.SpecRequestService;
import xd.firewolfik.spec.service.SpectatorService;
import xd.firewolfik.spec.service.VanishService;
import xd.firewolfik.spec.service.VisibilityService;

public final class Main extends JavaPlugin {

    private ConfigService configService;
    private MessageService messageService;
    private SoundService soundService;
    private ActionBarService actionBarService;
    private VisibilityService visibilityService;
    private SpectatorService spectatorService;
    private SpecRequestService requestService;
    private SpecSessionManager sessionManager;
    private PermissionTrackerService permissionTracker;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        initializeServices();
        registerCommands();
        registerListeners();

        actionBarService.refresh();
        permissionTracker.start();
    }

    @Override
    public void onDisable() {
        if (permissionTracker != null) {
            permissionTracker.stop();
        }
        if (spectatorService != null) {
            spectatorService.shutdown();
        }
    }

    public void reloadPlugin() {
        configService.reload();
        soundService.reload();
        requestService.reload();
        actionBarService.refresh();
        visibilityService.refreshActiveSessions();
    }

    private void initializeServices() {
        SessionRepository repository = new SessionRepository(this);
        sessionManager = new SpecSessionManager(repository);
        sessionManager.load();

        configService = new ConfigService(this);
        messageService = new MessageService(configService);
        soundService = new SoundService(this, configService);

        visibilityService = new VisibilityService(
                this,
                sessionManager,
                configService,
                new GamemodeMaskService(this),
                new VanishService(this)
        );

        actionBarService = new ActionBarService(this, configService, sessionManager);
        requestService = new SpecRequestService(configService, messageService, soundService, repository);
        permissionTracker = new PermissionTrackerService(this, requestService);

        spectatorService = new SpectatorService(
                sessionManager,
                new PlayerStateService(this),
                visibilityService,
                actionBarService,
                messageService,
                soundService
        );
    }

    private void registerCommands() {
        SpecCommand executor = new SpecCommand(
                this,
                messageService,
                spectatorService,
                requestService,
                soundService
        );

        PluginCommand command = Objects.requireNonNull(getCommand("spec"), "Command /spec missing in plugin.yml");
        command.setExecutor(executor);
        command.setTabCompleter(new SpecTabCompleter(requestService, spectatorService));
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerConnectionListener(this, sessionManager, spectatorService, visibilityService, permissionTracker), this);
        pm.registerEvents(new SpectatorProtectionListener(sessionManager), this);
        pm.registerEvents(new PlayerChatListener(this, requestService), this);
    }
}
