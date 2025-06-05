# 🧩 SimplePrecedenceParser：语法分析器说明

## 📌 概述

`SimplePrecedenceParser.java` 实现了一个基于简化 C 语言子集的**移进-规约式语法分析器**。该分析器采用“优先尝试规约、其次移进”的启发式策略，并支持包括 `if`、`while`、`printf`、赋值、声明等常见语句。

它不依赖传统的**优先关系表**驱动，而是采用内置规则和栈操作完成分析，适合教学或小型解释器原型开发使用。

---

## 🧪 输入示例

**输入 C 代码片段：**

```c
int main() {
    int i = 0;
    int s = 50;
    while (i <= s) {
        i = i + 3;
    }
    printf("%d\n", i);
    if (i % 2 == 0) {
        printf("Even Number\n");
    } else {
        printf("Odd Number\n");
    }
    return 0;
}
```

**对应的 Token 序列（简化形式）**：

```text
(KW_INT, int) (KW_MAIN, main) (LPAREN, () (RPAREN, )) (LBRACE, {) 
(KW_INT, int) (ID, i) (OP_ASSIGN, =) (NUM, 0) (SEMICOLON, ;) 
(KW_INT, int) (ID, s) (OP_ASSIGN, =) (NUM, 50) (SEMICOLON, ;) 
(KW_WHILE, while) (LPAREN, () (ID, i) (OP_LE, <=) (ID, s) (RPAREN, )) 
(LBRACE, {) (ID, i) (OP_ASSIGN, =) (ID, i) (OP_ADD, +) (NUM, 3) (SEMICOLON, ;) (RBRACE, }) 
(IO_PRINTF, printf) (LPAREN, () (STR, "%d\n") (COMMA, ,) (ID, i) (RPAREN, )) (SEMICOLON, ;) 
(KW_IF, if) (LPAREN, () (ID, i) (OP_MOD, %) (NUM, 2) (OP_EQ, ==) (NUM, 0) (RPAREN, )) 
(LBRACE, {) (IO_PRINTF, printf) (LPAREN, () (STR, "Even Number\n") (RPAREN, )) (SEMICOLON, ;) (RBRACE, }) 
(KW_ELSE, else) (LBRACE, {) (IO_PRINTF, printf) (LPAREN, () (STR, "Odd Number\n") (RPAREN, )) (SEMICOLON, ;) (RBRACE, }) 
(KW_RETURN, return) (NUM, 0) (SEMICOLON, ;) (RBRACE, })
```

---

## 🔁 分析过程概览

语法分析器的主循环由以下阶段构成：

### 1. 初始化

* 栈初始化为 `[$]`
* 输入 token 序列末尾添加 `$`
* 当前 token 指向输入起始位置

---

### 2. 主循环：移进-规约策略

```text
while (未接受 && 当前 token 非 $):
    → 尝试规约（tryReduce）【优先】
    → 若无法规约，则移进当前 token
```

#### 🛠 规约阶段 (`tryReduce`)

* 连续尝试从栈顶匹配文法规则右部，若匹配成功即规约为对应的非终结符
* 匹配优先级由规则长度和类型决定，例如：

  * `(KW_INT, Expr, OP_ASSIGN, Expr, SEMICOLON)` → `DeclStmt`
  * `DeclStmt` → `Stmt`，再进入 `StmtList`
* 每次规约操作都会记录步骤并尝试下一次规约

#### ➡️ 移进阶段

* 将当前 token 类型压入栈顶
* 向前推进一个 token
* 记录移进步骤

---

### 3. 接受状态判断

* 若栈状态为 `[$, Program]`，分析成功
* 若当前 token 为 `$`，触发 `tryFinalReduce()` 尝试剩余规约

---

## 🧠 核心机制与特性

| 机制/组件              | 描述                                                      |
| ------------------ | ------------------------------------------------------- |
| `tryReduce()`      | 多轮规约尝试，从长规则开始匹配，优先匹配特定语句类型                              |
| `tryFinalReduce()` | 输入结束后启发式规约，例如将 `KW_INT KW_MAIN (...) Block` 规约为 Program |
| 文法规则集              | 包括 `Program → ...`、`Stmt → ...`、`Expr → ...` 等基本规则      |
| 语法栈                | 使用字符串栈模拟文法符号栈                                           |
| `addParseStep()`   | 记录每次移进与规约，便于调试输出分析轨迹                                    |
| 优先表未用              | 尽管定义了 `precedenceTable`，但并未在主逻辑中驱动移进/规约决策               |

---

## 🔍 示例流程简要（分析 `int i = 0;`）

```text
移进 → KW_INT
移进 → ID(i) 规约 → Expr
移进 → OP_ASSIGN
移进 → NUM(0) 规约 → Expr
移进 → SEMICOLON
规约：KW_INT Expr OP_ASSIGN Expr SEMICOLON → DeclStmt
规约：DeclStmt → Stmt
规约：Stmt → StmtList（若为首条语句）
```

---

## ✅ 分析成功标志

* 栈状态为 `[$, Program]`
* 所有 token 已处理完毕
* 控制台输出 `语法分析成功完成！`

---

## 🧾 附录：文法规则（核心片段）

```text
Program    → KW_INT KW_MAIN LPAREN RPAREN Block
Block      → LBRACE StmtList RBRACE | LBRACE RBRACE
StmtList   → StmtList Stmt | Stmt
Stmt       → DeclStmt | AssignStmt | WhileStmt | IfStmt | ElseIfStmt | PrintStmt | ReturnStmt
DeclStmt   → KW_INT Expr SEMICOLON | KW_INT Expr OP_ASSIGN Expr SEMICOLON
AssignStmt → Expr OP_ASSIGN Expr SEMICOLON
Expr       → Expr OP_ADD Expr | Expr OP_MOD Expr | ID | NUM | STR
```
