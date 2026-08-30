package com.example.richchat.config;

import net.minecraft.text.TextColor;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Shared color categories and strict #RRGGBB parsing. */
public final class RichChatColors {
    public static final String[] CATEGORIES = {
            "plain", "bold", "italic", "strikethrough", "list", "inlineCode", "codeBlock",
            "link", "heading1", "heading2", "heading3", "heading4", "heading5", "heading6",
            "quote", "latex", "tableHeader", "tableBody"
    };

    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put("plain", "#FFFFFF");
        DEFAULTS.put("bold", "#FFFFFF");
        DEFAULTS.put("italic", "#FFFFFF");
        DEFAULTS.put("strikethrough", "#FFFFFF");
        DEFAULTS.put("list", "#FFFFFF");
        DEFAULTS.put("inlineCode", "#D0D0D0");
        DEFAULTS.put("codeBlock", "#D0D0D0");
        DEFAULTS.put("link", "#55FFFF");
        DEFAULTS.put("heading1", "#5555FF");
        DEFAULTS.put("heading2", "#55FF55");
        DEFAULTS.put("heading3", "#FFFF55");
        DEFAULTS.put("heading4", "#55FFFF");
        DEFAULTS.put("heading5", "#FF55FF");
        DEFAULTS.put("heading6", "#FF5555");
        DEFAULTS.put("quote", "#AAAAAA");
        DEFAULTS.put("latex", "#FFFFFF");
        DEFAULTS.put("tableHeader", "#FFFFFF");
        DEFAULTS.put("tableBody", "#FFFFFF");
    }

    private RichChatColors() {
    }

    public static Map<String, String> defaultValues() {
        return new LinkedHashMap<>(DEFAULTS);
    }

    public static String defaultValue(String category) {
        return DEFAULTS.get(category);
    }

    public static String normalize(String value) {
        if (value == null || !value.matches("#[0-9a-fA-F]{6}")) {
            return null;
        }
        return value.toUpperCase(Locale.ROOT);
    }

    public static TextColor toTextColor(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        return TextColor.fromRgb(Integer.parseInt(normalized.substring(1), 16));
    }
}
