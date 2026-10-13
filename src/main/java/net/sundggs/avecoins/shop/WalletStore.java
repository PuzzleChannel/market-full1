/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.shop;

import java.nio.file.Path;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.sundggs.avecoins.config.AtomicJsonStore;
import net.sundggs.avecoins.shop.WalletData;

public final class WalletStore {
    private static AtomicJsonStore<WalletData> store;

    public static synchronized void initialize(MinecraftServer server) {
        Path path = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("avecoins").resolve("wallets.json");
        store = new AtomicJsonStore<WalletData>(path, WalletData.class, WalletData::defaults, WalletData::validate);
        store.load();
    }

    public static synchronized WalletData get() {
        WalletStore.ensureInitialized();
        return store.get();
    }

    public static synchronized boolean save(WalletData candidate) {
        WalletStore.ensureInitialized();
        return store.save(candidate);
    }

    public static synchronized boolean reload() {
        WalletStore.ensureInitialized();
        return store.reload();
    }

    public static synchronized void clear() {
        store = null;
    }

    private static void ensureInitialized() {
        if (store == null) {
            throw new IllegalStateException("Wallet store is not initialized");
        }
    }

    private WalletStore() {
    }
}

