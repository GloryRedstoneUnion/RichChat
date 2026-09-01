# RichChat

RichChat is a client-side Fabric mod for Minecraft 1.20.1 that renders Markdown and LaTeX-inspired syntax directly in the vanilla chat HUD. Rendered messages retain their original source for hover inspection, and existing chat history can be re-rendered when the feature is toggled or its colors change.

Latest release: [RichChat 1.2.1](https://github.com/GloryRedstoneUnion/RichChat/releases/tag/v1.2.1)

## Features

- Markdown styles: bold, italic, strikethrough, inline code, links, headings, lists, quotes, fenced code blocks, and tables.
- LaTeX-to-Unicode conversion: scripts, roots, fractions, Greek letters, mathematical symbols, and common operators.
- VS Code-inspired semantic colors, configurable with strict `#RRGGBB` values.
- Source hover: inspect the original unrendered message without losing vanilla styles or interactions.
- Live tables: the table appears as soon as its separator row confirms the syntax, and later rows update the same HUD entry.
- Pixel-aligned table layout using Minecraft's actual client font metrics.
- Multi-message fenced code and LaTeX blocks, including stack-based nested code fences.
- Client-side commands with category and hexadecimal color suggestions.
- Lossless toggling: team colors, click events, hover events, and chat prefixes are preserved.

## Requirements

| Dependency | Version |
| --- | --- |
| Minecraft | 1.20.1 |
| Fabric Loader | 0.15.11 or newer |
| Fabric API | 0.92.2+1.20.1 |
| Java | 17 |
| Yarn mappings | 1.20.1+build.10 |
| Fabric Loom | 1.6 |

## Installation

1. Download `RichChat-1.2.1.jar` from the [v1.2.1 release](https://github.com/GloryRedstoneUnion/RichChat/releases/tag/v1.2.1).
2. Place the JAR in `.minecraft/mods/`.
3. Install Fabric API `0.92.2+1.20.1` in the same directory.
4. Start Minecraft 1.20.1 with Fabric Loader.

## Usage

Send Markdown or LaTeX syntax as a normal chat message.

### Markdown

```text
**bold** *italic* ~~strikethrough~~ `inline code`
[Minecraft](https://minecraft.net)
# Heading
- List item
> Quote
```

Fenced code blocks can span multiple chat messages:

````text
```python
def greet(name):
    return f"Hello, {name}!"
```
````

### LaTeX

```text
$x^2 + \frac{1}{2}$
$\sqrt{x}$
$\alpha + \beta = \gamma$
$D = \max(0, h - 3)$
```

Representative output:

| Source | Output |
| --- | --- |
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
| `\partial` | `∂` |
| `\nabla` | `∇` |

Block formulas can also span messages:

```text
$$
\sum_{i=1}^{n} \frac{1}{i^2} = \frac{\pi^2}{6}
$$
```

### Tables

```text
| Function | Integral | Field |
| :---: | :---: | :---: |
| $e^{-x^2}$ | $\sqrt{\pi}$ | Probability |
| $x^2 e^{-x}$ | $2$ | Quantum mechanics |
```

The first row is held briefly until the separator row confirms that the input is a Markdown table. The header then appears immediately, and each data row replaces the same in-progress HUD snapshot. The rendered layout omits outer top and bottom borders. Its header separator and content rows use the same vertical glyph, while column padding is calculated from Minecraft's client-side pixel measurements.

Markdown and supported LaTeX syntax are rendered inside table cells. Escaped pipes such as `left\|right` remain cell content.

### Commands

| Command | Description |
| --- | --- |
| `/richchat toggle` | Enable or disable rendering and refresh existing messages. |
| `/richchat status` | Show the current rendering and source-hover state. |
| `/richchat reload` | Reload the JSON configuration. |
| `/richchat color` | List all current semantic colors. |
| `/richchat color <category> <#RRGGBB>` | Set a color and refresh chat immediately. |
| `/richchat color reset <category\|all>` | Restore one category or all categories to defaults. |

Available categories:

```text
plain bold italic strikethrough list inlineCode codeBlock link
heading1 heading2 heading3 heading4 heading5 heading6
quote latex tableHeader tableBody
```

Commands are registered on the client and do not require server permissions. Invalid categories and color values are rejected before execution, and Brigadier provides suggestions while typing.

## Configuration

RichChat stores its settings in `.minecraft/config/richchat.json`:

```json
{
  "enabled": true,
  "showSourceOnHover": true,
  "colors": {
    "plain": "#D4D4D4",
    "bold": "#FFFFFF",
    "italic": "#D4D4D4",
    "strikethrough": "#808080",
    "list": "#D4D4D4",
    "inlineCode": "#CE9178",
    "codeBlock": "#CE9178",
    "link": "#3794FF",
    "heading1": "#569CD6",
    "heading2": "#4EC9B0",
    "heading3": "#DCDCAA",
    "heading4": "#C586C0",
    "heading5": "#9CDCFE",
    "heading6": "#CE9178",
    "quote": "#6A9955",
    "latex": "#B5CEA8",
    "tableHeader": "#4EC9B0",
    "tableBody": "#D4D4D4"
  }
}
```

Colors must use exactly six hexadecimal digits after `#`. Lowercase values are normalized to uppercase. Invalid values fall back to defaults, and older configuration files receive any missing color entries automatically.

## Rendering Behavior

### Chat prefixes and styles

Vanilla chat commonly uses `TranslatableText(chat.type.text, sender, message)`. RichChat extracts the message argument directly instead of parsing the flattened display string, so sender prefixes are preserved and a `>` in `<sender>` is not mistaken for a Markdown quote. It also recognizes common server layouts that add channel or team prefixes or flatten the component into literal text.

The parser retains each original `Style`, including team colors, click events, hover events, and fonts. The unmodified component tree is associated with its rendered HUD entry so disabling and re-enabling rendering remains lossless.

### Multi-message state

| Block | Start | End | Display behavior |
| --- | --- | --- | --- |
| Fenced code | `` ```lang `` | `` ``` `` | Buffered until the matching fence closes. |
| LaTeX block | `$$` | `$$` | Buffered until the closing delimiter arrives. |
| Markdown table | Header plus separator | First non-table row | Displayed and updated live after confirmation. |

Nested code fences use a stack: a language fence opens a level, and a bare fence closes the current level. An unmatched fence or malformed formula degrades to source text instead of silently dropping content.

### Performance

- A `ThreadLocal` re-entry guard prevents recursive processing when the mixin adds transformed messages back to `ChatHud`.
- Rendering is bypassed while disabled.
- The block tracker resets when rendering is disabled.
- Existing messages are refreshed in place instead of being resent through the parser pipeline.

## Architecture

```text
com.example.richchat
|-- RichChatMod
|-- config
|   |-- RichChatConfig
|   `-- RichChatColors
|-- parse
|   |-- ChatParser
|   |-- MarkdownRenderer
|   |-- LatexUnicodeRenderer
|   |-- TableRenderer
|   `-- MultiLineBlockTracker
|-- render
|   `-- SourceHoverHelper
`-- client
    |-- RichChatClient
    |-- ChatRefreshManager
    |-- ClientTableRenderer
    |-- command
    |   `-- RichChatCommand
    `-- mixin
        |-- ChatHudMixin
        `-- ChatHudAccessor
```

`TableRenderer` owns table parsing and semantic layout. `ClientTableRenderer` supplies real `TextRenderer` measurements and exact one-pixel spacing glyphs. `ChatHudMixin` owns streaming table snapshots and message ingress, while `ChatRefreshManager` reconstructs displayed history after configuration changes.

## Building and Testing

The build uses Java 17 and the checked-in Gradle wrapper:

```bash
./gradlew test --no-daemon
./gradlew build --no-daemon
```

Artifacts are written to:

```text
build/libs/RichChat-1.2.1.jar
build/libs/RichChat-1.2.1-sources.jar
```

The Gradle configuration includes Aliyun, Tencent Cloud, Fabric, Maven Central, and BMCLAPI endpoints to improve dependency availability in mainland China.

## Release History

### 1.2.1

- Corrected the final pixel-level drift in Markdown table borders.
- Aligned the header separator with content rows by using the same vertical glyph.
- Displayed confirmed tables immediately and updated subsequent rows in place.
- Removed the outer top and bottom table borders.

### 1.2.0

- Reworked Markdown table parsing to hide separator syntax and produce a structured chat layout.
- Added Markdown and LaTeX semantic colors inside table cells and client-side pixel width measurement.
- Preserved malformed table input as source text.
- Fixed rendering for messages with channel, team, nested component, and flattened literal prefixes.
- Fixed stale rendered content after `/richchat toggle` and preserved prefixes when rendering was re-enabled.

### 1.1.0

- Added VS Code-inspired default colors, distinct heading colors, color commands, and suggestions.
- Improved Markdown fences, escapes, nested spans, links, and cross-style parsing.
- Improved LaTeX delimiters, malformed-input handling, common operators, fractions, roots, and scripts.
- Added Unicode-aware table width handling for CJK, full-width characters, combining marks, and emoji.

### 1.0.0

- Added client-side Markdown and LaTeX rendering for Minecraft 1.20.1.
- Added source hover, multi-message blocks, nested code fences, tables, client commands, and persistent configuration.

## Known Limitations

- LaTeX is converted to a readable Unicode approximation; it is not full mathematical typesetting.
- Matrices and unsupported complex formulas remain source text.
- Fenced code and `$$` blocks are buffered until their closing delimiter arrives.
- The first table candidate row is buffered until the following separator row confirms table syntax.
- Very wide tables can still wrap at the configured Minecraft chat width.

## License

RichChat is available under the [MIT License](LICENSE).

## Acknowledgements

- [FabricMC](https://fabricmc.net/) for Fabric Loader and Fabric API.
- [Yarn mappings](https://github.com/FabricMC/yarn) for readable Minecraft mappings.
- [BMCLAPI](https://bmclapi2.bangbang93.com/) for mirrored Minecraft metadata and assets.
- [Aliyun Maven](https://maven.aliyun.com/) and Tencent Cloud mirrors for dependency availability.
