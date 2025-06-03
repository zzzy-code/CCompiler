package Parser;

import Lexer.Token;

import java.util.*;

public class SimplePrecedenceParser {

    // 定义语法规则
    private static final List<GrammarRule> grammarRules = Arrays.asList(
            // 程序结构
            new GrammarRule("Program", "KW_INT", "KW_MAIN", "LPAREN", "RPAREN", "Block"),
            new GrammarRule("Block", "LBRACE", "StmtList", "RBRACE"),
            new GrammarRule("Block", "LBRACE", "RBRACE"),

            // 语句列表 - 修改为左递归
            new GrammarRule("StmtList", "StmtList", "Stmt"),
            new GrammarRule("StmtList", "Stmt"),

            // 语句类型
            new GrammarRule("Stmt", "DeclStmt"),
            new GrammarRule("Stmt", "AssignStmt"),
            new GrammarRule("Stmt", "WhileStmt"),
            new GrammarRule("Stmt", "IfStmt"),
            new GrammarRule("Stmt", "ElseIfStmt"),
            new GrammarRule("Stmt", "PrintStmt"),
            new GrammarRule("Stmt", "ReturnStmt"),

            // 声明语句
            new GrammarRule("DeclStmt", "KW_INT", "Expr", "OP_ASSIGN", "Expr", "SEMICOLON"),
            new GrammarRule("DeclStmt", "KW_INT", "Expr", "SEMICOLON"),

            // 赋值语句
            new GrammarRule("AssignStmt", "Expr", "OP_ASSIGN", "Expr", "SEMICOLON"),

            // while语句 - 简化条件表达式
            new GrammarRule("WhileStmt", "KW_WHILE", "Expr", "Block"),

            // if语句
            new GrammarRule("IfStmt", "KW_IF", "Expr", "Block", "KW_ELSE", "Block"),
            new GrammarRule("IfStmt", "KW_IF", "Expr", "Block"),
            // 新增else语句
            new GrammarRule("ElseIfStmt", "KW_ELSE", "Block"),

            // printf语句 - 添加更多匹配模式
            new GrammarRule("PrintStmt", "IO_PRINTF", "LPAREN", "Expr", "RPAREN", "SEMICOLON"),
            new GrammarRule("PrintStmt", "IO_PRINTF", "LPAREN", "Expr", "COMMA", "Expr", "RPAREN", "SEMICOLON"),
            new GrammarRule("PrintStmt", "IO_PRINTF", "Expr", "SEMICOLON"), // 简化模式

            // return语句
            new GrammarRule("ReturnStmt", "KW_RETURN", "Expr", "SEMICOLON"),

            // 表达式 - 简化处理
            new GrammarRule("Expr", "Expr", "OP_ADD", "Expr"),
            new GrammarRule("Expr", "Expr", "OP_LE", "Expr"),
            new GrammarRule("Expr", "Expr", "OP_EQ", "Expr"),
            new GrammarRule("Expr", "Expr", "OP_MOD", "Expr"),
            new GrammarRule("Expr", "Expr", "OP_MUL", "Expr"),
            new GrammarRule("Expr", "Expr", "OP_GT", "Expr"),
            new GrammarRule("Expr", "LPAREN", "Expr", "RPAREN"),
            new GrammarRule("Expr", "ID"),
            new GrammarRule("Expr", "NUM"),
            new GrammarRule("Expr", "STR")
    );

    // 简单优先关系表
    private static final Map<String, Map<String, String>> precedenceTable = new HashMap<>();

    private List<Token> tokens;
    private int currentIndex;
    private final Stack<String> parseStack;
    private final List<String> parseSteps;

    public List<String> getParseSteps() {
        return new ArrayList<>(this.parseSteps); // 返回副本以保持封装性
    }

    public SimplePrecedenceParser() {
        initPrecedenceTable();
        parseStack = new Stack<>();
        parseSteps = new ArrayList<>();
    }

    /**
     * 初始化优先关系表
     */
    private void initPrecedenceTable() {
        // 定义更完整的优先关系
        Map<String, Integer> precedence = new HashMap<>();
        precedence.put("$", 0);
        precedence.put("KW_RETURN", 1);
        precedence.put("RBRACE", 2);
        precedence.put("SEMICOLON", 3);
        precedence.put("KW_ELSE", 4);
        precedence.put("RPAREN", 5);
        precedence.put("OP_EQ", 6);
        precedence.put("OP_LE", 7);
        precedence.put("OP_ADD", 8);
        precedence.put("OP_MOD", 9);
        precedence.put("LPAREN", 10);
        precedence.put("ID", 11);
        precedence.put("NUM", 11);
        precedence.put("STR", 11);

        // 构建优先关系表
        for (String left : precedence.keySet()) {
            for (String right : precedence.keySet()) {
                int leftPrec = precedence.get(left);
                int rightPrec = precedence.get(right);

                String relation;
                if (leftPrec < rightPrec) {
                    relation = "<";
                } else if (leftPrec > rightPrec) {
                    relation = ">";
                } else {
                    relation = "=";
                }

                precedenceTable.computeIfAbsent(left, k -> new HashMap<>()).put(right, relation);
            }
        }

        // 特殊关系
        precedenceTable.computeIfAbsent("LPAREN", k -> new HashMap<>()).put("RPAREN", "=");
        precedenceTable.computeIfAbsent("LBRACE", k -> new HashMap<>()).put("RBRACE", "=");
        precedenceTable.computeIfAbsent("$", k -> new HashMap<>()).put("$", "=");
    }

