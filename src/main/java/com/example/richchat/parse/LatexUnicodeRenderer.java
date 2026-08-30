package com.example.richchat.parse;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * LaTeX 公式 → Unicode 近似转换器.
 *
 * <p>识别两类定界符:</p>
 * <ul>
 *   <li>行内公式 {@code $...$}</li>
 *   <li>块级公式 {@code $$...$$}</li>
 * </ul>
 *
 * <p>支持的转换示例:</p>
 * <ul>
 *   <li>{@code x^2} → {@code x²}, {@code x_1} → {@code x₁}</li>
 *   <li>{@code \sqrt{x}} → {@code √x}, {@code \frac{a}{b}} → {@code a/b}</li>
 *   <li>希腊字母: {@code \alpha} → {@code α}, {@code \beta} → {@code β}, ...</li>
 *   <li>数学符号: {@code \sum} → {@code Σ}, {@code \int} → {@code ∫}, ...</li>
 * </ul>
 *
 * <p>复杂公式 (矩阵、多行公式等) 保留原始文本. 不识别的转义序列保持原样.</p>
 *
 * <p>{@code \$} 被识别为转义, 不作为公式定界符.</p>
 */
public final class LatexUnicodeRenderer {

    private LatexUnicodeRenderer() {
    }

    /** LaTeX 命令 → Unicode 符号映射表. */
    private static final Map<String, String> SYMBOLS = new HashMap<>();

    /** 上标字符映射表. */
    private static final Map<Character, Character> SUPERSCRIPT = new HashMap<>();

    /** 下标字符映射表. */
    private static final Map<Character, Character> SUBSCRIPT = new HashMap<>();

