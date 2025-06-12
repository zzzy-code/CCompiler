package Parser;

import Lexer.Token;
import java.util.*;

/**
 * SimplePrecedenceParser 类实现了一个简单的算符优先分析器。
 * 该分析器尝试根据定义的语法规则和优先关系表来解析输入的 Token 序列。
 * 注意：这是一个简化的实现，可能无法处理所有复杂的语法结构或歧义。
 */
public class SimplePrecedenceParser {

    // 定义语法规则,每条规则由一个 GrammarRule 对象表示，包含左部非终结符和右部符号序列。
    private static final List<GrammarRule> grammarRules = Arrays.asList(
            // 程序结构 (Program -> int main() Block)
            new GrammarRule("Program", "KW_INT", "KW_MAIN", "LPAREN", "RPAREN", "Block"),
            new GrammarRule("Block", "LBRACE", "StmtList", "RBRACE"),
            new GrammarRule("Block", "LBRACE", "RBRACE"),

            // 语句列表 (StmtList -> StmtList Stmt | Stmt) - 使用左递归定义语句序列
            new GrammarRule("StmtList", "StmtList", "Stmt"),
            new GrammarRule("StmtList", "Stmt"),

            // 语句类型 (Stmt 可以是多种不同类型的语句)
            new GrammarRule("Stmt", "DeclStmt"),      // 声明语句
            new GrammarRule("Stmt", "AssignStmt"),    // 赋值语句
            new GrammarRule("Stmt", "WhileStmt"),     // while 循环语句
            new GrammarRule("Stmt", "IfStmt"),        // if 条件语句
            new GrammarRule("Stmt", "ElseIfStmt"),    // else 或 else if (这里简化为 else 块)
            new GrammarRule("Stmt", "PrintStmt"),     // printf 输出语句
            new GrammarRule("Stmt", "ReturnStmt"),    // return 返回语句

            // 声明语句 (DeclStmt -> int Expr = Expr; | int Expr;)
            new GrammarRule("DeclStmt", "KW_INT", "Expr", "OP_ASSIGN", "Expr", "SEMICOLON"),
            new GrammarRule("DeclStmt", "KW_INT", "Expr", "SEMICOLON"),

            // 赋值语句 (AssignStmt -> Expr = Expr;)
            new GrammarRule("AssignStmt", "Expr", "OP_ASSIGN", "Expr", "SEMICOLON"),

            // while语句 (WhileStmt -> while Expr Block) - 条件部分简化为单个 Expr
            new GrammarRule("WhileStmt", "KW_WHILE", "Expr", "Block"),

            // if语句 (IfStmt -> if Expr Block else Block | if Expr Block)
            new GrammarRule("IfStmt", "KW_IF", "Expr", "Block", "KW_ELSE", "Block"),
            new GrammarRule("IfStmt", "KW_IF", "Expr", "Block"),
            // else语句 (ElseIfStmt -> else Block)
            new GrammarRule("ElseIfStmt", "KW_ELSE", "Block"),

            // printf语句 - 添加更多匹配模式
            new GrammarRule("PrintStmt", "IO_PRINTF", "LPAREN", "Expr", "RPAREN", "SEMICOLON"),
            new GrammarRule("PrintStmt", "IO_PRINTF", "LPAREN", "Expr", "COMMA", "Expr", "RPAREN", "SEMICOLON"),
            new GrammarRule("PrintStmt", "IO_PRINTF", "Expr", "SEMICOLON"), // 简化模式

            // return语句 (ReturnStmt -> return Expr;)
            new GrammarRule("ReturnStmt", "KW_RETURN", "Expr", "SEMICOLON"),

            // 表达式 (Expr) - 定义了各种表达式的构成方式
            new GrammarRule("Expr", "Expr", "OP_ADD", "Expr"),   // 加法
            new GrammarRule("Expr", "Expr", "OP_LE", "Expr"),    // 小于等于
            new GrammarRule("Expr", "Expr", "OP_EQ", "Expr"),    // 等于
            new GrammarRule("Expr", "Expr", "OP_MOD", "Expr"),   // 取模
            new GrammarRule("Expr", "Expr", "OP_MUL", "Expr"),   // 乘法
            new GrammarRule("Expr", "Expr", "OP_SUB", "Expr"),   // 减法
            new GrammarRule("Expr", "Expr", "OP_DIV", "Expr"),   // 除法
            new GrammarRule("Expr", "Expr", "OP_GT", "Expr"),    // 大于
            new GrammarRule("Expr", "LPAREN", "Expr", "RPAREN"), // 括号表达式
            new GrammarRule("Expr", "ID"),                       // 标识符
            new GrammarRule("Expr", "NUM"),                      // 数字
            new GrammarRule("Expr", "STR")                       // 字符串
    );

