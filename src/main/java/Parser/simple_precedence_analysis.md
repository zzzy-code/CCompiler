# SimplePrecedenceParser1 语法分析步骤

概述

`SimplePrecedenceParser1.java` 实现了一个简单优先语法分析器，用于解析简单的 C 语言程序结构。它通过优先关系表和移进-规约操作，验证输入 token 序列是否符合定义的文法。本文档展示了分析以下 C 程序的详细步骤：

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

对应的 token 字符串为：

```
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

## 语法分析方法

- **简单优先语法分析**：基于符号间的优先关系（`<`、`=`、`>`），通过移进（shift）和规约（reduce）操作解析 token 序列。
- **优先关系表**：定义了符号优先级（如 `OP_MOD` &gt; `OP_ADD` &gt; `OP_LE` &gt; `OP_EQ`）和特殊关系（如 `LPAREN = RPAREN`）。
- **文法规则**：定义了程序结构、语句、表达式等，核心规则包括：
  - `Program → KW_INT KW_MAIN LPAREN RPAREN Block`
  - `Block → LBRACE StmtList RBRACE`
  - `Stmt → DeclStmt | AssignStmt | WhileStmt | IfStmt | ElseIfStmt | PrintStmt | ReturnStmt`
  - `Expr → Expr OP_ADD Expr | ID | NUM | STR` 等

## 分析步骤

以下是语法分析的详细步骤，展示栈状态、当前 token 和操作。栈以 `[..., 符号]` 形式显示，省略前缀以突出最近变化。

1. **初始化**

  - 栈：`[$]`
  - 当前 token：`(KW_INT, int)`
  - 操作：初始化栈，添加结束符 `$`。

2. **移进 KW_INT**

  - 当前 token：`(KW_INT, int)`
  - 栈：`[..., $ , KW_INT]`
  - 优先关系：`$ < KW_INT`
  - 操作：移进 `KW_INT`
  - 说明：将 `int` 压入栈，开始解析 `int main()`。

3. **移进 KW_MAIN**

  - 当前 token：`(KW_MAIN, main)`
  - 栈：`[..., $ , KW_INT , KW_MAIN]`
  - 优先关系：默认 `<`
  - 操作：移进 `KW_MAIN`

4. **移进 LPAREN**

  - 当前 token：`(LPAREN, ()`
  - 栈：`[..., KW_INT , KW_MAIN , LPAREN]`
  - 操作：移进 `LPAREN`

5. **移进 RPAREN**

  - 当前 token：`(RPAREN, ))`
  - 栈：`[..., KW_MAIN , LPAREN , RPAREN]`
  - 优先关系：`LPAREN = RPAREN`
  - 操作：移进 `RPAREN`

6. **移进 LBRACE**

  - 当前 token：`(LBRACE, {)`
  - 栈：`[..., LPAREN , RPAREN , LBRACE]`
  - 操作：移进 `LBRACE`

7. **移进 KW_INT (int i = 0;)**

  - 当前 token：`(KW_INT, int)`
  - 栈：`[..., LBRACE , KW_INT]`
  - 操作：移进 `KW_INT`

8. **移进 ID, 规约 Expr**

  - 当前 token：`(ID, i)`
  - 栈：`[..., LBRACE , KW_INT , ID]`
  - 操作：移进 `ID`，规约 `Expr → ID`
  - 栈：`[..., LBRACE , KW_INT , Expr]`

9. **移进 OP_ASSIGN, NUM, 规约 Expr**

  - 当前 token：`(OP_ASSIGN, =), (NUM, 0)`
  - 栈：`[..., KW_INT , Expr , OP_ASSIGN , NUM]`
  - 操作：移进 `OP_ASSIGN`, `NUM`，规约 `Expr → NUM`
  - 栈：`[..., KW_INT , Expr , OP_ASSIGN , Expr]`

10. **移进 SEMICOLON, 规约 DeclStmt**

  - 当前 token：`(SEMICOLON, ;)`
  - 栈：`[..., Expr , OP_ASSIGN , Expr , SEMICOLON]`
  - 操作：移进 `SEMICOLON`，规约 `DeclStmt → KW_INT Expr OP_ASSIGN Expr SEMICOLON`
  - 栈：`[..., LBRACE , DeclStmt]`

11. **规约 Stmt**

  - 栈：`[..., LBRACE , DeclStmt]`
  - 操作：规约 `Stmt → DeclStmt`
  - 栈：`[..., LBRACE , Stmt]`

12. **处理 int s = 50;（类似步骤 7-11）**

  - 最终栈：`[..., LBRACE , Stmt , Stmt]`
  - 操作：移进并规约，得到第二个 `Stmt`

13. **规约 StmtList**

  - 栈：`[..., LBRACE , Stmt , Stmt]`
  - 操作：规约 `StmtList → StmtList Stmt`（假设第一个 `Stmt` 为 `StmtList`）
  - 栈：`[..., LBRACE , StmtList]`

14. **处理 while (i &lt;= s) {...}**

  - 移进 `KW_WHILE, LPAREN, ID, OP_LE, ID, RPAREN`
  - 规约 `ID → Expr`, `ID → Expr`, `Expr OP_LE Expr → Expr`
  - 移进 `LBRACE, ID, OP_ASSIGN, ID, OP_ADD, NUM, SEMICOLON, RBRACE`
  - 规约 `ID → Expr`, `NUM → Expr`, `Expr OP_ADD Expr → Expr`, `Expr OP_ASSIGN Expr SEMICOLON → AssignStmt`, `Stmt → AssignStmt`, `Stmt → StmtList`, `Block → LBRACE StmtList RBRACE`
  - 规约 `WhileStmt → KW_WHILE Expr Block`, `Stmt → WhileStmt`
  - 栈：`[..., LBRACE , StmtList , Stmt]`

15. **处理 printf("%d\\n", i);**

  - 移进 `IO_PRINTF, LPAREN, STR, COMMA, ID, RPAREN, SEMICOLON`
  - 规约 `STR → Expr`, `ID → Expr`, `PrintStmt → IO_PRINTF LPAREN Expr COMMA Expr RPAREN SEMICOLON`, `Stmt → PrintStmt`
  - 栈：`[..., LBRACE , StmtList , Stmt]`

16. **处理 if (i % 2 == 0) {...} else {...}**

  - 移进 `KW_IF, LPAREN, ID, OP_MOD, NUM, OP_EQ, NUM, RPAREN`
  - 规约 `ID → Expr`, `NUM → Expr`, `Expr OP_MOD Expr → Expr`, `Expr OP_EQ Expr → Expr`
  - 移进 `LBRACE, IO_PRINTF, LPAREN, STR, RPAREN, SEMICOLON, RBRACE`
  - 规约 `STR → Expr`, `PrintStmt → IO_PRINTF LPAREN Expr RPAREN SEMICOLON`, `Stmt → PrintStmt`, `Block → LBRACE StmtList RBRACE`
  - 规约 `IfStmt → KW_IF Expr Block`
  - 移进 `KW_ELSE, LBRACE, IO_PRINTF, LPAREN, STR, RPAREN, SEMICOLON, RBRACE`
  - 规约 `ElseIfStmt → KW_ELSE Block`, `Stmt → ElseIfStmt`
  - 栈：`[..., LBRACE , StmtList , Stmt]`

17. **处理 return 0;**

  - 移进 `KW_RETURN, NUM, SEMICOLON`
  - 规约 `NUM → Expr`, `ReturnStmt → KW_RETURN Expr SEMICOLON`, `Stmt → ReturnStmt`
  - 栈：`[..., LBRACE , StmtList , Stmt]`

18. **移进 RBRACE, 规约 Block**

  - 移进 `(RBRACE, })`
  - 规约 `Block → LBRACE StmtList RBRACE`
  - 栈：`[..., KW_INT , KW_MAIN , LPAREN , RPAREN , Block]`

19. **规约 Program**

  - 当前 token：`($, $)`
  - 规约 `Program → KW_INT KW_MAIN LPAREN RPAREN Block`
  - 栈：`[..., $ , Program]`

20. **完成**

  - 栈：`[$ , Program]`
  - 当前 token：`($, $)`
  - 结果：语法分析成功

## 最终结果

- **栈**：`[$ , Program]`
- **输出**：

  ```
  语法分析成功完成！
  === 最终结果 ===
  语法分析成功
  ```

## 说明

- **优先关系**：通过优先关系表（如 `OP_MOD > OP_ADD > OP_LE > OP_EQ`）决定移进或规约，确保正确解析表达式和语句。
- **规约优先级**：优先处理 `PrintStmt` 和表达式/语句规则，避免规约冲突。
- **调试信息**：代码通过 `addParseStep` 记录每一步，实际运行可输出详细栈状态。
- **简化**：因 token 序列较长，部分重复步骤已概括，完整步骤可运行代码查看。