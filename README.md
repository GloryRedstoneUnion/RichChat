# RichChat

> Minecraft Fabric 模组 —— 在聊天栏中渲染 **Markdown** 与 **LaTeX**，鼠标悬停查看原始源码。

## 项目简介

RichChat 是一个客户端 Fabric 模组，它把玩家发送的聊天消息中的 Markdown 标记和 LaTeX 公式实时渲染为 Minecraft 原生 Text 样式，同时保留原始源码供鼠标悬停查看。让聊天栏也能像富文本编辑器一样显示精美格式。

### 核心特性

- **Markdown 渲染**：粗体、斜体、删除线、行内代码、链接、标题、列表、引用、表格、代码块
- **LaTeX 渲染**：将常见 LaTeX 公式转为 Unicode 近似字符（上下标、希腊字母、数学符号等）
- **悬停看源码**：对每条已渲染消息悬停时显示其原始未渲染文本
- **多行块支持**：跨消息累积多行代码块、多行 LaTeX 块（`$$...$$`）、Markdown 表格
- **嵌套代码块**：外层 ```` ```markdown ```` 代码块内可包含 ```` ```python ```` 内层代码块，正确匹配闭合
- **客户端命令**：`/richchat toggle|status|reload`，状态持久化到配置文件
- **即时刷新**：切换开关时已存在的消息立即重新渲染或还原为源码
- **零开销**：关闭时跳过所有转换逻辑，无性能损耗

## 环境要求

| 依赖 | 版本 |
|------|------|
| Minecraft | 1.20.1 |
| Fabric Loader | ≥ 0.15.11 |
| Fabric API | 0.92.2+1.20.1 |
| Java | 17 |
| Yarn Mappings | 1.20.1+build.10 |
| Loom Plugin | 1.6-SNAPSHOT |

## 安装

### 方式一：使用预构建 jar

1. 下载 `RichChat-1.0.0.jar`
2. 将其放入 `.minecraft/mods/` 目录
3. 确保 `fabric-api-0.92.2+1.20.1.jar` 也已在 mods 目录
4. 启动 Minecraft 1.20.1（Fabric Loader）

### 方式二：从源码构建

```bash
git clone <repo-url>
cd RichChat
./gradlew build
# 产物: build/libs/RichChat-1.0.0.jar
```

## 使用方法

### 聊天栏渲染

直接在聊天栏发送含 Markdown 或 LaTeX 标记的消息：

```
**粗体** *斜体* ~~删除~~ `代码`
```

支持 LaTeX：

```
$x^2 + \frac{1}{2}$        → x² + 1/2
$\sqrt{x}$                 → √x
$\alpha + \beta = \gamma$  → α + β = γ
```

支持多行代码块：

````
```python
def greet(name):
    return f"Hello, {name}!"
```
````

支持多行公式块：

```
$$
\sum_{i=1}^{n} \frac{1}{i^2} = \frac{\pi^2}{6}
$$
```

支持 Markdown 表格：

```
| 函数 | 积分结果 | 应用领域 |
|:---:|:---:|:---:|
| $e^{-x^2}$ | $\sqrt{\pi}$ | 概率论 |
| $x^2 e^{-x}$ | $2$ | 量子力学 |
```

> 表格单元格内的 LaTeX 也会被渲染。

### 鼠标悬停

将鼠标悬停在任何已渲染的消息上，会显示该消息的原始未渲染源码（带标记的原文），便于查看格式细节。

### 命令系统

| 命令 | 说明 |
|------|------|
| `/richchat toggle` | 开关渲染功能（已存在消息会即时刷新） |
| `/richchat status` | 查看当前渲染与悬停状态 |
| `/richchat reload` | 重新加载配置文件 |

命令仅注册在客户端，无需服务端权限。

### 配置文件

配置文件位于 `.minecraft/config/richchat.json`：

```json
{
  "enabled": true,
  "showSourceOnHover": true
}
```

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | bool | `true` | 是否启用渲染 |
| `showSourceOnHover` | bool | `true` | 是否在悬停时显示原始源码 |

