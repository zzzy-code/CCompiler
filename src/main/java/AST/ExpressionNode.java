package AST;

public abstract class ExpressionNode implements ASTNode {
    public String resultPlace;
    // 为了简化 printTree，我们可能不需要每个 ExpressionNode 都实现它，除非想看表达式树

    @Override
    public String printTree(String indent, boolean isLast) {
        return indent + (isLast ? "└── " : "├── ") + NodeName() +
                (resultPlace != null && !resultPlace.equals(NodeName()) ? " [" + resultPlace + "]" : "") + "\n";
    }

    // 辅助方法获取节点名，子类可以覆盖
    protected String NodeName() {
        return this.getClass().getSimpleName() + (resultPlace != null ? " (" + resultPlace + ")" : "");
    }
}
