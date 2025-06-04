package AssemblyGenerator;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AssemblyGenerator 类负责将三地址码 (TAC) 指令列表转换为汇编代码。
 * 它主要针对一种简化的类 8086 汇编语言。
 */
public class AssemblyGenerator {

    private List<String> assemblyCode;
    private Map<String, Integer> variableOffsets;
    private int currentOffset;

    // 用于追踪哪个临时变量是由哪个比较操作产生的 (符号, 例如 "<=", ">")
    private Map<String, String> tempVarComparisonOrigin = new HashMap<>();

    // 定义用于匹配不同类型三地址码指令的正则表达式模式
    private static final Pattern ASSIGN_BINARY_OP_PATTERN = Pattern.compile("(\\S+)\\s*=\\s*(\\S+)\\s*([+\\-*/%]|<=|==|<|>|>=|!=)\\s*(\\S+)");
    private static final Pattern ASSIGN_COPY_PATTERN = Pattern.compile("(\\S+)\\s*=\\s*(\\S+)");
    private static final Pattern IF_FALSE_GOTO_PATTERN = Pattern.compile("IF_FALSE\\s+(\\S+)\\s+GOTO\\s+(L\\d+)");
    private static final Pattern GOTO_PATTERN = Pattern.compile("GOTO\\s+(L\\d+)");
    private static final Pattern LABEL_PATTERN = Pattern.compile("(L\\d+):");
    private static final Pattern DECLARE_PATTERN = Pattern.compile("DECLARE\\s+(\\S+)");
    private static final Pattern PRINT_VAR_PATTERN = Pattern.compile("PRINT\\s+(\\S+)");
    private static final Pattern PRINT_STR_PATTERN = Pattern.compile("PRINT_STR\\s+\"([^\"]*)\"");
    private static final Pattern PRINT_NEWLINE_PATTERN = Pattern.compile("PRINT_NEWLINE");
    private static final Pattern RETURN_PATTERN = Pattern.compile("RETURN\\s+(\\S+)");

    /**
     * AssemblyGenerator 的构造函数。
     * 初始化汇编代码列表、变量偏移量映射和当前偏移量。
     */
    public AssemblyGenerator() {
        this.assemblyCode = new ArrayList<>();
        this.variableOffsets = new HashMap<>();
        this.currentOffset = -2;
    }

    /**
     * 获取变量、临时变量或字面量在汇编代码中的表示形式。
     *
     * @param varOrTempOrLiteral 变量名、临时变量名 (如 _t1) 或数字字面量。
     * @return 其在汇编中的表示，例如 "WORD PTR [BP-2]", "123", 或 "AX" (假设临时变量在 AX)。
     */
    private String getVarAssemblyPlace(String varOrTempOrLiteral) {
        if (variableOffsets.containsKey(varOrTempOrLiteral)) {
            return "WORD PTR [BP" + (variableOffsets.get(varOrTempOrLiteral)) + "]";
        } else if (varOrTempOrLiteral.matches("-?\\d+")) {
            return varOrTempOrLiteral;
        } else if (varOrTempOrLiteral.startsWith("_t")) {
            return "AX";
        }
        System.err.println("汇编警告: 未找到变量/临时变量 '" + varOrTempOrLiteral + "' 的存储位置，将直接使用其名。");
        return varOrTempOrLiteral;
    }

    /**
     * 将操作数加载到指定的寄存器。
     *
     * @param operand  要加载的操作数 (变量名、临时变量名或字面量)。
     * @param register 目标寄存器名 (例如, "AX", "BX")。
     */
    private void loadOperandToRegister(String operand, String register) {
        if (operand.matches("-?\\d+")) {
            assemblyCode.add("    MOV " + register + ", " + operand);
        } else if (variableOffsets.containsKey(operand)) {
            assemblyCode.add("    MOV " + register + ", " + getVarAssemblyPlace(operand));
        } else if (operand.startsWith("_t")) {
            if (!register.equals("AX")) {
                assemblyCode.add("    MOV " + register + ", AX ; 从 AX (假设存有 " + operand + ") 复制到 " + register);
            }
        } else {
            System.err.println("汇编错误: 无法加载未知操作数 '" + operand + "' 到寄存器 " + register);
            assemblyCode.add("    ; 错误: 无法加载操作数 " + operand);
        }
    }

