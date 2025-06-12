package AST;

/**
 * ProgramNode 代表整个程序的根节点。
 * 它通常包含一个主代码块。
 */
public class ProgramNode implements ASTNode {
    public BlockNode block;

    /**
     * ProgramNode 的构造函数。
     *
     * @param block 程序的主代码块节点。
     */
    public ProgramNode(BlockNode block) {
        this.block = block;
    }

    /**
     * 生成整个程序的三地址码。
     * 通常会在主代码块的 TAC 前后添加程序的开始和结束标记。
     *
     * @param context TAC 生成的上下文环境。
     * @return 通常返回 null。
     */
    @Override
    public String generateTAC(TACContext context) {
        context.emit("START_PROGRAM");
        if (block != null) {
            block.generateTAC(context);
        }
        context.emit("END_PROGRAM");
        return null;
    }

    /**
     * 打印程序节点的树形结构。
     *
     * @param indent 当前节点的缩进字符串 (对于根节点通常为空字符串)。
     * @param isLast 布尔值 (对于根节点通常为 true，除非它是某个更大结构的一部分)。
     * @return 表示该节点及其子树的格式化字符串。
     */
    @Override
    public String printTree(String indent, boolean isLast) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append(isLast ? "└── " : "├── ").append("ProgramNode\n");
        if (block != null) {
            sb.append(block.printTree(indent + (isLast ? "    " : "│   "), true));
        }
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
