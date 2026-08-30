package com.example.richchat.client;

import com.example.richchat.parse.TableRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

/** Client adapter that measures rendered cells with Minecraft's real font metrics. */
public final class ClientTableRenderer {
    private static final Identifier TABLE_FONT = Identifier.of("minecraft", "uniform");

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
        int ruleWidth = Math.max(1, renderer.getWidth(Text.literal("─").setStyle(fontStyle)));
        TableRenderer.Metrics metrics = new TableRenderer.Metrics() {
            @Override
            public int width(Text text) {
                // TableRenderer works in whole padding units. Round every
                // cell up independently so a 5px glyph and an 8px glyph do
                // not produce different effective column boundaries.
                return (renderer.getWidth(text) + spaceWidth - 1) / spaceWidth;
            }

            @Override
            public int spaceWidth() {
                return 1;
            }

            @Override
            public String spaces(int width) {
                return " ".repeat(Math.max(0, width));
            }

            @Override
            public String rule(int width) {
                int pixels = Math.max(1, width * spaceWidth);
                int count = Math.max(1, (pixels + ruleWidth - 1) / ruleWidth);
                return "─".repeat(count);
            }
        };
        return TableRenderer.renderWithMetrics(lines, metrics);
    }
}
