package AST;

/**
 * DeclarationNode 代表一个变量声明语句，例如 int x; 或 int x = 10;。
 * 它继承自 StatementNode。
 */
public class DeclarationNode extends StatementNode {
    String varName;
    ExpressionNode initializer;

    /**
     * DeclarationNode 的构造函数。
     *
     * @param varName     被声明的变量名。
     * @param initializer 初始化表达式；如果无初始化，则为 null。
     */
    public DeclarationNode(String varName, ExpressionNode initializer) {
        this.varName = varName;
        this.initializer = initializer;
    }

    /**
     * 生成变量声明的三地址码。
     * 1. 发出一条 "DECLARE varName" 指令（这可能是一个伪指令或特定于TAC方言的指令）。
     * 2. 如果存在初始化表达式，则递归生成其三地址码，并获取结果的 "place"。
     * 3. 发出一条 "varName = initPlace" 的赋值指令。
     *
     * @param context TAC 生成的上下文环境。
     * @return 对于语句节点，通常返回 null。
     */
    @Override
    public String generateTAC(TACContext context) {
        context.emit("DECLARE " + varName);
        if (initializer != null) {
            String initPlace = initializer.generateTAC(context);
            context.emit(varName + " = " + initPlace);
        }
        return null;
    }

    /**
     * 打印声明节点的树形结构，包括变量名和可能的初始化表达式。
     *
     * @param indent 当前节点的缩进字符串。
     * @param isLast 布尔值，指示当前节点是否为其父节点的最后一个子节点。
     * @return 表示该节点及其子树的格式化字符串。
     */
    @Override
    public String printTree(String indent, boolean isLast) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append(isLast ? "└── " : "├── ").append("DeclarationNode(int ").append(varName).append(")\n");
        if (initializer != null) {
            String assignIndent = indent + (isLast ? "    " : "│   ");
            sb.append(assignIndent).append("    └── Assign:\n");
            sb.append(initializer.printTree(assignIndent + "        ", true));
        }
        return sb.toString();
    }
}