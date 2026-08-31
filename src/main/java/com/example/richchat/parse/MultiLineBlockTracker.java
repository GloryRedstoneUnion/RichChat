package com.example.richchat.parse;

import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 多行块状态机.
 *
 * <p>vanilla 聊天每行都是独立消息, 当某行以 {@code ```} 或 {@code $$} 开头但在该消息内
 * 未闭合时, 需要累积后续消息, 直到看到闭合标记, 再整体渲染为一个块.</p>
 *
 * <p>支持三种多行块:</p>
 * <ul>
 *   <li>代码块: {@code ```} 开头, {@code ```} 结尾. <b>支持嵌套</b>:
 *       外层 {@code ```markdown} 内部可以包含 {@code ```python ... ```},
 *       内层 {@code ```} 只闭合内层, 外层继续累积, 直到外层 {@code ```} 出现.</li>
 *   <li>LaTeX 块: {@code $$} 开头, {@code $$} 结尾.</li>
 *   <li>表格: 表头行 + 分隔行 + 数据行, 以非表格行结束.</li>
 * </ul>
 *
 * <p>状态机是单例的 (聊天 HUD 全局共享). 代码块和 LaTeX 块在闭合前会累积；表格在
 * 确认表头后按行返回实时快照，由客户端替换上一条快照消息.</p>
 *
 * <p><b>代码块嵌套规则:</b></p>
 * <ul>
 *   <li>trimmed 严格等于 {@code ```} → 闭合当前层 (出栈); 栈空时真正闭合.</li>
 *   <li>trimmed 是 {@code ```} + 语言后缀 (如 {@code ```python}) → 开启嵌套层 (入栈),
 *       当作内容累积.</li>
 *   <li>其他行 → 当作内容累积.</li>
 * </ul>
 * <p>这样 {@code ```markdown} 外层包含 {@code ```python ... ```} 内层时,
 * 内层的 {@code ```} 只闭合内层, 不会误闭合外层.</p>
 *
 * <p><b>表格的 look-ahead:</b> 表格首行进来时, 无法立即判断是否为表格 (需要看下一行
 * 是否是分隔行). 因此首行会被暂存 (PENDING_TABLE), 第二行进来时:</p>
 * <ul>
 *   <li>是分隔行 → 确认为表格, 进入 IN_TABLE 状态.</li>
 *   <li>不是分隔行 → 把暂存的首行作为普通消息补显示, 当前行按 NORMAL 处理.</li>
 * </ul>
 */
public final class MultiLineBlockTracker {

    /** 状态机动作类型. */
    public enum ActionType {
        /** 累积中, 该消息被吞掉, 不显示. */
        ACCUMULATE,
        /** 代码块 / LaTeX 块闭合, 渲染累积的内容为一条新消息. */
        RENDER_BLOCK,
        /** 表格闭合, 渲染累积的表格为一条新消息, 当前非表格行按 NORMAL 处理. */
        RENDER_TABLE,
        /** 表格仍在接收行, 返回当前快照而不处理额外消息. */
        RENDER_TABLE_LIVE,
        /** 不在块中, 走正常渲染流程. */
        NORMAL,
        /** 暂存的首行不是表格, 先补显示首行, 当前行按 NORMAL 处理. */
        FALLBACK_NORMAL
    }

    /** 状态机处理结果. */
    public static final class Result {
        /** 动作类型. */
        public final ActionType type;
        /** 块内容 (不含定界符), 仅 {@link ActionType#RENDER_BLOCK} 时有效. */
        public final String content;
        /** 完整原始源码 (含定界符), 用于悬停显示. */
        public final String source;
        /** true=LaTeX 块, false=代码块. 仅 {@link ActionType#RENDER_BLOCK} 时有效. */
        public final boolean isLatex;
        /** 表格行内容; 在最终或实时表格动作中有效. */
        public final List<String> tableBodies;
        /** 待补显示的 Text (FALLBACK_NORMAL 时有效). */
        public final Text flushBefore;

        private Result(ActionType type, String content, String source, boolean isLatex,
                       List<String> tableBodies, Text flushBefore) {
            this.type = type;
            this.content = content;
            this.source = source;
            this.isLatex = isLatex;
            this.tableBodies = tableBodies;
            this.flushBefore = flushBefore;
        }

        public static Result accumulate() {
            return new Result(ActionType.ACCUMULATE, null, null, false, null, null);
        }

        public static Result normal() {
            return new Result(ActionType.NORMAL, null, null, false, null, null);
        }

        public static Result renderBlock(String content, String source, boolean isLatex) {
            return new Result(ActionType.RENDER_BLOCK, content, source, isLatex, null, null);
        }

        public static Result renderTable(List<String> tableBodies) {
            return new Result(ActionType.RENDER_TABLE, null, null, false, tableBodies, null);
        }

        public static Result renderTableLive(List<String> tableBodies) {
            return new Result(ActionType.RENDER_TABLE_LIVE, null, null, false, tableBodies, null);
        }

        public static Result fallbackNormal(Text flushBefore) {
            return new Result(ActionType.FALLBACK_NORMAL, null, null, false, null, flushBefore);
        }
    }

    private enum State { IDLE, PENDING_TABLE, IN_TABLE, IN_CODE, IN_LATEX }

    private State state = State.IDLE;

    // PENDING_TABLE: 暂存首行 (Text 对象, 用于补显示)
    private Text pendingTableText;
    private String pendingTableBody;

    // IN_TABLE: 累积表格行 (消息体, 已剥离聊天前缀), 每行返回实时快照
    private final List<String> tableBodyBuffer = new ArrayList<>();

    // IN_CODE: 代码块累积 (支持嵌套, 用栈记录各层语言)
    private final StringBuilder codeBlockContent = new StringBuilder();
    private final StringBuilder codeBlockSource = new StringBuilder();
    /** 代码块嵌套栈: 每层记录开始标记后的语言后缀 (如 "markdown", "python", 或 ""). */
    private final Deque<String> codeFenceStack = new ArrayDeque<>();

    // IN_LATEX: LaTeX 块累积
    private final StringBuilder latexBlockContent = new StringBuilder();
    private final StringBuilder latexBlockSource = new StringBuilder();

    public MultiLineBlockTracker() {
    }

    /**
     * 处理一条消息.
     *
     * @param message 原始 Text 对象 (用于补显示).
     * @param body    消息体字符串 (已剥离 {@code <sender> } 前缀).
     * @return 处理结果.
     */
    public Result process(Text message, String body) {
        if (body == null) body = "";

        switch (state) {
            case IDLE:
                return processIdle(message, body);
            case PENDING_TABLE:
                return processPendingTable(message, body);
            case IN_TABLE:
                return processInTable(message, body);
            case IN_CODE:
                return processInCode(body);
            case IN_LATEX:
                return processInLatex(body);
            default:
                return Result.normal();
        }
    }

    /** 当前是否在块累积中. */
    public boolean isInBlock() {
        return state != State.IDLE;
    }

    /** 重置状态 (切换世界 / 退出游戏时调用). */
    public void reset() {
        state = State.IDLE;
        pendingTableText = null;
        pendingTableBody = null;
        tableBodyBuffer.clear();
        codeBlockContent.setLength(0);
        codeBlockSource.setLength(0);
        codeFenceStack.clear();
        latexBlockContent.setLength(0);
        latexBlockSource.setLength(0);
    }

    // ===== IDLE: 检测各种块的开始 =====

    private Result processIdle(Text message, String body) {
        String trimmed = body.trim();

        // 代码块开始: ``` 开头且单行内未闭合
        if (trimmed.startsWith("```")) {
            int second = trimmed.indexOf("```", 3);
            if (second == -1) {
                state = State.IN_CODE;
                codeBlockContent.setLength(0);
                codeBlockSource.setLength(0);
                codeFenceStack.clear();
                // 记录语言后缀 (如 "markdown", "python", 或 "")
                String lang = trimmed.substring(3).trim();
                codeFenceStack.push(lang);
                codeBlockSource.append(body);
                return Result.accumulate();
            }
            // 单行闭合, 走 NORMAL
            return Result.normal();
        }

        // LaTeX 块开始: $$ 开头且单行内未闭合
        if (trimmed.startsWith("$$")) {
            int second = trimmed.indexOf("$$", 2);
            if (second == -1) {
                state = State.IN_LATEX;
                latexBlockContent.setLength(0);
                latexBlockSource.setLength(0);
                int openIdx = body.indexOf("$$");
                String afterOpen = body.substring(openIdx + 2);
                if (!afterOpen.isEmpty()) {
                    latexBlockContent.append(afterOpen).append('\n');
                }
                latexBlockSource.append(body);
                return Result.accumulate();
            }
            // 单行闭合, 走 NORMAL
            return Result.normal();
        }

        // 表格开始: 首行是表格行
        if (TableRenderer.isTableRow(body)) {
            state = State.PENDING_TABLE;
            pendingTableText = message;
            pendingTableBody = body;
            return Result.accumulate();
        }

        return Result.normal();
    }

    // ===== PENDING_TABLE: 等待分隔行确认 =====

    private Result processPendingTable(Text message, String body) {
        if (TableRenderer.isTableSeparator(body)) {
            // 确认是表格, 进入 IN_TABLE
            state = State.IN_TABLE;
            tableBodyBuffer.clear();
            tableBodyBuffer.add(pendingTableBody);
            tableBodyBuffer.add(body);
            pendingTableText = null;
            pendingTableBody = null;
            return Result.renderTableLive(List.copyOf(tableBodyBuffer));
        }
        // 不是表格, 把暂存的首行补显示, 当前行按 NORMAL
        Text flush = pendingTableText;
        pendingTableText = null;
        pendingTableBody = null;
        state = State.IDLE;
        return Result.fallbackNormal(flush);
    }

    // ===== IN_TABLE: 累积表格行, 直到非表格行 =====

    private Result processInTable(Text message, String body) {
        if (TableRenderer.isTableRow(body)) {
            tableBodyBuffer.add(body);
            return Result.renderTableLive(List.copyOf(tableBodyBuffer));
        }
        // 表格结束 (当前行非表格行)
        state = State.IDLE;
        List<String> bodies = new ArrayList<>(tableBodyBuffer);
        tableBodyBuffer.clear();
        return Result.renderTable(bodies);
    }

    // ===== IN_CODE: 累积代码块, 支持嵌套 =====
    //
    // 嵌套规则:
    //   - trimmed 严格等于 "```" → 闭合当前层 (出栈); 栈空时真正闭合, 输出累积内容.
    //   - trimmed 是 "```" + 语言后缀 → 开启嵌套层 (入栈), 当作内容累积.
    //   - 其他行 → 当作内容累积.
    // 这样外层 ```markdown 内部包含 ```python ... ``` 时, 内层 ``` 只闭合内层,
    // 不会误闭合外层.

    private Result processInCode(String body) {
        String trimmed = body.trim();

        if (trimmed.equals("```")) {
            // 闭合当前层
            codeFenceStack.pop();
            if (codeFenceStack.isEmpty()) {
                // 栈空, 真正闭合, 输出累积内容 (不含最后的 ```)
                String content = stripTrailingNewline(codeBlockContent.toString());
                String source = stripTrailingNewline(codeBlockSource.toString()) + "\n" + body;
                codeBlockContent.setLength(0);
                codeBlockSource.setLength(0);
                state = State.IDLE;
                return Result.renderBlock(content, source, false);
            }
            // 栈非空, 内层闭合, 当作内容累积
            codeBlockContent.append(body).append('\n');
            codeBlockSource.append(body).append('\n');
            return Result.accumulate();
        }

        if (trimmed.startsWith("```")) {
            // 嵌套代码块开始 (带语言后缀), 入栈, 当作内容累积
            String lang = trimmed.substring(3).trim();
            codeFenceStack.push(lang);
            codeBlockContent.append(body).append('\n');
            codeBlockSource.append(body).append('\n');
            return Result.accumulate();
        }

        // 普通内容行
        codeBlockContent.append(body).append('\n');
        codeBlockSource.append(body).append('\n');
        return Result.accumulate();
    }

    // ===== IN_LATEX: 累积 LaTeX 块, 直到 $$ 闭合 =====

    private Result processInLatex(String body) {
        int closeIdx = body.indexOf("$$");
        if (closeIdx >= 0) {
            String beforeClose = body.substring(0, closeIdx);
            if (!beforeClose.isEmpty()) {
                latexBlockContent.append(beforeClose).append('\n');
                latexBlockSource.append(beforeClose).append('\n');
            }
            String content = stripTrailingNewline(latexBlockContent.toString());
            String source = stripTrailingNewline(latexBlockSource.toString()) + "$$";
            latexBlockContent.setLength(0);
            latexBlockSource.setLength(0);
            state = State.IDLE;
            return Result.renderBlock(content, source, true);
        }
        latexBlockContent.append(body).append('\n');
        latexBlockSource.append(body).append('\n');
        return Result.accumulate();
    }

    /** 去掉末尾的单个换行符 (如果存在). */
    private static String stripTrailingNewline(String s) {
        if (s.endsWith("\n")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }
}
