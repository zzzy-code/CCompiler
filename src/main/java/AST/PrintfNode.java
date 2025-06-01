package AST;

public class PrintfNode extends StatementNode {
    ExpressionNode formatStringExpr;
    ExpressionNode argument;

    public PrintfNode(ExpressionNode formatStringExpr, ExpressionNode argument) {
        this.formatStringExpr = formatStringExpr;
        this.argument = argument;
    }

    @Override
    public String generateTAC(TACContext context) {
        String formatStringValue = "";
        String formatStringActualContent = ""; // 不含引号

        if (formatStringExpr instanceof StringLiteralNode) {
            formatStringValue = ((StringLiteralNode) formatStringExpr).valueWithQuotes; // 例如 "\"%d\\n\""
            if (formatStringValue.length() >= 2 && formatStringValue.startsWith("\"") && formatStringValue.endsWith("\"")) {
                formatStringActualContent = formatStringValue.substring(1, formatStringValue.length() - 1); // 例如 "%d\n"
            } else {
                formatStringActualContent = formatStringValue;
            }
        } else {
            System.err.println("警告：Printf 的格式化字符串不是一个直接的字符串字面量 AST 节点。");
            // 尝试获取其计算值，但这通常不适用于格式字符串
            if (formatStringExpr != null) {
                formatStringExpr.generateTAC(context); // 如果它是个复杂表达式，先计算
                formatStringActualContent = formatStringExpr.resultPlace; // 使用其结果作为格式串 (非常规)
            } else {
                System.err.println("错误：Printf 的 formatStringExpr 为 null。");
                context.emit("; ERROR_PRINTF_NULL_FORMAT_STRING_EXPR");
                return null;
            }
        }

        // 根据您最新的 example.txt，printf("%d", i)
        if (formatStringActualContent.equals("%d") && argument != null) {
            String argPlace = argument.generateTAC(context);
            context.emit("PRINT " + argPlace);
        }
        // printf("Even Number") 或 printf("Odd Number")
        else if (argument == null && !formatStringActualContent.isEmpty()) {
            // 检查并处理字符串内容中的 \n (如果词法分析器保留了 \n 为 \\n)
            if (formatStringActualContent.contains("\\n")) {
                String[] parts = formatStringActualContent.split("\\\\n", -1);
                for (int i = 0; i < parts.length; i++) {
                    if (!parts[i].isEmpty()) {
                        context.emit("PRINT_STR \"" + parts[i] + "\"");
                    }
                    if (i < parts.length - 1 || formatStringActualContent.endsWith("\\n")) {
                        // 如果不是最后一部分，或者原始字符串以\n结尾，则加换行
                        // (处理 "text\n" 和 "\ntext" 以及 "\n" 本身)
                        if (i < parts.length - 1 || (i == parts.length -1 && formatStringActualContent.endsWith("\\n")))
                            context.emit("PRINT_NEWLINE");
                    }
                }
            } else { // 字符串中不包含 \n
                context.emit("PRINT_STR \"" + formatStringActualContent + "\"");
            }
        } else {
            context.emit("; COMPLEX_PRINTF Format=" + formatStringActualContent + (argument != null ? " Arg=" + (argument.resultPlace != null ? argument.resultPlace : "pending_arg") : ""));
            if (argument != null) { // 确保参数的TAC也被生成
                argument.generateTAC(context);
            }
        }
        return null;
    }

    @Override
    public String printTree(String indent, boolean isLast) { // 修改返回类型为 String
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