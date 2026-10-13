/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.permission;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.level.ServerPlayer;
import net.sundggs.avecoins.AvecoinsMod;

final class HybridPermissionAdapter {
    private static final AtomicBoolean FAILURE_LOGGED = new AtomicBoolean();
    private static volatile Accessor accessor;
    private static volatile boolean unavailable;

    static Boolean check(ServerPlayer player, String node) {
        Accessor bridge = HybridPermissionAdapter.resolve();
        if (bridge == null) {
            return null;
        }
        try {
            Object pluginManager = bridge.getPluginManager.invoke(null, new Object[0]);
            if (bridge.getPlugin.invoke(pluginManager, "LuckPerms") == null) {
                return null;
            }
            Object bukkitPlayer = bridge.getPlayer.invoke(null, player.getUUID());
            if (bukkitPlayer == null) {
                return null;
            }
            return (Boolean)bridge.hasPermission.invoke(bukkitPlayer, node);
        }
        catch (LinkageError | ReflectiveOperationException exception) {
            if (FAILURE_LOGGED.compareAndSet(false, true)) {
                AvecoinsMod.LOGGER.warn("LuckPerms/Bukkit permission lookup failed; denying hybrid permission checks", exception);
            }
            return false;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static Accessor resolve() {
        if (accessor != null || unavailable) {
            return accessor;
        }
        Class<HybridPermissionAdapter> clazz = HybridPermissionAdapter.class;
        synchronized (HybridPermissionAdapter.class) {
            block8: {
                if (accessor != null || unavailable) {
                    // ** MonitorExit[var0] (shouldn't be in output)
                    return accessor;
                }
                try {
                    ClassLoader loader = Thread.currentThread().getContextClassLoader();
                    Class<?> bukkit = Class.forName("org.bukkit.Bukkit", false, loader);
                    Class<?> pluginManager = Class.forName("org.bukkit.plugin.PluginManager", false, loader);
                    Class<?> player = Class.forName("org.bukkit.entity.Player", false, loader);
                    accessor = new Accessor(bukkit.getMethod("getPluginManager", new Class[0]), pluginManager.getMethod("getPlugin", String.class), bukkit.getMethod("getPlayer", UUID.class), player.getMethod("hasPermission", String.class));
                }
                catch (ClassNotFoundException exception) {
                    unavailable = true;
                }
                catch (LinkageError | ReflectiveOperationException exception) {
                    unavailable = true;
                    if (!FAILURE_LOGGED.compareAndSet(false, true)) break block8;
                    AvecoinsMod.LOGGER.warn("Could not initialize LuckPerms/Bukkit permission integration", exception);
                }
            }
            // ** MonitorExit[var0] (shouldn't be in output)
            return accessor;
        }
    }

    private HybridPermissionAdapter() {
    }

    private record Accessor(Method getPluginManager, Method getPlugin, Method getPlayer, Method hasPermission) {
    }
}

