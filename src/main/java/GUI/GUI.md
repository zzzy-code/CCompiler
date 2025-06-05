# 🖥️ C 编译器图形用户界面 (`MainGUI.java`) 详细说明

## 1. 🔍 概述

`MainGUI` 类为 C 编译器提供了一个图形用户界面。用户可以通过这个界面加载或直接输入 C 源代码，并分阶段执行编译过程（词法分析、语法分析、AST 构建、三地址码生成、汇编代码生成），同时在界面上查看各个阶段的输出结果。

## 2. 🧩 界面布局与组件

GUI 窗口主要分为三个区域：源代码输入区、处理结果输出区和控制按钮区。

* **窗口标题**: `"C 编译器 (词法 -> 优先分析 -> AST -> TAC -> ASM)"`
* **窗口大小**: 默认为 900x750像素。
* **默认关闭操作**: `JFrame.EXIT_ON_CLOSE` (关闭窗口即退出程序)。

### 2.1. 主面板 (`mainPanel`)

* 采用 `BorderLayout` 布局，组件间有 5 像素的间距。
* 设置了 5 像素的内边距。

### 2.2. 输入面板 (`inputPanel`)

* 位于主面板的上半部分（通过 `JSplitPane` 分割）。
* 带标题边框 "源代码输入："。
* 包含：

    * **`inputArea` (JTextArea)**
      用于用户输入或加载显示 C 源代码。
      字体设置为 `"Monospaced", Font.PLAIN, 大小 14`。
      内嵌于 `JScrollPane` (`inputScroll`) 以支持滚动。
      输入区域的首选高度设置为 200 像素。
    * **`loadFileButton` (JButton)**
      按钮文本为 `"选择并加载源文件"`。
      点击此按钮会调用 `chooseAndLoadFile()` 方法，打开文件选择对话框。
      位于输入面板的底部，右对齐。

### 2.3. 输出面板 (`outputPanel`)

* 位于主面板的下半部分（通过 `JSplitPane` 分割）。
* 带标题边框 "处理结果："。
* 包含：

    * **`outputArea` (JTextArea)**
      用于显示词法分析、语法分析、AST、三地址码、汇编代码等各个阶段的处理结果和错误信息。
      设置为不可编辑 (`outputArea.setEditable(false)`)。
      字体设置为 `"Monospaced", Font.PLAIN, 大小 14`。
      内嵌于 `JScrollPane` (`outputScroll`) 以支持滚动。
      启动时，会提示用户加载文件或输入代码。

### 2.4. 分割面板 (`JSplitPane`)

* 垂直分割输入面板和输出面板。
* 用户可以拖动分割条来调整输入区和输出区的高度比例。
* `splitPane.setResizeWeight(0.25)` 设置初始时输入区大约占 25% 的高度。

### 2.5. 按钮面板 (`buttonPanel`)

* 位于主面板的底部，采用 `FlowLayout` 居中对齐，按钮间有间距。
* 包含以下控制编译流程的按钮：

    * 🟢 **`lexButton` (JButton)**: "1. 词法分析"
    * 🔵 **`simpleParseButton` (JButton)**: "2. 简单优先分析过程"
    * 🟡 **`astParseButton` (JButton)**: "3. 构建并显示 AST"
    * 🟠 **`tacButton` (JButton)**: "4. 生成三地址码"
    * 🔴 **`asmButton` (JButton)**: "5. 生成汇编代码"
* 每个按钮都绑定了相应的 `actionListener` 来执行对应的编译阶段。
* 按钮的启用状态会根据编译的进度动态更新（例如，只有在词法分析成功后，后续的分析按钮才会被启用）。

## 3. 🧠 内部状态变量

`MainGUI` 类使用以下私有成员变量来存储编译过程中的中间数据：

* **`currentTokens` (List<Token>)**: 存储当前词法分析阶段生成的 Token 列表。
* **`currentAstRoot` (ProgramNode)**: 存储当前 AST 构建阶段生成的抽象语法树的根节点。
* **`currentTac` (List<String>)**: 存储当前三地址码生成阶段生成的指令列表。
* **`simpleParserInstance` (SimplePrecedenceParser)**: 存储简单优先语法分析器的实例，用于获取分析步骤。

