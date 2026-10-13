/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import java.util.Set;

public final class ResultFeedback {
    private static final Set<String> SUCCESS_CODES = Set.of("settings_saved", "configuration_reloaded", "crafting_saved", "crafting_restart_required", "recipe_saved", "recipe_restored", "recipe_grid_cleared", "recipe_slot_cleared", "cart_updated", "purchase_completed", "completed", "category_saved", "category_deleted", "product_saved", "product_deleted", "order_updated", "wallet_deposited", "wallet_withdrawn", "sale_completed", "sale_completed_wallet", "sale_completed_inventory", "market_listing_updated", "market_updated");

    public static boolean isSuccess(String resultCode) {
        return resultCode != null && SUCCESS_CODES.contains(resultCode);
    }

    private ResultFeedback() {
    }
}

