/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.shop;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.sundggs.avecoins.AvecoinsMod;
import net.sundggs.avecoins.config.AtomicJsonStore;
import net.sundggs.avecoins.shop.ModMenus;
import net.sundggs.avecoins.shop.ShopService;
import net.sundggs.avecoins.shop.WalletService;

public final class WalletMenu
extends AbstractContainerMenu {
    public static final int WALLET_SLOTS = 27;
    private final SimpleContainer walletContainer;
    private final Player owner;
    private final ShopService.ShopSnapshot snapshot;

    public WalletMenu(int containerId, Inventory inventory, String data) {
        this(containerId, inventory, new SimpleContainer(27), WalletMenu.readSnapshot(data));
    }

    public WalletMenu(int containerId, Inventory inventory, SimpleContainer walletContainer, ShopService.ShopSnapshot snapshot) {
        super(ModMenus.WALLET, containerId);
        int column;
        int row;
        this.walletContainer = walletContainer;
        this.owner = inventory.player;
        this.snapshot = snapshot;
        walletContainer.startOpen((ContainerUser)this.owner);
        for (row = 0; row < 3; ++row) {
            for (column = 0; column < 9; ++column) {
                this.addSlot(new CurrencySlot((Container)walletContainer, column + row * 9, 31 + column * 18, 102 + row * 18));
            }
        }
        for (row = 0; row < 3; ++row) {
            for (column = 0; column < 9; ++column) {
                this.addSlot(new Slot((Container)inventory, column + row * 9 + 9, 31 + column * 18, 202 + row * 18));
            }
        }
        for (int column2 = 0; column2 < 9; ++column2) {
            this.addSlot(new Slot((Container)inventory, column2, 31 + column2 * 18, 260));
        }
    }

    private static ShopService.ShopSnapshot readSnapshot(String data) {
        if (data == null || data.isBlank()) {
            return new ShopService.ShopSnapshot();
        }
        return (ShopService.ShopSnapshot)AtomicJsonStore.gson().fromJson(data, ShopService.ShopSnapshot.class);
    }

    public Container walletContainer() {
        return this.walletContainer;
    }

    public ShopService.ShopSnapshot snapshot() {
        return this.snapshot;
    }

    public boolean stillValid(Player player) {
        return player.isAlive();
    }

    public ItemStack quickMoveStack(Player player, int index) {
        boolean moved;
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = (Slot)this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();
        if (index < 27) {
            moved = this.moveItemStackTo(source, 27, this.slots.size(), true);
        } else {
            if (!WalletService.isCurrency(source)) {
                return ItemStack.EMPTY;
            }
            moved = this.moveItemStackTo(source, 0, 27, false);
        }
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (source.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        ServerPlayer serverPlayer;
        super.clicked(slotId, button, input, player);
        Player player2 = this.owner;
        if (player2 instanceof ServerPlayer && !WalletService.saveContainer(serverPlayer = (ServerPlayer)player2, this.walletContainer)) {
            AvecoinsMod.LOGGER.error("Could not persist the wallet for {}", serverPlayer.getScoreboardName());
        }
    }

    public void removed(Player player) {
        if (player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            WalletService.saveContainer(serverPlayer, this.walletContainer);
        }
        this.walletContainer.stopOpen((ContainerUser)player);
        super.removed(player);
    }

    private static final class CurrencySlot
    extends Slot {
        private CurrencySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        public boolean mayPlace(ItemStack stack) {
            return WalletService.isCurrency(stack);
        }
    }
}

