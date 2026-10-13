package net.sundggs.avecoins.shop;

import java.nio.file.Path;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.sundggs.avecoins.config.AtomicJsonStore;

/** Stores player listings in the active world's data directory. */
public final class PlayerMarketStore {
    private static AtomicJsonStore<PlayerMarketData> store;

    public static synchronized void initialize(MinecraftServer server) {
        Path path = server.getWorldPath(LevelResource.ROOT)
                .resolve("data").resolve("avecoins").resolve("player_market.json");
        store = new AtomicJsonStore<>(path, PlayerMarketData.class,
                PlayerMarketData::defaults, PlayerMarketData::validate);
        if (!store.load()) {
            throw new IllegalStateException("Could not load player market data");
        }
    }

    public static synchronized PlayerMarketData get() {
        ensureInitialized();
        return store.get();
    }

    public static synchronized boolean save(PlayerMarketData data) {
        ensureInitialized();
        PlayerMarketData candidate = data.copy();
        candidate.revision = Math.max(store.get().revision + 1L, candidate.revision);
        return store.save(candidate);
    }

    public static synchronized void clear() {
        store = null;
    }

    private static void ensureInitialized() {
        if (store == null) {
            throw new IllegalStateException("Player market store is not initialized");
        }
    }

    private PlayerMarketStore() {
    }
}