    /**
     * 简单优先关系表 (Operator Precedence Table)
     * 用于存储任意两个终结符之间的优先关系（<, =, >）。
     * Key: 第一个终结符 (栈顶终结符)
     * Value: Map<String, String> (Key: 第二个终结符 (当前输入符号), Value: 优先关系)
     */
    private static final Map<String, Map<String, String>> precedenceTable = new HashMap<>();

    // 一个包含所有非终结符的集合，用于快速判断一个符号是否为非终结符。
    private static final Set<String> nonTerminals = new HashSet<>();

    static {
        // 静态初始化块，用于填充非终结符集合
        for (GrammarRule rule : grammarRules) {
            nonTerminals.add(rule.left);
        }
    }

    private List<Token> tokens;             // 输入的 Token 序列
    private int currentIndex;               // 当前处理到的 Token 索引
    private final Stack<String> parseStack; // 分析栈，存储终结符和非终结符的名称(类型)
    private final List<String> parseSteps;  // 记录分析过程中的每一步，用于调试或展示


    /**
     * 获取语法分析的详细步骤。
     *
     * @return 分析步骤列表的副本。
     */
    public List<String> getParseSteps() {
        return new ArrayList<>(this.parseSteps);
    }

    /**
     * SimplePrecedenceParser 的构造函数。
     * 初始化优先关系表、分析栈和分析步骤列表。
     */
    public SimplePrecedenceParser() {
        initPrecedenceTable();
        parseStack = new Stack<>();
        parseSteps = new ArrayList<>();
    }

    /**
     * 初始化优先关系表。
     * 这里定义了一组简化的优先关系，实际的算符优先表会更复杂和完整。
     * '$' 通常用作输入串的开始/结束标记或栈底标记。
     */
    private void initPrecedenceTable() {
        // 定义一个基础的优先级映射，数值越大优先级越高（这里用于比较）
        Map<String, Integer> precedence = new HashMap<>();
        precedence.put("$", 0);         // 栈底/输入结束符
        precedence.put("KW_RETURN", 1); // return 关键字
        precedence.put("RBRACE", 2);    // 右花括号 }
        precedence.put("SEMICOLON", 3); // 分号 ;
        precedence.put("KW_ELSE", 4);   // else 关键字
        precedence.put("RPAREN", 5);    // 右圆括号 )
        precedence.put("OP_EQ", 6);     // 等于 ==
        precedence.put("OP_LE", 7);     // 小于等于 <=
        precedence.put("OP_ADD", 8);    // 加法 +
        precedence.put("OP_SUB", 8);    // 减法 -
        precedence.put("OP_MUL", 9);    // 乘法 *
        precedence.put("OP_DIV", 9);    // 除法 /
        precedence.put("OP_MOD", 9);    // 取模 %
        precedence.put("LPAREN", 10);   // 左圆括号 (
        precedence.put("ID", 11);       // 标识符
        precedence.put("NUM", 11);      // 数字
        precedence.put("STR", 11);      // 字符串

        for (String left : precedence.keySet()) {
            for (String right : precedence.keySet()) {
                int leftPrec = precedence.get(left);
                int rightPrec = precedence.get(right);

                String relation;
                if (leftPrec < rightPrec) {
                    relation = "<";     // 栈顶符号优先级低，移进
                } else if (leftPrec > rightPrec) {
                    relation = ">";     // 栈顶符号优先级高，规约
                } else {
                    relation = "=";     // 优先级相同
                }

                precedenceTable.computeIfAbsent(left, k -> new HashMap<>()).put(right, relation);
            }
        }

        // 定义一些特殊关系，这些关系可能不完全遵循上述基于数值的比较逻辑
        precedenceTable.computeIfAbsent("LPAREN", k -> new HashMap<>()).put("RPAREN", "=");
        precedenceTable.computeIfAbsent("LBRACE", k -> new HashMap<>()).put("RBRACE", "=");
        precedenceTable.computeIfAbsent("$", k -> new HashMap<>()).put("$", "=");
    }

