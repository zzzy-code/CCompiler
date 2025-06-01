package AST;

import java.util.ArrayList;
import java.util.List;

public class TACContext {
    private int tempCounter = 0;
    private int labelCounter = 0;
    public List<String> instructions = new ArrayList<>();

    public String newTemp() {
        return "_t" + (tempCounter++);
    }

    public String newLabel() {
        return "L" + (labelCounter++);
    }

    public void emit(String instruction) {
        this.instructions.add(instruction);
    }
}

