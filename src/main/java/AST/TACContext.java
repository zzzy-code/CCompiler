package AST;

import java.util.ArrayList;
import java.util.List;

/**
 * TACContext 类用于在生成三地址码 (Three-Address Code, TAC) 的过程中提供上下文环境。
 * 它负责管理临时变量的生成、标签的生成以及存储生成的 TAC 指令。
 */
public class TACContext {
    private int tempCounter = 0;
    private int labelCounter = 0;
    public List<String> instructions = new ArrayList<>();

    /**
     * 生成一个新的、唯一的临时变量名。
     * 临时变量通常用于存储表达式计算的中间结果。
     *
     * @return 新的临时变量名 (例如, "_t0", "_t1", ...)。
     */
    public String newTemp() {
        return "_t" + (tempCounter++);
    }

    /**
     * 生成一个新的、唯一的标签名。
     * 标签用于标记代码中的特定位置，以便进行跳转 (例如, GOTO L0)。
     *
     * @return 新的标签名 (例如, "L0", "L1", ...)。
     */
    public String newLabel() {
        return "L" + (labelCounter++);
    }

    /**
     * 将一条生成的三地址码指令添加到指令列表中。
     *
     * @param instruction 要添加的 TAC 指令字符串。
     */
    public void emit(String instruction) {
        this.instructions.add(instruction);
    }
}

