package AST;

public class StringLiteralNode extends ExpressionNode {
    String valueWithQuotes;

    public StringLiteralNode(String valueWithQuotes) {
        this.valueWithQuotes = valueWithQuotes;
        this.resultPlace = valueWithQuotes;
    }

    @Override
    public String generateTAC(TACContext context) {
        return this.resultPlace;
    }

    @Override
    public String NodeName() {
        return "String(" + valueWithQuotes + ")";
    }

    @Override
    public String printTree(String indent, boolean isLast) {
        return super.printTree(indent, isLast); // 使用 ExpressionNode 的默认实现
    }
}
