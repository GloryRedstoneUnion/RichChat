package com.example.richchat.parse;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import com.example.richchat.config.RichChatConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Markdown 表格 → Minecraft {@link Text} 渲染器.
 *
 * <p>输入为表格的多行文本 (已剥离聊天前缀), 至少包含:</p>
 * <ol>
 *   <li>表头行: {@code | h1 | h2 | h3 |}</li>
 *   <li>对齐行: {@code |:---:|:---|---:|}</li>
 *   <li>数据行: {@code | a | b | c |}</li>
 * </ol>
 *
 * <p>渲染输出为对齐的 ASCII 表格:</p>
 * <pre>
 * | h1 | h2 | h3 |
 * |:--:|:---|--:|
 * | a  | b  | c  |
 * </pre>
 *
 * <p>表头加粗, 数据行按对齐方式 (左 / 中 / 右) 填充空格. 列宽取该列所有单元格
 * 的最大长度.</p>
 *
 * <p><b>单元格内的 Markdown / LaTeX:</b> 每个单元格内容会先经过
 * {@link ChatParser#renderSource(String)} 渲染 (LaTeX → Unicode 近似 + Markdown → Text),
 * 然后再做对齐填充. 对齐宽度按渲染后的可见字符长度计算.</p>
 */
public final class TableRenderer {

    /** 单元格对齐方式. */
    private enum Alignment { LEFT, CENTER, RIGHT }

    private TableRenderer() {
    }

    /**
     * 渲染表格行列表为对齐的 Text.
     *
     * @param lines 表格行 (含表头 / 对齐行 / 数据行).
     * @return 渲染后的 Text; 行数不足 2 时按普通文本换行输出.
     */
    public static Text render(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return Text.empty();
        }
        if (lines.size() < 2) {
            MutableText result = Text.empty();
            for (int i = 0; i < lines.size(); i++) {
                if (i > 0) result.append(Text.literal("\n"));
                result.append(Text.literal(lines.get(i)));
            }
            return result;
        }

        // 1. 解析每行为 cells (字符串形式)
        List<String[]> rows = new ArrayList<>();
        int maxCols = 0;
        for (String line : lines) {
            String[] cells = splitRow(line);
            rows.add(cells);
            maxCols = Math.max(maxCols, cells.length);
        }

        // 2. 对齐方式 (从第二行, 即分隔行解析)
        Alignment[] aligns = new Alignment[maxCols];
        String[] sepRow = rows.get(1);
        for (int c = 0; c < maxCols; c++) {
            aligns[c] = c < sepRow.length ? parseAlignment(sepRow[c]) : Alignment.LEFT;
        }

        // 3. 渲染每个单元格为 Text (表头 + 数据行, 跳过分隔行)
        //    同时计算每个单元格渲染后的纯文本长度
        int rowCount = rows.size();
        Text[][] cellTexts = new Text[rowCount][maxCols];
        int[][] cellWidths = new int[rowCount][maxCols];
        int[] colWidths = new int[maxCols];

        for (int r = 0; r < rowCount; r++) {
            if (r == 1) continue; // 跳过分隔行
            String[] row = rows.get(r);
            for (int c = 0; c < maxCols; c++) {
                String cell = c < row.length ? row[c] : "";
                // 渲染单元格内容 (LaTeX + Markdown)
                Text rendered = ChatParser.renderSource(cell);
                cellTexts[r][c] = applyBaseColor(rendered, r == 0 ? "tableHeader" : "tableBody");
                int w = visibleLength(rendered);
                cellWidths[r][c] = w;
                colWidths[c] = Math.max(colWidths[c], w);
            }
        }

        // 4. 渲染
        MutableText result = Text.empty();

        // 表头 (加粗)
        result.append(renderRow(cellTexts[0], cellWidths[0], aligns, colWidths, true));
        result.append(Text.literal("\n"));

        // 分隔线
        result.append(renderSeparator(aligns, colWidths));
        result.append(Text.literal("\n"));

        // 数据行
        for (int i = 2; i < rowCount; i++) {
            result.append(renderRow(cellTexts[i], cellWidths[i], aligns, colWidths, false));
            if (i < rowCount - 1) {
                result.append(Text.literal("\n"));
            }
        }

        return result;
    }

    /**
     * 拆分表格行为单元格数组.
     *
     * <p>规则:</p>
     * <ul>
     *   <li>去掉首尾的 {@code |}.</li>
     *   <li>按 {@code |} 分隔.</li>
     *   <li>每个单元格 trim 空白.</li>
     * </ul>
     *
     * @param line 表格行文本.
     * @return 单元格数组.
     */
    private static String[] splitRow(String line) {
        String t = line.trim();
        if (t.startsWith("|")) t = t.substring(1);
        if (t.endsWith("|")) t = t.substring(0, t.length() - 1);
        String[] parts = t.split("\\|", -1);
        String[] result = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = parts[i].trim();
        }
        return result;
    }

    /**
     * 从分隔行单元格解析对齐方式.
     *
     * <ul>
     *   <li>{@code :---:} → 居中</li>
     *   <li>{@code ---:} → 右对齐</li>
     *   <li>{@code :---} 或 {@code ---} → 左对齐</li>
     * </ul>
     */
    private static Alignment parseAlignment(String s) {
        String t = s.trim();
        boolean leftColon = t.startsWith(":");
        boolean rightColon = t.endsWith(":");
        if (leftColon && rightColon) return Alignment.CENTER;
        if (rightColon) return Alignment.RIGHT;
        return Alignment.LEFT;
    }

    /**
     * 渲染一个数据行 / 表头行, 每个单元格按渲染后 Text 显示, 用空格填充对齐.
     *
     * @param cellTexts   单元格渲染后的 Text.
     * @param cellWidths  每个单元格渲染后的可见长度.
     * @param aligns      每列对齐方式.
     * @param colWidths   每列最大宽度.
     * @param bold        是否加粗 (表头为 true).
     */
    private static Text renderRow(Text[] cellTexts, int[] cellWidths,
                                  Alignment[] aligns, int[] colWidths, boolean bold) {
        MutableText row = Text.empty();
        Style pipeStyle = bold ? Style.EMPTY.withBold(true) : Style.EMPTY;
        Style padStyle = Style.EMPTY;
        row.append(Text.literal("|").setStyle(pipeStyle));
        for (int c = 0; c < colWidths.length; c++) {
            Text cellText = c < cellTexts.length ? cellTexts[c] : Text.empty();
            int cellW = c < cellWidths.length ? cellWidths[c] : 0;
            int pad = colWidths[c] - cellW;

            // 左填充
            int leftPad = switch (aligns[c]) {
                case RIGHT -> pad;
                case CENTER -> pad / 2;
                case LEFT -> 0;
            };
            int rightPad = pad - leftPad;

            row.append(Text.literal(" ").setStyle(padStyle));
            if (leftPad > 0) row.append(Text.literal(" ".repeat(leftPad)).setStyle(padStyle));
            // 单元格内容: 若表头, 给内容加粗
            if (bold) {
                row.append(makeBold(cellText));
            } else {
                row.append(cellText);
            }
            if (rightPad > 0) row.append(Text.literal(" ".repeat(rightPad)).setStyle(padStyle));
            row.append(Text.literal(" ").setStyle(padStyle));
            row.append(Text.literal("|").setStyle(pipeStyle));
        }
        return row;
    }

    /**
     * 把 Text 的所有节点 style 都加上 bold=true.
     */
    private static Text makeBold(Text text) {
        MutableText result = Text.empty();
        text.visit((style, string) -> {
            result.append(Text.literal(string).setStyle(style.withBold(true)));
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }

    private static Text applyBaseColor(Text text, String category) {
        MutableText result = Text.empty();
        net.minecraft.text.TextColor color = RichChatConfig.INSTANCE.getColor(category);
        text.visit((style, string) -> {
            result.append(Text.literal(string).setStyle(color == null ? style : style.withColor(color)));
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }

    /**
     * 渲染分隔行 (如 {@code |:---:|:---|---:|}).
     */
    private static Text renderSeparator(Alignment[] aligns, int[] colWidths) {
        MutableText row = Text.empty();
        row.append(Text.literal("|"));
        for (int c = 0; c < colWidths.length; c++) {
            String sep = makeSeparator(colWidths[c], aligns[c]);
            row.append(Text.literal(" " + sep + " "));
            row.append(Text.literal("|"));
        }
        return row;
    }

    /**
     * 生成指定宽度的分隔线段.
     *
     * <ul>
     *   <li>居中: {@code :---:} (2 个冒号 + (w-2) 个 -)</li>
     *   <li>右对齐: {@code ---:} ((w-1) 个 - + 1 个 :)</li>
     *   <li>左对齐: {@code :---} (1 个 : + (w-1) 个 -)</li>
     * </ul>
     */
    private static String makeSeparator(int width, Alignment align) {
        return switch (align) {
            case CENTER -> ":" + "-".repeat(Math.max(1, width - 2)) + ":";
            case RIGHT -> "-".repeat(Math.max(1, width - 1)) + ":";
            case LEFT -> ":" + "-".repeat(Math.max(1, width - 1));
        };
    }

    /**
     * 计算渲染后 Text 的可见字符长度 (用 visit 展开).
     */
    private static int visibleLength(Text text) {
        final int[] count = {0};
        text.visit((style, string) -> {
            count[0] += string.codePointCount(0, string.length());
            return Optional.empty();
        }, Style.EMPTY);
        return count[0];
    }

    /**
     * 判断字符串是否为表格行 (包含 {@code |} 且不以代码块 / 公式块标记开头).
     *
     * @param s 待判断的字符串.
     * @return true=是表格行.
     */
    public static boolean isTableRow(String s) {
        if (s == null || s.isEmpty()) return false;
        String t = s.trim();
        if (t.startsWith("```") || t.startsWith("$$")) return false;
        return t.contains("|");
    }

    /**
     * 判断字符串是否为表格分隔行 (如 {@code |:---:|:---|}).
     *
     * @param s 待判断的字符串.
     * @return true=是分隔行.
     */
    public static boolean isTableSeparator(String s) {
        if (s == null || s.isEmpty()) return false;
        String t = s.trim();
        if (t.startsWith("|")) t = t.substring(1).trim();
        if (t.endsWith("|")) t = t.substring(0, t.length() - 1).trim();
        if (t.isEmpty()) return false;
        String[] parts = t.split("\\|");
        for (String p : parts) {
            String pp = p.trim();
            if (!pp.matches(":?-{1,}:?")) return false;
            if (pp.length() < 1) return false;
        }
        return parts.length > 0;
    }
}
