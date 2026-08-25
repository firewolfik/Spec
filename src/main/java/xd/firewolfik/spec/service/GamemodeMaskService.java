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

public final class GamemodeMaskService {
    private static final String MODERN_PACKET_CLASS =
            "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket";
    private static final String GAME_TYPE_CLASS = "net.minecraft.world.level.GameType";

    private final Main plugin;
    private final boolean modern;
    private final String nmsVersion;
    private Object survivalGameType;
    private Class<?> gameTypeClass;
    private boolean broken;

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
            Object packet = modern ? createModernPacket(moderator) : createLegacyPacket(moderator);
            send(viewer, packet);
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
        } catch (ClassNotFoundException exception) {
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

    private Object createModernPacket(Player moderator) throws Exception {
        Class<?> packetClass = Class.forName(MODERN_PACKET_CLASS);
        Class<?> actionClass = Class.forName(MODERN_PACKET_CLASS + "$Action");
        Object updateGameMode = actionClass.getField("UPDATE_GAME_MODE").get(null);
        Object handle = getHandle(moderator);

        Constructor<?> constructor = packetClass.getDeclaredConstructor(EnumSet.class, Collection.class);
        @SuppressWarnings({"unchecked", "rawtypes"})
        Object actions = EnumSet.of((Enum) updateGameMode);
        Object packet = constructor.newInstance(actions, Collections.singletonList(handle));

        Field entriesField = packetClass.getDeclaredField("entries");
        entriesField.setAccessible(true);
        List<Object> entries = new ArrayList<Object>((List<?>) entriesField.get(packet));
        entries.replaceAll(this::withSurvival);
        entriesField.set(packet, entries);
        return packet;
    }

    private Object createLegacyPacket(Player moderator) throws Exception {
        String base = "net.minecraft.server." + nmsVersion + '.';
        Class<?> packetClass = Class.forName(base + "PacketPlayOutPlayerInfo");
        Class<?> dataClass = Class.forName(base + "PacketPlayOutPlayerInfo$PlayerInfoData");
        Class<?> actionClass = Class.forName(base + "PacketPlayOutPlayerInfo$EnumPlayerInfoAction");
        Object updateGameMode = actionClass.getField("UPDATE_GAME_MODE").get(null);

        Object handle = getHandle(moderator);
        Object profile = handle.getClass().getMethod("getProfile").invoke(handle);

        Constructor<?> dataConstructor = findLegacyDataConstructor(dataClass);
        Class<?> legacyGameTypeClass = dataConstructor.getParameterTypes()[2];
        Object survival = legacyGameTypeClass.getField("SURVIVAL").get(null);
        Object data = dataConstructor.newInstance(profile, 0, survival, null);

        Constructor<?> packetConstructor = packetClass.getConstructor(actionClass, Iterable.class);
        Object packet = packetConstructor.newInstance(updateGameMode, Collections.emptyList());

        for (Method method : packetClass.getMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length == 1 && parameters[0] == dataClass) {
                method.invoke(packet, data);
                return packet;
            }
        }
        throw new IllegalStateException("Cannot attach PlayerInfoData to PacketPlayOutPlayerInfo");
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

    private Object withSurvival(Object entry) {
        try {
            return replaceRecordComponent(entry, resolveGameTypeClass(), resolveSurvival());
        } catch (Throwable throwable) {
            throw new IllegalStateException("Cannot rewrite tab entry gamemode", throwable);
        }
    }

    private Class<?> resolveGameTypeClass() throws ClassNotFoundException {
        if (gameTypeClass == null) {
            gameTypeClass = Class.forName(GAME_TYPE_CLASS);
        }
        return gameTypeClass;
    }

    private Object resolveSurvival() throws Exception {
        if (survivalGameType == null) {
            survivalGameType = resolveGameTypeClass().getField("SURVIVAL").get(null);
        }
        return survivalGameType;
    }

    private static Object replaceRecordComponent(Object record, Class<?> targetType, Object replacement)
            throws Exception {
        Method componentsAccessor = Class.class.getMethod("getRecordComponents");
        Object[] components = (Object[]) componentsAccessor.invoke(record.getClass());
        Class<?>[] types = new Class<?>[components.length];
        Object[] values = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            types[i] = (Class<?>) components[i].getClass().getMethod("getType").invoke(components[i]);
            Method accessor = (Method) components[i].getClass().getMethod("getAccessor").invoke(components[i]);
            values[i] = accessor.invoke(record);
            if (types[i] == targetType) {
                values[i] = replacement;
            }
        }
        Constructor<?> canonical = record.getClass().getDeclaredConstructor(types);
        return canonical.newInstance(values);
    }

    private static void send(Player viewer, Object packet) throws Exception {
        Object handle = getHandle(viewer);
        Object connection;
        try {
            connection = handle.getClass().getField("connection").get(handle);
        } catch (NoSuchFieldException ignored) {
            connection = handle.getClass().getField("playerConnection").get(handle);
        }
        String packetClassName = packet.getClass().getName();
        for (Method method : connection.getClass().getMethods()) {
            String name = method.getName();
            Class<?>[] parameters = method.getParameterTypes();
            if ((name.equals("send") || name.equals("sendPacket"))
                    && parameters.length == 1
                    && parameters[0].isAssignableFrom(packet.getClass())) {
                method.setAccessible(true);
                method.invoke(connection, packet);
                return;
            }
        }
        throw new IllegalStateException("No send method found on connection for " + packetClassName);
    }

    private static Object getHandle(Player player) throws Exception {
        return player.getClass().getMethod("getHandle").invoke(player);
    }
}
