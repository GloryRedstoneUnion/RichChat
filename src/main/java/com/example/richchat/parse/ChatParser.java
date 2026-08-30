package com.example.richchat.parse;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import com.example.richchat.config.RichChatConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 消息解析主类.
 *
 * <p>负责将原始聊天消息 {@link Text} 解析为渲染后的 Text. 解析策略分为两层:</p>
 *
 * <h3>第一层: 结构化识别 (优先)</h3>
 * <p>vanilla 聊天消息本质上是 {@link TranslatableTextContent}; 常见参数位置固定，
 * 但服务器插件可能在聊天组件外层或参数列表前面追加频道/队伍前缀:</p>
 * <ul>
 *   <li>{@code chat.type.text} = {@code <%s> %s} —— sender/message are the final two args</li>
 *   <li>{@code chat.type.team.text} = {@code %s <%s> %s} —— args[0]=team prefix,
 *       args[1]=sender, args[2]=message</li>
 * </ul>
 * <p>直接从 {@code TranslatableTextContent} 提取参数, sender (含 team 颜色) 原样 append,
 * {@code <} {@code >} 用 {@link Text#literal(String)} 拼接, 不参与 Markdown 渲染,
 * 因此 {@code >} 永远不会被引用语法吞掉. message 部分递归渲染.</p>
 *
 * <h3>第二层: 通用渲染 (fallback)</h3>
 * <p>非 vanilla 聊天结构 (命令反馈、第三方 mod 消息、纯 LiteralText 等) 用
 * {@link Text#visit(StringVisitable.StyledVisitor, Style)} 展开, 对每个带样式段
 * 应用 LaTeX + Markdown 渲染, 保留原段样式.</p>
 *
 * <p>该类不使用正则识别聊天前缀, 完全基于 MC 的 Text 数据结构.</p>
 */
public final class ChatParser {

    /** 普通玩家消息翻译键: {@code <%s> %s}. */
    private static final String KEY_CHAT_TEXT = "chat.type.text";

    /** Team 玩家消息翻译键: {@code %s <%s> %s}. */
    private static final String KEY_CHAT_TEAM_TEXT = "chat.type.team.text";

    private ChatParser() {
    }

    /**
     * 解析原始 Text, 返回渲染后的 Text.
     *
     * @param original 原始 Text.
     * @return 渲染后的 Text (与 original 不是同一对象).
     */
    public static Text parse(Text original) {
        if (original == null) {
            return Text.empty();
        }

        // 第一层: 识别 vanilla 聊天消息结构
        Text structured = parseChatStructure(original);
        if (structured != null) {
            return structured;
        }

        Text textualPrefix = parseTextualChatPrefix(original);
        if (textualPrefix != null) {
            return textualPrefix;
        }

        // 第二层: 通用渲染 (非聊天消息或无法识别的结构)
        return renderTextBody(original);
    }

    /**
     * 识别 vanilla 聊天消息的 TranslatableText 结构.
     *
     * <p>支持两种翻译键:</p>
     * <ul>
     *   <li>{@code chat.type.text} —— 普通玩家消息 {@code <sender> message}.</li>
     *   <li>{@code chat.type.team.text} —— team 消息 {@code [team] <sender> message}.</li>
     * </ul>
     *
     * @param original 原始 Text.
     * @return 重构后的 Text (前缀 literal 拼接 + 消息体渲染); 不匹配时返回 null.
     */
    private static Text parseChatStructure(Text original) {
        Text direct = parseDirectChatStructure(original);
        if (direct != null) {
            return direct;
        }

        // Some servers prepend a team/channel label as a sibling of the
        // vanilla chat component, e.g. "[Survival] " + chat.type.text.
        // Walk the Text tree so only the actual message argument is parsed.
        List<Text> siblings = original.getSiblings();
        for (int i = 0; i < siblings.size(); i++) {
            Text parsed = parseChatStructure(siblings.get(i));
            if (parsed != null) {
                MutableText copy = original.copy();
                copy.getSiblings().set(i, parsed);
                return copy;
            }
        }
        return null;
    }

    private static Text parseDirectChatStructure(Text original) {
        TextContent content = original.getContent();
        if (!(content instanceof TranslatableTextContent ttc)) {
            return null;
        }

        String key = ttc.getKey();
        Object[] args = ttc.getArgs();
        if (args == null || args.length == 0) {
            return null;
        }

        // chat.type.text: "<%s> %s". Some chat plugins add a prefix argument;
        // keep every argument before the sender untouched and always render
        // only the final argument as the message body.
        if (KEY_CHAT_TEXT.equals(key) && args.length >= 2) {
            int senderIndex = args.length - 2;
            Text sender = asText(args[senderIndex]);
            Text message = asText(args[args.length - 1]);
            MutableText result = Text.empty().setStyle(original.getStyle());
            for (int i = 0; i < senderIndex; i++) {
                result.append(asText(args[i]));
            }
            result.append(Text.literal("<").setStyle(original.getStyle()));
            result.append(sender);
            result.append(Text.literal("> ").setStyle(original.getStyle()));
            result.append(renderTextBody(message));
            return result;
        }

        // chat.type.team.text: "%s <%s> %s" —— args[0]=team prefix,
        // args[1]=sender, args[2]=message.
        if (KEY_CHAT_TEAM_TEXT.equals(key) && args.length >= 3) {
            Text teamPrefix = asText(args[0]);
            Text sender = asText(args[1]);
            Text message = asText(args[2]);
            MutableText result = Text.empty().setStyle(original.getStyle());
            result.append(teamPrefix);
            result.append(Text.literal(" <").setStyle(original.getStyle()));
            result.append(sender);
            result.append(Text.literal("> ").setStyle(original.getStyle()));
            result.append(renderTextBody(message));
            return result;
        }

        return null;
    }

    /**
     * 将 TranslatableText 参数对象转换为 Text.
     *
     * <p>vanilla 聊天消息的参数通常已是 {@link Text} (含 team 颜色等样式);
     * 极少数情况下可能是 String 或其他对象, 用 {@link String#valueOf(Object)} 兜底.</p>
     *
     * @param arg 原始参数.
     * @return 对应的 Text.
     */
    private static Text asText(Object arg) {
        if (arg instanceof Text t) {
            return t;
        }
        if (arg == null) {
            return Text.empty();
        }
        if (arg instanceof String s) {
            return Text.literal(s);
        }
        return Text.literal(String.valueOf(arg));
    }

    /**
     * 对任意 Text 应用 LaTeX + Markdown 渲染, 保留原段样式.
     *
     * <p>用 {@link Text#visit(StringVisitable.StyledVisitor, Style)} 递归展开,
     * 对每个带样式段: 先 LaTeX → Unicode 近似, 再 Markdown → Text, 渲染时
     * 继承原段样式 (颜色、格式等).</p>
     *
     * <p>本方法不识别聊天前缀, 适用于已剥离前缀的消息体, 或非聊天消息.</p>
     *
     * @param text 待渲染的 Text.
     * @return 渲染后的 Text.
     */
    private static Text renderTextBody(Text text) {
        if (text == null) {
            return Text.empty();
        }

        List<StyleSegment> segments = new ArrayList<>();
        text.visit((style, string) -> {
            if (string != null && !string.isEmpty()) {
                segments.add(new StyleSegment(style, string));
            }
            return Optional.empty();
        }, Style.EMPTY);

        if (segments.isEmpty()) {
            return text;
        }

        if (requiresCrossSegmentParse(segments)) {
            StringBuilder source = new StringBuilder();
            for (StyleSegment segment : segments) source.append(segment.text);
            return MarkdownRenderer.render(LatexUnicodeRenderer.render(source.toString()), segments.get(0).style);
        }

        MutableText result = Text.empty();
        for (StyleSegment seg : segments) {
            for (LatexUnicodeRenderer.Segment part : LatexUnicodeRenderer.renderSegments(seg.text)) {
                if (!part.text().isEmpty()) {
                    Style style = part.formula() ? seg.style.withColor(RichChatConfig.INSTANCE.getColor("latex")) : seg.style;
                    String source = part.formula() ? part.text() : LatexUnicodeRenderer.renderBare(part.text());
                    result.append(MarkdownRenderer.render(source, style));
                }
            }
        }
        return result;
    }

    /**
     * 将字符串渲染为 Text, 不附加悬停事件.
     *
     * <p>本方法为字符串入口, 使用空样式作为基础.
     * 用于命令反馈等非聊天消息场景.</p>
     *
     * @param source 原始字符串.
     * @return 渲染后的 Text.
     */
    public static Text renderSource(String source) {
        return renderSource(source, Style.EMPTY);
    }

    /**
     * 渲染字符串并使用指定样式作为普通文本的基础样式.
     *
     * <p>表格单元格使用此入口提供表头/表体颜色；Markdown 和 LaTeX
     * 的语义颜色仍会在其对应片段上覆盖基础颜色.</p>
     *
     * @param source    原始字符串.
     * @param baseStyle 普通文本基础样式.
     * @return 渲染后的 Text.
     */
    public static Text renderSource(String source, Style baseStyle) {
        MutableText result = Text.empty();
        if (baseStyle == null) {
            baseStyle = Style.EMPTY;
        }
        for (LatexUnicodeRenderer.Segment part : LatexUnicodeRenderer.renderSegments(source)) {
            Style style = part.formula()
                    ? baseStyle.withColor(RichChatConfig.INSTANCE.getColor("latex"))
                    : baseStyle;
            String sourceText = part.formula() ? part.text() : LatexUnicodeRenderer.renderBare(part.text());
            result.append(MarkdownRenderer.render(sourceText, style));
        }
        return result;
    }

    /**
     * Re-render a source string recovered from ChatHud while preserving a
     * textual chat prefix such as {@code [Survival] <Player> }.
     */
    public static Text renderSourcePreservingChatPrefix(String source) {
        if (source == null || source.isEmpty()) {
            return renderSource(source);
        }
        int bodyStart = findTextualChatBodyStart(source);
        if (bodyStart < 0) {
            return renderSource(source);
        }
        MutableText result = Text.empty();
        result.append(Text.literal(source.substring(0, bodyStart)));
        result.append(renderSource(source.substring(bodyStart)));
        return result;
    }

    private static Text parseTextualChatPrefix(Text original) {
        if (original == null || !original.getSiblings().isEmpty()) {
            return null;
        }
        String source = original.getString();
        int bodyStart = findTextualChatBodyStart(source);
        if (bodyStart < 0 || bodyStart >= source.length()) {
            return null;
        }
        MutableText result = Text.empty().setStyle(original.getStyle());
        result.append(Text.literal(source.substring(0, bodyStart)).setStyle(original.getStyle()));
        result.append(renderTextBody(Text.literal(source.substring(bodyStart)).setStyle(original.getStyle())));
        return result;
    }

    /** Return the first character after a textual "[prefix] <player> " header. */
    private static int findTextualChatBodyStart(String source) {
        int close = source.indexOf("> ");
        int open = close < 0 ? -1 : source.lastIndexOf('<', close);
        if (open < 0 || open >= close) {
            return -1;
        }
        String beforePlayer = source.substring(0, open);
        if (!beforePlayer.isEmpty()
                && !(beforePlayer.startsWith("[") && beforePlayer.endsWith("] "))) {
            return -1;
        }
        return close + 2;
    }

    /**
     * 从聊天 Text 中提取消息体字符串 (剥离 {@code <sender> } 前缀).
     *
     * <p>用于多行块状态机判断: 只对消息体部分判断是否开始 / 结束代码块或 LaTeX 块,
     * 否则 {@code <player> $$} 会被识别为普通文本而不是 LaTeX 块开始.</p>
     *
     * <p>识别规则:</p>
     * <ul>
     *   <li>{@code chat.type.text} → 返回最后一个参数 (message).</li>
     *   <li>{@code chat.type.team.text} → 返回 args[2] (message).</li>
     *   <li>其他 → 返回 {@link Text#getString()} (整段文本).</li>
     * </ul>
     *
     * @param original 原始 Text.
     * @return 消息体字符串.
     */
    public static String extractMessageBody(Text original) {
        if (original == null) {
            return "";
        }
        Text chatNode = findChatNode(original);
        if (chatNode != null && chatNode.getContent() instanceof TranslatableTextContent ttc) {
            String key = ttc.getKey();
            Object[] args = ttc.getArgs();
            if (args != null) {
                if (KEY_CHAT_TEXT.equals(key) && args.length >= 2) {
                    return asText(args[args.length - 1]).getString();
                }
                if (KEY_CHAT_TEAM_TEXT.equals(key) && args.length >= 3) {
                    return asText(args[2]).getString();
                }
            }
        }
        return original.getString();
    }

    private static Text findChatNode(Text text) {
        if (text == null) {
            return null;
        }
        TextContent content = text.getContent();
        if (content instanceof TranslatableTextContent ttc
                && (KEY_CHAT_TEXT.equals(ttc.getKey()) || KEY_CHAT_TEAM_TEXT.equals(ttc.getKey()))) {
            return text;
        }
        for (Text sibling : text.getSiblings()) {
            Text found = findChatNode(sibling);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * 渲染多行代码块 (已累积的内容, 不含 {@code ```} 定界符).
     *
     * @param content 代码块内容.
     * @return 渲染后的 Text (深灰色).
     */
    public static Text renderMultiLineCodeBlock(String content) {
        return MarkdownRenderer.renderCodeBlock(content);
    }

    /**
     * 渲染多行 LaTeX 块 (已累积的公式内容, 不含 {@code $$} 定界符).
     *
     * <p>先用 {@link LatexUnicodeRenderer#renderBlock} 转换为 Unicode 近似字符串,
     * 再用 {@link MarkdownRenderer} 解析 (公式中可能含 {@code ^} {@code _} 等
     * 已被 LaTeX 处理的标记, 但不会再被 Markdown 误识别).</p>
     *
     * @param formula 公式内容.
     * @return 渲染后的 Text.
     */
    public static Text renderMultiLineLatexBlock(String formula) {
        String latexProcessed = LatexUnicodeRenderer.renderBlock(formula);
        return MarkdownRenderer.render(latexProcessed,
                Style.EMPTY.withColor(RichChatConfig.INSTANCE.getColor("latex")));
    }

    /**
     * 渲染多行 Markdown 表格.
     *
     * <p>输入为表格的所有行 (表头行 + 分隔行 + 数据行), 内部调用
     * {@link TableRenderer#render} 进行对齐渲染.</p>
     *
     * @param tableBodies 表格行列表 (每行为已剥离聊天前缀的消息体).
     * @return 渲染后的 Text (盒式表格; 客户端入口会进一步使用像素测量).
     */
    public static Text renderTable(java.util.List<String> tableBodies) {
        return TableRenderer.render(tableBodies);
    }

    /**
     * 将表格行列表构建为悬停用的源码字符串 (每行用 \n 分隔).
     *
     * @param tableBodies 表格行列表.
     * @return 拼接后的源码字符串.
     */
    public static String buildTableSource(java.util.List<String> tableBodies) {
        return String.join("\n", tableBodies);
    }

    /**
     * 一个带样式的字符段, 由 {@link Text#visit(StringVisitable.StyledVisitor, Style)} 产生.
     */
    private static final class StyleSegment {
        final Style style;
        final String text;

        StyleSegment(Style style, String text) {
            this.style = style;
            this.text = text;
        }
    }

    private static boolean requiresCrossSegmentParse(List<StyleSegment> segments) {
        int totalBackticks = 0;
        int totalBold = 0;
        int totalStrike = 0;
        for (StyleSegment segment : segments) {
            totalBackticks += count(segment.text, "`");
            totalBold += count(segment.text, "**");
            totalStrike += count(segment.text, "~~");
        }
        return (totalBackticks % 2 == 0 && totalBackticks > 0)
                || (totalBold % 2 == 0 && totalBold > 0)
                || (totalStrike % 2 == 0 && totalStrike > 0);
    }

    private static int count(String source, String token) {
        int count = 0;
        for (int i = 0; i <= source.length() - token.length(); i++) {
            if (source.startsWith(token, i)) {
                count++;
                i += token.length() - 1;
            }
        }
        return count;
    }
}
