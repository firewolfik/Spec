package xd.firewolfik.spec.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xd.firewolfik.spec.config.ConfigService;
import xd.firewolfik.spec.message.MessageService;
import xd.firewolfik.spec.model.SpecRequest;
import xd.firewolfik.spec.repository.SessionRepository;
import xd.firewolfik.spec.util.ChatComponentUtil;

/**
 * Coordinates player spectator requests initiated via chat keywords,
 * dispatches interactive alert buttons to staff, and persists alert preferences in SQLite.
 */
public final class SpecRequestService {

    private final ConfigService config;
    private final MessageService messages;
    private final SoundService sounds;
    private final SessionRepository sessionRepository;

    private final Map<UUID, SpecRequest> activeRequests = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> disabledAlerts = Collections.synchronizedSet(new HashSet<>());

    public SpecRequestService(
            ConfigService config,
            MessageService messages,
            SoundService sounds,
            SessionRepository sessionRepository
    ) {
        this.config = config;
        this.messages = messages;
        this.sounds = sounds;
        this.sessionRepository = sessionRepository;

        loadSavedAlertPreferences();
    }

    private void loadSavedAlertPreferences() {
        disabledAlerts.clear();
        disabledAlerts.addAll(sessionRepository.loadDisabledAlerts());
    }

    public void reload() {
        loadSavedAlertPreferences();
    }

    public boolean isAlertsEnabled(UUID moderatorId) {
        return !disabledAlerts.contains(moderatorId);
    }

    /**
     * Toggles alerts for the moderator and persists the setting in the database.
     *
     * @return true if alerts are now enabled, false if disabled.
     */
    public boolean toggleAlerts(Player moderator) {
        UUID id = moderator.getUniqueId();
        boolean currentlyDisabled = disabledAlerts.contains(id);
        boolean nowEnabled = currentlyDisabled;

        if (nowEnabled) {
            disabledAlerts.remove(id);
            sessionRepository.setAlertsDisabled(id, false);
        } else {
            disabledAlerts.add(id);
            sessionRepository.setAlertsDisabled(id, true);
        }

        return nowEnabled;
    }

    public boolean handleChatMessage(Player player, String message) {
        if (!config.getBoolean("requests.enabled", true)) {
            return false;
        }

        if (!matchesKeyword(message)) {
            return false;
        }

        long cooldownMs = config.getInt("requests.cooldown", 30) * 1000L;
        Long lastTime = cooldowns.get(player.getUniqueId());
        long now = System.currentTimeMillis();

        if (lastTime != null && (now - lastTime) < cooldownMs) {
            return false;
        }

        cooldowns.put(player.getUniqueId(), now);
        SpecRequest request = new SpecRequest(player.getUniqueId(), player.getName(), message, now);
        activeRequests.put(player.getUniqueId(), request);

        messages.send(player, "messages.request-created", "player", player.getName());
        broadcastAlert(request);
        return true;
    }

    public boolean hasActiveRequest(UUID targetId) {
        SpecRequest request = activeRequests.get(targetId);
        if (request == null) {
            return false;
        }

        long timeoutMs = config.getInt("requests.expire", 180) * 1000L;
        if (request.isExpired(timeoutMs)) {
            activeRequests.remove(targetId);
            return false;
        }

        return true;
    }

    public void consumeRequest(UUID targetId) {
        activeRequests.remove(targetId);
    }

    public Set<UUID> getActiveRequestPlayerIds() {
        cleanExpiredRequests();
        return new HashSet<>(activeRequests.keySet());
    }

    private void cleanExpiredRequests() {
        long timeoutMs = config.getInt("requests.expire", 180) * 1000L;
        for (Map.Entry<UUID, SpecRequest> entry : activeRequests.entrySet()) {
            if (entry.getValue().isExpired(timeoutMs)) {
                activeRequests.remove(entry.getKey());
            }
        }
    }

    private boolean matchesKeyword(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }

        String lowerMessage = message.toLowerCase(Locale.ROOT);
        for (String keyword : config.getKeywords()) {
            if (lowerMessage.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private void broadcastAlert(SpecRequest request) {
        List<String> templateLines = config.getMessageLines("requests.message");
        if (templateLines.isEmpty()) {
            return;
        }

        String buttonText = config.getString("requests.button.text");
        String buttonHover = config.getButtonHover();
        String buttonCommand = config.getString("requests.button.command");

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", request.requesterName());
        placeholders.put("message", request.triggerMessage());
        placeholders.put("prefix", config.getString("prefix"));

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (!staff.hasPermission("spec.use") || !isAlertsEnabled(staff.getUniqueId())) {
                continue;
            }

            for (String line : templateLines) {
                BaseComponent[] components = ChatComponentUtil.buildLineWithButton(
                        line,
                        buttonText,
                        buttonHover,
                        buttonCommand,
                        placeholders
                );
                staff.spigot().sendMessage(components);
            }

            sounds.play(staff, "request");
        }
    }
}
