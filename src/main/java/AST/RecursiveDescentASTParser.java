package AST;

import Lexer.Token; // 使用您提供的 Token 类
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

// 假设 AST 节点类 (ProgramNode, BlockNode 等) 和 TACContext 已定义并可访问
// 这些通常会通过 import 语句导入，或者如果它们在同一个文件中则直接可见。
// (为了这个示例，假设它们是可访问的顶层类或在同一个包中)

public class RecursiveDescentASTParser {
    private List<Token> tokens;
    private int currentTokenIndex;
    private Token currentToken;

    public RecursiveDescentASTParser(List<Token> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            throw new IllegalArgumentException("Token list cannot be null or empty for parser.");
        }
        this.tokens = tokens;
        this.currentTokenIndex = 0;
        this.currentToken = tokens.get(0);
    }

    private void consume(String expectedType) {
        if (currentToken.type.equals(expectedType)) {
            currentTokenIndex++;
            if (currentTokenIndex < tokens.size()) {
                currentToken = tokens.get(currentTokenIndex);
            } else {
                // 已经到达列表末尾，通常在列表末尾有一个特殊的 EOF token
                currentToken = new Token("EOF_INTERNAL", "$"); // 使用内部 EOF 标记
            }
        } else {
            throw new RuntimeException("Parser Error: Expected token type " + expectedType +
                    " but found " + currentToken.type + " ('" + currentToken.value +
                    "') at approx. index " + currentTokenIndex);
        }
    }

    public ProgramNode parseProgram() {
        //文法: Program -> KW_INT KW_MAIN LPAREN RPAREN Block
        consume("KW_INT");
        consume("KW_MAIN");
        consume("LPAREN");
        consume("RPAREN");
        BlockNode block = parseBlock();
        // 检查是否所有 Token 都已消耗 (除了我们可能手动添加的 EOF)
        if (!currentToken.type.equals("EOF") && !currentToken.type.equals("EOF_INTERNAL") && !currentToken.type.equals("$")) {
            System.err.println("Warning: Parser finished, but unconsumed tokens remain, starting with: " + currentToken);
        }
        return new ProgramNode(block);
    }

    private BlockNode parseBlock() {
        //文法: Block -> LBRACE StmtList RBRACE | LBRACE RBRACE
        consume("LBRACE");
        List<StatementNode> statements = new ArrayList<>();
        // StmtList 的处理: 只要不是 RBRACE，就尝试解析 Stmt
        while (!currentToken.type.equals("RBRACE") && !currentToken.type.equals("EOF") && !currentToken.type.equals("EOF_INTERNAL")) {
            statements.add(parseStatement());
        }
        consume("RBRACE");
        return new BlockNode(statements);
    }

    private StatementNode parseStatement() {
        //文法: Stmt -> DeclStmt | AssignStmt | WhileStmt | IfStmt | ElseIfStmt | PrintStmt | ReturnStmt
        // ElseIfStmt 通常不应作为一个独立的 Statement，而是 IfStmt 的一部分。
        // 我们将基于 token 类型来决定。
        switch (currentToken.type) {
            case "KW_INT":
                return parseDeclarationStatement();
            case "ID":
                // 需要向前看一个 token 来区分是赋值语句还是函数调用（如果支持的话）
                // 对于 example.txt, ID 开头的语句是赋值语句
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

    private DeclarationNode parseDeclarationStatement() {
        //文法: DeclStmt -> KW_INT Expr OP_ASSIGN Expr SEMICOLON | KW_INT Expr SEMICOLON
        //这里的 Expr 实际上是 ID
        consume("KW_INT");
        Token idToken = currentToken;
        consume("ID"); // Expr 对应 ID
        IdentifierNode varNameNode = new IdentifierNode(idToken.value);

        ExpressionNode initializer = null;
        if (currentToken.type.equals("OP_ASSIGN")) {
            consume("OP_ASSIGN");
            initializer = parseExpression(); // 右侧的 Expr
        }
        consume("SEMICOLON");
        return new DeclarationNode(varNameNode.name, initializer);
    }

    private AssignmentNode parseAssignmentStatement() {
        //文法: AssignStmt -> Expr OP_ASSIGN Expr SEMICOLON
        //这里的 LHS Expr 实际上是 ID
        Token idToken = currentToken;
        consume("ID"); // LHS Expr 对应 ID
        IdentifierNode varNode = new IdentifierNode(idToken.value);

        consume("OP_ASSIGN");
        ExpressionNode expr = parseExpression(); // RHS Expr
        consume("SEMICOLON");
        return new AssignmentNode(varNode, expr);
    }

    private WhileNode parseWhileStatement() {
        //文法: WhileStmt -> KW_WHILE Expr Block
        //您的文法中 Expr 代表条件，它应该被括号包围
        consume("KW_WHILE");
        consume("LPAREN"); // 假设您的词法分析器能正确识别 example.txt 中的括号
        ExpressionNode condition = parseExpression();
        consume("RPAREN");
        BlockNode body = parseBlock();
        return new WhileNode(condition, body);
    }

    private IfNode parseIfStatement() {
        //文法: IfStmt -> KW_IF Expr Block KW_ELSE Block | KW_IF Expr Block
        consume("KW_IF");
        consume("LPAREN"); // 假设
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

    private PrintfNode parsePrintfStatement() {
        //文法: PrintStmt -> IO_PRINTF LPAREN Expr RPAREN SEMICOLON (Expr is STR)
        //      PrintStmt -> IO_PRINTF LPAREN Expr COMMA Expr RPAREN SEMICOLON (Expr1 is STR, Expr2 is arg)
        //      PrintStmt -> IO_PRINTF Expr SEMICOLON (Expr is STR)
        //这里的 Expr 可以是 STR 类型
        consume("IO_PRINTF");
        ExpressionNode formatStringExpr;
        ExpressionNode argument = null;

        if (currentToken.type.equals("LPAREN")) {
            consume("LPAREN");
            formatStringExpr = parsePrimaryExpression(); // 期望是 STR (StringLiteralNode)
            if (!(formatStringExpr instanceof StringLiteralNode)) {
                throw new RuntimeException("Parser Error: Expected string literal for printf format, but found " + formatStringExpr.getClass().getSimpleName());
            }
            if (currentToken.type.equals("COMMA")) {
                consume("COMMA");
                argument = parseExpression();
            }
            consume("RPAREN");
        } else { // 简化模式 IO_PRINTF STR SEMICOLON
            formatStringExpr = parsePrimaryExpression(); // 期望是 STR
            if (!(formatStringExpr instanceof StringLiteralNode)) {
                throw new RuntimeException("Parser Error: Expected string literal for printf format (simplified mode), but found " + formatStringExpr.getClass().getSimpleName());
            }
        }
        consume("SEMICOLON");
        return new PrintfNode(formatStringExpr, argument);
    }

    private ReturnNode parseReturnStatement() {
        //文法: ReturnStmt -> KW_RETURN Expr SEMICOLON
        consume("KW_RETURN");
        ExpressionNode expr = parseExpression();
        consume("SEMICOLON");
        return new ReturnNode(expr);
    }

    // --- 表达式解析 (与上次类似, 采用递归下降处理优先级) ---
    // Expr -> AdditiveExpr ( (OP_LE | OP_EQ) AdditiveExpr )*
    private ExpressionNode parseExpression() {
        ExpressionNode left = parseAdditiveExpression();
        while (currentToken.type.equals("OP_LE") || currentToken.type.equals("OP_EQ") || currentToken.type.equals("OP_GT")) {
            Token opToken = currentToken;
            consume(opToken.type);
            ExpressionNode right = parseAdditiveExpression();
            left = new BinaryOpNode(left, opToken.value, right); // opToken.value is "<=" or "=="
        }
        return left;
    }

    // AdditiveExpr -> MultiplicativeExpr ( OP_ADD MultiplicativeExpr )*
    private ExpressionNode parseAdditiveExpression() {
        ExpressionNode left = parseMultiplicativeExpression();
        while (currentToken.type.equals("OP_ADD") || currentToken.type.equals("OP_MUL")) {
            Token opToken = currentToken;
            consume(opToken.type);
            ExpressionNode right = parseMultiplicativeExpression();
            left = new BinaryOpNode(left, opToken.value, right); // opToken.value is "+"
        }
        return left;
    }

    // MultiplicativeExpr -> PrimaryExpr ( OP_MOD PrimaryExpr )* (仅为 example.txt 处理 OP_MOD)
    private ExpressionNode parseMultiplicativeExpression() {
        ExpressionNode left = parsePrimaryExpression();
        while (currentToken.type.equals("OP_MOD")) {
            Token opToken = currentToken;
            consume(opToken.type);
            ExpressionNode right = parsePrimaryExpression();
            left = new BinaryOpNode(left, opToken.value, right); // opToken.value is "%"
        }
        return left;
    }

    // PrimaryExpr -> ID | NUM | STR | LPAREN Expr RPAREN
    private ExpressionNode parsePrimaryExpression() {
        Token t = currentToken;
        if (t.type.equals("ID")) {
            consume("ID");
            return new IdentifierNode(t.value);
        } else if (t.type.equals("NUM")) {
            consume("NUM");
            return new NumberNode(Integer.parseInt(t.value));
        } else if (t.type.equals("STR")) {
            consume("STR");
            return new StringLiteralNode(t.value); // t.value 应该包含引号，如 "\"text\""
        } else if (t.type.equals("LPAREN")) {
            consume("LPAREN");
            ExpressionNode expr = parseExpression();
            consume("RPAREN");
            return expr;
        } else {
            throw new RuntimeException("Parser Error: Unexpected token in primary expression: " + t);
        }
    }
}