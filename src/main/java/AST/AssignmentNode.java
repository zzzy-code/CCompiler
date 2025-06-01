package AST;

public class AssignmentNode extends StatementNode {
    IdentifierNode variable;
    ExpressionNode expression;

    public AssignmentNode(IdentifierNode variable, ExpressionNode expression) {
        this.variable = variable;
        this.expression = expression;
    }

    @Override
    public String generateTAC(TACContext context) {
        String exprPlace = expression.generateTAC(context);
        context.emit(variable.name + " = " + exprPlace);
        return null;
    }

    @Override
    public String printTree(String indent, boolean isLast) {
        StringBuilder sb = new StringBuilder(); // 使用 StringBuilder 构建字符串
        sb.append(indent).append(isLast ? "└── " : "├── ").append("AssignmentNode\n");
        String newIndent = indent + (isLast ? "    " : "│   ");
        // variable 是 ExpressionNode 的子类，会调用其 printTree
        sb.append(variable.printTree(newIndent + " LValue: ", false)); // 标记为LValue
        sb.append(expression.printTree(newIndent + " RValue: ", true)); // 标记为RValue
        return sb.toString();
    }
}