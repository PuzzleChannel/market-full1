/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class CraftingConfig {
    public static final int SCHEMA_VERSION = 1;
    public static final List<String> COINS = List.of("avecoins:coppercoin", "avecoins:ironcoin", "avecoins:goldcoin", "avecoins:diamondcoin", "avecoins:netheritecoin");
    public static final List<String> TICKETS = List.of("avecoins:goldticket", "avecoins:diamondticket", "avecoins:netheriteticket");
    public static final Set<String> MANAGED_RESULTS = Set.copyOf(Stream.concat(COINS.stream(), TICKETS.stream()).toList());
    public int schemaVersion = 1;
    public boolean coinCraftingEnabled = true;
    public Map<String, Boolean> coins = new LinkedHashMap<String, Boolean>(CraftingConfig.enabledMap(COINS));
    public boolean ticketCraftingEnabled = true;
    public Map<String, Boolean> tickets = new LinkedHashMap<String, Boolean>(CraftingConfig.enabledMap(TICKETS));

    public static CraftingConfig defaults() {
        return new CraftingConfig();
    }

    public static CraftingConfig validate(CraftingConfig input) {
        CraftingConfig output = new CraftingConfig();
        output.schemaVersion = 1;
        output.coinCraftingEnabled = input.coinCraftingEnabled;
        output.ticketCraftingEnabled = input.ticketCraftingEnabled;
        output.coins = CraftingConfig.validatedToggleMap(input.coins, COINS);
        output.tickets = CraftingConfig.validatedToggleMap(input.tickets, TICKETS);
        return output;
    }

    public boolean isEnabled(String itemId) {
        if (COINS.contains(itemId)) {
            return this.coinCraftingEnabled && this.coins.getOrDefault(itemId, true) != false;
        }
        if (TICKETS.contains(itemId)) {
            return this.ticketCraftingEnabled && this.tickets.getOrDefault(itemId, true) != false;
        }
        return true;
    }

    private static Map<String, Boolean> enabledMap(List<String> ids) {
        LinkedHashMap<String, Boolean> result = new LinkedHashMap<String, Boolean>();
        ids.forEach(id -> result.put((String)id, true));
        return result;
    }

    private static Map<String, Boolean> validatedToggleMap(Map<String, Boolean> input, List<String> ids) {
        Map<String, Boolean> result = CraftingConfig.enabledMap(ids);
        if (input != null) {
            ids.forEach(id -> result.put((String)id, input.get(id) == null || Boolean.TRUE.equals(input.get(id))));
        }
        return result;
    }
}

