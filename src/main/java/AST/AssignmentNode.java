package AST;

/**
 * AssignmentNode 代表一个赋值语句节点，例如 a = b + c。
 * 它继承自 StatementNode (假设 StatementNode 是 ASTNode 的一个子接口或抽象类，用于表示语句)。
 */
public class AssignmentNode extends StatementNode {
    public IdentifierNode variable;
    public ExpressionNode expression;

    /**
     * AssignmentNode 的构造函数。
     *
     * @param variable   被赋值的变量 (IdentifierNode)。
     * @param expression 右侧的表达式 (ExpressionNode)。
     */
    public AssignmentNode(IdentifierNode variable, ExpressionNode expression) {
        this.variable = variable;
        this.expression = expression;
    }

    /**
     * 生成赋值语句的三地址码。
     * 1. 递归生成右侧表达式的三地址码，并获取其结果的 "place"。
     * 2. 发出一条形如 "variableName = expressionPlace" 的三地址指令。
     *
     * @param context TAC 生成的上下文环境。
     * @return 对于语句节点，通常返回 null。
     */
    @Override
    public String generateTAC(TACContext context) {
        String exprPlace = expression.generateTAC(context);
        context.emit(variable.name + " = " + exprPlace);
        return null;
    }

    /**
     * 打印赋值节点的树形结构。
     *
     * @param indent 当前节点的缩进字符串。
     * @param isLast 布尔值，指示当前节点是否为其父节点的最后一个子节点。
     * @return 表示该节点及其子树的格式化字符串。
     */
    @Override
    public String printTree(String indent, boolean isLast) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append(isLast ? "└── " : "├── ").append("AssignmentNode\n");
        String newIndent = indent + (isLast ? "    " : "│   ");
        sb.append(variable.printTree(newIndent + " LValue: ", false));
        sb.append(expression.printTree(newIndent + " RValue: ", true));
        return sb.toString();
    }

    /**
     * accept 方法是访问者模式的关键部分，实现了 "双重分派" (Double Dispatch)。
     * 当外部代码需要用某个访问者来处理一个 AST 节点时，它会调用该节点的 accept 方法，
     * 并将访问者对象作为参数传入。
     *
     * @param visitor 一个实现了 ASTVisitor 接口的访问者对象 (例如 SemanticAnalyzer)。
     * @param <T>     该方法返回值的类型，与访问者定义的返回类型一致。
     * @return 访问者处理完该节点后返回的结果。
     */
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}