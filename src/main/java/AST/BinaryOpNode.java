package AST;

public class BinaryOpNode extends ExpressionNode {
    ExpressionNode left;
    String operatorTokenValue;
    ExpressionNode right;

    public BinaryOpNode(ExpressionNode left, String operatorTokenValue, ExpressionNode right) {
        this.left = left;
        this.operatorTokenValue = operatorTokenValue;
        this.right = right;
    }

    @Override
    public String generateTAC(TACContext context) {
        String leftPlace = left.generateTAC(context);
        String rightPlace = right.generateTAC(context);
        this.resultPlace = context.newTemp();
        context.emit(this.resultPlace + " = " + leftPlace + " " + operatorTokenValue + " " + rightPlace);
        return this.resultPlace;
    }

    // 确保 NodeName (或您在 ExpressionNode 中定义的辅助方法名) 是 public 或 protected
    // 如果 NodeName 是在 ExpressionNode 中定义的 protected String NodeName()，这里不需要重复定义，可以直接调用
    @Override
    protected String NodeName() { // 覆盖父类的方法以提供特定名称
        return "BinaryOp(" + operatorTokenValue + ")" + (resultPlace != null ? " -> " + resultPlace : "");
    }

    @Override
    public String printTree(String indent, boolean isLast) { // 修改返回类型为 String
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append(isLast ? "└── " : "├── ").append(NodeName()).append("\n");
        String newIndent = indent + (isLast ? "    " : "│   ");
        sb.append(left.printTree(newIndent, false));
        sb.append(right.printTree(newIndent, true));
        return sb.toString();
    }
}