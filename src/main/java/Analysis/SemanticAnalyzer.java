package Analysis;

import AST.*;
import java.util.Objects;

/**
 * SemanticAnalyzer 类实现了 ASTVisitor 接口，用于对抽象语法树 (AST) 进行语义分析。
 * 它通过遍历 AST 来检查程序的语义正确性，例如类型匹配、变量声明等。
 * 这个过程采用了访问者设计模式。
 */
public class SemanticAnalyzer implements ASTVisitor<String> {
    private final SymbolTable symbolTable;

    /**
     * SemanticAnalyzer 的构造函数。
     * 初始化一个新的符号表。
     */
    public SemanticAnalyzer() {
        this.symbolTable = new SymbolTable();
    }

    /**
     * 一个私有辅助方法，用于检查类型是否匹配。
     * @param expected 期望的类型字符串。
     * @param actual 实际的类型字符串。
     * @param context 发生类型检查的上下文描述，用于生成错误信息。
     * @throws SemanticException 如果类型不匹配。
     */
    private void expectType(String expected, String actual, String context) {
        if (!Objects.equals(expected, actual)) {
            throw new SemanticException("类型错误: " + context + ". 期望类型 " + expected + " 但得到类型 " + actual);
        }
    }

    /**
     * 访问程序根节点 (ProgramNode)。
     * 分析的入口点，直接继续访问其内部的代码块。
     * @param node 当前访问的 ProgramNode。
     * @return 总是返回 null，因为程序本身没有类型。
     */
    @Override
    public String visit(ProgramNode node) {
        node.block.accept(this);
        return null;
    }

    /**
     * 访问代码块节点 (BlockNode)。
     * 每个代码块代表一个新的作用域。
     * @param node 当前访问的 BlockNode。
     * @return 总是返回 null，因为代码块本身没有类型。
     */
    @Override
    public String visit(BlockNode node) {
        symbolTable.enterScope();
        for (StatementNode statement : node.statements) {
            statement.accept(this);
        }
        symbolTable.exitScope();
        return null;
    }

    /**
     * 访问声明语句节点 (DeclarationNode)。
     * 检查初始化表达式的类型是否与变量类型匹配，并在符号表中声明新变量。
     * @param node 当前访问的 DeclarationNode。
     * @return 总是返回 null，因为声明语句没有类型。
     */
    @Override
    public String visit(DeclarationNode node) {
        String initType = "VOID";
        if (node.initializer != null) {
            initType = node.initializer.accept(this);
        }
        String varType = "INT";
        if (node.initializer != null) {
            expectType(varType, initType, "在变量 '" + node.varName + "' 的声明中");
        }
        symbolTable.declare(new Symbol(node.varName, varType));
        return null;
    }

    /**
     * 访问赋值语句节点 (AssignmentNode)。
     * 检查赋值号左右两侧的表达式类型是否一致。
     * @param node 当前访问的 AssignmentNode。
     * @return 总是返回 null，因为赋值语句没有类型。
     */
    @Override
    public String visit(AssignmentNode node) {
        String varType = node.variable.accept(this);
        String exprType = node.expression.accept(this);
        expectType(varType, exprType, "在对 '" + node.variable.name + "' 的赋值操作中");
        return null;
    }

    /**
     * 访问标识符节点 (IdentifierNode)。
     * 在符号表中查找该标识符，以确保它已被声明，并返回其类型。
     * @param node 当前访问的 IdentifierNode。
     * @return 返回该标识符在符号表中的类型。
     * @throws SemanticException 如果标识符未声明。
     */
    @Override
    public String visit(IdentifierNode node) {
        System.out.println("    [LOOKUP] Looking for symbol '" + node.name + "'.");
        Symbol symbol = symbolTable.lookup(node.name);
        if (symbol == null) {
            System.err.println("    [FAILED] Symbol '" + node.name + "' NOT FOUND!"); // 错误日志
            throw new SemanticException("语义错误: 变量 '" + node.name + "' 未声明。");
        }
        System.out.println("    [FOUND] Symbol '" + node.name + "' found with type " + symbol.type());
        return symbol.type();
    }

    /**
     * 访问二元运算节点 (BinaryOpNode)。
     * 检查左右操作数的类型是否都为 INT，并设置该节点自身的类型。
     * @param node 当前访问的 BinaryOpNode。
     * @return 返回该二元运算结果的类型，在此简化语言中总是 "INT"。
     */
    @Override
    public String visit(BinaryOpNode node) {
        String leftType = node.left.accept(this);
        String rightType = node.right.accept(this);

        expectType("INT", leftType, "在运算符 '" + node.operatorTokenValue + "' 的左侧");
        expectType("INT", rightType, "在运算符 '" + node.operatorTokenValue + "' 的右侧");

        node.setType("INT");
        return "INT";
    }

    /**
     * 访问 if 语句节点 (IfNode)。
     * 检查条件表达式的类型是否为 INT（或布尔值，此处简化为INT），并递归访问其代码块。
     * @param node 当前访问的 IfNode。
     * @return 总是返回 null。
     */
    @Override
    public String visit(IfNode node) {
        String conditionType = node.condition.accept(this);
        expectType("INT", conditionType, "在 if 语句的条件中");

        node.trueBlock.accept(this);
        if (node.falseBlock != null) {
            node.falseBlock.accept(this);
        }
        return null;
    }

    /**
     * 访问 while 语句节点 (WhileNode)。
     * 检查条件表达式的类型是否为 INT，并递归访问其循环体。
     * @param node 当前访问的 WhileNode。
     * @return 总是返回 null。
     */
    @Override
    public String visit(WhileNode node) {
        String conditionType = node.condition.accept(this);
        expectType("INT", conditionType, "在 while 语句的条件中");

        node.body.accept(this);
        return null;
    }

    /**
     * 访问 printf 语句节点 (PrintfNode)。
     * 检查格式化字符串和参数的类型是否符合预期。
     * @param node 当前访问的 PrintfNode。
     * @return 总是返回 null。
     */
    @Override
    public String visit(PrintfNode node) {
        String formatType = node.formatStringExpr.accept(this);
        expectType("STRING", formatType, "在 printf 的第一个参数位置");
        if (node.argument != null) {
            String argType = node.argument.accept(this);
            expectType("INT", argType, "在 printf 的第二个参数位置");
        }
        return null;
    }

    /**
     * 访问 return 语句节点 (ReturnNode)。
     * 检查返回表达式的类型是否为 INT。
     * @param node 当前访问的 ReturnNode。
     * @return 总是返回 null。
     */
    @Override
    public String visit(ReturnNode node) {
        String returnType = node.expression.accept(this);
        expectType("INT", returnType, "在 return 语句中");
        return null;
    }

    /**
     * 访问数字节点 (NumberNode)。
     * 数字字面量的类型总是 INT。
     * @param node 当前访问的 NumberNode。
     * @return 返回 "INT"。
     */
    @Override
    public String visit(NumberNode node) {
        return "INT";
    }

    /**
     * 访问字符串字面量节点 (StringLiteralNode)。
     * 字符串字面量的类型总是 STRING。
     * @param node 当前访问的 StringLiteralNode。
     * @return 返回 "STRING"。
     */
    @Override
    public String visit(StringLiteralNode node) {
        return "STRING";
    }
}