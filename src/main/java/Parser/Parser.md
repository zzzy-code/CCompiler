# 📘 Simple C Parser

## 📌 简介

本项目实现了一个用于解析**简化 C 程序结构**的**简单优先文法分析器**。它采用 BNF（巴科斯范式）描述语法结构，并通过定义**符号优先关系表**来实现确定性自顶向下语法分析，支持声明、赋值、循环、条件语句、打印、返回语句及基本表达式的识别与解析。

---

## 📚 1. 语法规则（BNF 格式）

### 程序结构

```
Program     → KW_INT KW_MAIN LPAREN RPAREN Block
```

### 语句块

```
Block       → LBRACE StmtList RBRACE
Block       → LBRACE RBRACE
```

### 语句列表

```
StmtList    → StmtList Stmt
StmtList    → Stmt
```

### 各类语句

```
Stmt        → DeclStmt
Stmt        → AssignStmt
Stmt        → WhileStmt
Stmt        → IfStmt
Stmt        → ElseIfStmt
Stmt        → PrintStmt
Stmt        → ReturnStmt
```

### 声明语句

```
DeclStmt    → KW_INT Expr OP_ASSIGN Expr SEMICOLON
DeclStmt    → KW_INT Expr SEMICOLON
```

### 赋值语句

```
AssignStmt  → Expr OP_ASSIGN Expr SEMICOLON
```

### 循环语句

```
WhileStmt   → KW_WHILE Expr Block
```

### 条件语句

```
IfStmt      → KW_IF Expr Block KW_ELSE Block
IfStmt      → KW_IF Expr Block
ElseIfStmt  → KW_ELSE Block
```

### 输出语句

```
PrintStmt   → IO_PRINTF LPAREN Expr RPAREN SEMICOLON
PrintStmt   → IO_PRINTF LPAREN Expr COMMA Expr RPAREN SEMICOLON
PrintStmt   → IO_PRINTF Expr SEMICOLON
```

### 返回语句

```
ReturnStmt  → KW_RETURN Expr SEMICOLON
```

### 表达式（Expr）

```
Expr        → Expr OP_ADD Expr
Expr        → Expr OP_LE Expr
Expr        → Expr OP_EQ Expr
Expr        → Expr OP_MOD Expr
Expr        → LPAREN Expr RPAREN
Expr        → ID
Expr        → NUM
Expr        → STR
```

---

## 🔤 2. 终结符与非终结符

### 终结符（Terminal Symbols）

* 关键字类：

    * `KW_INT`, `KW_MAIN`, `KW_WHILE`, `KW_IF`, `KW_ELSE`, `KW_RETURN`
* 分隔符与括号：

    * `LPAREN`, `RPAREN`, `LBRACE`, `RBRACE`, `SEMICOLON`, `COMMA`
* 输入输出：

    * `IO_PRINTF`
* 运算符：

    * `OP_ASSIGN`, `OP_ADD`, `OP_LE`, `OP_EQ`, `OP_MOD`
* 标识符与常量：

    * `ID`, `NUM`, `STR`
* 输入结束符：

    * `$`

### 非终结符（Non-terminal Symbols）

`Program`, `Block`, `StmtList`, `Stmt`, `DeclStmt`, `AssignStmt`, `WhileStmt`, `IfStmt`, `ElseIfStmt`, `PrintStmt`, `ReturnStmt`, `Expr`

---

## 📈 3. 优先关系表说明

### ✅ 优先级顺序（从低到高）

| 优先级 | 符号类型               |
| --- | ------------------ |
| 0   | `$`                |
| 1   | `KW_RETURN`        |
| 2   | `RBRACE`           |
| 3   | `SEMICOLON`        |
| 4   | `KW_ELSE`          |
| 5   | `RPAREN`           |
| 6   | `OP_EQ`            |
| 7   | `OP_LE`            |
| 8   | `OP_ADD`           |
| 9   | `OP_MOD`           |
| 10  | `LPAREN`           |
| 11  | `ID`, `NUM`, `STR` |

### ⚖️ 优先关系规则

对于符号 `a` 与 `b`，若其优先级分别为 `prec(a)` 与 `prec(b)`：

* 若 `prec(a) < prec(b)`，则有：`a < b`
* 若 `prec(a) > prec(b)`，则有：`a > b`
* 若 `prec(a) = prec(b)`，则有：`a = b`

### 🔁 特殊优先关系

| 符号对               | 关系  |
| ----------------- | --- |
| `LPAREN`/`RPAREN` | `=` |
| `LBRACE`/`RBRACE` | `=` |
| `$`/`$`           | `=` |

---

## ⚙️ 4. 文法特性与适用范围

### ✅ 简单优先文法的特性：

* **唯一的符号对优先关系**：无二义性
* **左递归支持**：通过优先级解决递归问题（如 `Expr → Expr OP_ADD Expr`）
* **明确的表达式结构**：优先级设计支持 C 表达式解析

### 🎯 适用范围：

* 适用于**简化版 C 语言程序**的语法分析器设计
* 特别适合教学、语言实验工具或编译器前端简化实现
* 可作为更复杂语法分析的起点框架
