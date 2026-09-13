package xd.firewolfik.spec.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import xd.firewolfik.spec.model.SpecSession;
import xd.firewolfik.spec.repository.SessionRepository;

/**
 * Thread-safe registry and lifecycle coordinator for active moderator spectator sessions.
 * Synchronizes modifications with {@link SessionRepository}.
 */
public final class SpecSessionManager {

    private final SessionRepository repository;
    private final Map<UUID, SpecSession> sessions = new ConcurrentHashMap<>();

    public SpecSessionManager(SessionRepository repository) {
        this.repository = repository;
    }

    public synchronized void load() {
        sessions.clear();
        sessions.putAll(repository.load());
    }

    public boolean contains(UUID moderatorId) {
        return sessions.containsKey(moderatorId);
    }

    public SpecSession get(UUID moderatorId) {
        return sessions.get(moderatorId);
    }

    public Collection<SpecSession> getAll() {
        return new ArrayList<>(sessions.values());
    }

    public synchronized void put(SpecSession session) {
        sessions.put(session.moderatorId(), session);
        save();
    }

    public synchronized boolean remove(UUID moderatorId) {
        SpecSession removed = sessions.remove(moderatorId);
        if (removed != null) {
            save();
            return true;
        }
        return false;
    }

    public synchronized void save() {
        repository.save(sessions.values());
    }
}
