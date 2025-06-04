package AST;

/**
 * ReturnNode 代表一个 return 返回语句节点。
 * 它包含一个可选的表达式，其值将被返回。
 */
public class ReturnNode extends StatementNode {
    ExpressionNode expression;

    /**
     * ReturnNode 的构造函数。
     *
     * @param expression 要返回其值的表达式节点。
     */
    public ReturnNode(ExpressionNode expression) {
        this.expression = expression;
    }

    /**
     * 生成 return 语句的三地址码。
     * 1. 递归生成返回表达式的三地址码，得到其结果的 "place"。
     * 2. 发出 "RETURN exprPlace" 指令。
     *
     * @param context TAC 生成的上下文环境。
     * @return 对于语句节点，通常返回 null。
     */
    @Override
    public String generateTAC(TACContext context) {
        String exprPlace = expression.generateTAC(context);
        context.emit("RETURN " + exprPlace);
        return null;
    }

    /**
     * 打印 return 节点的树形结构。
     *
     * @param indent 当前节点的缩进字符串。
     * @param isLast 布尔值，指示当前节点是否为其父节点的最后一个子节点。
     * @return 表示该节点及其子树的格式化字符串。
     */
    @Override
    public String printTree(String indent, boolean isLast) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append(isLast ? "└── " : "├── ").append("ReturnNode\n");
        if (expression != null) {
            sb.append(expression.printTree(indent + (isLast ? "    " : "│   ") + " Value: ", true));
        } else {
            sb.append(indent).append(isLast ? "    " : "│   ").append("└── Value: <null>\n");
        }
        return sb.toString();
    }
}
