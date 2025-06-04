package AST;

/**
 * StatementNode 是所有语句类型 AST 节点的抽象基类。
 * 语句通常不直接计算出值，而是执行某些操作或改变程序状态。
 * 它实现了 ASTNode 接口，意味着所有具体的语句节点都需要提供
 * generateTAC 和 printTree 方法的实现。
 */
public abstract class StatementNode implements ASTNode {}
