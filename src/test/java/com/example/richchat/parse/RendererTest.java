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
        assertEquals("D = max(0, h - 3)",
                LatexUnicodeRenderer.render("$D = \\max(0, h - 3)$"));
        assertEquals("Σ", LatexUnicodeRenderer.render("$$\\sum$$"));
        assertEquals("¬ x", LatexUnicodeRenderer.render("\\neg x"));
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
    void chatBodyRendersWhenChatComponentIsNestedAfterPrefix() {
        Text source = Text.empty()
                .append(Text.literal("[Survival] "))
                .append(Text.translatable("chat.type.text", Text.literal("Chat"),
                        Text.literal("# Modern heading")));
        Text rendered = ChatParser.parse(source);
        assertEquals("[Survival] <Chat> Modern heading", rendered.getString());
        assertTrue(rendered.getString().startsWith("[Survival] <Chat> "));
    }

    @Test
    void chatTextWithExtraPrefixArgumentRendersTheLastArgumentAsBody() {
        Text source = Text.translatable("chat.type.text", Text.literal("[Survival] "),
                Text.literal("Chat"), Text.literal("## Server heading"));
        Text rendered = ChatParser.parse(source);
        assertEquals("[Survival] <Chat> Server heading", rendered.getString());
        assertEquals("## Server heading", ChatParser.extractMessageBody(source));
    }

    @Test
    void recoveredChatSourceRendersOnlyBodyAfterPlayerPrefix() {
        Text rendered = ChatParser.renderSourcePreservingChatPrefix(
                "[Survival] <Chat> **bold** $x^2$");
        assertEquals("[Survival] <Chat> bold x²", rendered.getString());
    }

    @Test
    void flattenedPrefixedChatLiteralStillRendersOnlyItsBody() {
        Text rendered = ChatParser.parse(Text.literal("[Survival] <Chat> # Modern heading"));
        assertEquals("[Survival] <Chat> Modern heading", rendered.getString());
    }

    @Test
    void markdownKeepsNestedStylesAndEscapedMarkers() {
        Text rendered = MarkdownRenderer.render("**bold *and italic*** \\*literal\\* [link](https://example.com)");
        assertEquals("bold and italic *literal* link", rendered.getString());
        assertTrue(rendered.getString().contains("literal*"));
        final boolean[] sawBoldItalic = {false};
        rendered.visit((style, string) -> {
            if (style.isBold() && style.isItalic()) sawBoldItalic[0] = true;
            return java.util.Optional.empty();
        }, Style.EMPTY);
        assertTrue(sawBoldItalic[0]);
    }

    @Test
    void markdownFencesOnlyCloseOnBareFenceLine() {
        Text rendered = MarkdownRenderer.render("```java\n```python\nreturn 1;\n```\nend");
        assertTrue(rendered.getString().contains("```python"));
        assertTrue(rendered.getString().contains("return 1;"));
        assertTrue(rendered.getString().endsWith("end"));
    }

    @Test
    void tableWidthUsesUnicodeDisplayWidth() {
        Text rendered = TableRenderer.render(List.of("| 表头 |", "|---|", "| 😀 |"));
        assertTrue(rendered.getString().contains("😀"));
        String[] rows = rendered.getString().split("\\n", -1);
        int lineWidth = TableRenderer.displayWidth(rows[0]);
        for (String row : rows) assertEquals(lineWidth, TableRenderer.displayWidth(row), row);
    }

    @Test
    void tableRendersCommonLatexCommandsAndKeepsColumnsAligned() {
        Text rendered = TableRenderer.render(List.of(
                "| 物理量 | 符号 | LaTeX 表示 | 常用单位 | 状态说明 |",
                "|:---:|:---:|:---:|:---:|:---:|",
                "| 实体质量 | m | m | \\text{kg} | 基础常数 |",
                "| 运动速度 | $\\vec{v}$ | \\vec{v} | \\text{m/s} | 矢量 |",
                "| 动能 | $E_k$ | \\frac{1}{2}mv^2 | \\text{J} | 标量计算 |"));
        String[] rows = rendered.getString().split("\\n", -1);
        assertTrue(rows.length >= 3);
        int lineWidth = TableRenderer.displayWidth(rows[0]);
        for (String row : rows) assertEquals(lineWidth, TableRenderer.displayWidth(row), row);
        assertTrue(rendered.getString().contains("kg"));
        assertTrue(rendered.getString().contains("v⃗"));
        assertTrue(rendered.getString().contains("1/2mv²"));
    }

    @Test
    void screenshotTableKeepsChineseColumnsAlignedAndRendersBareLatex() {
        Text rendered = TableRenderer.render(List.of(
                "| 序号 | 方块类型 | 信号输出 (S) | 导电性 | 状态方程 |",
                "|:---:|:---:|:---:|:---:|:---:|",
                "| 1 | 红石块 | 15 | 充能 | f(x) = 1 |",
                "| 2 | 红石火把 | 15 | 附着 | F(x) = \\neg x |",
                "| 3 | 侦测器 | 15 | 脉冲 | \\delta(t) |"));

        String[] rows = rendered.getString().split("\\n", -1);
        assertEquals(7, rows.length);
        int lineWidth = TableRenderer.displayWidth(rows[0]);
        for (String row : rows) {
            assertEquals(lineWidth, TableRenderer.displayWidth(row), row);
        }
        assertTrue(rendered.getString().contains("¬ x"));
        assertTrue(rendered.getString().contains("δ(t)"));
        assertFalse(rendered.getString().contains("\\neg"));
    }

    @Test
    void tableUsesVisibleBoxLayoutInsteadOfMarkdownSeparatorSyntax() {
        Text rendered = TableRenderer.render(List.of(
                "| 方块 | 硬度 | 工具 |",
                "|---|---:|:---:|",
                "| 泥土 | 0.5 | 锹 |"));
        String value = rendered.getString();
        assertTrue(value.startsWith("┌"));
        assertTrue(value.contains("│ 方块"));
        assertTrue(value.contains("├"));
        assertTrue(value.endsWith("┘"));
        assertFalse(value.contains("---"));
    }

    @Test
    void tableTreatsEscapedPipeAsCellContent() {
        Text rendered = TableRenderer.render(List.of(
                "| Name | Value |",
                "|---|---|",
                "| left\\|right | ok |"));
        assertTrue(rendered.getString().contains("left|right"));
        String[] rows = rendered.getString().split("\\n", -1);
        int lineWidth = TableRenderer.displayWidth(rows[0]);
        for (String row : rows) assertEquals(lineWidth, TableRenderer.displayWidth(row), row);
    }

    @Test
    void tableUsesUniformMinecraftFontForEveryCell() {
        Text rendered = TableRenderer.render(List.of(
                "| Name | Value |",
                "|---|---|",
                "| x | 42 |"));
        final boolean[] allUniform = {true};
        rendered.visit((style, string) -> {
            if (!string.equals("\n") && !"minecraft:uniform".equals(String.valueOf(style.getFont()))) allUniform[0] = false;
            return java.util.Optional.empty();
        }, Style.EMPTY);
        assertTrue(allUniform[0]);
    }

    @Test
    void tableKeepsSemanticColorsInsideCells() {
        Text rendered = TableRenderer.render(List.of(
                "| Header | Formula | Code |",
                "|---|---|---|",
                "| **bold** | $x^2$ | `code` |"));
        final boolean[] foundLatex = {false};
        final boolean[] foundCode = {false};
        rendered.visit((style, string) -> {
            int color = style.getColor() == null ? -1 : style.getColor().getRgb();
            if (0xB5CEA8 == color && string.contains("x²")) foundLatex[0] = true;
            if (0xCE9178 == color && string.contains("code")) foundCode[0] = true;
            return java.util.Optional.empty();
        }, Style.EMPTY);
        assertTrue(foundLatex[0]);
        assertTrue(foundCode[0]);
    }

    @Test
    void malformedTableInputFallsBackToSourceLines() {
        Text rendered = TableRenderer.render(List.of(
                "| Header | Value |",
                "not a separator",
                "| body | 1 |"));
        assertEquals("| Header | Value |\nnot a separator\n| body | 1 |", rendered.getString());
        assertFalse(rendered.getString().contains("┌"));
    }

    @Test
    void colorsRequireStrictHexValues() {
        assertEquals("#AABBCC", RichChatColors.normalize("#aabbcc"));
        assertNull(RichChatColors.normalize("FFFFFF"));
        assertNull(RichChatColors.normalize("#FFF"));
        assertNull(RichChatColors.normalize("#GGGGGG"));
    }

    @Test
    void defaultsFollowVsCodePaletteWithDistinctHeadingColors() {
        assertEquals("#D4D4D4", RichChatColors.defaultValue("plain"));
        assertEquals("#CE9178", RichChatColors.defaultValue("codeBlock"));
        assertEquals("#3794FF", RichChatColors.defaultValue("link"));
        assertEquals("#569CD6", RichChatColors.defaultValue("heading1"));
        assertEquals("#4EC9B0", RichChatColors.defaultValue("heading2"));
        assertEquals("#DCDCAA", RichChatColors.defaultValue("heading3"));
        assertEquals("#C586C0", RichChatColors.defaultValue("heading4"));
        assertEquals("#9CDCFE", RichChatColors.defaultValue("heading5"));
        assertEquals("#CE9178", RichChatColors.defaultValue("heading6"));
    }
}
