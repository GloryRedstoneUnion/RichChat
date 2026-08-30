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
 *   <li>对每条 ChatHudLine 的 {@code content()} (已渲染 Text):
 *       <ul>
 *         <li>反推原始 source: 从根 Style 的 HoverEvent(SHOW_TEXT) 取出 value Text,
 *             再取其字符串. 该字符串即为渲染时存入的原始源码.</li>
 *         <li>若反推失败 (无 HoverEvent): 用 content().getString() 作为 source.</li>
 *       </ul>
 *   </li>
 *   <li>根据当前 enabled 状态:
 *       <ul>
 *         <li>enabled=true → 重新走 ChatParser.parse(source) + 悬停.</li>
 *         <li>enabled=false → 直接 literal(source), 无悬停.</li>
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

            // 反推原始 source
            String source = extractSource(oldContent);

            // 重新渲染
            Text newContent;
            if (enabled) {
                // 直接走字符串渲染 (不识别聊天前缀, 因为刷新时已经丢失了原 TranslatableText 结构)
                // 但保留原始 source 的字符串渲染结果
                newContent = renderSource(source);
                newContent = SourceHoverHelper.withSourceHover(newContent, source, showHover);
            } else {
                // 关闭: 显示原始 source, 不渲染, 不悬停
                newContent = Text.literal(source);
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

    private static Text renderSource(String source) {
        if (source != null) {
            String[] lines = source.split("\\n", -1);
            if (lines.length >= 2 && TableRenderer.isTableSeparator(lines[1])) {
                return ClientTableRenderer.render(java.util.Arrays.asList(lines));
            }
        }
        return ChatParser.renderSource(source);
    }
}
