package Analysis;

/**
 * Symbol 是一个记录 (record) 类，用于表示符号表中的一个条目。
 * 它封装了一个符号的名称和类型。
 * 使用 record 类可以自动生成构造函数、getter、equals、hashCode 和 toString 方法。
 * @param name 符号的名称（例如，变量名 "x"）。
 * @param type 符号的类型（例如，"INT", "STRING"）。
 */
public record Symbol(String name, String type) {

    /**
     * 覆盖默认的 toString 方法，提供一个更具可读性的字符串表示形式。
     * @return 格式为 "Symbol{name='...', type='...'}" 的字符串。
     */
    @Override
    public String toString() {
        return "Symbol{" + "name='" + name + '\'' + ", type='" + type + '\'' + '}';
    }
}