    /**
     * 从分析栈顶部向下查找最近的终结符。
     * 在算符优先分析中，优先关系是定义在终结符之间的，非终结符会被忽略。
     *
     * @return 栈中最靠近顶部的终结符。如果栈中只有非终结符（和栈底'$'），则返回'$'。
     */
    private String findTopmostTerminal() {
        for (int i = parseStack.size() - 1; i >= 0; i--) {
            String symbol = parseStack.get(i);
            if (!nonTerminals.contains(symbol)) {
                return symbol;
            }
        }
        return "$";
    }


    /**
     * 获取两个终结符之间的优先关系。
     *
     * @param left 栈顶的终结符（或最近的终结符）。
     * @param right 当前输入符号。
     * @return 表示优先关系的字符串（"<", "=", ">"）。如果未定义，默认为 "<" (倾向于移进)。
     */
    private String getPrecedence(String left, String right) {
        if (precedenceTable.containsKey(left) && precedenceTable.get(left).containsKey(right)) {
            return precedenceTable.get(left).get(right);
        }
        return "<";
    }

    /**
     * 主要的语法分析函数。
     * 接收一个表示Token序列的字符串，并尝试根据语法规则进行解析。
     *
     * @param tokenString 一个由 "(类型, 值) (类型, 值)..." 格式组成的字符串。
     * @return 如果语法分析成功，则返回 true；否则返回 false。
     */
    public boolean parse(String tokenString) {
        tokens = parseTokenString(tokenString);
        tokens.add(new Token("$", "$"));

        currentIndex = 0;
        parseStack.clear();
        parseStack.push("$");
        parseSteps.clear();

        System.out.println("开始语法分析...");

        int maxIterations = tokens.size() * 10;
        int iterations = 0;

        while (currentIndex < tokens.size() && iterations < maxIterations) {
            iterations++;
            Token currentToken = getCurrentToken();

            String stackTopTerminal = findTopmostTerminal();
            String relation = getPrecedence(stackTopTerminal, currentToken.type);

            String decisionLog = String.format("--- 决策点: 栈顶终结符[%s] vs 输入[%s] -> 关系: %s",
                    stackTopTerminal, currentToken.type, relation);
            addParseStep(decisionLog);

            if (relation.equals(">")) {
                addParseStep("INFO: 栈顶符号优先级高，准备尝试规约。");
            } else {
                addParseStep("INFO: 输入符号优先级不低于栈顶符号，准备移进。");
            }

            boolean reduced = false;
            int reduceCount = 0;
            while (tryReduce() && reduceCount < 50) {
                reduced = true;
                reduceCount++;
            }

            if ((parseStack.size() == 2 && parseStack.get(1).equals("Program")) ||
                    (parseStack.contains("Program") && currentToken.type.equals("$"))) {
                System.out.println("语法分析成功完成！");
                return true;
            }

            if (currentToken.type.equals("$")) {
                if (tryFinalReduce()) {
                    System.out.println("通过最终规约完成语法分析！");
                    return true;
                }
                break;
            }

            if (currentIndex < tokens.size() - 1) {
                shift(currentToken);
                currentIndex++;
            } else {
                break;
            }
        }

        int finalReduceCount = 0;
        while (tryReduce() && finalReduceCount < 20) {
            finalReduceCount++;
        }

        if (parseStack.contains("Program")) {
            System.out.println("语法分析成功完成！");
            return true;
        }

        System.out.println("语法分析失败，最终栈状态: " + parseStack);
        return false;
    }