    static {
        // 希腊字母小写
        SYMBOLS.put("\\alpha", "α");
        SYMBOLS.put("\\beta", "β");
        SYMBOLS.put("\\gamma", "γ");
        SYMBOLS.put("\\delta", "δ");
        SYMBOLS.put("\\epsilon", "ε");
        SYMBOLS.put("\\varepsilon", "ε");
        SYMBOLS.put("\\zeta", "ζ");
        SYMBOLS.put("\\eta", "η");
        SYMBOLS.put("\\theta", "θ");
        SYMBOLS.put("\\vartheta", "ϑ");
        SYMBOLS.put("\\iota", "ι");
        SYMBOLS.put("\\kappa", "κ");
        SYMBOLS.put("\\lambda", "λ");
        SYMBOLS.put("\\mu", "μ");
        SYMBOLS.put("\\nu", "ν");
        SYMBOLS.put("\\xi", "ξ");
        SYMBOLS.put("\\omicron", "ο");
        SYMBOLS.put("\\pi", "π");
        SYMBOLS.put("\\varpi", "ϖ");
        SYMBOLS.put("\\rho", "ρ");
        SYMBOLS.put("\\varrho", "ϱ");
        SYMBOLS.put("\\sigma", "σ");
        SYMBOLS.put("\\varsigma", "ς");
        SYMBOLS.put("\\tau", "τ");
        SYMBOLS.put("\\upsilon", "υ");
        SYMBOLS.put("\\phi", "φ");
        SYMBOLS.put("\\varphi", "φ");
        SYMBOLS.put("\\chi", "χ");
        SYMBOLS.put("\\psi", "ψ");
        SYMBOLS.put("\\omega", "ω");
        // 希腊字母大写
        SYMBOLS.put("\\Gamma", "Γ");
        SYMBOLS.put("\\Delta", "Δ");
        SYMBOLS.put("\\Theta", "Θ");
        SYMBOLS.put("\\Lambda", "Λ");
        SYMBOLS.put("\\Xi", "Ξ");
        SYMBOLS.put("\\Pi", "Π");
        SYMBOLS.put("\\Sigma", "Σ");
        SYMBOLS.put("\\Upsilon", "Υ");
        SYMBOLS.put("\\Phi", "Φ");
        SYMBOLS.put("\\Psi", "Ψ");
        SYMBOLS.put("\\Omega", "Ω");
        // 数学运算符
        SYMBOLS.put("\\sum", "Σ");
        SYMBOLS.put("\\int", "∫");
        SYMBOLS.put("\\oint", "∮");
        SYMBOLS.put("\\iint", "∬");
        SYMBOLS.put("\\iiint", "∭");
        SYMBOLS.put("\\prod", "∏");
        SYMBOLS.put("\\coprod", "∐");
        SYMBOLS.put("\\bigcup", "⋃");
        SYMBOLS.put("\\bigcap", "⋂");
        SYMBOLS.put("\\bigoplus", "⨁");
        SYMBOLS.put("\\bigotimes", "⨂");
        // 关系与集合
        SYMBOLS.put("\\infty", "∞");
        SYMBOLS.put("\\times", "×");
        SYMBOLS.put("\\div", "÷");
        SYMBOLS.put("\\pm", "±");
        SYMBOLS.put("\\mp", "∓");
        SYMBOLS.put("\\cdot", "·");
        SYMBOLS.put("\\cdots", "⋯");
        SYMBOLS.put("\\ldots", "…");
        SYMBOLS.put("\\vdots", "⋮");
        SYMBOLS.put("\\ddots", "⋱");
        SYMBOLS.put("\\leq", "≤");
        SYMBOLS.put("\\geq", "≥");
        SYMBOLS.put("\\neq", "≠");
        SYMBOLS.put("\\approx", "≈");
        SYMBOLS.put("\\equiv", "≡");
        SYMBOLS.put("\\sim", "∼");
        SYMBOLS.put("\\simeq", "≃");
        SYMBOLS.put("\\cong", "≅");
        SYMBOLS.put("\\propto", "∝");
        SYMBOLS.put("\\partial", "∂");
        SYMBOLS.put("\\nabla", "∇");
        SYMBOLS.put("\\forall", "∀");
        SYMBOLS.put("\\exists", "∃");
        SYMBOLS.put("\\nexists", "∄");
        SYMBOLS.put("\\in", "∈");
        SYMBOLS.put("\\notin", "∉");
        SYMBOLS.put("\\ni", "∋");
        SYMBOLS.put("\\subset", "⊂");
        SYMBOLS.put("\\supset", "⊃");
        SYMBOLS.put("\\subseteq", "⊆");
        SYMBOLS.put("\\supseteq", "⊇");
        SYMBOLS.put("\\cup", "∪");
        SYMBOLS.put("\\cap", "∩");
        SYMBOLS.put("\\emptyset", "∅");
        SYMBOLS.put("\\varnothing", "∅");
        // 箭头
        SYMBOLS.put("\\rightarrow", "→");
        SYMBOLS.put("\\leftarrow", "←");
        SYMBOLS.put("\\to", "→");
        SYMBOLS.put("\\gets", "←");
        SYMBOLS.put("\\Rightarrow", "⇒");
        SYMBOLS.put("\\Leftarrow", "⇐");
        SYMBOLS.put("\\leftrightarrow", "↔");
        SYMBOLS.put("\\Leftrightarrow", "⇔");
        SYMBOLS.put("\\mapsto", "↦");
        SYMBOLS.put("\\uparrow", "↑");
        SYMBOLS.put("\\downarrow", "↓");
        SYMBOLS.put("\\updownarrow", "↕");
        // 其他
        SYMBOLS.put("\\degree", "°");
        SYMBOLS.put("\\bullet", "•");
        SYMBOLS.put("\\star", "★");
        SYMBOLS.put("\\dagger", "†");
        SYMBOLS.put("\\ddagger", "‡");
        SYMBOLS.put("\\prime", "′");
        SYMBOLS.put("\\dprime", "″");
        SYMBOLS.put("\\hbar", "ℏ");
        SYMBOLS.put("\\ell", "ℓ");
        SYMBOLS.put("\\Re", "ℜ");
        SYMBOLS.put("\\Im", "ℑ");
        SYMBOLS.put("\\aleph", "ℵ");
        SYMBOLS.put("\\angle", "∠");
        SYMBOLS.put("\\perp", "⊥");
        SYMBOLS.put("\\parallel", "∥");
        SYMBOLS.put("\\sqrt", "√");

        // 上标
        SUPERSCRIPT.put('0', '⁰');
        SUPERSCRIPT.put('1', '¹');
        SUPERSCRIPT.put('2', '²');
        SUPERSCRIPT.put('3', '³');
        SUPERSCRIPT.put('4', '⁴');
        SUPERSCRIPT.put('5', '⁵');
        SUPERSCRIPT.put('6', '⁶');
        SUPERSCRIPT.put('7', '⁷');
        SUPERSCRIPT.put('8', '⁸');
        SUPERSCRIPT.put('9', '⁹');
        SUPERSCRIPT.put('+', '⁺');
        SUPERSCRIPT.put('-', '⁻');
        SUPERSCRIPT.put('=', '⁼');
        SUPERSCRIPT.put('(', '⁽');
        SUPERSCRIPT.put(')', '⁾');
        SUPERSCRIPT.put('a', 'ᵃ');
        SUPERSCRIPT.put('b', 'ᵇ');
        SUPERSCRIPT.put('c', 'ᶜ');
        SUPERSCRIPT.put('d', 'ᵈ');
        SUPERSCRIPT.put('e', 'ᵉ');
        SUPERSCRIPT.put('f', 'ᶠ');
        SUPERSCRIPT.put('g', 'ᵍ');
        SUPERSCRIPT.put('h', 'ʰ');
        SUPERSCRIPT.put('i', 'ⁱ');
        SUPERSCRIPT.put('j', 'ʲ');
        SUPERSCRIPT.put('k', 'ᵏ');
        SUPERSCRIPT.put('l', 'ˡ');
        SUPERSCRIPT.put('m', 'ᵐ');
        SUPERSCRIPT.put('n', 'ⁿ');
        SUPERSCRIPT.put('o', 'ᵒ');
        SUPERSCRIPT.put('p', 'ᵖ');
        SUPERSCRIPT.put('r', 'ʳ');
        SUPERSCRIPT.put('s', 'ˢ');
        SUPERSCRIPT.put('t', 'ᵗ');
        SUPERSCRIPT.put('u', 'ᵘ');
        SUPERSCRIPT.put('v', 'ᵛ');
        SUPERSCRIPT.put('w', 'ʷ');
        SUPERSCRIPT.put('x', 'ˣ');
        SUPERSCRIPT.put('y', 'ʸ');
        SUPERSCRIPT.put('z', 'ᶻ');
        SUPERSCRIPT.put('A', 'ᴬ');
        SUPERSCRIPT.put('B', 'ᴮ');
        SUPERSCRIPT.put('D', 'ᴰ');
        SUPERSCRIPT.put('E', 'ᴱ');
        SUPERSCRIPT.put('G', 'ᴳ');
        SUPERSCRIPT.put('H', 'ᴴ');
        SUPERSCRIPT.put('I', 'ᴵ');
        SUPERSCRIPT.put('J', 'ᴶ');
        SUPERSCRIPT.put('K', 'ᴷ');
        SUPERSCRIPT.put('L', 'ᴸ');
        SUPERSCRIPT.put('M', 'ᴹ');
        SUPERSCRIPT.put('N', 'ᴺ');
        SUPERSCRIPT.put('O', 'ᴼ');
        SUPERSCRIPT.put('P', 'ᴾ');
        SUPERSCRIPT.put('R', 'ᴿ');
        SUPERSCRIPT.put('T', 'ᵀ');
        SUPERSCRIPT.put('U', 'ᵁ');
        SUPERSCRIPT.put('V', 'ⱽ');
        SUPERSCRIPT.put('W', 'ᵂ');
        SUPERSCRIPT.put('-', '⁻');
        SUPERSCRIPT.put('+', '⁺');
        SUPERSCRIPT.put(':', '⁾');
        SUPERSCRIPT.put(' ', ' ');

        // 下标
        SUBSCRIPT.put('0', '₀');
        SUBSCRIPT.put('1', '₁');
        SUBSCRIPT.put('2', '₂');
        SUBSCRIPT.put('3', '₃');
        SUBSCRIPT.put('4', '₄');
        SUBSCRIPT.put('5', '₅');
        SUBSCRIPT.put('6', '₆');
        SUBSCRIPT.put('7', '₇');
        SUBSCRIPT.put('8', '₈');
        SUBSCRIPT.put('9', '₉');
        SUBSCRIPT.put('+', '₊');
        SUBSCRIPT.put('-', '₋');
        SUBSCRIPT.put('=', '₌');
        SUBSCRIPT.put('(', '₍');
        SUBSCRIPT.put(')', '₎');
        SUBSCRIPT.put('a', 'ₐ');
        SUBSCRIPT.put('e', 'ₑ');
        SUBSCRIPT.put('h', 'ₕ');
        SUBSCRIPT.put('i', 'ᵢ');
        SUBSCRIPT.put('j', 'ⱼ');
        SUBSCRIPT.put('k', 'ₖ');
        SUBSCRIPT.put('l', 'ₗ');
        SUBSCRIPT.put('m', 'ₘ');
        SUBSCRIPT.put('n', 'ₙ');
        SUBSCRIPT.put('o', 'ₒ');
        SUBSCRIPT.put('p', 'ₚ');
        SUBSCRIPT.put('r', 'ᵣ');
        SUBSCRIPT.put('s', 'ₛ');
        SUBSCRIPT.put('t', 'ₜ');
        SUBSCRIPT.put('u', 'ᵤ');
        SUBSCRIPT.put('v', 'ᵥ');
        SUBSCRIPT.put('x', 'ₓ');
    }

