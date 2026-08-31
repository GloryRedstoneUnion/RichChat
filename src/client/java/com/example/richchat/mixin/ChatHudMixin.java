package com.example.richchat.mixin;

import com.example.richchat.config.RichChatConfig;
import com.example.richchat.client.ClientTableRenderer;
import com.example.richchat.parse.ChatParser;
import com.example.richchat.parse.MultiLineBlockTracker;
import com.example.richchat.render.SourceHoverHelper;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 注入 {@link ChatHud}, 在消息入队前对其 {@link Text} 做解析与转换.
 *
 * <p>本 Mixin 拦截 {@code ChatHud.addMessage(Text, MessageSignatureData, MessageIndicator)},
 * 当 RichChat 启用时:</p>
 * <ol>
 *   <li>提取消息体 (剥离 {@code <sender> } 前缀), 交给多行块状态机判断.</li>
 *   <li>状态机返回块累积、实时表格快照、块完成或普通消息动作.</li>
 *   <li>按动作类型重新调用 addMessage 完成渲染.</li>
 * </ol>
 *
 * <p>所有重新调用 addMessage 都通过 ThreadLocal 防止无限递归.</p>
 *
 * <p>当 RichChat 关闭时, 直接放行原调用, 不做任何转换 (零性能损耗),
 * 但仍会重置多行块状态机以避免遗留状态干扰.</p>
 */
