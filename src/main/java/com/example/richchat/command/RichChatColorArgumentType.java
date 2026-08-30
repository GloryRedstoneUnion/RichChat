package com.example.richchat.command;

import com.example.richchat.config.RichChatColors;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.LiteralMessage;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Collection;

/** Brigadier arguments that validate color categories and #RRGGBB values before execution. */
public final class RichChatColorArgumentType implements ArgumentType<String> {
    private static final SimpleCommandExceptionType INVALID_CATEGORY =
            new SimpleCommandExceptionType(Text.literal("未知颜色类别"));
    private static final SimpleCommandExceptionType INVALID_HEX =
            new SimpleCommandExceptionType(Text.literal("颜色必须是 #RRGGBB 格式"));
    private final boolean hex;
    private final boolean allowAll;

    private RichChatColorArgumentType(boolean hex, boolean allowAll) {
        this.hex = hex;
        this.allowAll = allowAll;
    }

    public static RichChatColorArgumentType category() {
        return new RichChatColorArgumentType(false, false);
    }

    public static RichChatColorArgumentType resetCategory() {
        return new RichChatColorArgumentType(false, true);
    }

    public static RichChatColorArgumentType hex() {
        return new RichChatColorArgumentType(true, false);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
            reader.skip();
        }
        String value = reader.getString().substring(start, reader.getCursor());
        if (hex) {
            if (RichChatColors.normalize(value) == null) {
                reader.setCursor(start);
                throw INVALID_HEX.createWithContext(reader);
            }
        } else if (RichChatColors.defaultValue(value) == null && !(allowAll && "all".equalsIgnoreCase(value))) {
            reader.setCursor(start);
            throw INVALID_CATEGORY.createWithContext(reader);
        }
        return value;
    }

    @Override
    public Collection<String> getExamples() {
        return hex ? Arrays.asList("#FFFFFF", "#55FFFF") : Arrays.asList("plain", "codeBlock");
    }

    public static <S> java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestCategories(
            com.mojang.brigadier.context.CommandContext<S> context,
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder,
            boolean includeAll) {
        String[] values = includeAll
                ? java.util.stream.Stream.concat(Arrays.stream(RichChatColors.CATEGORIES), java.util.stream.Stream.of("all"))
                .toArray(String[]::new)
                : RichChatColors.CATEGORIES;
        String remaining = builder.getRemaining().toLowerCase(java.util.Locale.ROOT);
        for (String value : values) {
            if (value.toLowerCase(java.util.Locale.ROOT).startsWith(remaining)) {
                Message tooltip = new LiteralMessage("颜色类别: " + value);
                builder.suggest(value, tooltip);
            }
        }
        return builder.buildFuture();
    }

    /** Suggestions for the value argument keep the format discoverable in the chat input. */
    public static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestHexValues(
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        String[] examples = {"#FFFFFF", "#D4D4D4", "#569CD6", "#CE9178"};
        for (String example : examples) {
            if (example.startsWith(remaining.toUpperCase(java.util.Locale.ROOT))) {
                builder.suggest(example, new LiteralMessage("六位十六进制颜色"));
            }
        }
        return builder.buildFuture();
    }
}
