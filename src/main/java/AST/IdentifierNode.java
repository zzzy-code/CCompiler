package AST;

public class IdentifierNode extends ExpressionNode {
    String name;

    public IdentifierNode(String name) {
        this.name = name;
        this.resultPlace = name;
    }

    @Override
    public String generateTAC(TACContext context) {
        return this.resultPlace;
    }

    @Override
    protected String NodeName() { // 覆盖父类的方法
        return "Identifier(" + name + ")";
    }

    @Override
    public String printTree(String indent, boolean isLast) { // 修改返回类型为 String
        // 使用 ExpressionNode 中的默认 printTree 实现，因为它只打印节点名
        return super.printTree(indent, isLast);
    }
}