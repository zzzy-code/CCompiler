package Analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * SymbolTable 类实现了一个支持嵌套作用域的符号表。
 * 它使用一个栈来管理作用域，每个作用域都是一个从符号名到符号对象的映射。
 */
public class SymbolTable {
    private final Stack<Map<String, Symbol>> scopeStack;

    /**
     * SymbolTable 的构造函数。
     * 初始化作用域栈，并自动进入全局作用域。
     */
    public SymbolTable() {
        this.scopeStack = new Stack<>();
        enterScope();
    }

    /**
     * 进入一个新的作用域。
     * 在访问一个新的代码块 (BlockNode) 时调用。
     */
    public void enterScope() {
        scopeStack.push(new HashMap<>());
    }

    /**
     * 退出当前作用域。
     * 在离开一个代码块 (BlockNode) 时调用。
     */
    public void exitScope() {
        if (!scopeStack.isEmpty()) {
            scopeStack.pop();
        }
    }

    /**
     * 在当前作用域中声明一个新符号。
     * @param symbol 要声明的符号对象。
     * @throws SemanticException 如果同名符号已在当前作用域中声明。
     * @throws IllegalStateException 如果当前没有活动的作用域。
     */
    public void declare(Symbol symbol) {
        if (scopeStack.isEmpty()) {
            throw new IllegalStateException("Cannot declare symbol, no scope is active.");
        }
        Map<String, Symbol> currentScope = scopeStack.peek();

        if (currentScope.containsKey(symbol.name())) {
            throw new SemanticException("语义错误: 变量 '" + symbol.name() + "' 已在此作用域中声明。");
        }
        currentScope.put(symbol.name(), symbol);
    }

    /**
     * 从内到外查找一个符号。
     * 它会从当前作用域（栈顶）开始，逐层向外（全局作用域）查找。
     * @param name 要查找的符号名称。
     * @return 如果找到，返回对应的 Symbol 对象；如果所有作用域中都未找到，则返回 null。
     */
    public Symbol lookup(String name) {
        if (scopeStack.isEmpty()) {
            return null;
        }

        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            Map<String, Symbol> scope = scopeStack.get(i);
            if (scope.containsKey(name)) {

                return scope.get(name);
            }
        }

        return null;
    }
}