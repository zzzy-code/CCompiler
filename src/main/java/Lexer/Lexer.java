package Lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lexer 类（词法分析器）负责将输入的字符串（源代码）分解为一系列的词法单元（Token）。
 * 每个词法单元代表语言中的一个有意义的单元，例如关键字、标识符、运算符或字面量。
 */
public class Lexer {
    // 定义 Token 规则列表，每个规则包含一个正则表达式和对应的 Token 类型。
    private static final List<TokenRule> TOKEN_RULES = new ArrayList<>();

    // 静态初始化块，在类加载时执行，用于填充 TOKEN_RULES 列表。
    static {
        // 关键字: 语言中具有特殊含义的保留字。
        addTokenRule("\\bint\\b", "KW_INT");         // 匹配关键字 "int"
        addTokenRule("\\bmain\\b", "KW_MAIN");       // 匹配关键字 "main"
        addTokenRule("\\bwhile\\b", "KW_WHILE");     // 匹配关键字 "while"
        addTokenRule("\\bif\\b", "KW_IF");           // 匹配关键字 "if"
        addTokenRule("\\belse\\b", "KW_ELSE");       // 匹配关键字 "else"
        addTokenRule("\\breturn\\b", "KW_RETURN");   // 匹配关键字 "return"
        addTokenRule("\\bscanf\\b", "IO_SCANF");     // 匹配关键字 "scanf" (用于输入)
        addTokenRule("\\bprintf\\b", "IO_PRINTF");   // 匹配关键字 "printf" (用于输出)

        // 标识符: 用于标识变量、函数等的名称。
        // 必须以字母或下划线开头，后跟字母、数字或下划线。
        addTokenRule("[a-zA-Z_][a-zA-Z0-9_]*", "ID");

        // 数字: 整数字面量。
        addTokenRule("\\d+", "NUM");                // 匹配一个或多个数字

        // 运算符: 对操作数执行操作的符号。
        addTokenRule("\\+", "OP_ADD");            // 加法运算符
        addTokenRule("-", "OP_SUB");              // 减法运算符
        addTokenRule("\\*", "OP_MUL");            // 乘法运算符
        addTokenRule("/", "OP_DIV");              // 除法运算符
        addTokenRule("%", "OP_MOD");              // 模运算符
        addTokenRule("==", "OP_EQ");              // 等于运算符
        addTokenRule("=", "OP_ASSIGN");           // 赋值运算符
        addTokenRule("<=", "OP_LE");              // 小于等于运算符
        addTokenRule(">", "OP_GT");               // 大于运算符

        // 界符: 用于分隔代码结构或表示特殊含义的符号。
        addTokenRule(Pattern.quote("("), "LPAREN");    // 左圆括号，使用 Pattern.quote 转义特殊字符
        addTokenRule(Pattern.quote(")"), "RPAREN");    // 右圆括号
        addTokenRule("\\{", "LBRACE");              // 左花括号
        addTokenRule("\\}", "RBRACE");              // 右花括号
        addTokenRule(";", "SEMICOLON");             // 分号
        addTokenRule(",", "COMMA");                 // 逗号
        addTokenRule("&", "AMPERSAND");             // & 符号 (例如用于取地址)

        // 字符串: 主要用于 printf 等函数的格式字符串。
        // 匹配双引号括起来的字符串，允许包含转义的双引号 \"。
        addTokenRule("\"(\\\\\"|[^\"])*\"", "STR");

        // 注释: 在词法分析阶段通常被忽略。
        // 如果规则的类型为 null，则匹配到的内容将被忽略。
        addTokenRule("//.*", null);                  // 匹配单行注释 (从 // 到行尾)
        addTokenRule("/\\*[\\s\\S]*?\\*/", null);    // 匹配多行注释 (从 /* 到 */，包括换行符，非贪婪匹配)
    }

    /**
     * 向 TOKEN_RULES 列表中添加一个新的词法规则。
     *
     * @param regex 正则表达式字符串，用于匹配词法单元。
     * @param type  词法单元的类型。如果为 null，则匹配到的内容将被忽略（例如注释）。
     */
    private static void addTokenRule(String regex, String type) {
        TOKEN_RULES.add(new TokenRule(Pattern.compile(regex), type));
    }

    /**
     * 一个记录 (record) 类，用于封装单个词法规则。
     * 包含一个已编译的正则表达式模式 (Pattern) 和对应的词法单元类型 (String)。
     *
     * @param pattern 已编译的正则表达式。
     * @param type    词法单元的类型。
     */
    private record TokenRule(Pattern pattern, String type) {}

    /**
     * 对输入的字符串执行词法分析，将其转换为 Token 列表。
     *
     * @param input 要进行词法分析的源代码字符串。
     * @return 包含从输入中解析出的 Token 的列表。
     * @throws RuntimeException 如果在输入中遇到无法识别的非法字符。
     */
    public static List<Token> lex(String input) {
        List<Token> tokens = new ArrayList<>();
        int pos = 0;
        int len = input.length();

        while (pos < len) {
            // 1. 跳过空白字符 (空格、制表符、换行符、回车符)
            Matcher whitespace = Pattern.compile("[ \t\n\r]+").matcher(input.substring(pos));
            if (whitespace.lookingAt()) {
                pos += whitespace.end();
                continue;
            }

            // 2. 尝试匹配 Token
            boolean matched = false;
            for (TokenRule rule : TOKEN_RULES) {
                Matcher matcher = rule.pattern.matcher(input.substring(pos));
                if (matcher.lookingAt()) {
                    String value = matcher.group();
                    if (rule.type != null) {
                        tokens.add(new Token(rule.type, value));
                    }
                    pos += matcher.end();
                    matched = true;
                    break;
                }
            }

            // 3. 如果没有匹配到任何 Token 规则
            if (!matched) {
                throw new RuntimeException("Lexer Error: 非法字符 '" + input.charAt(pos) + "' at position " + pos);
            }
        }

        return tokens;
    }
}