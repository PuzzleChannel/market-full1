/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.sundggs.avecoins.AvecoinsMod;
import net.sundggs.avecoins.config.ModConfigManager;
import net.sundggs.avecoins.message.ModMessages;
import net.sundggs.avecoins.network.ModNetworking;
import net.sundggs.avecoins.permission.PermissionService;

public final class ModCommands {
    private static String activeCustomAlias = "";

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(ModCommands.root("avc"));
            dispatcher.register(ModCommands.root("avecoins"));
            String customAlias = ModConfigManager.general().customShopCommand;
            if (!customAlias.isEmpty()) {
                if (dispatcher.getRoot().getChild(customAlias) == null) {
                    dispatcher.register((LiteralArgumentBuilder)Commands.literal((String)customAlias).requires(source -> Commands.LEVEL_OWNERS.check(source.permissions())).executes(context -> ModCommands.openShop((CommandSourceStack)context.getSource())));
                    activeCustomAlias = customAlias;
                } else {
                    activeCustomAlias = "";
                    AvecoinsMod.LOGGER.warn("The configured shop alias /{} conflicts with an existing command and was not registered", customAlias);
                }
            } else {
                activeCustomAlias = "";
            }
        });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> root(String name) {
        return (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal((String)name).requires(source -> Commands.LEVEL_OWNERS.check(source.permissions())).then(Commands.literal((String)"gui").executes(context -> ModCommands.openDashboard((CommandSourceStack)context.getSource())))).then(Commands.literal((String)"shop").executes(context -> ModCommands.openShop((CommandSourceStack)context.getSource())))).then(Commands.literal((String)"wallet").executes(context -> ModCommands.openWallet((CommandSourceStack)context.getSource())));
    }

    private static int openDashboard(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        }
        catch (CommandSyntaxException exception) {
            source.sendFailure(ModMessages.translated("message.avecoins.player_required", new Object[0]));
            return 0;
        }
        if (!PermissionService.has(player, "avecoins.gui")) {
            source.sendFailure(ModMessages.translated("message.avecoins.permission_denied", new Object[0]));
            return 0;
        }
        ModNetworking.openDashboard(player, "");
        return 1;
    }

    private static int openShop(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        }
        catch (CommandSyntaxException exception) {
            source.sendFailure(ModMessages.translated("message.avecoins.player_required", new Object[0]));
            return 0;
        }
        if (!ModConfigManager.general().shopEnabled) {
            source.sendFailure(ModMessages.translated("message.avecoins.shop_disabled", new Object[0]));
            return 0;
        }
        if (!PermissionService.has(player, "avecoins.shop.use")) {
            source.sendFailure(ModMessages.translated("message.avecoins.permission_denied", new Object[0]));
            return 0;
        }
        ModNetworking.openPlayerMarket(player);
        return 1;
    }

    private static int openWallet(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        }
        catch (CommandSyntaxException exception) {
            source.sendFailure(ModMessages.translated("message.avecoins.player_required", new Object[0]));
            return 0;
        }
        if (!ModConfigManager.general().shopEnabled) {
            source.sendFailure(ModMessages.translated("message.avecoins.shop_disabled", new Object[0]));
            return 0;
        }
        if (!PermissionService.has(player, "avecoins.shop.use")) {
            source.sendFailure(ModMessages.translated("message.avecoins.permission_denied", new Object[0]));
            return 0;
        }
        ModNetworking.openWallet(player);
        return 1;
    }

    public static String activeCustomAlias() {
        return activeCustomAlias;
    }

    private ModCommands() {
    }
}

