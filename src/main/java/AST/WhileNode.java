package AST;

/**
 * WhileNode 代表一个 while 循环语句节点。
 * 它包含一个循环条件和一个循环体代码块。
 */
public class WhileNode extends StatementNode {
    ExpressionNode condition;
    BlockNode body;

    /**
     * WhileNode 的构造函数。
     *
     * @param condition 循环条件表达式节点。
     * @param body      循环体代码块节点。
     */
    public WhileNode(ExpressionNode condition, BlockNode body) {
        this.condition = condition;
        this.body = body;
    }

    /**
     * 生成 while 循环的三地址码。
     * 逻辑如下：
     * 1. 创建一个 "start_loop" 标签 (startLabel)，标记循环的开始。
     * 2. 创建一个 "end_loop" 标签 (endLabel)，标记循环的结束。
     * 3. 发出 startLabel。
     * 4. 生成条件表达式的 TAC，得到其结果 place (condPlace)。
     * 5. 发出 "IF_FALSE condPlace GOTO endLabel" 指令 (如果条件为假，则跳出循环)。
     * 6. 如果循环体 (body) 不为 null，则为其生成 TAC。
     * 7. 发出 "GOTO startLabel" 指令 (无条件跳转回循环开始，重新评估条件)。
     * 8. 发出 endLabel。
     *
     * @param context TAC 生成的上下文环境。
     * @return 对于语句节点，通常返回 null。
     */
    @Override
    public String generateTAC(TACContext context) {
        String startLabel = context.newLabel();
        String endLabel = context.newLabel();
        context.emit(startLabel + ":");
        String condPlace = condition.generateTAC(context);
        context.emit("IF_FALSE " + condPlace + " GOTO " + endLabel);
        if (body != null) {
            body.generateTAC(context);
        }
        context.emit("GOTO " + startLabel);
        context.emit(endLabel + ":");
        return null;
    }

    /**
     * 打印 while 节点的树形结构，包括条件和循环体。
     *
     * @param indent 当前节点的缩进字符串。
     * @param isLast 布尔值，指示当前节点是否为其父节点的最后一个子节点。
     * @return 表示该节点及其子树的格式化字符串。
     */
    @Override
    public String printTree(String indent, boolean isLast) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append(isLast ? "└── " : "├── ").append("WhileNode\n");
        String newIndent = indent + (isLast ? "    " : "│   ");
        sb.append(condition.printTree(newIndent + " Condition: ", false));
        if (body != null) {
            sb.append(body.printTree(newIndent + " Body:      ", true));
        } else {
            sb.append(newIndent).append("└── Body: <empty>\n");
        }
        return sb.toString();
    }
}
