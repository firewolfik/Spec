package xd.firewolfik.spec.manager;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import xd.firewolfik.spec.model.SpecSession;
import xd.firewolfik.spec.repository.SessionRepository;

public final class SpecSessionManager {
    private final SessionRepository repository;
    private final Map<UUID, SpecSession> sessions = new HashMap<>();

    public SpecSessionManager(SessionRepository repository) {
        this.repository = repository;
    }

    public void load() {
        sessions.clear();
        sessions.putAll(repository.load());
    }

    public boolean contains(UUID moderatorId) {
        return sessions.containsKey(moderatorId);
    }

    public @Nullable SpecSession get(UUID moderatorId) {
        return sessions.get(moderatorId);
    }

    public Collection<SpecSession> getAll() {
        return List.copyOf(sessions.values());
    }

    public void put(SpecSession session) {
        sessions.put(session.moderatorId(), session);
        save();
    }

    public @Nullable SpecSession remove(UUID moderatorId) {
        SpecSession removed = sessions.remove(moderatorId);
        if (removed != null) {
            save();
        }
        return removed;
    }

    public void save() {
        repository.save(sessions.values());
    }
}
