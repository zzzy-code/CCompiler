package AST;

/**
 * ExpressionNode 是所有表达式类型 AST 节点的抽象基类。
 * 表达式通常会计算出一个值。
 */
public abstract class ExpressionNode implements ASTNode {
    /**
     * 存储表达式计算结果的 "place" (位置)。
     * 这可以是一个临时变量名 (如 t1, t2) 或一个已存在的变量名。
     * 在三地址码生成过程中被赋值。
     */
    public String resultPlace;

    /**
     * 默认的 printTree 实现，适用于大多数简单的表达式节点。
     * 它打印节点名称，如果 resultPlace 不为空且与节点名不同，则附带打印 resultPlace。
     *
     * @param indent 当前节点的缩进字符串。
     * @param isLast 布尔值，指示当前节点是否为其父节点的最后一个子节点。
     * @return 表示该节点的格式化字符串。
     */
    @Override
    public String printTree(String indent, boolean isLast) {
        return indent + (isLast ? "└── " : "├── ") + NodeName() +
                (resultPlace != null && !resultPlace.equals(NodeName()) ? " [" + resultPlace + "]" : "") + "\n";
    }

    /**
     * 辅助方法，用于获取节点的名称，主要用于 printTree。
     * 子类可以覆盖此方法以提供更具体的节点名称（例如，包含操作符或值的名称）。
     * 默认实现返回类名，如果 resultPlace 不为 null，则在括号中附带 resultPlace。
     *
     * @return 节点的名称字符串。
     */
    protected String NodeName() {
        return this.getClass().getSimpleName() + (resultPlace != null ? " (" + resultPlace + ")" : "");
    }
}
