package Lexer;

/**
 * Token 类代表词法分析过程中从源代码中识别出的一个独立的词法单元。
 */
public class Token {
    public String type;
    public String value;

    /**
     * Token 类的构造函数。
     *
     * @param type  词法单元的类型。
     * @param value 词法单元的值（在源代码中的文本）。
     */
    public Token(String type, String value) {
        this.type = type;   // 初始化类型
        this.value = value; // 初始化值
    }

    /**
     * 返回 Token 对象的字符串表示形式。
     * 这对于调试和打印 Token 序列非常有用。
     *
     * @return 格式为 "(类型, 值)" 的字符串。例如："(KW_INT, int)" 或 "(ID, myVar)"。
     */
    @Override
    public String toString() {
        return String.format("(%s, %s)", type, value);
    }
}