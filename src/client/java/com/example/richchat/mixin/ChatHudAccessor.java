package com.example.richchat.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/**
 * {@link ChatHud} 字段访问器 Mixin.
 *
 * <p>暴露私有字段 {@code messages} (List&lt;ChatHudLine&gt;), 用于在切换渲染开关时
 * 遍历所有已存在消息重新渲染.</p>
 *
 * <p>1.20.1 ChatHud 的字段:</p>
 * <ul>
 *   <li>{@code private final List<ChatHudLine> messages} —— 聊天历史 (含全部消息).</li>
 * </ul>
 */
@Mixin(ChatHud.class)
public interface ChatHudAccessor {

    /**
     * 获取 messages 字段 (聊天历史列表).
     *
     * @return 聊天历史列表引用 (修改会影响 ChatHud 内部状态).
     */
    @Accessor("messages")
    List<ChatHudLine> richchat$getMessages();

    /** Rebuild ChatHud's wrapped visible-message cache after history changes. */
    @Invoker("refresh")
    void richchat$refresh();
}
