package net.sharewire.mapsclustering;

import androidx.annotation.Nullable;

public class Preconditions {

    public static <T> T checkNotNull(@Nullable T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }

    public static void checkArgument(boolean expression) {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }

    private Preconditions() {
    }
}