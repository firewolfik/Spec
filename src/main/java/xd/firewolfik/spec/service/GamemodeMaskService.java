package xd.firewolfik.spec.service;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xd.firewolfik.spec.Main;

/**
 * Masks the spectator player's gamemode in the player tab list
 * by sending synthetic packets so other players see them in SURVIVAL mode.
 * <p>
 * Caches reflection members on first use for zero runtime lookup overhead.
 */
public final class GamemodeMaskService {

    private static final String MODERN_PACKET_CLASS =
            "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket";
    private static final String MODERN_ACTION_CLASS =
            "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action";
    private static final String GAME_TYPE_CLASS =
            "net.minecraft.world.level.GameType";

    private final Main plugin;
    private final boolean modern;
    private final String nmsVersion;
    private boolean broken;

    // Cached shared reflection
    private Method getHandleMethod;
    private Field connectionField;
    private Method sendPacketMethod;

    // Cached modern reflection
    private Constructor<?> modernConstructor;
    private Object modernUpdateGameModeAction;
    private Field modernEntriesField;
    private Class<?> gameTypeClass;
    private Object survivalGameType;
    private Method recordComponentsMethod;

    // Cached legacy reflection
    private Constructor<?> legacyPacketConstructor;
    private Constructor<?> legacyDataConstructor;
    private Object legacyUpdateGameModeAction;
    private Method legacyAddDataMethod;
    private Object legacySurvivalGameType;

    public GamemodeMaskService(Main plugin) {
        this.plugin = plugin;
        this.nmsVersion = resolveNmsVersion();
        this.modern = isModernServer();
    }

