/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RecipeDefinition {
    public int count = 1;
    public List<String> grid = new ArrayList<String>(Collections.nCopies(9, ""));

    public RecipeDefinition() {
    }

    public RecipeDefinition(int count, List<String> grid) {
        this.count = count;
        this.grid = new ArrayList<String>(grid);
    }
}

