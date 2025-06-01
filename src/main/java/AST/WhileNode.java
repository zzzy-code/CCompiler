package AST;

public class WhileNode extends StatementNode {
    ExpressionNode condition;
    BlockNode body;

    public WhileNode(ExpressionNode condition, BlockNode body) {
        this.condition = condition;
        this.body = body;
    }

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
