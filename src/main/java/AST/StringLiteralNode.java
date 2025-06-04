package AST;

/**
 * StringLiteralNode 代表源代码中的一个字符串字面量，例如 "hello world"。
 * 它是一种表达式节点。
 */
public class StringLiteralNode extends ExpressionNode {
    String valueWithQuotes;

    /**
     * StringLiteralNode 的构造函数。
     *
     * @param valueWithQuotes 包含引号的字符串字面量值。
     */
    public StringLiteralNode(String valueWithQuotes) {
        this.valueWithQuotes = valueWithQuotes;
        this.resultPlace = valueWithQuotes;
    }

    /**
     * 生成字符串字面量节点的三地址码。
     * 字符串字面量的值是已知的，直接返回其包含引号的值作为 "place"。
     * 实际的 TAC 指令 (如 PRINT_STR) 会根据需要处理这个值。
     *
     * @param context TAC 生成的上下文环境 (此处未使用)。
     * @return 包含引号的字符串值。
     */
    @Override
    public String generateTAC(TACContext context) {
        return this.resultPlace;
    }

    /**
     * 覆盖父类的方法，提供特定于字符串字面量节点的名称。
     *
     * @return 格式为 "String("带引号的值")" 的字符串。
     */
    @Override
    public String NodeName() {
        return "String(" + valueWithQuotes + ")";
    }

    /**
     * 打印字符串字面量节点的树形结构。
     * 此处直接调用父类 (ExpressionNode) 的 printTree 实现。
     *
     * @param indent 当前节点的缩进字符串。
     * @param isLast 布尔值，指示当前节点是否为其父节点的最后一个子节点。
     * @return 表示该节点的格式化字符串。
     */
    @Override
    public String printTree(String indent, boolean isLast) {
        return super.printTree(indent, isLast);
    }
}
