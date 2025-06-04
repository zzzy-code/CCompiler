package AST;

/**
 * BinaryOpNode 代表一个二元运算表达式节点，例如 a + b, x * y。
 * 它包含左右两个操作数 (表达式) 和一个操作符。
 */
public class BinaryOpNode extends ExpressionNode {
    ExpressionNode left;
    String operatorTokenValue;
    ExpressionNode right;

    /**
     * BinaryOpNode 的构造函数。
     *
     * @param left               左操作数表达式节点。
     * @param operatorTokenValue 操作符的字符串值。
     * @param right              右操作数表达式节点。
     */
    public BinaryOpNode(ExpressionNode left, String operatorTokenValue, ExpressionNode right) {
        this.left = left;
        this.operatorTokenValue = operatorTokenValue;
        this.right = right;
    }

    /**
     * 生成二元运算的三地址码。
     * 1. 递归生成左右操作数的三地址码，并获取它们结果的 "place"。
     * 2. 创建一个新的临时变量来存储本次二元运算的结果。
     * 3. 发出一条形如 "result = leftPlace operator rightPlace" 的三地址指令。
     *
     * @param context TAC 生成的上下文环境。
     * @return 存储该二元运算结果的新临时变量的名称 (place)。
     */
    @Override
    public String generateTAC(TACContext context) {
        String leftPlace = left.generateTAC(context);
        String rightPlace = right.generateTAC(context);
        this.resultPlace = context.newTemp();
        context.emit(this.resultPlace + " = " + leftPlace + " " + operatorTokenValue + " " + rightPlace);
        return this.resultPlace;
    }

    /**
     * 覆盖父类的方法，提供特定于二元运算节点的名称。
     * 例如 "BinaryOp(+)" 或 "BinaryOp(*) -> t1" (如果已生成TAC并有结果位置)。
     *
     * @return 节点的名称字符串。
     */
    @Override
    protected String NodeName() {
        return "BinaryOp(" + operatorTokenValue + ")" + (resultPlace != null ? " -> " + resultPlace : "");
    }

    /**
     * 打印二元运算节点的树形结构及其左右子树。
     *
     * @param indent 当前节点的缩进字符串。
     * @param isLast 布尔值，指示当前节点是否为其父节点的最后一个子节点。
     * @return 表示该节点及其子树的格式化字符串。
     */
    @Override
    public String printTree(String indent, boolean isLast) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append(isLast ? "└── " : "├── ").append(NodeName()).append("\n");
        String newIndent = indent + (isLast ? "    " : "│   ");
        sb.append(left.printTree(newIndent, false));
        sb.append(right.printTree(newIndent, true));
        return sb.toString();
    }
}