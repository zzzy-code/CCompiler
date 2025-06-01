package AST;

public class ReturnNode extends StatementNode {
    ExpressionNode expression;

    public ReturnNode(ExpressionNode expression) {
        this.expression = expression;
    }

    @Override
    public String generateTAC(TACContext context) {
        String exprPlace = expression.generateTAC(context);
        context.emit("RETURN " + exprPlace);
        return null;
    }

    @Override
    public String printTree(String indent, boolean isLast) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append(isLast ? "└── " : "├── ").append("ReturnNode\n");
        if (expression != null) {
            sb.append(expression.printTree(indent + (isLast ? "    " : "│   ") + " Value: ", true));
        } else {
            sb.append(indent).append(isLast ? "    " : "│   ").append("└── Value: <null>\n");
        }
        return sb.toString();
    }
}
