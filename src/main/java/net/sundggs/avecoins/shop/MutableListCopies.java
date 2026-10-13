/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

final class MutableListCopies {
    static <T> List<T> copy(List<T> source, UnaryOperator<T> copier) {
        ArrayList result = new ArrayList(source.size());
        for (T value : source) {
            result.add(copier.apply(value));
        }
        return result;
    }

    static <T> void restore(List<T> target, List<T> backup, UnaryOperator<T> copier) {
        if (target.size() != backup.size()) {
            throw new IllegalArgumentException("Rollback target and backup sizes differ");
        }
        for (int index = 0; index < backup.size(); ++index) {
            target.set(index, copier.apply(backup.get(index)));
        }
    }

    private MutableListCopies() {
    }
}