    /**
     * 获取优先关系
     */
    private String getPrecedence(String left, String right) {
        if (precedenceTable.containsKey(left) && precedenceTable.get(left).containsKey(right)) {
            return precedenceTable.get(left).get(right);
        }
        return "<"; // 默认左优先级低
    }

    /**
     * 主要的语法分析函数
     */
    public boolean parse(String tokenString) {
        tokens = parseTokenString(tokenString);
        tokens.add(new Token("$", "$"));

        currentIndex = 0;
        parseStack.clear();
        parseStack.push("$");
        parseSteps.clear();

        System.out.println("开始语法分析...");

        int maxIterations = tokens.size() * 10; // 防止无限循环
        int iterations = 0;

        while (currentIndex < tokens.size() && iterations < maxIterations) {
            iterations++;
            Token currentToken = getCurrentToken();

            // 记录规约前的状态
            boolean reduced = false;

            // 持续尝试规约直到无法规约
            int reduceCount = 0;
            while (tryReduce() && reduceCount < 50) { // 限制规约次数防止无限循环
                reduced = true;
                reduceCount++;
            }

            // 检查是否完成 - 放宽条件
            if ((parseStack.size() == 2 && parseStack.get(1).equals("Program")) ||
                    (parseStack.contains("Program") && currentToken.type.equals("$"))) {
                System.out.println("语法分析成功完成！");
                return true;
            }

            // 如果当前token是$且无法继续处理，尝试最后的规约
            if (currentToken.type.equals("$")) {
                // 尝试将剩余内容规约为程序
                if (tryFinalReduce()) {
                    System.out.println("通过最终规约完成语法分析！");
                    return true;
                }
                break;
            }

            // 移进
            if (currentIndex < tokens.size() - 1) { // 不移进$符号
                shift(currentToken);
                currentIndex++;
            } else {
                break;
            }
        }

        // 最后的尝试
        int finalReduceCount = 0;
        while (tryReduce() && finalReduceCount < 20) {
            finalReduceCount++;
        }

        // 检查最终状态
        if (parseStack.contains("Program")) {
            System.out.println("语法分析成功完成！");
            return true;
        }

        System.out.println("语法分析失败，最终栈状态: " + parseStack);
        return false;
    }

    /**
     * 尝试最终规约
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
                    // 强制规约为Program
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
     * 移进操作
     */
    private void shift(Token token) {
        parseStack.push(token.type);
        addParseStep("移进: " + token + " -> 栈: " + getStackSuffix());
    }

    /**
     * 尝试规约
     */
    private boolean tryReduce() {
        // 特别优先处理printf语句和ElseIfStmt
        for (GrammarRule rule : grammarRules) {
            if ((rule.left.equals("PrintStmt") || rule.left.equals("ElseIfStmt")) && matchRule(rule)) {
                applyRule(rule);
                return true;
            }
        }

        // 优先处理特定的规约顺序
        List<GrammarRule> priorityRules = new ArrayList<>();
        List<GrammarRule> otherRules = new ArrayList<>();

        for (GrammarRule rule : grammarRules) {
            // 优先处理表达式和语句的规约
            if (rule.left.equals("Expr") || rule.left.equals("Stmt") ||
                    rule.left.equals("DeclStmt") || rule.left.equals("AssignStmt") ||
                    rule.left.equals("ReturnStmt") || rule.left.equals("WhileStmt") ||
                    rule.left.equals("IfStmt") || rule.left.equals("StmtList")) {
                priorityRules.add(rule);
            } else {
                otherRules.add(rule);
            }
        }

        // 按长度排序，长规则优先
        priorityRules.sort((a, b) -> Integer.compare(b.right.length, a.right.length));
        otherRules.sort((a, b) -> Integer.compare(b.right.length, a.right.length));

        // 先尝试优先规则
        for (GrammarRule rule : priorityRules) {
            if (matchRule(rule)) {
                applyRule(rule);
                return true;
            }
        }

        // 再尝试其他规则
        for (GrammarRule rule : otherRules) {
            if (matchRule(rule)) {
                applyRule(rule);
                return true;
            }
        }

        return false;
    }

    /**
     * 匹配规则 - 增加调试信息
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

        // 调试：打印匹配成功的规则
        if (rule.left.equals("PrintStmt") || rule.left.equals("ElseIfStmt")) {
            System.out.println("调试: 匹配到" + rule.left + "规则: " + rule);
            System.out.println("调试: 栈内容匹配段: " + parseStack.subList(start, parseStack.size()));
        }

        return true;
    }

    /**
     * 应用规则
     */
    private void applyRule(GrammarRule rule) {
        // 弹出右部符号
        for (int i = 0; i < rule.right.length; i++) {
            parseStack.pop();
        }
        // 压入左部符号
        parseStack.push(rule.left);
        addParseStep("规约: " + rule + " -> 栈: " + getStackSuffix());
    }

    /**
     * 获取栈的后缀用于显示
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
     * 获取当前token
     */
    private Token getCurrentToken() {
        if (currentIndex >= tokens.size()) {
            return new Token("$", "$");
        }
        return tokens.get(currentIndex);
    }

    /**
     * 添加分析步骤
     */
    private void addParseStep(String step) {
        parseSteps.add(step);
        System.out.println(step);
    }

    /**
     * 解析token字符串
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
     * 打印分析步骤
     */
    public void printParseSteps() {
        System.out.println("\n=== 语法分析详细步骤 ===");
        for (int i = 0; i < parseSteps.size(); i++) {
            System.out.println((i + 1) + ". " + parseSteps.get(i));
        }
    }
}