    /**
     * 主生成方法，将三地址码指令列表转换为汇编代码列表。
     *
     * @param tacInstructions 输入的三地址码指令列表。
     * @return 生成的汇编代码行列表。
     */
    public List<String> generate(List<String> tacInstructions) {
        assemblyCode.clear();
        variableOffsets.clear();
        tempVarComparisonOrigin.clear();
        currentOffset = -2;

        assemblyCode.add(".MODEL SMALL");
        assemblyCode.add(".STACK 100H");
        assemblyCode.add(".DATA");
        int stringCounter = 0;
        Map<String, String> stringLabelMap = new HashMap<>();
        for (String tac : tacInstructions) {
            Matcher m = PRINT_STR_PATTERN.matcher(tac);
            if (m.matches()) {
                String strContent = m.group(1);
                if (!stringLabelMap.containsKey(strContent)) {
                    String label = "msg" + stringCounter++;
                    assemblyCode.add(label + " DB '" + strContent + "', '$'");
                    stringLabelMap.put(strContent, label);
                }
            }
        }
        assemblyCode.add("newline_char DB 0DH, 0AH, '$'");
        assemblyCode.add("num_buffer DB 7 DUP('$') ; 缓冲区用于数字转换");

        assemblyCode.add(".CODE");
        assemblyCode.add("MAIN PROC");
        assemblyCode.add("    MOV AX, @DATA");
        assemblyCode.add("    MOV DS, AX");
        assemblyCode.add("");
        assemblyCode.add("    PUSH BP");
        assemblyCode.add("    MOV BP, SP");
        assemblyCode.add("");

        for (String tac : tacInstructions) {
            assemblyCode.add("    ; TAC: " + tac);
            Matcher m;

            if (tac.equals("START_PROGRAM") || tac.equals("END_PROGRAM")) {
                continue;
            }

            // 处理 DECLARE 指令 (变量声明)
            m = DECLARE_PATTERN.matcher(tac);
            if (m.matches()) {
                String varName = m.group(1);
                if (!variableOffsets.containsKey(varName)) {
                    assemblyCode.add("    SUB SP, 2       ; 为 " + varName + " 在栈上分配空间 [BP" + currentOffset + "]");
                    variableOffsets.put(varName, currentOffset);
                    currentOffset -= 2;
                }
                continue;
            }

            // 处理赋值带二元运算的指令 (例如, dest = op1 + op2)
            m = ASSIGN_BINARY_OP_PATTERN.matcher(tac);
            if (m.matches()) {
                String dest = m.group(1);
                String op1 = m.group(2);
                String symbol = m.group(3);
                String op2 = m.group(4);

                loadOperandToRegister(op1, "AX");
                loadOperandToRegister(op2, "BX");

                switch (symbol) {
                    case "+":
                        assemblyCode.add("    ADD AX, BX");
                        break;
                    case "*":
                        assemblyCode.add("    IMUL BX         ; AX = AX * BX");
                        break;
                    case "%":
                        assemblyCode.add("    CWD             ; 符号扩展 AX 到 DX:AX (为 IDIV)");
                        assemblyCode.add("    IDIV BX         ; AX = 商, DX = 余数");
                        assemblyCode.add("    MOV AX, DX      ; 余数到 AX");
                        break;
                    case "<=":
                    case "==":
                    case ">":
                    case "<":
                    case ">=":
                    case "!=":
                        assemblyCode.add("    CMP AX, BX");
                        tempVarComparisonOrigin.put(dest, symbol);
                        break;
                    default:
                        assemblyCode.add("    ; 未知或未处理的二元操作符: " + symbol);
                }

                if (variableOffsets.containsKey(dest) && !symbol.matches("<=|==|>|<|>=|!=")) {
                    assemblyCode.add("    MOV " + getVarAssemblyPlace(dest) + ", AX");
                }
                continue;
            }

            // 处理简单赋值/拷贝指令 (例如, dest = source)
            m = ASSIGN_COPY_PATTERN.matcher(tac);
            if (m.matches()) {
                String dest = m.group(1);
                String source = m.group(2);
                loadOperandToRegister(source, "AX");
                assemblyCode.add("    MOV " + getVarAssemblyPlace(dest) + ", AX");
                continue;
            }

            // 处理 IF_FALSE GOTO 指令 (条件跳转)
            m = IF_FALSE_GOTO_PATTERN.matcher(tac);
            if (m.matches()) {
                String condVar = m.group(1);
                String label = m.group(2);
                String originalComparison = tempVarComparisonOrigin.get(condVar);

                if (originalComparison != null) {
                    switch (originalComparison) {
                        case "<=": assemblyCode.add("    JG " + label + "  ; !(A <= B) => (A > B)"); break;
                        case "==": assemblyCode.add("    JNE " + label + " ; !(A == B) => (A != B)"); break;
                        case ">":  assemblyCode.add("    JLE " + label + " ; !(A > B)  => (A <= B)"); break;
                        case "<":  assemblyCode.add("    JGE " + label + " ; !(A < B)  => (A >= B)"); break;
                        case ">=": assemblyCode.add("    JL " + label + "  ; !(A >= B) => (A < B)"); break;
                        case "!=": assemblyCode.add("    JE " + label + "  ; !(A != B) => (A == B)"); break;
                        default:
                            assemblyCode.add("    ; IF_FALSE " + condVar + " (源比较 '" + originalComparison + "' 未处理) GOTO " + label);
                            loadOperandToRegister(condVar, "AX");
                            assemblyCode.add("    CMP AX, 0");
                            assemblyCode.add("    JE " + label);
                            break;
                    }
                } else {
                    assemblyCode.add("    ; IF_FALSE " + condVar + " (无源比较信息) GOTO " + label);
                    loadOperandToRegister(condVar, "AX");
                    assemblyCode.add("    CMP AX, 0          ; 假设 0 为 false");
                    assemblyCode.add("    JE " + label + "       ; 如果 AX == 0 (false) 则跳转");
                }
                continue;
            }

            // 处理 GOTO 指令 (无条件跳转)
            m = GOTO_PATTERN.matcher(tac);
            if (m.matches()) {
                assemblyCode.add("    JMP " + m.group(1));
                continue;
            }

            // 处理标签定义
            m = LABEL_PATTERN.matcher(tac);
            if (m.matches()) {
                assemblyCode.add(m.group(1) + ":");
                continue;
            }

            // 处理 PRINT (打印变量) 指令
            m = PRINT_VAR_PATTERN.matcher(tac);
            if (m.matches()) {
                String varToPrint = m.group(1);
                loadOperandToRegister(varToPrint, "AX");
                assemblyCode.add("    CALL PRINT_NUM");
                assemblyCode.add("    CALL PRINT_NEWLINE  ; <<< 所有输出后自动换行");
                continue;
            }

            // 处理 PRINT_STR (打印字符串) 指令
            m = PRINT_STR_PATTERN.matcher(tac);
            if (m.matches()) {
                String strContent = m.group(1);
                String msgLabel = stringLabelMap.get(strContent);
                if (msgLabel != null) {
                    assemblyCode.add("    LEA DX, " + msgLabel);
                    assemblyCode.add("    MOV AH, 09H");
                    assemblyCode.add("    INT 21H");
                    assemblyCode.add("    CALL PRINT_NEWLINE  ; <<< 所有输出后自动换行");
                } else {
                    assemblyCode.add("    ; 错误: 找不到字符串 '" + strContent + "' 对应的消息标签");
                }
                continue;
            }

            // 处理 PRINT_NEWLINE (打印换行) 指令
            m = PRINT_NEWLINE_PATTERN.matcher(tac);
            if (m.matches()) {
                assemblyCode.add("    CALL PRINT_NEWLINE");
                continue;
            }

            // 处理 RETURN 指令
            m = RETURN_PATTERN.matcher(tac);
            if (m.matches()) {
                String retVal = m.group(1);
                if (retVal.equals("0")) {
                    assemblyCode.add("    MOV AL, 0           ; 直接将返回码 0 放入 AL");
                } else if (retVal.matches("-?\\d+")) {
                    assemblyCode.add("    MOV AL, " + retVal + "      ; 设置返回码");
                } else {
                    loadOperandToRegister(retVal, "AX");
                    assemblyCode.add("    MOV AL, AL          ; AL 是 AX 的低字节, 作为返回码");
                }
                assemblyCode.add("    MOV AH, 4CH         ; DOS 终止程序功能号");
                assemblyCode.add("    INT 21H");
                continue;
            }
            assemblyCode.add("    ; 未能翻译的TAC: " + tac);
        }

        // 添加主过程的结束部分和程序结束标记
        assemblyCode.add("");
        assemblyCode.add("    POP BP");
        assemblyCode.add("    RET");
        assemblyCode.add("MAIN ENDP");
        assemblyCode.add("");
        addPrintNumProcedure();
        addPrintNewlineProcedure();
        assemblyCode.add("END MAIN");

        return assemblyCode;
    }

