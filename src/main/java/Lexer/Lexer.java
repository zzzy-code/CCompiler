package Lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer {
    // 定义 Token 规则（正则表达式 + Token 类型）
    private static final List<TokenRule> TOKEN_RULES = new ArrayList<>();

    static {
        // 关键字
        addTokenRule("\\bint\\b", "KW_INT");
        addTokenRule("\\bmain\\b", "KW_MAIN");
        addTokenRule("\\bwhile\\b", "KW_WHILE");
        addTokenRule("\\bif\\b", "KW_IF");
        addTokenRule("\\belse\\b", "KW_ELSE");
        addTokenRule("\\breturn\\b", "KW_RETURN");
        addTokenRule("\\bscanf\\b", "IO_SCANF");
        addTokenRule("\\bprintf\\b", "IO_PRINTF");

        // 标识符
        addTokenRule("[a-zA-Z_][a-zA-Z0-9_]*", "ID");

        // 数字
        addTokenRule("\\d+", "NUM");

        // 运算符
        addTokenRule("\\+", "OP_ADD");
        addTokenRule("-", "OP_SUB");
        addTokenRule("\\*", "OP_MUL");
        addTokenRule("/", "OP_DIV");
        addTokenRule("%", "OP_MOD");
        addTokenRule("==", "OP_EQ");
        addTokenRule("=", "OP_ASSIGN");
        addTokenRule("<=", "OP_LE");
        addTokenRule(">", "OP_GT");

        // 界符
        addTokenRule(Pattern.quote("("), "LPAREN");
        addTokenRule(Pattern.quote(")"), "RPAREN");
        addTokenRule("\\{", "LBRACE");
        addTokenRule("\\}", "RBRACE");
        addTokenRule(";", "SEMICOLON");
        addTokenRule(",", "COMMA");
        addTokenRule("&", "AMPERSAND");

        // 字符串（printf 格式串）
        addTokenRule("\"(\\\\\"|[^\"])*\"", "STR");

        // 注释（忽略）
        addTokenRule("//.*", null);      // 单行注释
        addTokenRule("/\\*[\\s\\S]*?\\*/", null); // 多行注释
    }

    private static void addTokenRule(String regex, String type) {
        TOKEN_RULES.add(new TokenRule(Pattern.compile(regex), type));
    }

    private record TokenRule(Pattern pattern, String type) {}

    public static List<Token> lex(String input) {
        List<Token> tokens = new ArrayList<>();
        int pos = 0;
        int len = input.length();

        while (pos < len) {
            // 跳过空白字符
            Matcher whitespace = Pattern.compile("[ \t\n\r]+").matcher(input.substring(pos));
            if (whitespace.lookingAt()) {
                pos += whitespace.end();
                continue;
            }

            // 尝试匹配 Token
            boolean matched = false;
            for (TokenRule rule : TOKEN_RULES) {
                Matcher matcher = rule.pattern.matcher(input.substring(pos));
                if (matcher.lookingAt()) {
                    String value = matcher.group();
                    if (rule.type != null) { // 如果不是注释
                        tokens.add(new Token(rule.type, value));
                    }
                    pos += matcher.end();
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                throw new RuntimeException("Lexer Error: 非法字符 '" + input.charAt(pos) + "' at position " + pos);
            }
        }

        return tokens;
    }
}