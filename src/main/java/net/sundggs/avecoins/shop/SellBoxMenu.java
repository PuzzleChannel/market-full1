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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.sundggs.avecoins.config.AtomicJsonStore;
import net.sundggs.avecoins.shop.ModMenus;
import net.sundggs.avecoins.shop.ShopService;

public final class SellBoxMenu
extends AbstractContainerMenu {
    public static final int SELL_SLOTS = 27;
    private final SimpleContainer sellContainer;
    private ShopService.ShopSnapshot snapshot;

    public SellBoxMenu(int containerId, Inventory inventory, String data) {
        this(containerId, inventory, new SimpleContainer(27), SellBoxMenu.readSnapshot(data));
    }

    public SellBoxMenu(int containerId, Inventory inventory, SimpleContainer sellContainer, ShopService.ShopSnapshot snapshot) {
        super(ModMenus.SELL_BOX, containerId);
        int column;
        int row;
        this.sellContainer = sellContainer;
        this.snapshot = snapshot;
        sellContainer.startOpen((ContainerUser)inventory.player);
        for (row = 0; row < 3; ++row) {
            for (column = 0; column < 9; ++column) {
                this.addSlot(new Slot((Container)sellContainer, column + row * 9, 31 + column * 18, 102 + row * 18));
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

    public Container sellContainer() {
        return this.sellContainer;
    }

    public ShopService.ShopSnapshot snapshot() {
        return this.snapshot;
    }

    public void updateSnapshot(ShopService.ShopSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public boolean stillValid(Player player) {
        return player.isAlive();
    }

    public ItemStack quickMoveStack(Player player, int index) {
        boolean moved;
        Slot slot = (Slot)this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();
        boolean bl = moved = index < 27 ? this.moveItemStackTo(source, 27, this.slots.size(), true) : this.moveItemStackTo(source, 0, 27, false);
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

    public void removed(Player player) {
        super.removed(player);
        this.sellContainer.stopOpen((ContainerUser)player);
        if (player instanceof ServerPlayer) {
            this.clearContainer(player, (Container)this.sellContainer);
        }
    }
}

