package AST;

import java.util.List;

public class BlockNode implements ASTNode { // BlockNode 直接实现 ASTNode
    List<StatementNode> statements;

    public BlockNode(List<StatementNode> statements) {
        this.statements = statements;
    }

    @Override
    public String generateTAC(TACContext context) {
        if (statements != null) {
            for (StatementNode stmt : statements) {
                stmt.generateTAC(context);
            }
        }
        return null;
    }

    @Override
    public String printTree(String indent, boolean isLast) { // 修改返回类型为 String
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