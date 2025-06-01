package AST;

public class IfNode extends StatementNode {
    ExpressionNode condition;
    BlockNode trueBlock;
    BlockNode falseBlock; // 可以为 null

    public IfNode(ExpressionNode condition, BlockNode trueBlock, BlockNode falseBlock) {
        this.condition = condition;
        this.trueBlock = trueBlock;
        this.falseBlock = falseBlock;
    }

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
            context.emit(elseLabel + ":"); // This is L_END_IF when no else block
        }
        return null;
    }

    @Override
    public String printTree(String indent, boolean isLast) { // 修改返回类型为 String
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
}