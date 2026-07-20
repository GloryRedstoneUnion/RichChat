package com.example.richchat.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.example.richchat.RichChatMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * RichChat 配置文件读写.
 *
 * <p>配置文件位于 {@code config/richchat.json}, 包含以下字段:</p>
 * <ul>
 *   <li>{@code enabled} —— 是否启用渲染功能 (默认 true).</li>
 *   <li>{@code showSourceOnHover} —— 是否在鼠标悬停时显示原始源码 (默认 true).</li>
 * </ul>
 *
 * <p>配置以单例 ({@link #INSTANCE}) 形式持有, 修改后调用 {@link #save()} 持久化.</p>
 */
public class RichChatConfig {

    /** 全局单例. 在客户端初始化时加载. */
    public static final RichChatConfig INSTANCE = new RichChatConfig();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("richchat.json");

    /** 是否启用渲染. */
    private boolean enabled = true;

    /** 是否在悬停时显示原始源码. */
    private boolean showSourceOnHover = true;

    private RichChatConfig() {
    }

    /**
     * 从磁盘加载配置. 文件不存在或解析失败时使用默认值.
     */
    public void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
                RichChatConfig loaded = GSON.fromJson(json, RichChatConfig.class);
                if (loaded != null) {
                    this.enabled = loaded.enabled;
                    this.showSourceOnHover = loaded.showSourceOnHover;
                }
            }
        } catch (Exception e) {
            RichChatMod.LOGGER.warn("[RichChat] 加载配置失败, 使用默认值: {}", e.getMessage());
        }
        RichChatMod.LOGGER.info("[RichChat] 配置: enabled={}, showSourceOnHover={}",
                this.enabled, this.showSourceOnHover);
    }

    /**
     * 将当前配置写入磁盘.
     */
    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            String json = GSON.toJson(this);
            Files.writeString(CONFIG_PATH, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            RichChatMod.LOGGER.error("[RichChat] 保存配置失败: {}", e.getMessage());
        }
    }

    /**
     * @return 是否启用渲染.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled 是否启用渲染.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return 是否在悬停时显示原始源码.
     */
    public boolean isShowSourceOnHover() {
        return showSourceOnHover;
    }

    /**
     * @param showSourceOnHover 是否在悬停时显示原始源码.
     */
    public void setShowSourceOnHover(boolean showSourceOnHover) {
        this.showSourceOnHover = showSourceOnHover;
    }
}
