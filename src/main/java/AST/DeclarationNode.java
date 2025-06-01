package AST;

public class DeclarationNode extends StatementNode {
    String varName;
    ExpressionNode initializer; //可以为 null

    public DeclarationNode(String varName, ExpressionNode initializer) {
        this.varName = varName;
        this.initializer = initializer;
    }

    @Override
    public String generateTAC(TACContext context) {
        context.emit("DECLARE " + varName);
        if (initializer != null) {
            String initPlace = initializer.generateTAC(context);
            context.emit(varName + " = " + initPlace);
        }
        return null;
    }

    @Override
    public String printTree(String indent, boolean isLast) { // 修改返回类型为 String
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append(isLast ? "└── " : "├── ").append("DeclarationNode(int ").append(varName).append(")\n");
        if (initializer != null) {
            // 为 "Assign:" 创建一个合适的缩进和前缀
            String assignIndent = indent + (isLast ? "    " : "│   ");
            // initializer 本身是一个 ExpressionNode，它有自己的 printTree
            sb.append(assignIndent).append("    └── Assign:\n"); // 先打印 "Assign:" 标签
            sb.append(initializer.printTree(assignIndent + "        ", true)); // 再打印 initializer 的树
        }
        return sb.toString();
    }
}