@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

    @Shadow
    public abstract void addMessage(Text message, MessageSignatureData signature, MessageIndicator indicator);

    /** 防止递归处理的重入标志. */
    @Unique
    private static final ThreadLocal<Boolean> richchat$processing = ThreadLocal.withInitial(() -> false);

    /** 多行块状态机 (聊天 HUD 全局共享). */
    @Unique
    private static final MultiLineBlockTracker richchat$blockTracker = new MultiLineBlockTracker();

    /** Current in-progress table snapshot stored in ChatHud history. */
    @Unique
    private static Text richchat$liveTableSnapshot;

    /**
     * HEAD 注入 addMessage, 在消息进入 HUD 之前完成渲染与悬停事件附加.
     */
    @Inject(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void richchat$onAddMessage(Text message, MessageSignatureData signature,
                                       MessageIndicator indicator, CallbackInfo ci) {
        // 防止递归
        if (richchat$processing.get()) {
            return;
        }
        // 关闭时跳过所有转换逻辑, 并重置状态机避免遗留状态
        if (!RichChatConfig.INSTANCE.isEnabled()) {
            richchat$blockTracker.reset();
            richchat$liveTableSnapshot = null;
            // Messages received while disabled still need to be recoverable
            // when the renderer is enabled again.
            SourceHoverHelper.rememberOriginal(message, message);
            return;
        }
        if (message == null) {
            return;
        }

        String source = message.getString();
        if (source == null || source.isEmpty()) {
            return;
        }

        // 提取消息体 (剥离 <sender> 前缀), 用于多行块判断
        String body = ChatParser.extractMessageBody(message);
        MultiLineBlockTracker.Result result = richchat$blockTracker.process(message, body);

        switch (result.type) {
            case ACCUMULATE -> {
                // 累积中, 该消息不显示
                ci.cancel();
            }
            case RENDER_BLOCK -> {
                // 代码块 / LaTeX 块闭合
                Text blockRendered = result.isLatex
                        ? ChatParser.renderMultiLineLatexBlock(result.content)
                        : ChatParser.renderMultiLineCodeBlock(result.content);
                boolean showHover = RichChatConfig.INSTANCE.isShowSourceOnHover();
                Text blockOriginal = Text.literal(result.source);
                Text finalText = SourceHoverHelper.withSourceHover(
                        blockRendered, result.source, showHover, blockOriginal);
                ci.cancel();
                richchat$readd(finalText, signature, indicator);
            }
            case RENDER_TABLE -> {
                // 表格闭合: finalize the live snapshot, then process the
                // current non-table message normally.
                boolean showHover = RichChatConfig.INSTANCE.isShowSourceOnHover();
                Text tableRendered = ClientTableRenderer.renderLive(result.tableBodies);
                String tableSource = ChatParser.buildTableSource(result.tableBodies);
                Text tableOriginal = Text.literal(tableSource);
                Text tableWithHover = SourceHoverHelper.withSourceHover(
                        tableRendered, tableSource, showHover, tableOriginal);
                ci.cancel();
                if (!richchat$replaceLiveTable(tableWithHover, tableSource)) {
                    richchat$readd(tableWithHover, signature, indicator);
                }
                richchat$liveTableSnapshot = null;
                // 当前行 (非表格行) 按 NORMAL 处理
                Text normalRendered = ChatParser.parse(message);
                Text normalWithHover = SourceHoverHelper.withSourceHover(
                        normalRendered, source, showHover, message);
                richchat$readd(normalWithHover, signature, indicator);
            }
            case RENDER_TABLE_LIVE -> {
                // Confirmed table or a newly received row: replace the one
                // existing HUD snapshot so rows appear immediately without
                // duplicating the whole table in chat history.
                boolean showHover = RichChatConfig.INSTANCE.isShowSourceOnHover();
                Text tableRendered = ClientTableRenderer.renderLive(result.tableBodies);
                String tableSource = ChatParser.buildTableSource(result.tableBodies);
                Text tableOriginal = Text.literal(tableSource);
                Text tableWithHover = SourceHoverHelper.withSourceHover(
                        tableRendered, tableSource, showHover, tableOriginal);
                ci.cancel();
                if (!richchat$replaceLiveTable(tableWithHover, tableSource)) {
                    richchat$readd(tableWithHover, signature, indicator);
                }
                richchat$liveTableSnapshot = tableWithHover;
            }
            case FALLBACK_NORMAL -> {
                // 暂存的首行不是表格, 先补显示首行, 再按 NORMAL 处理当前行
                boolean showHover = RichChatConfig.INSTANCE.isShowSourceOnHover();
                Text flushRendered = ChatParser.parse(result.flushBefore);
                String flushSource = result.flushBefore.getString();
                Text flushWithHover = SourceHoverHelper.withSourceHover(
                        flushRendered, flushSource, showHover, result.flushBefore);
                ci.cancel();
                richchat$readd(flushWithHover, signature, indicator);
                // 当前行按 NORMAL 处理
                Text normalRendered = ChatParser.parse(message);
                Text normalWithHover = SourceHoverHelper.withSourceHover(
                        normalRendered, source, showHover, message);
                richchat$readd(normalWithHover, signature, indicator);
            }
            case NORMAL -> {
                // 正常渲染
                Text transformed = ChatParser.parse(message);
                boolean showHover = RichChatConfig.INSTANCE.isShowSourceOnHover();
                Text finalText = SourceHoverHelper.withSourceHover(
                        transformed, source, showHover, message);
                ci.cancel();
                richchat$readd(finalText, signature, indicator);
            }
        }
    }

    /**
     * 重新调用 addMessage (设置 ThreadLocal 防递归).
     */
    @Unique
    private void richchat$readd(Text message, MessageSignatureData signature, MessageIndicator indicator) {
        richchat$processing.set(true);
        try {
            this.addMessage(message, signature, indicator);
        } finally {
            richchat$processing.remove();
        }
    }

    /** Replace the current table snapshot in history and rebuild wrapped lines. */
    @Unique
    private boolean richchat$replaceLiveTable(Text replacement, String source) {
        if (richchat$liveTableSnapshot == null) {
            return false;
        }
        ChatHudAccessor accessor = (ChatHudAccessor) (Object) this;
        List<ChatHudLine> messages = accessor.richchat$getMessages();
        for (int i = 0; i < messages.size(); i++) {
            ChatHudLine old = messages.get(i);
            Text content = old.content();
            if (content != richchat$liveTableSnapshot
                    && !richchat$isPreviousTableSnapshot(content, source)) {
                continue;
            }
            messages.set(i, new ChatHudLine(
                    old.creationTick(), replacement, old.signature(), old.indicator()));
            accessor.richchat$refresh();
            return true;
        }
        return false;
    }

    @Unique
    private static boolean richchat$isPreviousTableSnapshot(Text content, String source) {
        Text original = SourceHoverHelper.getOriginal(content);
        if (original == null) {
            return false;
        }
        String previousSource = original.getString();
        if (!(source.equals(previousSource) || source.startsWith(previousSource + "\n"))) {
            return false;
        }
        String[] lines = previousSource.split("\n", -1);
        return lines.length >= 2 && com.example.richchat.parse.TableRenderer.isTableSeparator(lines[1]);
    }
}
