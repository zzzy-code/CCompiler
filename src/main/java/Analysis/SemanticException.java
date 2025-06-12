package Analysis;

/**
 * SemanticException 是一个自定义的运行时异常类。
 * 当在语义分析过程中检测到错误时（例如类型不匹配、变量重复声明等），
 * 会抛出此类型的异常。
 */
public class SemanticException extends RuntimeException {

    /**
     * 构造函数，用于创建一个带有特定错误消息的语义异常。
     * @param message 描述语义错误的详细信息。
     */
    public SemanticException(String message) {
        super(message);
    }
}