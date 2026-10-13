/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

final class CategoryPagination {
    static int pageCount(int itemCount, int itemsPerPage) {
        CategoryPagination.requireValid(itemCount, itemsPerPage);
        return Math.max(1, (itemCount + itemsPerPage - 1) / itemsPerPage);
    }

    static int clampPage(int page, int itemCount, int itemsPerPage) {
        return Math.max(0, Math.min(page, CategoryPagination.pageCount(itemCount, itemsPerPage) - 1));
    }

    static int startIndex(int page, int itemCount, int itemsPerPage) {
        return CategoryPagination.clampPage(page, itemCount, itemsPerPage) * itemsPerPage;
    }

    static int endIndex(int page, int itemCount, int itemsPerPage) {
        return Math.min(itemCount, CategoryPagination.startIndex(page, itemCount, itemsPerPage) + itemsPerPage);
    }

    static int pageForIndex(int index, int itemCount, int itemsPerPage) {
        CategoryPagination.requireValid(itemCount, itemsPerPage);
        if (index < 0 || index >= itemCount) {
            return 0;
        }
        return index / itemsPerPage;
    }

    private static void requireValid(int itemCount, int itemsPerPage) {
        if (itemCount < 0) {
            throw new IllegalArgumentException("Item count cannot be negative");
        }
        if (itemsPerPage <= 0) {
            throw new IllegalArgumentException("Items per page must be positive");
        }
    }

    private CategoryPagination() {
    }
}