配置在游戏重启后保留。

## 功能详解

### Markdown 支持范围

| 语法 | 渲染效果 |
|------|---------|
| `**text**` | **粗体** |
| `*text*` | *斜体* |
| `~~text~~` | ~~删除线~~ |
| `` `code` `` | 深灰色等宽代码 |
| `[text](url)` | 可点击链接 + 下划线 |
| `# 标题` | 蓝色粗体 |
| `## 标题` | 绿色粗体 |
| `### 标题` | 黄色粗体 |
| `- item` | • 前缀列表 |
| `> quote` | 灰色缩进引用 |
| `` ```lang ... ``` `` | 多行代码块 |
| `\| table \|` | 对齐表格 |

### LaTeX 支持范围

**定界符**：
- 行内公式：`$...$`
- 块级公式：`$$...$$`
- 支持 `\$` 转义

**转换规则**：

| LaTeX | Unicode |
|-------|---------|
| `x^2` | `x²` |
| `x_1` | `x₁` |
| `\sqrt{x}` | `√x` |
| `\sqrt[n]{x}` | `ⁿ√x` |
| `\frac{a}{b}` | `a/b` |
| `\alpha` | `α` |
| `\beta` | `β` |
| `\gamma` | `γ` |
| `\pi` | `π` |
| `\theta` | `θ` |
| `\infty` | `∞` |
| `\sum` | `Σ` |
| `\int` | `∫` |
| `\times` | `×` |
| `\pm` | `±` |
| `\leq` | `≤` |
| `\geq` | `≥` |
| `\cdot` | `·` |
| `\partial` | `∂` |
| `\nabla` | `∇` |
| `\propto` | `∝` |

复杂公式（矩阵、多行公式等）保留原始文本。

### 多行块状态机

由于 vanilla 聊天每行都是独立消息，RichChat 维护了一个跨消息状态机来识别多行块：

| 块类型 | 开始标记 | 结束标记 | 备注 |
|--------|---------|---------|------|
| 代码块 | `` ```lang `` | `` ``` `` | 支持嵌套，按栈匹配 |
| LaTeX 块 | `$$` | `$$` | 跨消息累积 |
| 表格 | 表头行 + 分隔行 | 非表格行 | 表头 + 分隔行 + 数据行 |

表格识别使用 look-ahead：首行暂存，第二行若是分隔行（如 `|:---:|`）才确认进入表格模式；否则把暂存的首行作为普通消息补显示。

### 性能优化

- **ThreadLocal 重入保护**：避免 Mixin 重新调用 `addMessage` 导致无限递归
- **关闭时零开销**：`enabled=false` 时 Mixin 直接放行原调用，不做任何字符串处理
- **状态机重置**：关闭渲染时同步重置多行块状态机，避免遗留状态干扰

## 技术架构

### 模块结构

```
com.example.richchat
├── RichChatMod                      # 模组入口
├── config
│   └── RichChatConfig               # 配置读写 (Gson)
├── parse
│   ├── ChatParser                   # 消息解析主入口
│   ├── MarkdownRenderer             # Markdown → Text
│   ├── LatexUnicodeRenderer         # LaTeX → Unicode
│   ├── TableRenderer                # 表格 → 对齐 Text
│   └── MultiLineBlockTracker        # 多行块状态机
├── render
│   └── SourceHoverHelper            # 悬停事件附加
└── client
    ├── RichChatClient               # 客户端入口
    ├── ChatRefreshManager           # 切换开关时刷新消息
    ├── command
    │   └── RichChatCommand          # /richchat 命令
    └── mixin
        ├── ChatHudMixin             # 注入 addMessage
        └── ChatHudAccessor          # 暴露 messages 字段
```

### 关键设计

**聊天前缀识别**：vanilla 聊天消息是 `TranslatableText(chat.type.text, sender, message)`，RichChat 直接从 `getArgs()` 取 sender 和 message，避免解析字符串。`<sender> ` 前缀原样拼接，不参与 Markdown 渲染，因此 `>` 不会被误识别为引用标记。

