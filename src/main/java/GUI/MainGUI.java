package GUI;

import AST.ProgramNode;
import AST.RecursiveDescentASTParser;
import AST.TACContext;
import AssemblyGenerator.AssemblyGenerator;
import Lexer.Lexer;
import Lexer.Token;
import Parser.SimplePrecedenceParser;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * MainGUI 类是编译器的图形用户界面 (GUI) 版本。
 * 它提供了一个界面来加载或输入源代码，并分阶段执行编译过程，显示中间结果。
 */
public class MainGUI extends JFrame {
    private JTextArea inputArea;        // 用于输入或显示源代码的文本区域
    private JTextArea outputArea;       // 用于显示各种分析和生成结果的文本区域
    private JButton lexButton;          // 执行词法分析的按钮
    private JButton simpleParseButton;  // 执行简单优先语法分析的按钮
    private JButton astParseButton;     // 构建和显示 AST 的按钮
    private JButton tacButton;          // 生成三地址码的按钮
    private JButton asmButton;          // 生成汇编代码的按钮
    private JButton loadFileButton;     // 用于选择和加载源文件的按钮

    // 用于在编译的各个阶段之间传递数据
    private List<Token> currentTokens = null;            // 当前的词法单元列表
    private ProgramNode currentAstRoot = null;           // 当前的 AST 根节点
    private List<String> currentTac = null;              // 当前生成的三地址码指令列表
    private SimplePrecedenceParser simpleParserInstance; // 简单优先分析器的实例


    /**
     * MainGUI 的构造函数。
     * 初始化窗口标题、大小、关闭操作，并设置界面组件。
     */
    public MainGUI() {
        setTitle("C 编译器 (词法 -> 优先分析 -> AST -> TAC -> ASM)");
        setSize(900, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initComponents();
        outputArea.setText("请点击“选择并加载源文件”按钮来加载您的 C 源代码文件，或直接在上方文本框中输入。");
        resetCompilationState(true);
    }

    /**
     * 初始化 GUI 组件，包括文本区域、按钮和布局。
     */
    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("源代码输入："));
        inputArea = new JTextArea();
        inputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.setPreferredSize(new Dimension(0, 200));
        inputPanel.add(inputScroll, BorderLayout.CENTER);

        loadFileButton = new JButton("选择并加载源文件");
        loadFileButton.addActionListener(e -> chooseAndLoadFile());
        JPanel inputButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        inputButtonPanel.add(loadFileButton);
        inputPanel.add(inputButtonPanel, BorderLayout.SOUTH);

        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(BorderFactory.createTitledBorder("处理结果："));
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputPanel.add(outputScroll, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        lexButton = new JButton("1. 词法分析");
        simpleParseButton = new JButton("2. 简单优先分析过程");
        astParseButton = new JButton("3. 构建并显示 AST");
        tacButton = new JButton("4. 生成三地址码");
        asmButton = new JButton("5. 生成汇编代码");

        lexButton.addActionListener(this::performLexicalAnalysis);
        simpleParseButton.addActionListener(this::performSimplePrecedenceParse);
        astParseButton.addActionListener(this::performASTConstruction);
        tacButton.addActionListener(this::performTACGeneration);
        asmButton.addActionListener(this::performAssemblyGeneration);

        simpleParseButton.setEnabled(false);
        astParseButton.setEnabled(false);
        tacButton.setEnabled(false);
        asmButton.setEnabled(false);

        buttonPanel.add(lexButton);
        buttonPanel.add(simpleParseButton);
        buttonPanel.add(astParseButton);
        buttonPanel.add(tacButton);
        buttonPanel.add(asmButton);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputPanel, outputPanel);
        splitPane.setResizeWeight(0.25);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    /**
     * 从指定文件路径读取内容到字符串 (与 Main.java 中的方法类似，但 GUI 中未使用此版本)。
     * GUI 版本中直接使用 Files.readAllBytes。
     *
     * @param filePath 文件路径。
     * @return 文件内容字符串。
     * @throws IOException 如果读取文件发生错误。
     */
    private String readFileToString(String filePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        return content.replaceAll("\\\\s*", "").trim();
    }