    public void maskForAllViewers(Player moderator) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            mask(viewer, moderator);
        }
    }

    public void mask(Player viewer, Player moderator) {
        if (broken || !viewer.isOnline() || !moderator.isOnline()) {
            return;
        }
        if (viewer.getUniqueId().equals(moderator.getUniqueId())) {
            return;
        }

        try {
            Object packet = modern ? buildModernPacket(moderator) : buildLegacyPacket(moderator);
            sendPacket(viewer, packet);
        } catch (Throwable throwable) {
            broken = true;
            plugin.getLogger().warning(
                    "SILENT mode tab masking is not supported on this server: "
                            + throwable.getClass().getSimpleName() + ": " + throwable.getMessage()
            );
        }
    }

    private static boolean isModernServer() {
        try {
            Class.forName(MODERN_PACKET_CLASS);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static String resolveNmsVersion() {
        String[] parts = Bukkit.getServer().getClass().getPackage().getName().split("\\.");
        if (parts.length > 3 && parts[3].startsWith("v")) {
            return parts[3];
        }
        return "";
    }

    // ==========================================
    // Modern Packet Construction (1.19.3+)
    // ==========================================

    private Object buildModernPacket(Player moderator) throws Exception {
        ensureModernInitialized();

        Object handle = getHandle(moderator);
        @SuppressWarnings({"unchecked", "rawtypes"})
        Object actions = EnumSet.of((Enum) modernUpdateGameModeAction);
        Object packet = modernConstructor.newInstance(actions, Collections.singletonList(handle));

        List<Object> entries = new ArrayList<>((List<?>) modernEntriesField.get(packet));
        entries.replaceAll(this::rewriteRecordToSurvival);
        modernEntriesField.set(packet, entries);

        return packet;
    }

    private void ensureModernInitialized() throws Exception {
        if (modernConstructor != null) {
            return;
        }

        Class<?> packetClass = Class.forName(MODERN_PACKET_CLASS);
        Class<?> actionClass = Class.forName(MODERN_ACTION_CLASS);
        modernUpdateGameModeAction = actionClass.getField("UPDATE_GAME_MODE").get(null);
        modernConstructor = packetClass.getDeclaredConstructor(EnumSet.class, Collection.class);

        modernEntriesField = packetClass.getDeclaredField("entries");
        modernEntriesField.setAccessible(true);

        gameTypeClass = Class.forName(GAME_TYPE_CLASS);
        survivalGameType = gameTypeClass.getField("SURVIVAL").get(null);
        recordComponentsMethod = Class.class.getMethod("getRecordComponents");
    }

    private Object rewriteRecordToSurvival(Object record) {
        try {
            Object[] components = (Object[]) recordComponentsMethod.invoke(record.getClass());
            Class<?>[] types = new Class<?>[components.length];
            Object[] values = new Object[components.length];

            for (int i = 0; i < components.length; i++) {
                types[i] = (Class<?>) components[i].getClass().getMethod("getType").invoke(components[i]);
                Method accessor = (Method) components[i].getClass().getMethod("getAccessor").invoke(components[i]);
                values[i] = accessor.invoke(record);

                if (types[i] == gameTypeClass) {
                    values[i] = survivalGameType;
                }
            }

            Constructor<?> canonical = record.getClass().getDeclaredConstructor(types);
            return canonical.newInstance(values);
        } catch (Throwable throwable) {
            throw new IllegalStateException("Failed to rewrite modern player info record", throwable);
        }
    }

    // ==========================================
    // Legacy Packet Construction (1.16.5 and older)
    // ==========================================

    private Object buildLegacyPacket(Player moderator) throws Exception {
        ensureLegacyInitialized();

        Object handle = getHandle(moderator);
        Object profile = handle.getClass().getMethod("getProfile").invoke(handle);
        Object data = legacyDataConstructor.newInstance(profile, 0, legacySurvivalGameType, null);

        Object packet = legacyPacketConstructor.newInstance(legacyUpdateGameModeAction, Collections.emptyList());
        legacyAddDataMethod.invoke(packet, data);

        return packet;
    }

    private void ensureLegacyInitialized() throws Exception {
        if (legacyPacketConstructor != null) {
            return;
        }

        String base = "net.minecraft.server." + nmsVersion + '.';
        Class<?> packetClass = Class.forName(base + "PacketPlayOutPlayerInfo");
        Class<?> dataClass = Class.forName(base + "PacketPlayOutPlayerInfo$PlayerInfoData");
        Class<?> actionClass = Class.forName(base + "PacketPlayOutPlayerInfo$EnumPlayerInfoAction");

        legacyUpdateGameModeAction = actionClass.getField("UPDATE_GAME_MODE").get(null);
        legacyDataConstructor = findLegacyDataConstructor(dataClass);

        Class<?> legacyGameTypeClass = legacyDataConstructor.getParameterTypes()[2];
        legacySurvivalGameType = legacyGameTypeClass.getField("SURVIVAL").get(null);

        legacyPacketConstructor = packetClass.getConstructor(actionClass, Iterable.class);

        for (Method method : packetClass.getMethods()) {
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 1 && params[0] == dataClass) {
                legacyAddDataMethod = method;
                break;
            }
        }

        if (legacyAddDataMethod == null) {
            throw new IllegalStateException("Could not find data injection method in PacketPlayOutPlayerInfo");
        }
    }

    private static Constructor<?> findLegacyDataConstructor(Class<?> dataClass) {
        for (Constructor<?> candidate : dataClass.getDeclaredConstructors()) {
            Class<?>[] parameters = candidate.getParameterTypes();
            if (parameters.length == 4
                    && parameters[0].getName().equals("com.mojang.authlib.GameProfile")
                    && parameters[2].getName().endsWith("EnumGamemode")) {
                candidate.setAccessible(true);
                return candidate;
            }
        }
        throw new IllegalStateException("PlayerInfoData canonical constructor not found");
    }

    // ==========================================
    // Network Dispatch Helpers
    // ==========================================

    private void sendPacket(Player viewer, Object packet) throws Exception {
        Object handle = getHandle(viewer);
        Object connection = resolveConnection(handle);

        if (sendPacketMethod == null) {
            sendPacketMethod = findSendMethod(connection.getClass(), packet.getClass());
            sendPacketMethod.setAccessible(true);
        }

        sendPacketMethod.invoke(connection, packet);
    }

    private Object resolveConnection(Object handle) throws Exception {
        if (connectionField == null) {
            try {
                connectionField = handle.getClass().getField("connection");
            } catch (NoSuchFieldException ignored) {
                connectionField = handle.getClass().getField("playerConnection");
            }
        }
        return connectionField.get(handle);
    }

    private static Method findSendMethod(Class<?> connectionClass, Class<?> packetClass) {
        for (Method method : connectionClass.getMethods()) {
            String name = method.getName();
            Class<?>[] parameters = method.getParameterTypes();
            if ((name.equals("send") || name.equals("sendPacket"))
                    && parameters.length == 1
                    && parameters[0].isAssignableFrom(packetClass)) {
                return method;
            }
        }
        throw new IllegalStateException("No sendPacket method found on " + connectionClass.getName());
    }

    private Object getHandle(Player player) throws Exception {
        if (getHandleMethod == null) {
            getHandleMethod = player.getClass().getMethod("getHandle");
        }
        return getHandleMethod.invoke(player);
    }
}
