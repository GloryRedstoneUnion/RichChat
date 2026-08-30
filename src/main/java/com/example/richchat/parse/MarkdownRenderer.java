package com.example.richchat.parse;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import com.example.richchat.config.RichChatConfig;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown → Minecraft {@link Text} 转换器.
 *
 * <p>支持的 Markdown 元素 (按优先级, 高 → 低):</p>
 * <ol>
 *   <li>代码块 {@code ```...```} → 灰色.</li>
 *   <li>行内代码 {@code `...`} → 灰色.</li>
 *   <li>链接 {@code [text](url)} → 下划线 + OPEN_URL 点击事件.</li>
 *   <li>粗体 {@code **...**} → MC bold.</li>
 *   <li>斜体 {@code *...*} → MC italic.</li>
 *   <li>删除线 {@code ~~...~~} → MC strikethrough.</li>
 *   <li>标题 {@code # / ## / ###} → 粗体 + 颜色分级 (# 蓝, ## 绿, ### 黄).</li>
 *   <li>列表 {@code - ...} → "• " 前缀.</li>
 *   <li>引用 {@code > ...} → 灰色 + 缩进.</li>
 * </ol>
 *
 * <p>反斜杠 {@code \} 保留其后的标记字符为普通文本.</p>
 *
 * <p>未闭合的标记优雅降级为普通文本.</p>
 *
 * <p><b>关于字体:</b> 早期版本曾使用 {@code minecraft:alt} (Unicode 字体) 作为代码字体,
 * 但该字体对 ASCII 字符 (数字、字母) 显示为带圈 / 全角符号, 对中文显示为方块,
 * 在中文环境下表现为"乱码". 现已改用默认字体 ({@code minecraft:default}) + 灰色
 * 区分代码, 兼容中英文.</p>
 */
public final class MarkdownRenderer {

    /** URL 正则. */
    private static final Pattern URL_PATTERN =
            Pattern.compile("(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)");

    private MarkdownRenderer() {
    }

    /**
     * 渲染 Markdown 字符串为 {@link MutableText}, 使用空样式作为基础.
     *
     * @param text 原始 Markdown 字符串.
     * @return 渲染后的 Text.
     */
    public static MutableText render(String text) {
        return render(text, Style.EMPTY);
    }

    /**
     * 渲染 Markdown 字符串为 {@link MutableText}, 所有生成的节点继承 {@code baseStyle}.
     *
     * <p>用于保留原聊天消息的颜色与格式: 普通文字、行内代码、链接、粗斜体等均会叠加在 {@code baseStyle} 之上;
     * 标题 / 引用 等具有强语义的元素会用自身颜色覆盖 {@code baseStyle} 的颜色.</p>
     *
     * @param text      原始 Markdown 字符串.
     * @param baseStyle 基础样式 (通常来自原 Text 节点).
     * @return 渲染后的 Text.
     */
    public static MutableText render(String text, Style baseStyle) {
        MutableText result = Text.empty();
        if (text == null || text.isEmpty()) {
            return result;
        }
        if (baseStyle.getColor() == null) {
            baseStyle = semanticStyle(baseStyle, "plain");
        }

        String[] lines = text.split("\n", -1);
        boolean inCodeBlock = false;
        StringBuilder codeBlock = new StringBuilder();

        for (int lineIdx = 0; lineIdx < lines.length; lineIdx++) {
            String line = lines[lineIdx];

            // 代码块开始 / 结束
            String trimmedLine = line.trim();
            if (trimmedLine.startsWith("```")) {
                int sameLineClose = trimmedLine.indexOf("```", 3);
                if (!inCodeBlock && sameLineClose > 3) {
                    result.append(renderInlineCode(trimmedLine.substring(3, sameLineClose), baseStyle));
                    if (lineIdx < lines.length - 1) result.append(Text.literal("\n"));
                    continue;
                }
                if (inCodeBlock) {
                    String raw = codeBlock.toString();
                    int firstLineEnd = raw.indexOf('\n');
                    String content = firstLineEnd >= 0 ? raw.substring(firstLineEnd + 1) : raw;
                    result.append(renderCodeBlock(content));
                    codeBlock.setLength(0);
                    inCodeBlock = false;
                } else {
                    inCodeBlock = true;
                    codeBlock.setLength(0);
                    codeBlock.append(line).append('\n');
                }
                if (!inCodeBlock && lineIdx < lines.length - 1) {
                    result.append(Text.literal("\n"));
                }
                continue;
            }

            if (inCodeBlock) {
                codeBlock.append(line);
                if (lineIdx < lines.length - 1) {
                    codeBlock.append('\n');
                }
                continue;
            }

            // 行级元素
            appendLine(result, line, baseStyle);

            if (lineIdx < lines.length - 1) {
                result.append(Text.literal("\n"));
            }
        }

        // 未闭合代码块: 作为代码内容输出
        if (inCodeBlock) {
            result.append(Text.literal(codeBlock.toString()).setStyle(baseStyle));
        }

        return result;
    }

    /**
     * 处理单行, 识别标题 / 列表 / 引用, 否则交给行内解析.
     *
     * @param baseStyle 基础样式, 普通文字与列表内容会继承该样式.
     */
    private static void appendLine(MutableText result, String line, Style baseStyle) {
        if (line.startsWith("# ")) {
            // 标题: 强语义, 覆盖颜色
            result.append(parseInline(line.substring(2),
                    semanticStyle(baseStyle, "heading1").withBold(true)));
        } else if (line.startsWith("## ")) {
            result.append(parseInline(line.substring(3),
                    semanticStyle(baseStyle, "heading2").withBold(true)));
        } else if (line.startsWith("### ")) {
            result.append(parseInline(line.substring(4),
                    semanticStyle(baseStyle, "heading3").withBold(true)));
        } else if (line.startsWith("#### ")) {
            result.append(parseInline(line.substring(5),
                    semanticStyle(baseStyle, "heading4").withBold(true)));
        } else if (line.startsWith("##### ")) {
            result.append(parseInline(line.substring(6),
                    semanticStyle(baseStyle, "heading5").withBold(true)));
        } else if (line.startsWith("###### ")) {
            result.append(parseInline(line.substring(7),
                    semanticStyle(baseStyle, "heading6").withBold(true)));
        } else if (line.startsWith("- ") || line.startsWith("* ")) {
            // 列表: 保留 baseStyle 颜色
            Style listStyle = semanticStyle(baseStyle, "list");
            result.append(Text.literal("• ").setStyle(listStyle));
            result.append(parseInline(line.substring(2), listStyle));
        } else if (line.startsWith("> ")) {
            // 引用: 强语义, 覆盖颜色
            Style quoteStyle = semanticStyle(baseStyle, "quote").withItalic(true);
            result.append(Text.literal("  ").setStyle(quoteStyle));
            result.append(parseInline(line.substring(2), quoteStyle));
        } else if (line.startsWith(">")) {
            Style quoteStyle = semanticStyle(baseStyle, "quote").withItalic(true);
            result.append(Text.literal("  ").setStyle(quoteStyle));
            result.append(parseInline("", quoteStyle));
        } else {
            // 普通行: 保留 baseStyle 颜色与格式
            result.append(parseInline(line, baseStyle));
        }
    }

    /**
     * 行内解析: 处理转义、行内代码、链接、粗体、斜体、删除线、URL.
     *
     * @param text      文本.
     * @param baseStyle 基础样式 (会传递给所有子节点).
     * @return 解析后的 MutableText.
     */
    private static MutableText parseInline(String text, Style baseStyle) {
        MutableText result = Text.empty();
        StringBuilder plain = new StringBuilder();
        int i = 0;

        while (i < text.length()) {
            char c = text.charAt(i);

            // 转义
            if (c == '\\' && i + 1 < text.length()
                    && isEscapableMarkdownChar(text.charAt(i + 1))) {
                plain.append(text.charAt(i + 1));
                i += 2;
                continue;
            }

            // 行内代码 `...`
            if (c == '`') {
                int end = text.indexOf('`', i + 1);
                if (end != -1) {
                    flushPlain(result, plain, baseStyle);
                    String code = text.substring(i + 1, end);
                    result.append(renderInlineCode(code, baseStyle));
                    i = end + 1;
                    continue;
                }
            }

            // 粗体 **...**
            if (c == '*' && i + 1 < text.length() && text.charAt(i + 1) == '*') {
                int end = findClosing(text, i + 2, "**");
                if (end != -1) {
                    flushPlain(result, plain, baseStyle);
                    String inner = text.substring(i + 2, end);
                    result.append(parseInline(inner, semanticStyle(baseStyle, "bold").withBold(true)));
                    i = end + 2;
                    continue;
                }
            }

            // 斜体 *...* (避免与 ** 冲突)
            if (c == '*' && (i + 1 >= text.length() || text.charAt(i + 1) != '*')) {
                int end = findClosing(text, i + 1, "*");
                if (end != -1 && end > i + 1) {
                    flushPlain(result, plain, baseStyle);
                    String inner = text.substring(i + 1, end);
                    result.append(parseInline(inner, semanticStyle(baseStyle, "italic").withItalic(true)));
                    i = end + 1;
                    continue;
                }
            }

            // 删除线 ~~...~~
            if (c == '~' && i + 1 < text.length() && text.charAt(i + 1) == '~') {
                int end = findClosing(text, i + 2, "~~");
                if (end != -1) {
                    flushPlain(result, plain, baseStyle);
                    String inner = text.substring(i + 2, end);
                    result.append(parseInline(inner, semanticStyle(baseStyle, "strikethrough").withStrikethrough(true)));
                    i = end + 2;
                    continue;
                }
            }

            // 链接 [text](url)
            if (c == '[') {
                int linkEnd = tryParseLink(text, i, plain, result, baseStyle);
                if (linkEnd > 0) {
                    i = linkEnd;
                    continue;
                }
            }

            // 裸 URL 自动识别 (http://, https://)
            if (i + 7 <= text.length()
                    && (text.startsWith("http://", i) || text.startsWith("https://", i))) {
                int urlEnd = i;
                while (urlEnd < text.length()
                        && !Character.isWhitespace(text.charAt(urlEnd))
                        && "<>()\"'".indexOf(text.charAt(urlEnd)) == -1) {
                    urlEnd++;
                }
                // 去掉末尾的标点
                while (urlEnd > i && ".,;:!?".indexOf(text.charAt(urlEnd - 1)) != -1) {
                    urlEnd--;
                }
                if (urlEnd > i) {
                    flushPlain(result, plain, baseStyle);
                    String url = text.substring(i, urlEnd);
                    Style urlStyle = semanticStyle(baseStyle, "link").withUnderline(true)
                            .withClickEvent(new net.minecraft.text.ClickEvent(
                                    net.minecraft.text.ClickEvent.Action.OPEN_URL, url));
                    result.append(Text.literal(url).setStyle(urlStyle));
                    i = urlEnd;
                    continue;
                }
            }

            // 普通字符
            plain.append(c);
            i++;
        }

        flushPlain(result, plain, baseStyle);
        return result;
    }

    /**
     * 渲染行内代码片段, 同时识别其中的 URL 并附加点击事件.
     *
     * <p>使用默认字体 + 深灰色区分代码 (不使用 {@code minecraft:alt} Unicode 字体,
     * 该字体对 ASCII / 中文显示为乱码).</p>
     */
    private static MutableText renderInlineCode(String code, Style baseStyle) {
        Style codeStyle = semanticStyle(baseStyle, "inlineCode");
        // 在代码片段中检测 URL
        Matcher m = URL_PATTERN.matcher(code);
        MutableText result = Text.empty();
        int last = 0;
        while (m.find()) {
            if (m.start() > last) {
                result.append(Text.literal(code.substring(last, m.start())).setStyle(codeStyle));
            }
            String url = m.group(1);
            Style urlStyle = semanticStyle(codeStyle, "link").withUnderline(true)
                    .withClickEvent(new net.minecraft.text.ClickEvent(
                            net.minecraft.text.ClickEvent.Action.OPEN_URL, url));
            result.append(Text.literal(url).setStyle(urlStyle));
            last = m.end();
        }
        if (last < code.length()) {
            result.append(Text.literal(code.substring(last)).setStyle(codeStyle));
        }
        // 合并 baseStyle (粗体/斜体等继承自父级)
        if (!baseStyle.equals(Style.EMPTY)) {
            result.setStyle(baseStyle);
        }
        return result;
    }

    /**
     * 渲染代码块 (公开接口, 供多行块状态机调用).
     *
     * <p>使用默认字体 + 深灰色区分代码 (不使用 {@code minecraft:alt} Unicode 字体).</p>
     *
     * @param content 代码块内容 (不含 ``` 定界符).
     * @return 渲染后的 Text.
     */
    public static MutableText renderCodeBlock(String content) {
        Style codeStyle = semanticStyle(Style.EMPTY, "codeBlock");
        return Text.literal(content).setStyle(codeStyle);
    }

    /**
     * 尝试解析 [text](url) 链接.
     *
     * @return 成功时返回链接结束后位置; 失败返回 0.
     */
    private static int tryParseLink(String text, int start, StringBuilder plain,
                                    MutableText result, Style baseStyle) {
        int textEnd = text.indexOf(']', start + 1);
        if (textEnd == -1 || textEnd + 1 >= text.length() || text.charAt(textEnd + 1) != '(') {
            return 0;
        }
        int urlEnd = text.indexOf(')', textEnd + 2);
        if (urlEnd == -1) {
            return 0;
        }
        flushPlain(result, plain, baseStyle);
        String linkText = text.substring(start + 1, textEnd);
        String url = text.substring(textEnd + 2, urlEnd);
        Style linkStyle = semanticStyle(baseStyle, "link").withUnderline(true)
                .withClickEvent(new net.minecraft.text.ClickEvent(
                        net.minecraft.text.ClickEvent.Action.OPEN_URL, url));
        result.append(parseInline(linkText, linkStyle));
        return urlEnd + 1;
    }

    /**
     * 将累积的普通字符刷新为 Text 节点.
     */
    private static void flushPlain(MutableText result, StringBuilder plain, Style style) {
        if (plain.length() > 0) {
            result.append(Text.literal(plain.toString()).setStyle(style));
            plain.setLength(0);
        }
    }

    private static Style semanticStyle(Style baseStyle, String category) {
        net.minecraft.text.TextColor color = RichChatConfig.INSTANCE.getColor(category);
        return color == null ? baseStyle : baseStyle.withColor(color);
    }

    private static boolean isEscapableMarkdownChar(char c) {
        return "\\`*_{}[]()#+-.!>|~".indexOf(c) >= 0;
    }

    /**
     * 查找闭合标记 (跳过反斜杠转义).
     *
     * @return 闭合位置; -1 表示未找到.
     */
    private static int findClosing(String text, int start, String marker) {
        int i = start;
        while (i <= text.length() - marker.length()) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) {
                i += 2;
                continue;
            }
            if (text.startsWith(marker, i)) {
                return i;
            }
            i++;
        }
        return -1;
    }
}
