package Parser;

/**
 * GrammarRule 类用于表示语法分析器中的一条产生式规则。
 * 一条产生式规则通常形如 A -> B C D，其中 A 是左部非终结符，B C D 是右部符号序列（可以是终结符或非终结符）。
 */
public class GrammarRule {
    public String left;
    public String[] right;

    /**
     * GrammarRule 的构造函数。
     *
     * @param left  产生式规则的左部非终结符名称。
     * @param right 产生式规则的右部符号序列。使用可变参数 (varargs) 使得可以方便地传入多个右部符号。
     */
    public GrammarRule(String left, String... right) {
        this.left = left;
        this.right = right;
    }

    /**
     * 返回该语法规则的字符串表示形式。
     * 例如，如果 left 是 "Expr" 且 right 是 {"Expr", "OP_ADD", "Term"}，
     * 则返回 "Expr -> Expr OP_ADD Term"。
     *
     * @return 语法规则的字符串表示。
     */
    @Override
    public String toString() {
        return left + " -> " + String.join(" ", right);
    }
}