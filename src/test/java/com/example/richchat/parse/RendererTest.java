package com.example.richchat.parse;

import com.example.richchat.config.RichChatColors;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RendererTest {
    @Test
    void markdownPreservesSingleLineFenceAndUnclosedFence() {
        assertEquals("code", MarkdownRenderer.render("```code```").getString());
        assertEquals("```java\nreturn 1;", MarkdownRenderer.render("```java\nreturn 1;").getString());
    }

    @Test
    void latexHandlesDelimitersAndMalformedInput() {
        assertEquals("x² + 1/2", LatexUnicodeRenderer.render("$x^2 + \\frac{1}{2}$"));
        assertEquals("Σ", LatexUnicodeRenderer.render("$$\\sum$$"));
        assertEquals("$x^2", LatexUnicodeRenderer.render("\\$x^2"));
        assertEquals("$x^2", LatexUnicodeRenderer.render("$x^2"));
        assertEquals("\\frac{a}", LatexUnicodeRenderer.render("$\\frac{a}$"));
    }

    @Test
    void latexSegmentsIdentifyFormulaColorBoundaries() {
        List<LatexUnicodeRenderer.Segment> segments = LatexUnicodeRenderer.renderSegments("a $x^2$ b");
        assertEquals(3, segments.size());
        assertFalse(segments.get(0).formula());
        assertTrue(segments.get(1).formula());
        assertEquals("x²", segments.get(1).text());
    }

    @Test
    void markdownCanParseAcrossStyledTextSegments() {
        Text source = Text.empty()
                .append(Text.literal("**bo").setStyle(Style.EMPTY.withItalic(true)))
                .append(Text.literal("ld**"));
        Text rendered = ChatParser.parse(source);
        assertEquals("bold", rendered.getString());
        boolean[] bold = {false};
        rendered.visit((style, string) -> {
            bold[0] |= style.isBold();
            return java.util.Optional.empty();
        }, Style.EMPTY);
        assertTrue(bold[0]);
    }

    @Test
    void tableWidthCountsCodePoints() {
        Text rendered = TableRenderer.render(List.of("| h |", "|---|", "| 😀 |"));
        assertTrue(rendered.getString().contains("😀"));
    }

    @Test
    void colorsRequireStrictHexValues() {
        assertEquals("#AABBCC", RichChatColors.normalize("#aabbcc"));
        assertNull(RichChatColors.normalize("FFFFFF"));
        assertNull(RichChatColors.normalize("#FFF"));
        assertNull(RichChatColors.normalize("#GGGGGG"));
    }
}
