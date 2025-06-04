package AST;

/**
 * ASTNode 接口是抽象语法树 (AST) 中所有节点的基接口。
 * 它定义了所有 AST 节点都必须实现的方法。
 */
public interface ASTNode {
    /**
     * 生成该 AST 节点对应的三地址码 (Three-Address Code, TAC)。
     *
     * @param context TAC 生成的上下文环境，用于管理临时变量、标签等。
     * @return 对于表达式节点，通常返回存储表达式结果的 "place" (临时变量或标识符)；
     * 对于语句节点，通常返回 null。
     */
    String generateTAC(TACContext context);

    /**
     * 生成用于打印 AST 结构（树形表示）的字符串。
     *
     * @param indent 当前节点的缩进字符串，用于在树形打印时对齐。
     * @param isLast 布尔值，指示当前节点是否为其父节点的最后一个子节点。
     * 这影响连接线的样式 (例如, "└── " vs "├── ")。
     * @return 表示该节点及其子树的格式化字符串。
     */
    String printTree(String indent, boolean isLast);
}