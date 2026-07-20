package com.example.richchat.client;

import com.example.richchat.RichChatMod;
import com.example.richchat.command.RichChatCommand;
import com.example.richchat.config.RichChatConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

/**
 * RichChat 客户端入口.
 *
 * <p>负责:</p>
 * <ol>
 *   <li>加载配置文件 ({@code config/richchat.json}).</li>
 *   <li>注册客户端命令 ({@code /richchat toggle|status|reload}).</li>
 * </ol>
 */
public class RichChatClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 加载配置
        RichChatConfig.INSTANCE.load();

        // 注册客户端命令 (通过回调, 获取活跃 dispatcher)
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                RichChatCommand.register(dispatcher));

        RichChatMod.LOGGER.info("[RichChat] 客户端初始化完成.");
    }
}
