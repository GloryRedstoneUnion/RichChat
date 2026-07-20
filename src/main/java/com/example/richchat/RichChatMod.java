package com.example.richchat;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RichChat 模组入口.
 *
 * <p>RichChat 是一个 Fabric 客户端模组, 在聊天栏中渲染 Markdown 与 LaTeX,
 * 并通过 HoverEvent 显示原始源码文本.</p>
 *
 * <p>本类作为 {@link ModInitializer} 的主入口, 仅负责日志初始化.
 * 实际客户端逻辑由 {@code com.example.richchat.client.RichChatClient} 完成.</p>
 */
public class RichChatMod implements ModInitializer {
    /** 模组 ID. */
    public static final String MOD_ID = "richchat";

    /** 模组日志器. */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[RichChat] 模组初始化 (主入口).");
    }
}