## 4. ⚙️ 核心功能方法

### 4.1. 构造函数 `MainGUI()`

* 初始化窗口的基本属性（标题、大小、关闭操作）。
* 调用 `initComponents()` 来创建和布局所有 GUI 组件。
* 在 `outputArea` 显示初始提示信息。
* 调用 `resetCompilationState(true)` 来设置按钮的初始启用状态（只有“词法分析”按钮可用）。

### 4.2. `initComponents()`

* 负责创建、配置和组织所有 Swing 组件，如上文“界面布局与组件”部分所述。

### 4.3. `readFileToString(String filePath)`

* 用于从指定文件路径读取内容到字符串。
* 在 `chooseAndLoadFile` 方法中被调用。
* 代码中包含 `content.replaceAll("\\s*", "").trim()`，这会移除所有空白字符（包括换行符、制表符、空格），这对于保留源代码原始格式可能不是最佳选择，词法分析器通常能更好地处理空白。

### 4.4. `chooseAndLoadFile()`

* 当用户点击 "选择并加载源文件" 按钮时触发。
* 使用 `JFileChooser` 让用户选择一个源文件（默认过滤 `.c` 和 `.txt` 文件）。
* 如果用户选择了一个文件：

    * 尝试使用 `readFileToString()` 读取文件内容 (这里调用时传入了 `selectedFile.getAbsolutePath()`)。
    * 将读取到的源代码设置到 `inputArea`。
    * 在 `outputArea` 显示文件加载成功的消息。
    * 调用 `resetCompilationState(true)` 重置编译状态并启用词法分析按钮。
    * 如果加载过程中发生 `IOException`，会在 `outputArea` 显示错误信息，并弹出错误对话框。

### 4.5. `resetCompilationState(boolean enableLex)`

* 用于重置所有存储编译中间结果的成员变量为 `null`。
* 根据参数 `enableLex` 设置 "词法分析" 按钮的启用状态。
* 禁用所有后续阶段的按钮（简单优先分析、AST 构建、TAC 生成、汇编生成）。

### 4.6. `performLexicalAnalysis(ActionEvent e)`

* 当点击 "1. 词法分析" 按钮时触发。
* 获取 `inputArea` 中的源代码。 如果为空，则提示用户。
* 调用 `Lexer.lex(source)` 进行词法分析，结果存储在 `currentTokens`。
* 将词法分析结果（每个 Token 一行）显示在 `outputArea`。
* 如果成功：

    * 启用 "简单优先分析过程" 和 "构建并显示 AST" 按钮。
    * 禁用 "生成三地址码" 和 "生成汇编代码" 按钮。
    * 重置 `currentAstRoot` 和 `currentTac`。
    * 弹出成功信息对话框。
* 如果发生异常，在 `outputArea` 显示错误信息，弹出错误对话框，并重置编译状态。

### 4.7. `performSimplePrecedenceParse(ActionEvent e)`

* 当点击 "2. 简单优先分析过程" 按钮时触发。
* 检查 `currentTokens` 是否存在，如果不存在则提示用户先进行词法分析。
* 创建 `SimplePrecedenceParser` 实例。
* 调用 `convertTokensToString(currentTokens)` 将 Token 列表转换为特定格式的字符串。
* 调用 `simpleParserInstance.parse(tokenString)` 执行简单优先语法分析。
* 获取分析步骤 (`simpleParserInstance.getParseSteps()`) 并连同最终成功/失败状态一起显示在 `outputArea`。
* 弹出相应的成功或失败信息对话框。
* 如果发生异常，处理方式同上。

### 4.8. `performASTConstruction(ActionEvent e)`

* 当点击 "3. 构建并显示 AST" 按钮时触发。
* 检查 `currentTokens` 是否存在。
* 创建一个新的 `ArrayList<>(currentTokens)`，并确保列表末尾有一个 "EOF" Token（如果原始列表没有）。
* 创建 `RecursiveDescentASTParser` 实例，并调用 `astParser.parseProgram()` 构建 AST，结果存储在 `currentAstRoot`。
* 如果 `currentAstRoot` 不为 null（构建成功）：

    * 调用 `currentAstRoot.printTree("", true)` 获取 AST 的字符串表示并显示在 `outputArea`。
    * 启用 "生成三地址码" 按钮，禁用 "生成汇编代码" 按钮，并重置 `currentTac`。
    * 弹出成功信息对话框。
