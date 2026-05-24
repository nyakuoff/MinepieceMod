package com.piecemod.util;

import net.minecraft.util.Formatting;

/** Small text helpers shared across the mod. */
public final class TextUtil {
    private TextUtil() {}

    /** Null-safe wrapper for {@link Formatting#strip(String)}. Returns "" for null inputs. */
    public static String strip(String s) {
        if (s == null) return "";
        String stripped = Formatting.strip(s);
        return stripped == null ? "" : stripped;
    }
}