    /**
     * 打开文件选择对话框，让用户选择一个源文件，并将其内容加载到输入区域。
     */
    private void chooseAndLoadFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("."));
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "C Source & Text Files (*.c, *.txt)", "c", "txt");
        fileChooser.setFileFilter(filter);
        fileChooser.setDialogTitle("选择要加载的源代码文件");

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                String sourceCode = readFileToString(selectedFile.getAbsolutePath());
                inputArea.setText(sourceCode);
                outputArea.setText("文件已加载: " + selectedFile.getName() + "\n路径: " + selectedFile.getAbsolutePath() + "\n");
                resetCompilationState(true);
            } catch (IOException ex) {
                outputArea.setText("加载文件 " + selectedFile.getName() + " 失败: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "加载文件失败: " + ex.getMessage(),
                        "文件错误", JOptionPane.ERROR_MESSAGE);
                resetCompilationState(true);
            }
        }
    }

    /**
     * 重置编译器的中间状态和按钮的启用状态。
     *
     * @param enableLex 如果为 true，则启用词法分析按钮；否则禁用。
     */
    private void resetCompilationState(boolean enableLex) {
        currentTokens = null;
        currentAstRoot = null;
        currentTac = null;
        simpleParserInstance = null;

        lexButton.setEnabled(enableLex);
        simpleParseButton.setEnabled(false);
        astParseButton.setEnabled(false);
        tacButton.setEnabled(false);
        asmButton.setEnabled(false);
    }

    /**
     * 执行词法分析。
     * 从输入区域获取源代码，调用 Lexer 进行分析，并在输出区域显示结果。
     *
     * @param e 按钮点击事件 (未使用)。
     */
    private void performLexicalAnalysis(ActionEvent e) {
        String source = inputArea.getText();
        if (source.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入或加载源代码！", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            currentTokens = Lexer.lex(source);
            StringBuilder sb = new StringBuilder("=== 词法分析结果 ===\n");
            for (Token token : currentTokens) {
                sb.append(token.toString()).append("\n");
            }
            outputArea.setText(sb.toString());

            simpleParseButton.setEnabled(true);
            astParseButton.setEnabled(true);
            tacButton.setEnabled(false);
            asmButton.setEnabled(false);
            currentAstRoot = null;
            currentTac = null;
            simpleParserInstance = null;

            JOptionPane.showMessageDialog(this, "词法分析完成！", "成功", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            outputArea.setText("词法分析错误: " + ex.getMessage() + "\n" + getStackTraceString(ex));
            JOptionPane.showMessageDialog(this, "词法分析错误: " + ex.getMessage(), "词法错误", JOptionPane.ERROR_MESSAGE);
            resetCompilationState(true);
        }
    }

    /**
     * 执行简单优先语法分析。
     * 需要先完成词法分析。将 Token 列表转换为字符串格式，调用 SimplePrecedenceParser。
     *
     * @param e 按钮点击事件 (未使用)。
     */
    private void performSimplePrecedenceParse(ActionEvent e) {
        if (currentTokens == null) {
            JOptionPane.showMessageDialog(this, "请先执行词法分析！", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            simpleParserInstance = new SimplePrecedenceParser();
            String tokenString = convertTokensToString(currentTokens);
            boolean success = simpleParserInstance.parse(tokenString);

            StringBuilder sb = new StringBuilder("=== 简单优先语法分析过程 ===\n");
            List<String> parseSteps = simpleParserInstance.getParseSteps();
            if (parseSteps != null && !parseSteps.isEmpty()) {
                for (String step : parseSteps) {
                    sb.append(step).append("\n");
                }
            } else {
                sb.append("（详细分析步骤请查看控制台，或确保 SimplePrecedenceParser.getParseSteps() 可用且返回了步骤）\n");
            }
            sb.append("\n语法分析最终状态: ").append(success ? "成功" : "失败").append("\n");
            outputArea.setText(sb.toString());

            if(success){
                JOptionPane.showMessageDialog(this, "简单优先语法分析完成！", "成功", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "简单优先语法分析失败。", "分析结果", JOptionPane.WARNING_MESSAGE);
            }

        } catch (Exception ex) {
            outputArea.setText("简单优先语法分析错误: " + ex.getMessage() + "\n" + getStackTraceString(ex));
            JOptionPane.showMessageDialog(this, "简单优先语法分析错误: " + ex.getMessage(), "语法错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 执行 AST 构建。
     * 需要先完成词法分析。使用 RecursiveDescentASTParser 构建 AST。
     *
     * @param e 按钮点击事件 (未使用)。
     */
    private void performASTConstruction(ActionEvent e) {
        if (currentTokens == null) {
            JOptionPane.showMessageDialog(this, "请先执行词法分析！", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            List<Token> tokensForAstParser = new ArrayList<>(currentTokens);
            boolean hasEOF = false;
            if (!tokensForAstParser.isEmpty()) {
                Token lastToken = tokensForAstParser.get(tokensForAstParser.size() - 1);
                if (lastToken.type.equals("EOF") || lastToken.type.equals("$")) {
                    hasEOF = true;
                }
            }
            if (!hasEOF) {
                tokensForAstParser.add(new Token("EOF", "$"));
            }

            RecursiveDescentASTParser astParser = new RecursiveDescentASTParser(tokensForAstParser);
            currentAstRoot = astParser.parseProgram();

            if (currentAstRoot != null) {
                String astStringRepresentation = currentAstRoot.printTree("", true);
                outputArea.setText("=== AST 结构展示 ===\n" + astStringRepresentation);

                tacButton.setEnabled(true);
                asmButton.setEnabled(false);
                currentTac = null;
                JOptionPane.showMessageDialog(this, "AST 构建完成！", "成功", JOptionPane.INFORMATION_MESSAGE);
            } else {
                outputArea.setText("AST 构建失败。解析器返回了 null。");
                tacButton.setEnabled(false);
                JOptionPane.showMessageDialog(this, "AST 构建失败。", "错误", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            outputArea.setText("AST 构建错误: " + ex.getMessage() + "\n" + getStackTraceString(ex));
            JOptionPane.showMessageDialog(this, "AST 构建错误: " + ex.getMessage(), "AST错误", JOptionPane.ERROR_MESSAGE);
            tacButton.setEnabled(false);
        }
    }

    /**
     * 执行三地址码 (TAC) 生成。
     * 需要先成功构建 AST。
     *
     * @param e 按钮点击事件 (未使用)。
     */
    private void performTACGeneration(ActionEvent e) {
        if (currentAstRoot == null) {
            JOptionPane.showMessageDialog(this, "请先成功构建 AST！", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            TACContext tacContext = new TACContext();
            currentAstRoot.generateTAC(tacContext);
            currentTac = tacContext.instructions;

            StringBuilder sb = new StringBuilder("=== 生成的三地址码 ===\n");
            for (String instruction : currentTac) {
                sb.append(instruction).append("\n");
            }
            outputArea.setText(sb.toString());
            asmButton.setEnabled(true);
            JOptionPane.showMessageDialog(this, "三地址码生成完成！", "成功", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            outputArea.setText("三地址码生成错误: " + ex.getMessage() + "\n" + getStackTraceString(ex));
            JOptionPane.showMessageDialog(this, "三地址码生成错误: " + ex.getMessage(), "TAC错误", JOptionPane.ERROR_MESSAGE);
            asmButton.setEnabled(false);
        }
    }

    /**
     * 执行汇编代码生成。
     * 需要先成功生成三地址码。
     * 生成后，会提示用户保存汇编代码到文件。
     *
     * @param e 按钮点击事件 (未使用)。
     */
    private void performAssemblyGeneration(ActionEvent e) {
        if (currentTac == null || currentTac.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先成功生成三地址码！", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            AssemblyGenerator asmGenerator = new AssemblyGenerator();
            List<String> assemblyCode = asmGenerator.generate(currentTac);

            StringBuilder sb = new StringBuilder("=== 生成的汇编代码 ===\n");
            for (String asmLine : assemblyCode) {
                sb.append(asmLine).append("\n");
            }
            outputArea.setText(sb.toString());
            JOptionPane.showMessageDialog(this, "汇编代码生成完成！", "成功", JOptionPane.INFORMATION_MESSAGE);

            JFileChooser fileChooser = new JFileChooser(".");
            fileChooser.setDialogTitle("保存汇编代码");
            fileChooser.setSelectedFile(new File("output.asm"));
            int userSelection = fileChooser.showSaveDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                try (Writer writer = new OutputStreamWriter(new FileOutputStream(fileToSave), Charset.forName("GBK"))) {
                    for (String asmLine : assemblyCode) {
                        writer.write(asmLine + System.lineSeparator());
                    }
                    JOptionPane.showMessageDialog(this,
                            "汇编代码已以 ANSI (GBK) 编码保存到: " + fileToSave.getAbsolutePath(),
                            "保存成功", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException exIO) {
                    outputArea.append("\n保存汇编文件失败: " + exIO.getMessage());
                    JOptionPane.showMessageDialog(this, "保存汇编文件失败: " + exIO.getMessage(), "文件错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception ex) {
            outputArea.setText("汇编代码生成错误: " + ex.getMessage() + "\n" + getStackTraceString(ex));
            JOptionPane.showMessageDialog(this, "汇编代码生成错误: " + ex.getMessage(), "汇编错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 将 Token 列表转换为 SimplePrecedenceParser 期望的特定格式的字符串。
     * 过滤掉 "EOF" 或 "$" 类型的 Token，因为简单优先分析器通常不直接处理它们。
     *
     * @param tokens Token 列表。
     * @return 格式化后的 Token 字符串。
     */
    private static String convertTokensToString(List<Token> tokens) {
        StringBuilder sb = new StringBuilder();
        for (Token token : tokens) {
            if (token.type.equals("EOF") || token.type.equals("$")){
                continue;
            }
            sb.append("(").append(token.type).append(", ").append(token.value).append(") ");
        }
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ' ') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * 将异常的堆栈跟踪信息转换为字符串，用于在输出区域显示。
     *
     * @param ex 发生的异常。
     * @return 堆栈跟踪的字符串表示。
     */
    private String getStackTraceString(Exception ex) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * GUI 程序的主入口点。
     * 设置系统观感 (Look and Feel) 并创建和显示主窗口。
     *
     * @param args 命令行参数 (未使用)。
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> {
            MainGUI frame = new MainGUI();
            frame.setVisible(true);
        });
    }
}