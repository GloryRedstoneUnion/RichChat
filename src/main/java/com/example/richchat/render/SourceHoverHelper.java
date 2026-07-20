package com.example.richchat.render;

import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * 渲染辅助工具.
 *
 * <p>负责为已渲染的 Text 附加 HoverEvent(SHOW_TEXT), 悬停时显示该消息的原始未渲染源码.</p>
 *
 * <p>悬停提示文本采用默认字体 (兼容中文等非 ASCII 字符) + 白色显示.
 * 不使用 {@code minecraft:alt} 等宽字体, 因为该字体对中文显示为乱码.</p>
 */
public final class SourceHoverHelper {

    private SourceHoverHelper() {
    }

    /**
     * 为目标 Text 附加显示原始源码的 HoverEvent.
     *
     * @param target  目标 Text.
     * @param source  原始源码字符串.
     * @param enabled 是否启用悬停 (false 时直接返回 target).
     * @return 附加了 HoverEvent 的 Text.
     */
    public static Text withSourceHover(Text target, String source, boolean enabled) {
        if (!enabled || source == null || source.isEmpty()) {
            return target;
        }
        if (!(target instanceof MutableText)) {
            // 不可变 Text, 复制后修改
            return applyHover(target.copy(), source);
        }
        return applyHover((MutableText) target, source);
    }

    /**
     * 构造悬停提示用的 Text.
     *
     * <p>使用默认字体 ( minecraft:default ), 确保中文 / Unicode 数学符号等都能正常显示.
     * 早期版本曾使用 {@code minecraft:alt} 等宽字体, 但该字体在中文环境下显示为乱码.</p>
     */
    public static Text buildHoverText(String source) {
        return Text.literal(source).formatted(Formatting.WHITE);
    }

    /**
     * 在 MutableText 上附加 HoverEvent.
     */
    private static MutableText applyHover(MutableText target, String source) {
        HoverEvent event = new HoverEvent(HoverEvent.Action.SHOW_TEXT, buildHoverText(source));
        // 在根 Style 上设置 HoverEvent; 子节点未显式覆盖时自动继承.
        Style rootStyle = target.getStyle();
        Style withHover = rootStyle.withHoverEvent(event);
        target.setStyle(withHover);
        return target;
    }
}
