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
        // VS Code Dark+-inspired defaults, with distinct heading levels.
        DEFAULTS.put("plain", "#D4D4D4");
        DEFAULTS.put("bold", "#FFFFFF");
        DEFAULTS.put("italic", "#D4D4D4");
        DEFAULTS.put("strikethrough", "#808080");
        DEFAULTS.put("list", "#D4D4D4");
        DEFAULTS.put("inlineCode", "#CE9178");
        DEFAULTS.put("codeBlock", "#CE9178");
        DEFAULTS.put("link", "#3794FF");
        DEFAULTS.put("heading1", "#569CD6");
        DEFAULTS.put("heading2", "#4EC9B0");
        DEFAULTS.put("heading3", "#DCDCAA");
        DEFAULTS.put("heading4", "#C586C0");
        DEFAULTS.put("heading5", "#9CDCFE");
        DEFAULTS.put("heading6", "#CE9178");
        DEFAULTS.put("quote", "#6A9955");
        DEFAULTS.put("latex", "#B5CEA8");
        DEFAULTS.put("tableHeader", "#4EC9B0");
        DEFAULTS.put("tableBody", "#D4D4D4");
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
