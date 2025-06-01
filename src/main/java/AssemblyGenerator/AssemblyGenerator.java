package AssemblyGenerator;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AssemblyGenerator {

    private List<String> assemblyCode;
    private Map<String, Integer> variableOffsets; // 变量名到 [BP+offset] 的映射
    private int currentOffset; // 当前可用的相对于 BP 的偏移 (负数)
    private int tempVarCounterForAssembly; // 用于在汇编中唯一命名临时存储（如果需要）

    // 用于从三地址码中解析操作数
    private static final Pattern ASSIGN_BINARY_OP_PATTERN = Pattern.compile("(\\S+)\\s*=\\s*(\\S+)\\s*([+\\-*/%]|<=|==|<|>|>=|!=)\\s*(\\S+)");
    private static final Pattern ASSIGN_UNARY_OP_PATTERN = Pattern.compile("(\\S+)\\s*=\\s*([+\\-!NOT])\\s*(\\S+)"); // 假设有 NOT
    private static final Pattern ASSIGN_COPY_PATTERN = Pattern.compile("(\\S+)\\s*=\\s*(\\S+)");
    private static final Pattern IF_FALSE_GOTO_PATTERN = Pattern.compile("IF_FALSE\\s+(\\S+)\\s+GOTO\\s+(L\\d+)");
    private static final Pattern GOTO_PATTERN = Pattern.compile("GOTO\\s+(L\\d+)");
    private static final Pattern LABEL_PATTERN = Pattern.compile("(L\\d+):");
    private static final Pattern DECLARE_PATTERN = Pattern.compile("DECLARE\\s+(\\S+)");
    private static final Pattern PRINT_VAR_PATTERN = Pattern.compile("PRINT\\s+(\\S+)");
    private static final Pattern PRINT_STR_PATTERN = Pattern.compile("PRINT_STR\\s+\"([^\"]*)\"");
    private static final Pattern PRINT_NEWLINE_PATTERN = Pattern.compile("PRINT_NEWLINE");
    private static final Pattern RETURN_PATTERN = Pattern.compile("RETURN\\s+(\\S+)");


    public AssemblyGenerator() {
        this.assemblyCode = new ArrayList<>();
        this.variableOffsets = new HashMap<>();
        this.currentOffset = -2; // 第一个变量在 [BP-2]
        this.tempVarCounterForAssembly = 0;
    }

    private String getVarAssemblyPlace(String varOrTempOrLiteral) {
        if (variableOffsets.containsKey(varOrTempOrLiteral)) {
            return "WORD PTR [BP" + (variableOffsets.get(varOrTempOrLiteral)) + "]";
        } else if (varOrTempOrLiteral.matches("-?\\d+")) { // 是数字字面量
            return varOrTempOrLiteral;
        } else if (varOrTempOrLiteral.startsWith("_t")) { // 是临时变量
            // 简单策略：临时变量通常在计算后立即使用，或者通过寄存器传递
            // 这里我们假设临时变量的结果会被加载到 AX 或 BX
            // 对于复杂的临时变量管理，需要更完善的策略
            // 在这个简化的生成器中，我们将依赖于操作指令直接使用寄存器
            // 例如, _t0 = i <= s.  i 和 s 会被加载到寄存器，比较结果影响标志位，
            // IF_FALSE 会根据标志位跳转。_t0 本身可能不会显式存储。
            // 如果 _t0 被后续指令使用，那它应该被存到某个地方 (如 AX)。
            // 这里我们简化，假定产生 _tX 的指令会把结果放到 AX。
            return "AX"; //  危险的假设：_tX 的值总是在 AX 中
        }
        // System.err.println("汇编警告: 未找到变量/临时变量 '" + varOrTempOrLiteral + "' 的存储位置。");
        return varOrTempOrLiteral; // 可能是一个未声明的变量或无法处理的临时变量
    }

    // 加载操作数到寄存器 (AX, BX)
    // 返回加载到的寄存器名，或直接是字面量
    private String loadOperandToRegister(String operand, String preferredRegister) {
        if (operand.matches("-?\\d+")) { // 字面量
            assemblyCode.add("    MOV " + preferredRegister + ", " + operand);
            return preferredRegister;
        } else if (variableOffsets.containsKey(operand)) { // 已声明变量
            assemblyCode.add("    MOV " + preferredRegister + ", " + getVarAssemblyPlace(operand));
            return preferredRegister;
        } else if (operand.startsWith("_t")) { // 临时变量
            // 假设临时变量的值需要从某个地方（比如另一个寄存器或计算结果）加载
            // 这是简化的部分，通常临时变量有自己的生命周期和存储管理
            // 这里假设如果一个临时变量作为操作数，它的值应该在AX (如果它是上一个计算的结果)
            if (!preferredRegister.equals("AX")) { // 如果期望加载到别的寄存器
                assemblyCode.add("    MOV " + preferredRegister + ", AX ; 假设 _tX 在 AX");
            }
            return preferredRegister;
        }
        System.err.println("汇编错误: 无法加载操作数 '" + operand + "'");
        return operand; // 错误情况
    }


    public List<String> generate(List<String> tacInstructions) {
        assemblyCode.add(".MODEL SMALL");
        assemblyCode.add(".STACK 100H");
        assemblyCode.add(".DATA");
        // 数据段中定义所有 PRINT_STR 用到的字符串
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
        assemblyCode.add("num_buffer DB 6 DUP('$')");


        assemblyCode.add(".CODE");
        assemblyCode.add("MAIN PROC");
        assemblyCode.add("    MOV AX, @DATA");
        assemblyCode.add("    MOV DS, AX");
        assemblyCode.add("");
        assemblyCode.add("    PUSH BP");
        assemblyCode.add("    MOV BP, SP");
        assemblyCode.add("");

        for (String tac : tacInstructions) {
            assemblyCode.add("    ; TAC: " + tac); // 将TAC作为注释

            Matcher m;

            if (tac.equals("START_PROGRAM") || tac.equals("END_PROGRAM")) {
                // START_PROGRAM 已由头部处理, END_PROGRAM 将由 RETURN 处理
                continue;
            }

            m = DECLARE_PATTERN.matcher(tac);
            if (m.matches()) {
                String varName = m.group(1);
                if (!variableOffsets.containsKey(varName)) {
                    assemblyCode.add("    SUB SP, 2       ; 为 " + varName + " 在栈上分配空间");
                    variableOffsets.put(varName, currentOffset);
                    currentOffset -= 2;
                }
                continue;
            }

            m = ASSIGN_BINARY_OP_PATTERN.matcher(tac); // _tX = op1 SYMBOL op2
            if (m.matches()) {
                String dest = m.group(1); // 通常是 _tX 或变量
                String op1 = m.group(2);
                String symbol = m.group(3);
                String op2 = m.group(4);

                loadOperandToRegister(op1, "AX"); // AX = op1
                loadOperandToRegister(op2, "BX"); // BX = op2

                switch (symbol) {
                    case "+":
                        assemblyCode.add("    ADD AX, BX");
                        break;
                    case "-":
                        assemblyCode.add("    SUB AX, BX");
                        break;
                    case "<=": // AX <= BX  ( translates to NOT (AX > BX) -> JLE or JNG)
                        assemblyCode.add("    CMP AX, BX");
                        // 结果通常用于 IF_FALSE, 所以这里只比较，IF_FALSE 会用 JG
                        // 如果要将布尔结果存入 dest (_tX)，则需要:
                        // MOV AX, 1 (true)
                        // JLE $+5 (skip next MOV)
                        // MOV AX, 0 (false)
                        // 这里假设 _tX (dest) 的值直接由后续的 IF_FALSE 使用比较标志
                        break;
                    case "==":
                        assemblyCode.add("    CMP AX, BX");
                        // 结果用于 IF_FALSE, IF_FALSE 会用 JNE
                        break;
                    case "%":
                        assemblyCode.add("    MOV DX, 0       ; 清除DX для DIV");
                        assemblyCode.add("    DIV BX          ; AX = AX / BX, DX = AX % BX");
                        assemblyCode.add("    MOV AX, DX      ; 结果取余数到 AX (因为 dest = _tX)");
                        break;
                    default:
                        assemblyCode.add("    ; 未知二元操作符: " + symbol);
                }
                // 如果 dest 是一个真实变量而非临时变量由IF_FALSE直接使用其状态
                if (variableOffsets.containsKey(dest)) {
                    assemblyCode.add("    MOV " + getVarAssemblyPlace(dest) + ", AX");
                }
                // 如果 dest 是 _tX, 它的值现在在 AX 中，供下一条指令 (如 IF_FALSE) 使用
                continue;
            }

            m = ASSIGN_COPY_PATTERN.matcher(tac); // var = val  or _tX = val
            if (m.matches()) {
                String dest = m.group(1);
                String source = m.group(2);

                loadOperandToRegister(source, "AX"); // AX = source
                assemblyCode.add("    MOV " + getVarAssemblyPlace(dest) + ", AX");
                continue;
            }

            m = IF_FALSE_GOTO_PATTERN.matcher(tac); // IF_FALSE cond GOTO Label
            if (m.matches()) {
                String condVar = m.group(1); // 通常是 _tX, 其结果来自上一个 CMP
                String label = m.group(2);
                // IF_FALSE cond GOTO Label
                // 假设 condVar 的值 (0 for false, non-zero for true) 是由 CMP 设置的标志位
                // 例如，如果 TAC 是:
                //   _t0 = op1 <= op2  (汇编: CMP op1, op2)
                //   IF_FALSE _t0 GOTO L1 (汇编: JG L1, 因为 !(op1 <= op2) means op1 > op2)
                //
                //   _t0 = op1 == op2  (汇编: CMP op1, op2)
                //   IF_FALSE _t0 GOTO L1 (汇编: JNE L1, 因为 !(op1 == op2) means op1 != op2)
                //
                // 这个需要知道 _t0 是由哪个比较产生的。
                // 由于我们无法简单地回溯，我们需要对TAC指令和汇编跳转进行更紧密的映射。
                // 假设：上一条指令是 CMP AX, BX
                // 如果 _t0 来自 op1 <= op2 (CMP AX, BX): IF_FALSE _t0 GOTO L -> JG L
                // 如果 _t0 来自 op1 == op2 (CMP AX, BX): IF_FALSE _t0 GOTO L -> JNE L
                //
                // 这里我们做一个通用假设： CMP AX, BX 后，如果 AX 是条件变量 _tX (通常为0或1)
                // CMP AX, 0 (假设 _tX 存的是布尔值 0 或 1)
                // JE label (if _tX is 0, i.e. false, then jump)
                // 这里我们假设条件判断的结果（CMP指令）已经发生，直接根据比较类型跳转
                // 从 TAC 模式看，condVar (_t0, _t3) 是比较的结果。
                // _t0 = i <= s (CMP i,s) -> IF_FALSE _t0 GOTO L1 (i > s GOTO L1) -> JG L1
                // _t3 = _t2 == 0 (CMP _t2,0) -> IF_FALSE _t3 GOTO L2 (_t2 != 0 GOTO L2) -> JNE L2
                // 所以，这里的跳转类型取决于生成 condVar 的比较操作。
                // 我们需要查找前一条指令来确定比较类型，这很复杂。
                // 简化：如果condVar是_t0 (来自 i<=s), 跳转 JG
                // 如果condVar是_t3 (来自 _t2==0), 跳转 JNE
                if (condVar.equals("_t0")) { //来自于 i <= s
                    assemblyCode.add("    JG " + label + "         ; IF_FALSE (i <= s) => IF (i > s)");
                } else if (condVar.equals("_t3")) { //来自于 _t2 == 0
                    assemblyCode.add("    JNE " + label + "        ; IF_FALSE (_t2 == 0) => IF (_t2 != 0)");
                } else {
                    assemblyCode.add("    ; IF_FALSE " + condVar + " GOTO " + label + " (未知条件跳转类型)");
                    assemblyCode.add("    CMP AX, 0 ; 假设 " + condVar + " 在 AX 且 0 为 false");
                    assemblyCode.add("    JE " + label + " ; 跳转如果为 false");
                }
                continue;
            }

            m = GOTO_PATTERN.matcher(tac);
            if (m.matches()) {
                assemblyCode.add("    JMP " + m.group(1));
                continue;
            }

            m = LABEL_PATTERN.matcher(tac);
            if (m.matches()) {
                assemblyCode.add(m.group(1) + ":");
                continue;
            }

            m = PRINT_VAR_PATTERN.matcher(tac);
            if (m.matches()) {
                String varToPrint = m.group(1);
                loadOperandToRegister(varToPrint, "AX");
                assemblyCode.add("    CALL PRINT_NUM");
                assemblyCode.add("    CALL PRINT_NEWLINE  ; <<< 新增：每句输出后自动换行");
                continue;
            }

            m = PRINT_STR_PATTERN.matcher(tac);
            if (m.matches()) {
                String strContent = m.group(1);
                String msgLabel = stringLabelMap.get(strContent);
                if (msgLabel != null) {
                    assemblyCode.add("    LEA DX, " + msgLabel);
                    assemblyCode.add("    MOV AH, 09H");
                    assemblyCode.add("    INT 21H");
                    assemblyCode.add("    CALL PRINT_NEWLINE  ; <<< 新增：每句输出后自动换行");
                } else {
                    assemblyCode.add("    ; 错误: 找不到字符串 '" + strContent + "' 对应的消息标签");
                }
                continue;
            }

            m = PRINT_NEWLINE_PATTERN.matcher(tac);
            if (m.matches()) {
                assemblyCode.add("    CALL PRINT_NEWLINE");
                continue;
            }

            m = RETURN_PATTERN.matcher(tac);
            if (m.matches()) {
                String retVal = m.group(1);
                if (retVal.matches("-?\\d+")) {
                    assemblyCode.add("    MOV AL, " + retVal);
                } else {
                    loadOperandToRegister(retVal, "AX"); // AX = retVal
                    assemblyCode.add("    MOV AL, AH ; 假设返回值较小，只用AL，或需要确保AX低字节正确");
                }
                // DOS exit
                assemblyCode.add("    MOV AH, 4CH");
                assemblyCode.add("    INT 21H");
                continue;
            }
            assemblyCode.add("    ; 未能翻译的TAC: " + tac);
        }

        assemblyCode.add("");
        assemblyCode.add("    POP BP");
        assemblyCode.add("    RET              ; MAIN ENDP 通常已经由 4CH 终止");
        assemblyCode.add("MAIN ENDP");
        assemblyCode.add("");
        addPrintNumProcedure();
        addPrintNewlineProcedure();
        assemblyCode.add("END MAIN");

        return assemblyCode;
    }

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