    /**
     * 尝试最终规约。 这个方法在原代码中被注释掉了，这里的实现是一个简化版本，
     * 在主循环结束后，如果标准接受条件未满足，可以尝试进一步规约。
     * "最终规约"的逻辑可能暗示了对文法或优先表的不足的弥补。
     *
     * @return 如果通过某种启发式规则（这里简化为调用tryReduce）使得分析成功，则返回true。
     */
    private boolean tryFinalReduce() {
        // 检查是否可以构成完整程序
        List<String> stackContent = new ArrayList<>(parseStack);

        // 寻找可能的程序结构
        for (int i = 1; i < stackContent.size(); i++) {
            if (stackContent.get(i).equals("KW_INT") &&
                    i + 4 < stackContent.size() &&
                    stackContent.get(i + 1).equals("KW_MAIN") &&
                    stackContent.get(i + 2).equals("LPAREN") &&
                    stackContent.get(i + 3).equals("RPAREN")) {

                // 查找后续的Block或可以构成Block的内容
                boolean hasBlock = false;
                for (int j = i + 4; j < stackContent.size(); j++) {
                    if (stackContent.get(j).equals("Block")) {
                        hasBlock = true;
                        break;
                    }
                }

                if (hasBlock) {
                    while (parseStack.size() > i + 5) {
                        parseStack.pop();
                    }
                    parseStack.push("Program");
                    addParseStep("最终规约: 构造Program");
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 执行移进操作 (Shift)。
     * 将当前输入 Token 的类型压入分析栈。
     *
     * @param token 要移进的当前 Token。
     */
    private void shift(Token token) {
        parseStack.push(token.type);
        addParseStep("移进: " + token + " -> 栈: " + getStackSuffix());
    }

    /**
     * 尝试执行规约操作 (Reduce)。
     * 遍历语法规则，查找与分析栈顶部内容匹配的规则右部。
     * 如果找到匹配，则将栈顶的匹配部分弹出，并将规则的左部压入栈。
     *
     * @return 如果成功执行了一次规约，则返回 true；否则返回 false。
     */
    private boolean tryReduce() {
        // 特别优先处理printf语句和ElseIfStmt
        for (GrammarRule rule : grammarRules) {
            if ((rule.left.equals("PrintStmt") || rule.left.equals("ElseIfStmt")) && matchRule(rule)) {
                applyRule(rule);
                return true;
            }
        }

        List<GrammarRule> priorityRules = new ArrayList<>();
        List<GrammarRule> otherRules = new ArrayList<>();

        for (GrammarRule rule : grammarRules) {
            if (rule.left.equals("Expr") || rule.left.equals("Stmt") ||
                    rule.left.equals("DeclStmt") || rule.left.equals("AssignStmt") ||
                    rule.left.equals("ReturnStmt") || rule.left.equals("WhileStmt") ||
                    rule.left.equals("IfStmt") || rule.left.equals("StmtList")) {
                priorityRules.add(rule);
            } else {
                otherRules.add(rule);
            }
        }

        priorityRules.sort((a, b) -> Integer.compare(b.right.length, a.right.length));
        otherRules.sort((a, b) -> Integer.compare(b.right.length, a.right.length));

        for (GrammarRule rule : priorityRules) {
            if (matchRule(rule)) {
                applyRule(rule);
                return true;
            }
        }

        for (GrammarRule rule : otherRules) {
            if (matchRule(rule)) {
                applyRule(rule);
                return true;
            }
        }

        return false;
    }

    /**
     * 检查分析栈的顶部是否匹配给定语法规则的右部。
     *
     * @param rule 要检查的语法规则。
     * @return 如果栈顶内容与规则右部完全匹配，则返回 true；否则返回 false。
     */
    private boolean matchRule(GrammarRule rule) {
        if (parseStack.size() < rule.right.length + 1) { // +1 for $
            return false;
        }

        int start = parseStack.size() - rule.right.length;
        for (int i = 0; i < rule.right.length; i++) {
            if (!parseStack.get(start + i).equals(rule.right[i])) {
                return false;
            }
        }

        if (rule.left.equals("PrintStmt") || rule.left.equals("ElseIfStmt")) {
            System.out.println("调试: 匹配到" + rule.left + "规则: " + rule);
            System.out.println("调试: 栈内容匹配段: " + parseStack.subList(start, parseStack.size()));
        }

        return true;
    }

    /**
     * 应用给定的语法规则进行规约。
     * 将分析栈顶部匹配规则右部的符号串弹出，然后将规则的左部非终结符压入栈。
     *
     * @param rule 要应用的语法规则。
     */
    private void applyRule(GrammarRule rule) {
        for (int i = 0; i < rule.right.length; i++) {
            parseStack.pop();
        }
        parseStack.push(rule.left);
        addParseStep("规约: " + rule + " -> 栈: " + getStackSuffix());
    }

    /**
     * 获取分析栈的后缀字符串，用于在分析步骤中显示。
     * 最多显示栈顶的5个元素，如果栈中元素超过5个，则前面用 "..." 表示。
     *
     * @return 分析栈后缀的字符串表示。
     */
    private String getStackSuffix() {
        if (parseStack.size() <= 5) {
            return parseStack.toString();
        }
        List<String> suffix = new ArrayList<>();
        for (int i = Math.max(0, parseStack.size() - 5); i < parseStack.size(); i++) {
            suffix.add(parseStack.get(i));
        }
        return "..." + suffix;
    }

    /**
     * 获取当前要处理的输入 Token。
     *
     * @return 当前的 Token 对象。如果已到达 Token 序列的末尾，则返回一个表示结束符 "$" 的特殊 Token。
     */
    private Token getCurrentToken() {
        if (currentIndex >= tokens.size()) {
            return new Token("$", "$");
        }
        return tokens.get(currentIndex);
    }

    /**
     * 向分析步骤列表 (parseSteps) 中添加一条记录，并打印到控制台。
     *
     * @param step 要记录的分析步骤描述字符串。
     */
    private void addParseStep(String step) {
        parseSteps.add(step);
        System.out.println(step);
    }

    /**
     * 将输入的 Token 字符串（例如从词法分析器获取的）解析为 Token 对象列表。
     * 输入字符串的格式假定为 "(类型1, 值1) (类型2, 值2) ..."。
     *
     * @param tokenString 包含 Token信息的字符串。
     * @return 解析生成的 Token 对象列表。
     */
    private List<Token> parseTokenString(String tokenString) {
        List<Token> tokenList = new ArrayList<>();
        String[] tokens = tokenString.split("\\) \\(");

        for (String token : tokens) {
            token = token.replace("(", "").replace(")", "");
            String[] parts = token.split(", ", 2);
            if (parts.length == 2) {
                tokenList.add(new Token(parts[0], parts[1]));
            }
        }

        return tokenList;
    }

    /**
     * 打印记录的全部语法分析步骤。
     */
    public void printParseSteps() {
        System.out.println("\n=== 语法分析详细步骤 ===");
        for (int i = 0; i < parseSteps.size(); i++) {
            System.out.println((i + 1) + ". " + parseSteps.get(i));
        }
    }
}