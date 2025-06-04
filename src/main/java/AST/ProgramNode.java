package AST;

/**
 * ProgramNode 代表整个程序的根节点。
 * 它通常包含一个主代码块。
 */
public class ProgramNode implements ASTNode {
    BlockNode block;

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
}
