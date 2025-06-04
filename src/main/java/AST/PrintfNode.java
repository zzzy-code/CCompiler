package AST;

/**
 * PrintfNode 代表一个 printf 输出语句节点。
 * 它通常包含一个格式化字符串和一个可选的参数。
 * 它继承自 StatementNode。
 */
public class PrintfNode extends StatementNode {
    ExpressionNode formatStringExpr;
    ExpressionNode argument;

    /**
     * PrintfNode 的构造函数。
     *
     * @param formatStringExpr 格式化字符串表达式节点。
     * @param argument         参数表达式节点 (可为 null)。
     */
    public PrintfNode(ExpressionNode formatStringExpr, ExpressionNode argument) {
        this.formatStringExpr = formatStringExpr;
        this.argument = argument;
    }

    /**
     * 生成 printf 语句的三地址码。
     * 这个实现处理几种情况：
     * 1. printf("%d", variable) -> PRINT variable_place
     * 2. printf("Some string") -> PRINT_STR "Some string"
     * 3. printf("String with \\n") -> PRINT_STR "String with " gef PRINT_NEWLINE
     *
     * @param context TAC 生成的上下文环境。
     * @return 对于语句节点，通常返回 null。
     */
    @Override
    public String generateTAC(TACContext context) {
        String formatStringValue = "";
        String formatStringActualContent = "";

        if (formatStringExpr instanceof StringLiteralNode) {
            formatStringValue = ((StringLiteralNode) formatStringExpr).valueWithQuotes;
            if (formatStringValue.length() >= 2 && formatStringValue.startsWith("\"") && formatStringValue.endsWith("\"")) {
                formatStringActualContent = formatStringValue.substring(1, formatStringValue.length() - 1);
            } else {
                formatStringActualContent = formatStringValue;
            }
        } else {
            System.err.println("警告：Printf 的格式化字符串不是一个直接的字符串字面量 AST 节点。");
            if (formatStringExpr != null) {
                formatStringExpr.generateTAC(context);
                formatStringActualContent = formatStringExpr.resultPlace;
            } else {
                System.err.println("错误：Printf 的 formatStringExpr 为 null。");
                context.emit("; ERROR_PRINTF_NULL_FORMAT_STRING_EXPR");
                return null;
            }
        }

        if (formatStringActualContent.equals("%d") && argument != null) {
            String argPlace = argument.generateTAC(context);
            context.emit("PRINT " + argPlace);
        }
        else if (argument == null && !formatStringActualContent.isEmpty()) {
            if (formatStringActualContent.contains("\\n")) {
                String[] parts = formatStringActualContent.split("\\\\n", -1);
                for (int i = 0; i < parts.length; i++) {
                    if (!parts[i].isEmpty()) {
                        context.emit("PRINT_STR \"" + parts[i] + "\"");
                    }
                    if (i < parts.length - 1 || formatStringActualContent.endsWith("\\n")) {
                        if (i < parts.length - 1 || (i == parts.length -1 && formatStringActualContent.endsWith("\\n")))
                            context.emit("PRINT_NEWLINE");
                    }
                }
            } else {
                context.emit("PRINT_STR \"" + formatStringActualContent + "\"");
            }
        } else {
            context.emit("; COMPLEX_PRINTF Format=" + formatStringActualContent + (argument != null ? " Arg=" + (argument.resultPlace != null ? argument.resultPlace : "pending_arg") : ""));
            if (argument != null) {
                argument.generateTAC(context);
            }
        }
        return null;
    }

    /**
     * 打印 printf 节点的树形结构。
     *
     * @param indent 当前节点的缩进字符串。
     * @param isLast 布尔值，指示当前节点是否为其父节点的最后一个子节点。
     * @return 表示该节点及其子树的格式化字符串。
     */
    @Override
    public String printTree(String indent, boolean isLast) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append(isLast ? "└── " : "├── ").append("PrintfNode\n");
        String newIndent = indent + (isLast ? "    " : "│   ");
        if (formatStringExpr != null) {
            sb.append(formatStringExpr.printTree(newIndent + " Format: ", argument == null));
        } else {
            sb.append(newIndent).append(argument == null ? "└── " : "├── ").append("Format: <null>\n");
        }
        if (argument != null) {
            sb.append(argument.printTree(newIndent + " Arg:    ", true));
        }
        return sb.toString();
    }
}