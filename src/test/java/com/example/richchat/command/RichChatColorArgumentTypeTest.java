package com.example.richchat.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RichChatColorArgumentTypeTest {
    @Test
    void categoryArgumentRejectsUnknownValuesAtParseTime() throws Exception {
        assertEquals("plain", RichChatColorArgumentType.category().parse(new StringReader("plain")));
        assertThrows(CommandSyntaxException.class,
                () -> RichChatColorArgumentType.category().parse(new StringReader("a")));
    }

    @Test
    void resetCategoryAcceptsAllAndHexArgumentRejectsMalformedColors() throws Exception {
        assertEquals("all", RichChatColorArgumentType.resetCategory().parse(new StringReader("all")));
        assertEquals("#AABBCC", RichChatColorArgumentType.hex().parse(new StringReader("#AABBCC")));
        assertThrows(CommandSyntaxException.class,
                () -> RichChatColorArgumentType.hex().parse(new StringReader("red")));
    }

    @Test
    void categorySuggestionsContainConfiguredNames() throws Exception {
        SuggestionsBuilder builder = new SuggestionsBuilder("", 0);
        var suggestions = RichChatColorArgumentType.suggestCategories(null, builder, true).get();
        assertTrue(suggestions.getList().stream().anyMatch(s -> "codeBlock".equals(s.getText())));
        assertTrue(suggestions.getList().stream().anyMatch(s -> "all".equals(s.getText())));
    }
}
