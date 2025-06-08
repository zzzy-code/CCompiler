package AST;

import Lexer.Token;
import java.util.*;

/**
 * RecursiveDescentASTParser 类实现了一个递归下降的语法分析器。
 * 它将词法分析器生成的 Token 列表转换为抽象语法树 (AST)。
 * 该解析器主要采用递归下降方法来解析语句和程序结构，
 * 并结合了简单（算符）优先解析 (Operator-Precedence Parsing) 的思想来处理复杂的表达式。
 */
public class RecursiveDescentASTParser {
    private List<Token> tokens;
    private int currentTokenIndex;
    private Token currentToken;

    // 为算符优先表达式解析器定义一个静态的优先级映射表
    private static final Map<String, Integer> PRECEDENCE = new HashMap<>();
    static {
        // 关系运算符的优先级最低 (0)
        PRECEDENCE.put("OP_GT", 0);  // 大于
        PRECEDENCE.put("OP_LE", 0);  // 小于等于
        PRECEDENCE.put("OP_EQ", 0);  // 等于
        // 加减法的优先级中等 (1)
        PRECEDENCE.put("OP_ADD", 1);
        PRECEDENCE.put("OP_SUB", 1);
        // 乘、除、取模的优先级最高 (2)
        PRECEDENCE.put("OP_MUL", 2);
        PRECEDENCE.put("OP_DIV", 2);
        PRECEDENCE.put("OP_MOD", 2);
    }

    /**
     * 获取给定操作符的优先级。
     * @param operatorType 操作符的 Token 类型。
     * @return 返回操作符的优先级数值。如果操作符未定义，返回 -1。
     */
    private int getPrecedence(String operatorType) {
        return PRECEDENCE.getOrDefault(operatorType, -1);
    }

