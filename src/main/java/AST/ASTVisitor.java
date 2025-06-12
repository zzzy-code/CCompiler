package AST;

/**
 * ASTVisitor 接口定义了访问者设计模式中的 "Visitor"角色。
 * 它为 AST 中的每一种具体节点类型都声明了一个 visit 方法。
 * 通过实现此接口，可以创建能够遍历整个 AST 并对不同类型的节点执行特定操作的类（例如，语义分析、代码生成等），
 * 而无需修改节点类本身。
 *
 * @param <T> visit 方法的通用返回类型。这使得访问者可以根据其功能返回不同类型的结果，
 * 例如，在语义分析中可能返回节点类型 (String)，在代码生成中可能返回 null。
 */
public interface ASTVisitor<T> {

    /**
     * 访问程序根节点 (ProgramNode)。
     * @param node 要访问的 ProgramNode 对象。
     * @return 访问该节点后的结果，类型为 T。
     */
    T visit(ProgramNode node);

    /**
     * 访问代码块节点 (BlockNode)。
     * @param node 要访问的 BlockNode 对象。
     * @return 访问该节点后的结果，类型为 T。
     */
    T visit(BlockNode node);

    /**
     * 访问声明语句节点 (DeclarationNode)。
     * @param node 要访问的 DeclarationNode 对象。
     * @return 访问该节点后的结果，类型为 T。
     */
    T visit(DeclarationNode node);

    /**
     * 访问赋值语句节点 (AssignmentNode)。
     * @param node 要访问的 AssignmentNode 对象。
     * @return 访问该节点后的结果，类型为 T。
     */
    T visit(AssignmentNode node);

    /**
     * 访问 if 条件语句节点 (IfNode)。
     * @param node 要访问的 IfNode 对象。
     * @return 访问该节点后的结果，类型为 T。
     */
    T visit(IfNode node);

    /**
     * 访问 while 循环语句节点 (WhileNode)。
     * @param node 要访问的 WhileNode 对象。
     * @return 访问该节点后的结果，类型为 T。
     */
    T visit(WhileNode node);

    /**
     * 访问 return 返回语句节点 (ReturnNode)。
     * @param node 要访问的 ReturnNode 对象。
     * @return 访问该节点后的结果，类型为 T。
     */
    T visit(ReturnNode node);

    /**
     * 访问 printf 输出语句节点 (PrintfNode)。
     * @param node 要访问的 PrintfNode 对象。
     * @return 访问该节点后的结果，类型为 T。
     */
    T visit(PrintfNode node);

    /**
     * 访问二元运算节点 (BinaryOpNode)。
     * @param node 要访问的 BinaryOpNode 对象。
     * @return 访问该节点后的结果，类型为 T。
     */
    T visit(BinaryOpNode node);

    /**
     * 访问标识符节点 (IdentifierNode)。
     * @param node 要访问的 IdentifierNode 对象。
     * @return 访问该节点后的结果，类型为 T。
     */
    T visit(IdentifierNode node);

    /**
     * 访问数字节点 (NumberNode)。
     * @param node 要访问的 NumberNode 对象。
     * @return 访问该节点后的结果，类型为 T。
     */
    T visit(NumberNode node);

    /**
     * 访问字符串字面量节点 (StringLiteralNode)。
     * @param node 要访问的 StringLiteralNode 对象。
     * @return 访问该节点后的结果，类型为 T。
     */
    T visit(StringLiteralNode node);
}