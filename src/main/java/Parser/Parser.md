# 📘 Simple C Parser

## 📌 简介

本项目实现了一个用于解析**简化 C 程序结构**的语法分析器 `SimplePrecedenceParser`，使用 BNF（巴科斯范式）定义语法规则，基于**移进-规约**策略对 Token 序列进行分析。

虽然名为“简单优先分析器”，但其当前实现更侧重于启发式的规约尝试，并未严格按照符号优先级表来驱动移进与规约的决策过程。

该分析器支持常见的 C 语言结构，如声明、赋值、循环、条件、打印、返回语句以及多种表达式。

---

## 📚 1. 语法规则（BNF）

### 🧱 程序结构

```bnf
Program     → KW_INT KW_MAIN LPAREN RPAREN Block
Block       → LBRACE StmtList RBRACE
            |  LBRACE RBRACE
StmtList    → StmtList Stmt
            |  Stmt
Stmt        → DeclStmt
            |  AssignStmt
            |  WhileStmt
            |  IfStmt
            |  ElseIfStmt
            |  PrintStmt
            |  ReturnStmt
```

### 🧾 各类语句

```bnf
DeclStmt    → KW_INT Expr OP_ASSIGN Expr SEMICOLON
            |  KW_INT Expr SEMICOLON

AssignStmt  → Expr OP_ASSIGN Expr SEMICOLON

WhileStmt   → KW_WHILE Expr Block

IfStmt      → KW_IF Expr Block
            |  KW_IF Expr Block KW_ELSE Block

ElseIfStmt  → KW_ELSE Block

PrintStmt   → IO_PRINTF LPAREN Expr RPAREN SEMICOLON
            |  IO_PRINTF LPAREN Expr COMMA Expr RPAREN SEMICOLON
            |  IO_PRINTF Expr SEMICOLON

ReturnStmt  → KW_RETURN Expr SEMICOLON
```

### 🧮 表达式（Expr）

```bnf
Expr        → Expr OP_ADD Expr
            |  Expr OP_SUB Expr
            |  Expr OP_MUL Expr
            |  Expr OP_DIV Expr
            |  Expr OP_MOD Expr
            |  Expr OP_LE Expr
            |  Expr OP_EQ Expr
            |  Expr OP_GT Expr
            |  LPAREN Expr RPAREN
            |  ID
            |  NUM
            |  STR
```

---

## 🔤 2. 终结符与非终结符

### ✅ 终结符（Terminal Symbols）

* **关键字**：`KW_INT`, `KW_MAIN`, `KW_WHILE`, `KW_IF`, `KW_ELSE`, `KW_RETURN`
* **分隔符与括号**：`LPAREN`, `RPAREN`, `LBRACE`, `RBRACE`, `SEMICOLON`, `COMMA`
* **输入输出**：`IO_PRINTF`
* **运算符**：`OP_ASSIGN`, `OP_ADD`, `OP_SUB`, `OP_MUL`, `OP_DIV`, `OP_MOD`, `OP_LE`, `OP_EQ`, `OP_GT`
* **常量与标识符**：`ID`, `NUM`, `STR`
* **输入结束标志**：`$`

### 🧩 非终结符（Non-Terminal Symbols）

`Program`, `Block`, `StmtList`, `Stmt`, `DeclStmt`, `AssignStmt`, `WhileStmt`, `IfStmt`, `ElseIfStmt`, `PrintStmt`, `ReturnStmt`, `Expr`

---

## ⚖️ 3. 优先关系表说明

### 🔢 优先级定义（按从低到高排列）

| 优先级 | 符号                           |
| --- | ---------------------------- |
| 0   | `$`（栈底）                      |
| 1   | `KW_RETURN`                  |
| 2   | `RBRACE`                     |
| 3   | `SEMICOLON`                  |
| 4   | `KW_ELSE`                    |
| 5   | `RPAREN`                     |
| 6   | `OP_EQ`                      |
| 7   | `OP_LE`                      |
| 8   | `OP_ADD`, `OP_SUB`           |
| 9   | `OP_MUL`, `OP_DIV`, `OP_MOD` |
| 10  | `LPAREN`                     |
| 11  | `ID`, `NUM`, `STR`           |

### 📐 优先关系生成逻辑

对于任意两个终结符 `left` 和 `right`：

* 若 `priority(left) < priority(right)` → `left < right`（移进）
* 若 `priority(left) > priority(right)` → `left > right`（规约）
* 若相等 → `left = right`

### 🧷 特殊关系（手动指定）

| 左符号      | 右符号      | 关系  |
| -------- | -------- | --- |
| `LPAREN` | `RPAREN` | `=` |
| `LBRACE` | `RBRACE` | `=` |
| `$`      | `$`      | `=` |

> ⚠️ 注意：虽然优先关系表被初始化，`parse()` 方法实际并未严格使用该表指导移进与规约操作，而是采用一种“尝试规约优先”的启发式策略。

---

## ⚙️ 4. 分析器特性与适用范围

### ✅ 核心特性

* **移进-规约机制**：不断移进输入符号并尝试规约栈顶内容。
* **重复规约尝试**：每次移进前后均尝试多轮规约（`tryReduce`）。
* **最终规约支持**：使用 `tryFinalReduce()` 在输入结束后应用额外规约。
* **规约优先级控制**：根据规则类型与右部长度排序，提高匹配效率。
* **支持左递归**：可处理左递归规则如 `Expr → Expr OP_ADD Expr` 等。

### 🎯 适用范围

* 能解析基于所定义文法的简化 C 程序结构。
* 不完全等同于传统的简单优先分析器，在处理复杂或模糊文法时行为略异。
* 可作为研究不同语法分析策略和调试方法的实验平台。

---

如需深入理解或调试，请参考核心类 `SimplePrecedenceParser.java` 中的以下方法：

* `parse()`：主解析逻辑
* `tryReduce()`：规约尝试核心
* `tryFinalReduce()`：解析末尾的最终规约逻辑
* `initPrecedenceTable()`：优先级初始化与规则定义

---