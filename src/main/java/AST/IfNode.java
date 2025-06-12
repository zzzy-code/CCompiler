package AST;

/**
 * IfNode 代表一个 if 条件语句节点。
 * 它可以包含一个可选的 else 分支。
 * 它继承自 StatementNode。
 */
public class IfNode extends StatementNode {
    public ExpressionNode condition;
    public BlockNode trueBlock;
    public BlockNode falseBlock;

    /**
     * IfNode 的构造函数。
     *
     * @param condition  条件表达式节点。
     * @param trueBlock  条件为真时执行的代码块节点。
     * @param falseBlock 条件为假时执行的代码块节点 (可为 null)。
     */
    public IfNode(ExpressionNode condition, BlockNode trueBlock, BlockNode falseBlock) {
        this.condition = condition;
        this.trueBlock = trueBlock;
        this.falseBlock = falseBlock;
    }

    /**
     * 生成 if 语句的三地址码。
     * 逻辑如下：
     * 1. 生成条件表达式的 TAC，得到其结果 place (condPlace)。
     * 2. 创建一个 "else" 标签 (elseLabel)。如果条件为假 (IF_FALSE)，则跳转到此标签。
     * 3. 如果存在 else 分支 (falseBlock != null)，则创建一个 "end_if" 标签 (endIfLabel)。
     * 如果不存在 else 分支，则 elseLabel 同时作为 if 语句结束的标签。
     * 4. 发出 "IF_FALSE condPlace GOTO elseLabel" 指令。
     * 5. (True branch) 如果 trueBlock 不为 null，则为其生成 TAC。
     * 6. (Else branch handling)
     * a. 如果存在 falseBlock：
     * i. 在 trueBlock 的 TAC 之后，发出 "GOTO endIfLabel" 指令，以跳过 else 分支。
     * ii. 发出 elseLabel。
     * iii. 为 falseBlock 生成 TAC。
     * iv. 发出 endIfLabel。
     * b. 如果不存在 falseBlock：
     * i. 直接发出 elseLabel (它标记了 if 语句的结束)。
     *
     * @param context TAC 生成的上下文环境。
     * @return 对于语句节点，通常返回 null。
     */
    @Override
    public String generateTAC(TACContext context) {
        String elseLabel = context.newLabel();
        String endIfLabel = (falseBlock != null) ? context.newLabel() : elseLabel;
        String condPlace = condition.generateTAC(context);
        context.emit("IF_FALSE " + condPlace + " GOTO " + elseLabel);
        if (trueBlock != null) {
            trueBlock.generateTAC(context);
        }
        if (falseBlock != null) {
            context.emit("GOTO " + endIfLabel);
            context.emit(elseLabel + ":");
            falseBlock.generateTAC(context);
            context.emit(endIfLabel + ":");
        } else {
            context.emit(elseLabel + ":");
        }
        return null;
    }

    /**
     * 打印 if 节点的树形结构，包括条件、真分支和可选的假分支。
     *
     * @param indent 当前节点的缩进字符串。
     * @param isLast 布尔值，指示当前节点是否为其父节点的最后一个子节点。
     * @return 表示该节点及其子树的格式化字符串。
     */
    @Override
    public String printTree(String indent, boolean isLast) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append(isLast ? "└── " : "├── ").append("IfNode\n");
        String newIndent = indent + (isLast ? "    " : "│   ");
        sb.append(condition.printTree(newIndent + " Condition: ", false));
        sb.append(trueBlock.printTree(newIndent + " TrueBranch: ", falseBlock == null));
        if (falseBlock != null) {
            sb.append(falseBlock.printTree(newIndent + " FalseBranch: ", true));
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