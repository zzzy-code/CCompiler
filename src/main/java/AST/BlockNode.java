package AST;

import java.util.List;

/**
 * BlockNode 代表一个代码块，通常由一对花括号 "{}" 包围，包含一个或多个语句。
 * 它直接实现 ASTNode 接口。
 */
public class BlockNode implements ASTNode {
    List<StatementNode> statements;

    /**
     * BlockNode 的构造函数。
     *
     * @param statements 组成该代码块的语句列表。
     */
    public BlockNode(List<StatementNode> statements) {
        this.statements = statements;
    }

    /**
     * 生成代码块的三地址码。
     * 依次为代码块中的每条语句生成三地址码。
     *
     * @param context TAC 生成的上下文环境。
     * @return 通常返回 null，因为代码块本身不直接对应一个 "place"。
     */
    @Override
    public String generateTAC(TACContext context) {
        if (statements != null) {
            for (StatementNode stmt : statements) {
                stmt.generateTAC(context);
            }
        }
        return null;
    }

    /**
     * 打印代码块节点的树形结构及其包含的所有语句。
     *
     * @param indent 当前节点的缩进字符串。
     * @param isLast 布尔值，指示当前节点是否为其父节点的最后一个子节点。
     * @return 表示该节点及其子树的格式化字符串。
     */
    @Override
    public String printTree(String indent, boolean isLast) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append(isLast ? "└── " : "├── ").append("BlockNode\n");
        String newIndent = indent + (isLast ? "    " : "│   ");
        if (statements != null) {
            for (int i = 0; i < statements.size(); i++) {
                sb.append(statements.get(i).printTree(newIndent, i == statements.size() - 1));
            }
        }
        return sb.toString();
    }
}