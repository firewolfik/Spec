package xd.firewolfik.spec.model;

import java.util.UUID;

public final class SpecRequest {

    private final UUID requesterId;
    private final String requesterName;
    private final String triggerMessage;
    private final long createdAt;

    public SpecRequest(UUID requesterId, String requesterName, String triggerMessage, long createdAt) {
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.triggerMessage = triggerMessage;
        this.createdAt = createdAt;
    }

    public UUID requesterId() {
        return requesterId;
    }

    public String requesterName() {
        return requesterName;
    }

    public String triggerMessage() {
        return triggerMessage;
    }

    public long createdAt() {
        return createdAt;
    }

    public boolean isExpired(long timeoutMillis) {
        return System.currentTimeMillis() - createdAt > timeoutMillis;
    }
}
