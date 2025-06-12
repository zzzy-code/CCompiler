package AST;

/**
 * IdentifierNode 代表源代码中的一个标识符，通常是一个变量名。
 * 它是一种表达式节点。
 */
public class IdentifierNode extends ExpressionNode {
    public String name;

    /**
     * IdentifierNode 的构造函数。
     *
     * @param name 标识符的名称字符串。
     */
    public IdentifierNode(String name) {
        this.name = name;
        this.resultPlace = name;
    }

    /**
     * 生成标识符节点的三地址码。
     * 对于标识符，其值已经存储在其名称代表的位置，所以直接返回其名称作为 "place"。
     *
     * @param context TAC 生成的上下文环境 (此处未使用，但接口要求)。
     * @return 标识符的名称，即其值的存储位置。
     */
    @Override
    public String generateTAC(TACContext context) {
        return this.resultPlace;
    }

    /**
     * 覆盖父类的方法，提供特定于标识符节点的名称。
     *
     * @return 格式为 "Identifier(名称)" 的字符串。
     */
    @Override
    protected String NodeName() {
        return "Identifier(" + name + ")";
    }

    /**
     * 打印标识符节点的树形结构。
     * 此处直接调用父类 (ExpressionNode) 的 printTree 实现，
     * 因为它已经能很好地处理只打印节点名的情况。
     *
     * @param indent 当前节点的缩进字符串。
     * @param isLast 布尔值，指示当前节点是否为其父节点的最后一个子节点。
     * @return 表示该节点的格式化字符串。
     */
    @Override
    public String printTree(String indent, boolean isLast) {
        return super.printTree(indent, isLast);
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