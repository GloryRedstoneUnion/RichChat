package com.example.richchat.command;

import com.example.richchat.RichChatMod;
import com.example.richchat.client.ChatRefreshManager;
import com.example.richchat.config.RichChatConfig;
import com.example.richchat.config.RichChatColors;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.mojang.brigadier.arguments.StringArgumentType;

/**
 * 客户端命令 {@code /richchat} 注册与处理.
 *
 * <p>子命令:</p>
 * <ul>
 *   <li>{@code /richchat toggle} —— 开关渲染功能.</li>
 *   <li>{@code /richchat status} —— 查看当前状态.</li>
 *   <li>{@code /richchat reload} —— 重载配置文件.</li>
 * </ul>
 *
 * <p>所有子命令均为客户端命令, 无需服务端权限.</p>
 */
public final class RichChatCommand {

    /** 命令根名. */
    public static final String ROOT = "richchat";
    public static final String SUB_TOGGLE = "toggle";
    public static final String SUB_STATUS = "status";
    public static final String SUB_RELOAD = "reload";
    public static final String SUB_COLOR = "color";

    private RichChatCommand() {
    }

    /**
     * 注册客户端命令到指定的 dispatcher.
     *
     * <p>通过 {@code ClientCommandRegistrationCallback.EVENT} 调用, 传入活跃的 dispatcher.</p>
     *
     * @param dispatcher 客户端命令 dispatcher.
     */
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralCommandNode<FabricClientCommandSource> root = dispatcher.register(
                ClientCommandManager.literal(ROOT)
                        .executes(RichChatCommand::status)
                        .then(ClientCommandManager.literal(SUB_TOGGLE)
                                .executes(RichChatCommand::toggle))
                        .then(ClientCommandManager.literal(SUB_STATUS)
                                .executes(RichChatCommand::status))
                        .then(ClientCommandManager.literal(SUB_RELOAD)
                                .executes(RichChatCommand::reload))
                        .then(ClientCommandManager.literal(SUB_COLOR)
                                .executes(RichChatCommand::colors)
                                .then(ClientCommandManager.literal("reset")
                                        .then(ClientCommandManager.argument("category", StringArgumentType.word())
                                                .executes(RichChatCommand::resetColor)))
                                .then(ClientCommandManager.argument("category", StringArgumentType.word())
                                        .then(ClientCommandManager.argument("value", StringArgumentType.word())
                                                .executes(RichChatCommand::setColor))))
        );
        RichChatMod.LOGGER.info("[RichChat] 命令 /{} 已注册.", ROOT);
    }

    /**
     * 切换渲染开关.
     */
    private static int toggle(CommandContext<FabricClientCommandSource> ctx) {
        RichChatConfig cfg = RichChatConfig.INSTANCE;
        boolean newState = !cfg.isEnabled();
        cfg.setEnabled(newState);
        cfg.save();
        // 刷新已存在消息, 让用户立即看到效果变化
        ChatRefreshManager.refreshChatHud();
        Text msg = Text.literal("[RichChat] 渲染: ")
                .formatted(Formatting.AQUA)
                .append(Text.literal(newState ? "ON" : "OFF")
                        .formatted(newState ? Formatting.GREEN : Formatting.RED));
        ctx.getSource().sendFeedback(msg);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * 显示当前状态.
     */
    private static int status(CommandContext<FabricClientCommandSource> ctx) {
        RichChatConfig cfg = RichChatConfig.INSTANCE;
        Text msg = Text.literal("[RichChat] 状态:")
                .formatted(Formatting.AQUA)
                .append(Text.literal("\n  渲染: ").formatted(Formatting.WHITE))
                .append(Text.literal(cfg.isEnabled() ? "ON" : "OFF")
                        .formatted(cfg.isEnabled() ? Formatting.GREEN : Formatting.RED))
                .append(Text.literal("\n  悬停显示源码: ").formatted(Formatting.WHITE))
                .append(Text.literal(cfg.isShowSourceOnHover() ? "ON" : "OFF")
                        .formatted(cfg.isShowSourceOnHover() ? Formatting.GREEN : Formatting.RED));
        ctx.getSource().sendFeedback(msg);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * 重载配置文件.
     */
    private static int reload(CommandContext<FabricClientCommandSource> ctx) {
        RichChatConfig.INSTANCE.load();
        // 配置变化后刷新已存在消息
        ChatRefreshManager.refreshChatHud();
        Text msg = Text.literal("[RichChat] 配置已重载.")
                .formatted(Formatting.GREEN);
        ctx.getSource().sendFeedback(msg);
        return Command.SINGLE_SUCCESS;
    }

    private static int colors(CommandContext<FabricClientCommandSource> ctx) {
        RichChatConfig cfg = RichChatConfig.INSTANCE;
        Text msg = Text.literal("[RichChat] 颜色:").formatted(Formatting.AQUA);
        for (String category : RichChatColors.CATEGORIES) {
            msg = msg.copy().append(Text.literal("\n  " + category + ": " + cfg.getColorHex(category))
                    .formatted(Formatting.WHITE));
        }
        ctx.getSource().sendFeedback(msg);
        return Command.SINGLE_SUCCESS;
    }

    private static int setColor(CommandContext<FabricClientCommandSource> ctx) {
        String category = StringArgumentType.getString(ctx, "category");
        String value = StringArgumentType.getString(ctx, "value");
        RichChatConfig cfg = RichChatConfig.INSTANCE;
        if (!cfg.setColorHex(category, value)) {
            ctx.getSource().sendError(Text.literal("用法: /richchat color <category> <#RRGGBB>"));
            return 0;
        }
        cfg.save();
        ChatRefreshManager.refreshChatHud();
        ctx.getSource().sendFeedback(Text.literal("[RichChat] 已设置 " + category + " = " + cfg.getColorHex(category))
                .formatted(Formatting.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int resetColor(CommandContext<FabricClientCommandSource> ctx) {
        String category = StringArgumentType.getString(ctx, "category");
        RichChatConfig cfg = RichChatConfig.INSTANCE;
        if ("all".equalsIgnoreCase(category)) {
            for (String name : RichChatColors.CATEGORIES) cfg.resetColor(name);
        } else if (!cfg.resetColor(category)) {
            ctx.getSource().sendError(Text.literal("未知颜色类别: " + category));
            return 0;
        }
        cfg.save();
        ChatRefreshManager.refreshChatHud();
        ctx.getSource().sendFeedback(Text.literal("[RichChat] 颜色已重置: " + category)
                .formatted(Formatting.GREEN));
        return Command.SINGLE_SUCCESS;
    }
}
