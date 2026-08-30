package com.example.richchat.client;

import com.example.richchat.parse.TableRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

/** Client adapter that lays out table columns using Minecraft's real pixel metrics. */
public final class ClientTableRenderer {
    private static final Identifier TABLE_FONT = Identifier.of("minecraft", "uniform");
    private static final Identifier SPACING_FONT = Identifier.of("richchat", "table_spacing");
    private static final String PIXEL_SPACE = "\uE000";
    private static final String BACK_ONE_PIXEL = "\uE001";

    private ClientTableRenderer() {
    }

    public static Text render(List<String> lines) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) {
            return TableRenderer.render(lines);
        }

        TextRenderer renderer = client.textRenderer;
        Style fontStyle = Style.EMPTY.withFont(TABLE_FONT);
        int spaceWidth = Math.max(1, renderer.getWidth(Text.literal(" ").setStyle(fontStyle)));
        int pipeWidth = Math.max(1, renderer.getWidth(Text.literal("│").setStyle(fontStyle)));
        TableRenderer.Metrics metrics = new TableRenderer.Metrics() {
            @Override
            public int width(Text text) {
                // Keep all column and padding calculations in actual pixels;
                // converting to character counts is what caused the original
                // border drift for CJK, bold, and formula glyphs.
                return renderer.getWidth(text);
            }

            @Override
            public int spaceWidth() {
                return spaceWidth;
            }

            @Override
            public Text padding(int width, Style style) {
                return Text.literal(PIXEL_SPACE.repeat(Math.max(0, width)))
                        .setStyle(style.withFont(SPACING_FONT));
            }

            @Override
            public Text rule(int width, char leading, Style style) {
                int leadingWidth = renderer.getWidth(
                        Text.literal(String.valueOf(leading)).setStyle(fontStyle));
                int advance = Math.max(1, width + pipeWidth - leadingWidth);

                // Pull the synthetic line one pixel into the box glyph so the
                // rasterized strokes touch, while preserving the exact total
                // advance required for the next vertical separator.
                return Text.empty()
                        .append(Text.literal(BACK_ONE_PIXEL)
                                .setStyle(style.withFont(SPACING_FONT)))
                        .append(Text.literal(PIXEL_SPACE.repeat(advance + 1))
                                .setStyle(style.withFont(SPACING_FONT).withStrikethrough(true)));
            }
        };
        return TableRenderer.renderWithMetrics(lines, metrics);
    }
}
