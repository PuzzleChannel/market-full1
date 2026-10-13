/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.permission;

import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.sundggs.avecoins.permission.HybridPermissionAdapter;

public final class PermissionService {
    public static boolean has(ServerPlayer player, String node) {
        Boolean hybridResult = HybridPermissionAdapter.check(player, node);
        if (hybridResult != null) {
            return hybridResult != false || !node.equals("avecoins.admin") && HybridPermissionAdapter.check(player, "avecoins.admin") == Boolean.TRUE;
        }
        if (node.equals("avecoins.shop.use")) {
            return true;
        }
        return Commands.LEVEL_OWNERS.check(player.permissions());
    }

    public static boolean isAdministrator(ServerPlayer player) {
        Boolean admin = HybridPermissionAdapter.check(player, "avecoins.admin");
        Boolean manage = HybridPermissionAdapter.check(player, "avecoins.shop.manage");
        if (admin != null || manage != null) {
            return admin == Boolean.TRUE || manage == Boolean.TRUE;
        }
        return Commands.LEVEL_OWNERS.check(player.permissions());
    }

    private PermissionService() {
    }
}