**样式保留**：用 `Text.visit(StyledVisitor, Style)` 展开 TranslatableText，对每个字符段保留其原始 Style（含 team 染色），渲染后的子节点继承该样式。

**悬停源码反推**：渲染时把原始 source 字符串放进 `HoverEvent(SHOW_TEXT, Text.literal(source))`。切换开关时通过 `content().getStyle().getHoverEvent().getValue(SHOW_TEXT).getString()` 反推 source，重新渲染替换。

**嵌套代码块**：用 `Deque<String> codeFenceStack` 记录嵌套层级。```` ```lang ```` 入栈，```` ``` ```` 出栈，栈空时才真正闭合。这样外层 ```` ```markdown ```` 内的 ```` ```python ... ``` ```` 不会误闭合外层。

## 构建配置

### 国内镜像源

`settings.gradle` 与 `build.gradle` 中已配置国内镜像，首次构建无需科学上网：

| 镜像 | 用途 |
|------|------|
| 阿里云 Maven | 主依赖仓库 |
| 阿里云 Google 镜像 | Google 仓库 |
| 腾讯云 Maven | 备选仓库 |
| BMCLAPI | Minecraft 版本清单与资产 |
| 腾讯云 Gradle | Gradle Wrapper 下载 |

`build.gradle` 中 Loom 配置：

```groovy
loom {
    splitEnvironmentSourceSets()
    customMinecraftManifest = "https://bmclapi2.bangbang93.com/version/1.20.1/json"
    mods {
        "richchat" {
            sourceSet sourceSets.main
            sourceSet sourceSets.client
        }
    }
}
```

### JDK 配置

`gradle.properties` 中显式指定 JDK 17 路径（如系统未自动识别）：

```properties
org.gradle.java.home=C:/Program Files/Java/jdk-17
```

## 测试用例

### 功能测试

| 输入 | 预期输出 |
|------|---------|
| `**粗体** *斜体* \`代码\`` | 粗体 + 斜体 + 深灰代码 |
| `$x^2 + \frac{1}{2}$` | `x² + 1/2` |
| `$$\sum_{i=1}^{n} \frac{1}{i^2}$$` | `Σ` 块级公式 |
| `` ```python\ndef greet():\n    pass\n``` `` | 多行深灰代码块 |
| `\| a \| b \|\n\|:---:\|\n\| 1 \| 2 \|` | 对齐表格 |
| `\sqrt[n]{x}` | `ⁿ√x` |
| `<player> > hello` | `>` 不被吞（聊天前缀识别） |

### 边界测试

| 输入 | 预期 |
|------|------|
| 空消息 | 不报错 |
| `**未闭合` | 优雅降级为普通文本 |
| `**粗体 *粗斜* **` | 正确嵌套渲染 |
| `\$\$not a formula\$\$` | 转义后原样输出 |
| 50 条连续消息 | 无明显卡顿 |

## 许可证

MIT License。详见 [LICENSE](file:///d:/MinecraftDev/Project/RichChat/LICENSE)。

## 已知限制

- LaTeX 仅支持基础符号转 Unicode，复杂公式（矩阵、多行公式）保留原文
- 多行块跨消息累积时，期间所有消息都会被吞掉直到块闭合
- 表格 look-ahead 暂存的首行若不是表格，会以普通消息补显示（顺序正确）
- 切换开关时刷新消息会丢失原 TranslatableText 结构，反推的 source 是字符串形式

## 致谢

- [FabricMC](https://fabricmc.net/) —— Fabric 模组加载器与 API
- [Yarn Mappings](https://github.com/FabricMC/yarn) —— Minecraft 反混淆映射
- [BMCLAPI](https://bmclapi2.bangbang93.com/) —— 国内 Minecraft 资产镜像
- [阿里云 Maven](https://maven.aliyun.com/) —— 国内 Maven 镜像
