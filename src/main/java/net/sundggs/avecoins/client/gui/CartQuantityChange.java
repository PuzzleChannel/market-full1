/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

final class CartQuantityChange {
    private static final int STACK_STEP = 64;

    private CartQuantityChange() {
    }

    static int adjust(int quantity, int direction, boolean shiftDown, int stock) {
        int step;
        int n = step = shiftDown ? 64 : 1;
        if (direction < 0) {
            if (shiftDown && quantity > 0 && quantity < 64) {
                return 1;
            }
            return Math.max(0, quantity - step);
        }
        int limit = stock < 0 ? 1024 : Math.min(stock, 1024);
        return Math.max(quantity, Math.min(quantity + step, limit));
    }
}

