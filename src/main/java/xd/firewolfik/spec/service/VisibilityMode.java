package xd.firewolfik.spec.service;

import java.util.Locale;

public enum VisibilityMode {
    VANISH,
    SILENT;

    public static VisibilityMode fromConfig(String raw) {
        if (raw != null && !raw.trim().isEmpty()) {
            String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
            for (VisibilityMode mode : values()) {
                if (mode.name().equals(normalized)) {
                    return mode;
                }
            }
        }
        return VANISH;
    }
}
