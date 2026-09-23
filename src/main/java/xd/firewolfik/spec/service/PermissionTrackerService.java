package xd.firewolfik.spec.service;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import xd.firewolfik.spec.Main;

public final class PermissionTrackerService {

    public static final String SPEC_PERMISSION = "spec.use";

    private final Main plugin;
    private final SpecRequestService requestService;
    private final Set<UUID> playersWithPermission = new HashSet<>();
    private BukkitTask pollTask;

    public PermissionTrackerService(Main plugin, SpecRequestService requestService) {
        this.plugin = plugin;
        this.requestService = requestService;
    }

    public void start() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(SPEC_PERMISSION)) {
                playersWithPermission.add(player.getUniqueId());
            }
        }

        pollTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkPermissions, 20L, 20L);
    }

    public void stop() {
        if (pollTask != null) {
            pollTask.cancel();
            pollTask = null;
        }
        playersWithPermission.clear();
    }

    public void handleJoin(final Player player) {
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    return;
                }
                if (player.hasPermission(SPEC_PERMISSION)) {
                    playersWithPermission.add(player.getUniqueId());
                    requestService.sendAlertStatus(player);
                } else {
                    playersWithPermission.remove(player.getUniqueId());
                }
            }
        });
    }

    public void handleQuit(Player player) {
        playersWithPermission.remove(player.getUniqueId());
    }

    private void checkPermissions() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            boolean hasPermissionNow = player.hasPermission(SPEC_PERMISSION);
            boolean hadPermission = playersWithPermission.contains(id);

            if (hasPermissionNow && !hadPermission) {
                playersWithPermission.add(id);
                requestService.sendAlertStatus(player);
            } else if (!hasPermissionNow && hadPermission) {
                playersWithPermission.remove(id);
            }
        }
    }
}
