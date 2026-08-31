package com.example.richchat.client;

import com.example.richchat.config.RichChatConfig;
import com.example.richchat.mixin.ChatHudAccessor;
import com.example.richchat.parse.ChatParser;
import com.example.richchat.parse.TableRenderer;
import com.example.richchat.render.SourceHoverHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天刷新管理器.
 *
 * <p>切换 RichChat 渲染开关时, 调用 {@link #refreshChatHud()} 遍历当前 ChatHud 的
 * 所有消息, 重新渲染, 让用户立即看到效果变化 (无需重新发送消息).</p>
 *
 * <p>核心思路:</p>
 * <ol>
 *   <li>从 ChatHud 的 {@code messages} 字段取出所有 ChatHudLine.</li>
 *   <li>对每条 ChatHudLine 的 {@code content()} (已渲染 Text) 优先查找入队时保存的
 *       原始 {@code Text}; 旧消息没有登记信息时才从根 Style 的 HoverEvent 或文本内容
 *       回退提取 source 字符串.</li>
 *   <li>根据当前 enabled 状态:
 *       <ul>
 *         <li>enabled=true → 对原始 {@code Text} 重新解析 (表格走表格渲染器) + 悬停.</li>
 *         <li>enabled=false → {@code original.copy()}, 保留原始样式和交互.</li>
 *       </ul>
 *   </li>
 *   <li>用新 Text 构造新 ChatHudLine, 通过 List.set 原地替换.</li>
 * </ol>
 *
 * <p>注意: 替换时不重新触发 addMessage (避免递归与状态机误判), 直接操作 List.</p>
 */
public final class ChatRefreshManager {

    private ChatRefreshManager() {
    }

    /**
     * 刷新当前 ChatHud 的所有消息, 按当前 enabled 状态重新渲染.
     */
    public static void refreshChatHud() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.inGameHud == null) {
            return;
        }
        ChatHud chatHud = mc.inGameHud.getChatHud();
        if (!(chatHud instanceof ChatHudAccessor accessor)) {
            return;
        }
        List<ChatHudLine> messages = accessor.richchat$getMessages();
        if (messages == null || messages.isEmpty()) {
            return;
        }

        boolean enabled = RichChatConfig.INSTANCE.isEnabled();
        boolean showHover = RichChatConfig.INSTANCE.isShowSourceOnHover();

        // 复制一份避免并发修改
        List<ChatHudLine> snapshot = new ArrayList<>(messages);

        for (int i = 0; i < snapshot.size(); i++) {
            ChatHudLine old = snapshot.get(i);
            Text oldContent = old.content();

            // Prefer the original component tree retained at message ingress.
            // The hover source is only a compatibility fallback for messages
            // created before the registry was populated.
            Text original = SourceHoverHelper.getOriginal(oldContent);
            String source;
            if (original == null) {
                source = extractSource(oldContent);
                original = Text.literal(source);
            } else {
                // Do not mistake a vanilla HoverEvent on the original message
                // for RichChat's source tooltip.
                source = original.getString();
            }

            // 重新渲染
            Text newContent;
            if (enabled) {
                // Reparse the original Text so structured chat prefixes and
                // their styles remain intact. Tables are the one renderer
                // that intentionally consumes a source-line collection.
                newContent = renderSource(original, source);
                newContent = SourceHoverHelper.withSourceHover(
                        newContent, source, showHover, original);
            } else {
                // Disable is lossless: retain team colors, click events,
                // hover events, and any other vanilla Style data.
                newContent = original.copy();
                SourceHoverHelper.rememberOriginal(newContent, original);
            }

            // 构造新 ChatHudLine 替换 (保留 creationTick / signature / indicator)
            ChatHudLine replacement = new ChatHudLine(
                    old.creationTick(),
                    newContent,
                    old.signature(),
                    old.indicator()
            );
            messages.set(i, replacement);
        }

        // ChatHud renders a separate wrapped visible-message cache. Rebuild it
        // after replacing history, otherwise the screen keeps the old Text.
        accessor.richchat$refresh();
    }

    /**
     * 从已渲染 Text 反推原始 source 字符串.
     *
     * <p>路径: Text 根 Style → HoverEvent → SHOW_TEXT → value(Text) → getString().</p>
     *
     * @param rendered 已渲染的 Text (含悬停事件).
     * @return 原始 source 字符串; 反推失败时返回 rendered.getString().
     */
    private static String extractSource(Text rendered) {
        if (rendered == null) {
            return "";
        }
        Style rootStyle = rendered.getStyle();
        if (rootStyle == null) {
            return rendered.getString();
        }
        HoverEvent hover = rootStyle.getHoverEvent();
        if (hover == null || hover.getAction() != HoverEvent.Action.SHOW_TEXT) {
            return rendered.getString();
        }
        Text hoverValue = hover.getValue(HoverEvent.Action.SHOW_TEXT);
        if (hoverValue == null) {
            return rendered.getString();
        }
        return hoverValue.getString();
    }

    private static Text renderSource(Text original, String source) {
        if (source != null) {
            String[] lines = source.split("\\n", -1);
            if (lines.length >= 2 && TableRenderer.isTableSeparator(lines[1])) {
                return ClientTableRenderer.renderLive(java.util.Arrays.asList(lines));
            }
        }
        return ChatParser.parse(original);
    }

}
