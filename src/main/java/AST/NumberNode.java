package AST;

public class NumberNode extends ExpressionNode {
    int value;

    public NumberNode(int value) {
        this.value = value;
        this.resultPlace = String.valueOf(value);
    }

    @Override
    public String generateTAC(TACContext context) {
        return this.resultPlace;
    }

    @Override
    protected String NodeName() { // 覆盖父类的方法
        return "Number(" + value + ")";
    }

    @Override
    public String printTree(String indent, boolean isLast) { // 修改返回类型为 String
        // 使用 ExpressionNode 中的默认 printTree 实现
        return super.printTree(indent, isLast);
    }
}