    /**
     * 渲染整个输入字符串, 将 {@code $...$} 与 {@code $$...$$} 区域转换为 Unicode 近似.
     *
     * @param input 原始字符串, 可包含 LaTeX 公式定界符.
     * @return 转换后的字符串, 公式区域已转换为 Unicode 近似; 未识别的公式保持原样.
     */
    public static String render(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder result = new StringBuilder(input.length());
        for (Segment segment : renderSegments(input)) result.append(segment.text());
        return result.toString();
    }

    /** Splits rendered input into plain and formula spans, retaining malformed formulas as plain text. */
    public static List<Segment> renderSegments(String input) {
        List<Segment> segments = new ArrayList<>();
        if (input == null || input.isEmpty()) return segments;
        StringBuilder plain = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == '\\' && i + 1 < input.length() && input.charAt(i + 1) == '$') {
                plain.append('$');
                i += 2;
                continue;
            }
            String marker = null;
            if (c == '$' && i + 1 < input.length() && input.charAt(i + 1) == '$') marker = "$$";
            else if (c == '$') marker = "$";
            if (marker != null) {
                int end = findClosing(input, i + marker.length(), marker);
                if (end >= 0) {
                    if (plain.length() > 0) {
                        segments.add(new Segment(plain.toString(), false));
                        plain.setLength(0);
                    }
                    segments.add(new Segment(renderFormula(input.substring(i + marker.length(), end)), true));
                    i = end + marker.length();
                    continue;
                }
                plain.append(marker);
                i += marker.length();
                continue;
            }
            plain.append(c);
            i++;
        }
        if (plain.length() > 0) segments.add(new Segment(plain.toString(), false));
        return segments;
    }

    public record Segment(String text, boolean formula) {
    }

    /** Converts only recognized LaTeX commands in an otherwise plain segment. */
    public static String renderBare(String input) {
        if (input == null || input.isEmpty() || !containsBareCommand(input)) return input;
        return renderFormula(input);
    }

    /**
     * 渲染多行 LaTeX 块 (公开接口, 供多行块状态机调用).
     *
     * <p>输入为已剥离 {@code $$} 定界符的公式内容, 内部调用 {@link #renderFormula}
     * 做 Unicode 近似转换.</p>
     *
     * @param formula 公式内容 (不含 {@code $$} 定界符).
     * @return Unicode 近似字符串.
     */
    public static String renderBlock(String formula) {
        return renderFormula(formula);
    }

    /**
     * 渲染单个公式内容 (已去除定界符).
     *
     * @param formula 公式内容.
     * @return Unicode 近似字符串.
     */
    private static String renderFormula(String formula) {
        StringBuilder sb = new StringBuilder(formula.length());
        int i = 0;
        while (i < formula.length()) {
            char c = formula.charAt(i);

            // LaTeX 命令: \name 或 \symbol
            if (c == '\\' && i + 1 < formula.length()) {
                String cmd = readLatexCommand(formula, i);
                if (cmd != null) {
                    if (cmd.equals("\\sqrt")) {
                        int consumed = handleSqrt(formula, i + cmd.length(), sb);
                        if (consumed > 0) {
                            i += cmd.length() + consumed;
                            continue;
                        }
                        sb.append(formula.substring(i));
                        break;
                    } else if (cmd.equals("\\frac")) {
                        int consumed = handleFrac(formula, i + cmd.length(), sb);
                        if (consumed > 0) {
                            i += cmd.length() + consumed;
                            continue;
                        }
                        sb.append(formula.substring(i));
                        break;
                    } else if (isWrapperCommand(cmd)) {
                        int consumed = handleWrapper(formula, i + cmd.length(), sb, cmd);
                        if (consumed > 0) {
                            i += cmd.length() + consumed;
                            continue;
                        }
                        sb.append(formula.substring(i));
                        break;
                    } else if (cmd.equals("\\left") || cmd.equals("\\right")) {
                        i += cmd.length();
                        continue;
                    } else if (cmd.equals("\\quad") || cmd.equals("\\qquad")) {
                        sb.append(cmd.equals("\\quad") ? "  " : "    ");
                        i += cmd.length();
                        continue;
                    } else if (cmd.equals("\\,") || cmd.equals("\\;") || cmd.equals("\\:")
                            || cmd.equals("\\!") || cmd.equals("\\ ")) {
                        if (!cmd.equals("\\!")) sb.append(' ');
                        i += cmd.length();
                        continue;
                    } else {
                        String replacement = SYMBOLS.get(cmd);
                        if (replacement != null) {
                            sb.append(replacement);
                            i += cmd.length();
                            continue;
                        }
                    }
                }
                // 未知命令: 保留反斜杠+下一字符
                sb.append(c);
                i++;
                continue;
            }

            // 上标 ^
            if (c == '^') {
                int consumed = handleScript(formula, i + 1, sb, true);
                if (consumed > 0) {
                    i += 1 + consumed;
                    continue;
                }
                sb.append(c);
                i++;
                continue;
            }

            // 下标 _
            if (c == '_') {
                int consumed = handleScript(formula, i + 1, sb, false);
                if (consumed > 0) {
                    i += 1 + consumed;
                    continue;
                }
                sb.append(c);
                i++;
                continue;
            }

            // 大括号: 跳过 (作为分组符)
            if (c == '{' || c == '}') {
                i++;
                continue;
            }

            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    private static boolean containsBareCommand(String input) {
        return input.matches("(?s).*\\\\(?:text|mathrm|mathbf|mathit|mathsf|mathbb|mathcal|mathfrak|operatorname|vec|overline|underline|left|right|quad|qquad|cdot|frac|sqrt)\\b.*");
    }

    private static boolean isWrapperCommand(String cmd) {
        return cmd.equals("\\text") || cmd.equals("\\mathrm") || cmd.equals("\\mathbf")
                || cmd.equals("\\mathit") || cmd.equals("\\mathsf") || cmd.equals("\\mathbb")
                || cmd.equals("\\mathcal") || cmd.equals("\\mathfrak") || cmd.equals("\\operatorname")
                || cmd.equals("\\vec") || cmd.equals("\\overline") || cmd.equals("\\underline");
    }

    private static int handleWrapper(String formula, int start, StringBuilder out, String command) {
        int j = start;
        while (j < formula.length() && Character.isWhitespace(formula.charAt(j))) j++;
        if (j >= formula.length()) return 0;
        String inner;
        int end;
        if (formula.charAt(j) == '{') {
            int close = findBraceClose(formula, j);
            if (close < 0) return 0;
            inner = formula.substring(j + 1, close);
            end = close + 1;
        } else {
            inner = String.valueOf(formula.charAt(j));
            end = j + 1;
        }
        String rendered = renderFormula(inner);
        if (command.equals("\\vec")) rendered += "⃗";
        else if (command.equals("\\overline")) rendered += "¯";
        else if (command.equals("\\underline")) rendered = "_" + rendered;
        else if (command.equals("\\mathbb")) rendered = toDoubleStruck(rendered);
        out.append(rendered);
        return end - start;
    }

    private static String toDoubleStruck(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            result.append(switch (c) {
                case 'A' -> '𝔸'; case 'B' -> '𝔹'; case 'C' -> 'ℂ'; case 'D' -> '𝔻';
                case 'E' -> '𝔼'; case 'F' -> '𝔽'; case 'G' -> '𝔾'; case 'H' -> 'ℍ';
                case 'I' -> '𝕀'; case 'J' -> '𝕁'; case 'K' -> '𝕂'; case 'L' -> '𝕃';
                case 'M' -> '𝕄'; case 'N' -> 'ℕ'; case 'O' -> '𝕆'; case 'P' -> 'ℙ';
                case 'Q' -> 'ℚ'; case 'R' -> 'ℝ'; case 'S' -> '𝕊'; case 'T' -> '𝕋';
                case 'U' -> '𝕌'; case 'V' -> '𝕍'; case 'W' -> '𝕎'; case 'X' -> '𝕏';
                case 'Y' -> '𝕐'; case 'Z' -> 'ℤ'; default -> c;
            });
        }
        return result.toString();
    }

    /**
     * 处理 {@code \sqrt{...}}, {@code \sqrt x} 或 {@code \sqrt[n]{x}}.
     *
     * <p>转换规则:</p>
     * <ul>
     *   <li>{@code \sqrt{x}} → {@code √x}</li>
     *   <li>{@code \sqrt x} → {@code √x}</li>
     *   <li>{@code \sqrt[n]{x}} → {@code ⁿ√x} (n 转为上标放在 √ 前, 数学上等价于 ⁿ√x)</li>
     * </ul>
     *
     * @param formula 完整公式字符串.
     * @param start   {@code \sqrt} 之后的位置.
     * @param out     输出缓冲.
     * @return 消耗的字符数 (0 表示处理失败).
     */
    private static int handleSqrt(String formula, int start, StringBuilder out) {
        int j = start;
        while (j < formula.length() && Character.isWhitespace(formula.charAt(j))) {
            j++;
        }
        if (j >= formula.length()) {
            return 0;
        }

        // 可选的 [n] 参数: \sqrt[n]{x} → ⁿ√x
        String rootIndex = null;
        if (formula.charAt(j) == '[') {
            int bracketClose = findBracketClose(formula, j);
            if (bracketClose != -1) {
                rootIndex = formula.substring(j + 1, bracketClose);
                j = bracketClose + 1;
                while (j < formula.length() && Character.isWhitespace(formula.charAt(j))) {
                    j++;
                }
                if (j >= formula.length()) {
                    return 0;
                }
            }
        }

        String inner;
        int endPos;
        if (formula.charAt(j) == '{') {
            int close = findBraceClose(formula, j);
            if (close == -1) {
                return 0;
            }
            inner = formula.substring(j + 1, close);
            endPos = close + 1;
        } else {
            // 单字符 sqrt
            inner = String.valueOf(formula.charAt(j));
            endPos = j + 1;
        }

        String renderedInner = renderFormula(inner);
        if (rootIndex != null && !rootIndex.isEmpty()) {
            // \sqrt[n]{x} → ⁿ√x
            String renderedRoot = renderFormula(rootIndex);
            out.append(toSuperscript(renderedRoot)).append('√').append(renderedInner);
        } else {
            out.append('√').append(renderedInner);
        }
        return endPos - start;
    }

    /**
     * 查找匹配的闭方括号.
     *
     * @param s       字符串.
     * @param openIdx 开方括号位置.
     * @return 闭方括号位置; -1 表示未找到.
     */
    private static int findBracketClose(String s, int openIdx) {
        int depth = 1;
        for (int i = openIdx + 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                i++;
                continue;
            }
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * 处理 {@code \frac{a}{b}}.
     *
     * @return 消耗的字符数 (0 表示处理失败).
     */
    private static int handleFrac(String formula, int start, StringBuilder out) {
        int j = start;
        while (j < formula.length() && Character.isWhitespace(formula.charAt(j))) {
            j++;
        }
        if (j >= formula.length() || formula.charAt(j) != '{') {
            return 0;
        }
        int close1 = findBraceClose(formula, j);
        if (close1 == -1) {
            return 0;
        }
        int k = close1 + 1;
        while (k < formula.length() && Character.isWhitespace(formula.charAt(k))) {
            k++;
        }
        if (k >= formula.length() || formula.charAt(k) != '{') {
            return 0;
        }
        int close2 = findBraceClose(formula, k);
        if (close2 == -1) {
            return 0;
        }
        String num = formula.substring(j + 1, close1);
        String den = formula.substring(k + 1, close2);
        out.append(renderFormula(num)).append('/').append(renderFormula(den));
        return close2 + 1 - start;
    }

    /**
     * 处理上标 {@code ^{...}} / {@code ^x} 或下标 {@code _{...}} / {@code _x}.
     *
     * @param superscript true=上标, false=下标.
     * @return 消耗的字符数 (0 表示处理失败).
     */
    private static int handleScript(String formula, int start, StringBuilder out, boolean superscript) {
        if (start >= formula.length()) {
            return 0;
        }
        if (formula.charAt(start) == '{') {
            int close = findBraceClose(formula, start);
            if (close != -1) {
                String inner = formula.substring(start + 1, close);
                String rendered = renderFormula(inner);
                out.append(superscript ? toSuperscript(rendered) : toSubscript(rendered));
                return close + 1 - start;
            }
            return 0;
        }
        // 单字符脚本
        String rendered = String.valueOf(formula.charAt(start));
        out.append(superscript ? toSuperscript(rendered) : toSubscript(rendered));
        return 1;
    }

    /**
     * 读取 LaTeX 命令名, 如 {@code \sum}, {@code \alpha}, {@code \{} 等.
     *
     * @return 命令字符串 (包含反斜杠); 不构成命令时返回 null.
     */
    private static String readLatexCommand(String s, int start) {
        if (s.charAt(start) != '\\') {
            return null;
        }
        StringBuilder sb = new StringBuilder("\\");
        int i = start + 1;
        while (i < s.length() && Character.isLetter(s.charAt(i))) {
            sb.append(s.charAt(i));
            i++;
        }
        if (sb.length() > 1) {
            return sb.toString();
        }
        // 单字符转义如 \{ \} \\
        if (i < s.length()) {
            sb.append(s.charAt(i));
            return sb.toString();
        }
        return null;
    }

    /**
     * 查找匹配的闭花括号.
     *
     * @param s       字符串.
     * @param openIdx 开花括号位置.
     * @return 闭花括号位置; -1 表示未找到.
     */
    private static int findBraceClose(String s, int openIdx) {
        int depth = 1;
        for (int i = openIdx + 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                i++;
                continue;
            }
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * 查找定界符的闭合位置, 跳过反斜杠转义.
     *
     * @param s      字符串.
     * @param start  起始位置.
     * @param marker 定界符 (如 "$", "$$").
     * @return 闭合位置; -1 表示未找到.
     */
    private static int findClosing(String s, int start, String marker) {
        int i = start;
        while (i <= s.length() - marker.length()) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                i += 2;
                continue;
            }
            if (s.startsWith(marker, i)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    /**
     * 将字符串转换为上标形式. 不存在对应上标的字符保持原样.
     */
    private static String toSuperscript(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            Character sup = SUPERSCRIPT.get(c);
            sb.append(sup != null ? sup : c);
        }
        return sb.toString();
    }

    /**
     * 将字符串转换为下标形式. 不存在对应下标的字符保持原样.
     */
    private static String toSubscript(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            Character sub = SUBSCRIPT.get(c);
            sb.append(sub != null ? sub : c);
        }
        return sb.toString();
    }
}