* 如果 AST 构建失败或发生异常，处理方式同上，并禁用 TAC 生成按钮。

### 4.9. `performTACGeneration(ActionEvent e)`

* 当点击 "4. 生成三地址码" 按钮时触发。
* 检查 `currentAstRoot` 是否存在。
* 创建 `TACContext` 实例。
* 调用 `currentAstRoot.generateTAC(tacContext)` 生成三地址码，结果指令列表存储在 `currentTac`。
* 将生成的三地址码（每条指令一行）显示在 `outputArea`。
* 如果成功，启用 "生成汇编代码" 按钮并弹出成功信息对话框。
* 如果发生异常，处理方式同上，并禁用汇编生成按钮。

### 4.10. `performAssemblyGeneration(ActionEvent e)`

* 当点击 "5. 生成汇编代码" 按钮时触发。
* 检查 `currentTac` 是否存在且不为空。
* 创建 `AssemblyGenerator` 实例，并调用 `asmGenerator.generate(currentTac)` 生成汇编代码。
* 将生成的汇编代码（每行一条）显示在 `outputArea`。
* 弹出成功信息对话框。
* **保存汇编代码**:

    * 使用 `JFileChooser` 提示用户选择保存汇编文件的位置和名称（默认为 "output.asm"）。
    * 如果用户确认保存，则将汇编代码以 "GBK" 编码写入选定的文件。
    * 显示保存成功或失败的信息。
* 如果发生异常，处理方式同上。

### 4.11. `convertTokensToString(List<Token> tokens)`

* 一个静态辅助方法，用于将 `List<Token>` 转换为 `SimplePrecedenceParser` 所期望的特定字符串格式 `(类型1, 值1) (类型2, 值2) ...`。
* 此方法会**过滤掉类型为 "EOF" 或 "\$" 的 Token**。

### 4.12. `getStackTraceString(Exception ex)`

* 一个辅助方法，用于将异常的堆栈跟踪信息转换为字符串，方便在 `outputArea` 中显示更详细的错误。

### 4.13. `main(String[] args)`

* GUI 程序的入口点。
* 尝试设置界面观感为当前操作系统的风格 (`UIManager.getSystemLookAndFeelClassName()`)。
* 使用 `SwingUtilities.invokeLater` 来确保 GUI 的创建和显示在事件调度线程 (Event Dispatch Thread, EDT) 中进行，这是 Swing 编程的最佳实践。
* 创建 `MainGUI` 实例并使其可见。

## 5. 🚀 使用流程

1. 用户启动程序，`MainGUI` 窗口出现。
2. 用户可以通过点击 "选择并加载源文件" 按钮从本地文件系统加载 C 源代码，或者直接在顶部的文本区域 (`inputArea`) 手动输入代码。
3. 点击 "1. 词法分析" 按钮，程序对输入区的代码进行词法分析，结果（Token 列表）显示在下方的 `outputArea`。
4. 如果词法分析成功，"2. 简单优先分析过程" 和 "3. 构建并显示 AST" 按钮被启用。
5. 用户可以点击 "2. 简单优先分析过程" 按钮，查看基于简单优先法的分析步骤和结果。
6. 用户可以点击 "3. 构建并显示 AST" 按钮，程序会使用递归下降解析器构建 AST，并在 `outputArea` 中以树形结构展示。
7. 如果 AST 构建成功，"4. 生成三地址码" 按钮被启用。用户点击后，生成的三地址码会显示在 `outputArea`。
8. 如果三地址码生成成功，"5. 生成汇编代码" 按钮被启用。用户点击后，生成的汇编代码会显示在 `outputArea`，并且程序会提示用户将汇编代码保存到文件。
9. 在任何阶段发生错误，错误信息会显示在 `outputArea`，并通常伴有错误提示对话框。后续依赖于出错阶段的按钮会被禁用，用户可能需要修正输入或从之前的阶段重新开始。
