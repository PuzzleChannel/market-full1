/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import java.util.Set;

public final class ResultFeedback {
    private static final Set<String> SUCCESS_CODES = Set.of("settings_saved", "configuration_reloaded", "crafting_saved", "crafting_restart_required", "recipe_saved", "recipe_restored", "recipe_grid_cleared", "recipe_slot_cleared", "cart_updated", "purchase_completed", "completed", "category_saved", "category_deleted", "product_saved", "product_deleted", "order_updated", "wallet_deposited", "wallet_withdrawn", "sale_completed", "sale_completed_wallet", "sale_completed_inventory", "market_listing_created", "market_listing_cancelled", "market_listing_updated", "market_purchase_queued", "market_delivery_accelerated", "market_delivery_claimed", "market_updated");
    private static final Set<String> INFORMATION_CODES = Set.of("market_confirm_purchase", "market_confirm_acceleration", "market_confirm_listing");

    public static boolean isSuccess(String resultCode) {
        return resultCode != null && SUCCESS_CODES.contains(resultCode);
    }

    public static boolean isInformation(String resultCode) {
        return resultCode != null && INFORMATION_CODES.contains(resultCode);
    }

    private ResultFeedback() {
    }
}

