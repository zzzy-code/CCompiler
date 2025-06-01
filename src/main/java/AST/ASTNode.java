package AST;

public interface ASTNode {
    String generateTAC(TACContext context); // 返回结果的 "place"
    String printTree(String indent, boolean isLast); // 新增：用于打印 AST 结构
}