    /**
     * RecursiveDescentASTParser 的构造函数。
     *
     * @param tokens 从词法分析器获得的 Token 列表。
     * @throws IllegalArgumentException 如果 Token 列表为 null 或为空。
     */
    public RecursiveDescentASTParser(List<Token> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            throw new IllegalArgumentException("Token list cannot be null or empty for parser.");
        }
        this.tokens = tokens;
        this.currentTokenIndex = 0;
        this.currentToken = tokens.get(0);
    }

    /**
     * 消耗 (consume) 当前 Token，并前进到下一个 Token。
     * 如果当前 Token 的类型与期望的类型不匹配，则抛出运行时异常。
     *
     * @param expectedType 期望的当前 Token 的类型字符串。
     * @throws RuntimeException 如果 Token 类型不匹配。
     */
    private void consume(String expectedType) {
        if (currentToken.type.equals(expectedType)) {
            currentTokenIndex++;
            if (currentTokenIndex < tokens.size()) {
                currentToken = tokens.get(currentTokenIndex);
            } else {
                currentToken = new Token("EOF_INTERNAL", "$");
            }
        } else {
            throw new RuntimeException("Parser Error: Expected token type " + expectedType +
                    " but found " + currentToken.type + " ('" + currentToken.value +
                    "') at approx. index " + currentTokenIndex);
        }
    }

    /**
     * 解析整个程序。
     * 文法规则 (示例): Program -> KW_INT KW_MAIN LPAREN RPAREN Block
     *
     * @return 构建的 ProgramNode AST 根节点。
     */
    public ProgramNode parseProgram() {
        //文法: Program -> KW_INT KW_MAIN LPAREN RPAREN Block
        consume("KW_INT");
        consume("KW_MAIN");
        consume("LPAREN");
        consume("RPAREN");
        BlockNode block = parseBlock();
        if (!currentToken.type.equals("EOF") && !currentToken.type.equals("EOF_INTERNAL") && !currentToken.type.equals("$")) {
            System.err.println("Warning: Parser finished, but unconsumed tokens remain, starting with: " + currentToken);
        }
        return new ProgramNode(block);
    }

    /**
     * 解析一个代码块。
     * 文法规则 (示例): Block -> LBRACE StmtList RBRACE | LBRACE RBRACE
     * StmtList (语句列表) 在此通过循环处理，直到遇到右花括号。
     *
     * @return 构建的 BlockNode AST 节点。
     */
    private BlockNode parseBlock() {
        //文法: Block -> LBRACE StmtList RBRACE | LBRACE RBRACE
        consume("LBRACE");
        List<StatementNode> statements = new ArrayList<>();
        while (!currentToken.type.equals("RBRACE") && !currentToken.type.equals("EOF") && !currentToken.type.equals("EOF_INTERNAL")) {
            statements.add(parseStatement());
        }
        consume("RBRACE");
        return new BlockNode(statements);
    }

    /**
     * 解析单个语句。
     * 根据当前 Token 的类型来决定调用哪个具体的语句解析方法。
     * 文法规则 (示例): Stmt -> DeclStmt | AssignStmt | WhileStmt | IfStmt | PrintStmt | ReturnStmt
     * (注意: ElseIfStmt 通常不是一个独立的顶层语句，而是 IfStmt 的一部分)
     *
     * @return 构建的 StatementNode AST 子类节点。
     * @throws RuntimeException 如果遇到无法开始一个语句的未知 Token。
     */
    private StatementNode parseStatement() {
        //文法: Stmt -> DeclStmt | AssignStmt | WhileStmt | IfStmt | ElseIfStmt | PrintStmt | ReturnStmt
        switch (currentToken.type) {
            case "KW_INT":
                return parseDeclarationStatement();
            case "ID":
                return parseAssignmentStatement();
            case "KW_WHILE":
                return parseWhileStatement();
            case "KW_IF":
                return parseIfStatement();
            case "IO_PRINTF":
                return parsePrintfStatement();
            case "KW_RETURN":
                return parseReturnStatement();
            default:
                throw new RuntimeException("Parser Error: Unexpected token to start a statement: " + currentToken);
        }
    }

    /**
     * 解析声明语句。
     * 文法规则 (示例): DeclStmt -> KW_INT ID (OP_ASSIGN Expr)? SEMICOLON
     * (原注释中 "Expr" 指代 ID，这里更正为 ID)
     *
     * @return 构建的 DeclarationNode AST 节点。
     */
    private DeclarationNode parseDeclarationStatement() {
        //文法: DeclStmt -> KW_INT Expr OP_ASSIGN Expr SEMICOLON | KW_INT Expr SEMICOLON
        consume("KW_INT");
        Token idToken = currentToken;
        consume("ID");
        ExpressionNode initializer = null;
        if (currentToken.type.equals("OP_ASSIGN")) {
            consume("OP_ASSIGN");
            initializer = parseExpression();
        }
        consume("SEMICOLON");
        return new DeclarationNode(idToken.value, initializer);
    }

    /**
     * 解析赋值语句。
     * 文法规则 (示例): AssignStmt -> ID OP_ASSIGN Expr SEMICOLON
     * (原注释中 LHS Expr 指代 ID)
     *
     * @return 构建的 AssignmentNode AST 节点。
     */
    private AssignmentNode parseAssignmentStatement() {
        //文法: AssignStmt -> Expr OP_ASSIGN Expr SEMICOLON
        Token idToken = currentToken;
        consume("ID");
        IdentifierNode varNode = new IdentifierNode(idToken.value);

        consume("OP_ASSIGN");
        ExpressionNode expr = parseExpression();
        consume("SEMICOLON");
        return new AssignmentNode(varNode, expr);
    }

    /**
     * 解析 while 循环语句。
     * 文法规则 (示例): WhileStmt -> KW_WHILE LPAREN Expr RPAREN Block
     * (假设条件表达式被括号包围)
     *
     * @return 构建的 WhileNode AST 节点。
     */
    private WhileNode parseWhileStatement() {
        //文法: WhileStmt -> KW_WHILE Expr Block
        consume("KW_WHILE");
        consume("LPAREN");
        ExpressionNode condition = parseExpression();
        consume("RPAREN");
        BlockNode body = parseBlock();
        return new WhileNode(condition, body);
    }

    /**
     * 解析 if 条件语句。
     * 文法规则 (示例): IfStmt -> KW_IF LPAREN Expr RPAREN Block (KW_ELSE Block)?
     * (假设条件表达式被括号包围)
     *
     * @return 构建的 IfNode AST 节点。
     */
    private IfNode parseIfStatement() {
        //文法: IfStmt -> KW_IF Expr Block KW_ELSE Block | KW_IF Expr Block
        consume("KW_IF");
        consume("LPAREN");
        ExpressionNode condition = parseExpression();
        consume("RPAREN");
        BlockNode trueBlock = parseBlock();
        BlockNode falseBlock = null;
        if (currentToken.type.equals("KW_ELSE")) {
            consume("KW_ELSE");
            falseBlock = parseBlock();
        }
        return new IfNode(condition, trueBlock, falseBlock);
    }

    /**
     * 解析 printf 输出语句。
     * 支持几种模式：
     * 1. printf(StringLiteral, Expression)
     * 2. printf(StringLiteral)
     * 3. printf StringLiteral (简化模式，不带括号，如果文法支持)
     *
     * @return 构建的 PrintfNode AST 节点。
     * @throws RuntimeException 如果格式字符串不是预期的字符串字面量。
     */
    private PrintfNode parsePrintfStatement() {
        //文法: PrintStmt -> IO_PRINTF LPAREN Expr RPAREN SEMICOLON (Expr is STR)
        //      PrintStmt -> IO_PRINTF LPAREN Expr COMMA Expr RPAREN SEMICOLON (Expr1 is STR, Expr2 is arg)
        //      PrintStmt -> IO_PRINTF Expr SEMICOLON (Expr is STR)
        consume("IO_PRINTF");
        ExpressionNode formatStringExpr;
        ExpressionNode argument = null;

        if (currentToken.type.equals("LPAREN")) {
            consume("LPAREN");
            formatStringExpr = parsePrimaryExpression();
            if (!(formatStringExpr instanceof StringLiteralNode)) {
                throw new RuntimeException("Parser Error: Expected string literal for printf format, but found " + formatStringExpr.getClass().getSimpleName());
            }
            if (currentToken.type.equals("COMMA")) {
                consume("COMMA");
                argument = parseExpression();
            }
            consume("RPAREN");

        } else {
            formatStringExpr = parsePrimaryExpression();
            if (!(formatStringExpr instanceof StringLiteralNode)) {
                throw new RuntimeException("Parser Error: Expected string literal for printf format (simplified mode), but found " + formatStringExpr.getClass().getSimpleName());
            }
        }
        consume("SEMICOLON");
        return new PrintfNode(formatStringExpr, argument);
    }

    /**
     * 解析 return 返回语句。
     * 文法规则 (示例): ReturnStmt -> KW_RETURN Expr SEMICOLON
     *
     * @return 构建的 ReturnNode AST 节点。
     */
    private ReturnNode parseReturnStatement() {
        //文法: ReturnStmt -> KW_RETURN Expr SEMICOLON
        consume("KW_RETURN");
        ExpressionNode expr = parseExpression();
        consume("SEMICOLON");
        return new ReturnNode(expr);
    }

    /**
     * 解析表达式的统一入口。
     * 这是一个经典的算符优先解析算法的实现 (使用两个栈)。
     * 它能正确处理操作符的优先级和左结合性。
     *
     * @return 构建的 ExpressionNode AST 节点，代表整个表达式树的根。
     */
    private ExpressionNode parseExpression() {
        Stack<ExpressionNode> operandStack = new Stack<>();
        Stack<Token> operatorStack = new Stack<>();

        operandStack.push(parsePrimaryExpression());

        while (isBinaryOperator(currentToken)) {
            Token currentOp = currentToken; // 当前要处理的操作符

            while (!operatorStack.isEmpty() &&
                    !operatorStack.peek().type.equals("LPAREN") &&
                    getPrecedence(operatorStack.peek().type) >= getPrecedence(currentOp.type)) {
                applyOperator(operandStack, operatorStack);
            }

            operatorStack.push(currentOp);
            consume(currentOp.type);

            operandStack.push(parsePrimaryExpression());
        }

        while (!operatorStack.isEmpty()) {
            if (operatorStack.peek().type.equals("LPAREN")) {
                throw new RuntimeException("Parser Error: Mismatched parentheses, unclosed '('.");
            }
            applyOperator(operandStack, operatorStack);

        }

        if (operandStack.size() != 1) {
            throw new RuntimeException("Parser Error: Invalid expression structure. Operand stack size is " + operandStack.size() + ", expected 1.");
        }
        return operandStack.pop(); // 返回表达式树的根节点
    }

    /**
     * 从操作数栈和操作符栈中弹出元素，构建一个二元操作节点，并将新节点压回操作数栈。
     *
     * @param operandStack 操作数栈。
     * @param operatorStack 操作符栈。
     */
    private void applyOperator(Stack<ExpressionNode> operandStack, Stack<Token> operatorStack) {
        if (operatorStack.isEmpty()) return;
        Token opToken = operatorStack.pop();
        if (operandStack.size() < 2) {
            throw new RuntimeException("Parser Error: Syntax error, missing operands for operator " + opToken.value);
        }

        ExpressionNode right = operandStack.pop();
        ExpressionNode left = operandStack.pop();
        operandStack.push(new BinaryOpNode(left, opToken.value, right));
    }

    /**
     * 检查给定的 Token 是否是一个已定义的二元操作符。
     *
     * @param token 要检查的 Token。
     * @return 如果是二元操作符则返回 true，否则返回 false。
     */
    private boolean isBinaryOperator(Token token) {
        return PRECEDENCE.containsKey(token.type);
    }

    /**
     * 检查给定的 Token 是否是一个操作数类型（ID, NUM, STR）。
     *
     * @param token 要检查的 Token。
     * @return 如果是操作数则返回 true，否则返回 false。
     */
    private boolean isOperand(Token token) {
        return token.type.equals("ID") || token.type.equals("NUM") || token.type.equals("STR");
    }

    /**
     * 解析基础表达式 (Primary Expression)。
     * 这是表达式解析的原子单位，包括标识符、数字、字符串或括号括起来的子表达式。
     *
     * @return 构建的 ExpressionNode AST 节点。
     * @throws RuntimeException 如果遇到非预期的 Token。
     */
    private ExpressionNode parsePrimaryExpression() {
        Token t = currentToken;
        if (isOperand(t)) {
            consume(t.type);
            if (t.type.equals("ID")) return new IdentifierNode(t.value);
            if (t.type.equals("NUM")) return new NumberNode(Integer.parseInt(t.value));
            if (t.type.equals("STR")) return new StringLiteralNode(t.value);
        }
        else if (t.type.equals("LPAREN")) {
            consume("LPAREN");

            ExpressionNode expr = parseExpression();
            consume("RPAREN");
            return expr;
        }

        throw new RuntimeException("Parser Error: Unexpected token for a primary expression: " + t);
    }
}