    /**
     * 向汇编代码列表中添加用于打印 AX 寄存器中16位有符号整数的子过程 (PRINT_NUM)。
     * 该过程处理负数、零，并将数字转换为字符串后使用 DOS 功能打印。
     */
    private void addPrintNumProcedure() {
        assemblyCode.addAll(Arrays.asList(
                "; 过程: PRINT_NUM - 打印 AX 中的16位有符号整数",
                "PRINT_NUM PROC",
                "    PUSH AX             ; 保存寄存器",
                "    PUSH BX",
                "    PUSH CX",
                "    PUSH DX",
                "    PUSH DI             ; 使用 DI 作为缓冲区指针",
                "    MOV CX, 0           ; 计数器，记录数字位数",
                "    CMP AX, 0           ; 检查是否为0",
                "    JGE positive_num_pm ; 如果是正数或0，跳转 (pm for PrintNum)",
                "    PUSH AX             ; 保存负号的AX",
                "    MOV AH, 02H         ; DOS功能：显示字符",
                "    MOV DL, '-'         ; 打印负号",
                "    INT 21H",
                "    POP AX              ; 恢复AX",
                "    NEG AX              ; AX = -AX (取绝对值)",
                "positive_num_pm:",
                "    CMP AX, 0           ; 再次检查是否为0 (原先就是0的情况)",
                "    JNE convert_loop_pm ; 如果不是0，开始转换",
                "    ; 如果 AX 就是 0",
                "    MOV AH, 02H         ; 直接打印 '0'",
                "    MOV DL, '0'",
                "    INT 21H",
                "    JMP print_done_num_pm  ; 跳转到结束",
                "convert_loop_pm:",
                "    MOV BX, 10          ; 除数",
                "    MOV DX, 0           ; 清除DX (高位)",
                "    DIV BX              ; AX = AX / 10, DX = AX % 10 (余数)",
                "    PUSH DX             ; 将余数 (数字位) 压栈",
                "    INC CX              ; 位数加1",
                "    CMP AX, 0           ; 商是否为0?",
                "    JNE convert_loop_pm ; 如果不为0，继续循环",
                "",
                "    ; CX 现在是数字的位数",
                "    LEA DI, num_buffer  ; DI 指向 num_buffer 的开始",
                "print_digits_to_buffer_loop_pm:",
                "    POP DX              ; 从栈中弹出数字位 (余数)",
                "    ADD DL, '0'         ; 转换为ASCII字符",
                "    MOV [DI], DL        ; 将字符存入缓冲区由 DI 指向的位置",
                "    INC DI              ; 移动缓冲区指针到下一个位置",
                "    LOOP print_digits_to_buffer_loop_pm ; 使用 CX 循环",
                "",
                "    MOV BYTE PTR [DI], '$' ; 在缓冲区当前 DI 位置 (即数字末尾) 添加字符串结束符 '$'",
                "",
                "    LEA DX, num_buffer  ; DX 指向要打印的字符串 (num_buffer)",
                "    MOV AH, 09H         ; DOS 功能：显示字符串",
                "    INT 21H",
                "",
                "print_done_num_pm:",
                "    POP DI              ; 恢复寄存器",
                "    POP DX",
                "    POP CX",
                "    POP BX",
                "    POP AX",
                "    RET",
                "PRINT_NUM ENDP",
                ""
        ));
    }

    /**
     * 向汇编代码列表中添加用于打印回车换行符的子过程 (PRINT_NEWLINE)。
     */
    private void addPrintNewlineProcedure() {
        assemblyCode.addAll(Arrays.asList(
                "; 过程: PRINT_NEWLINE - 打印回车换行",
                "PRINT_NEWLINE PROC",
                "    PUSH AX",
                "    PUSH DX",
                "    LEA DX, newline_char",
                "    MOV AH, 09H",
                "    INT 21H",
                "    POP DX",
                "    POP AX",
                "    RET",
                "PRINT_NEWLINE ENDP",
                ""
        ));
    }

}