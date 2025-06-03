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

public class MainGUI extends JFrame {
    private JTextArea inputArea;
    private JTextArea outputArea;
    private JButton lexButton;
    private JButton simpleParseButton;
    private JButton astParseButton;
    private JButton tacButton;
    private JButton asmButton;
    private JButton loadFileButton; // 修改变量名

    private List<Token> currentTokens = null;
    private ProgramNode currentAstRoot = null;
    private List<String> currentTac = null;
    private SimplePrecedenceParser simpleParserInstance;

    public MainGUI() {
        setTitle("C 编译器 (词法 -> 优先分析 -> AST -> TAC -> ASM)");
        setSize(900, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initComponents();
        // loadExampleFile(); // 启动时不再自动加载特定文件
        outputArea.setText("请点击“选择并加载源文件”按钮来加载您的 C 源代码文件，或直接在上方文本框中输入。");
        resetCompilationState(true); // 启动时启用词法分析按钮
    }

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

        loadFileButton = new JButton("选择并加载源文件"); // 修改按钮文本
        loadFileButton.addActionListener(e -> chooseAndLoadFile()); // 修改调用的方法
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

    private String readFileToString(String filePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        return content.replaceAll("\\\\s*", "").trim();
    }

    // 修改 loadExampleFile 为 chooseAndLoadFile
    private void chooseAndLoadFile() {
        JFileChooser fileChooser = new JFileChooser();
        // 设置默认打开目录为当前项目目录或用户上次打开的目录
        fileChooser.setCurrentDirectory(new File(".")); // "." 代表当前目录
        // 设置文件过滤器，例如只显示 .txt 或 .c 文件
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
                resetCompilationState(true); // 加载文件后，启用词法分析按钮并重置状态
            } catch (IOException ex) {
                outputArea.setText("加载文件 " + selectedFile.getName() + " 失败: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "加载文件失败: " + ex.getMessage(),
                        "文件错误", JOptionPane.ERROR_MESSAGE);
                resetCompilationState(true); // 即使加载失败，也允许用户再次尝试或手动输入
            }
        } else {
            // 用户取消了选择，可以不执行任何操作，或者给个提示
            // outputArea.append("\n未选择文件。\n");
        }
    }

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

    private void performSimplePrecedenceParse(ActionEvent e) {
        if (currentTokens == null) {
            JOptionPane.showMessageDialog(this, "请先执行词法分析！", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            simpleParserInstance = new SimplePrecedenceParser();
            String tokenString = convertTokensToString(currentTokens);
            // 假设 SimplePrecedenceParser 的 parse 方法内部会打印步骤到控制台
            // 并且我们通过 getParseSteps 获取步骤来显示在 JTextArea
            // SimplePrecedenceParser 的构造函数或 parse 方法不应直接修改 GUI
            boolean success = simpleParserInstance.parse(tokenString);

            StringBuilder sb = new StringBuilder("=== 简单优先语法分析过程 ===\n");
            List<String> parseSteps = simpleParserInstance.getParseSteps(); // 需要您在 SimplePrecedenceParser 中实现此方法
            if (parseSteps != null && !parseSteps.isEmpty()) {
                for (String step : parseSteps) {
                    sb.append(step).append("\n");
                }
            } else {
                // 如果 parse 方法直接打印到控制台，这里可能没有步骤可获取
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
                String astStringRepresentation = currentAstRoot.printTree("", true); // 假设 printTree 返回 String
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

    private String getStackTraceString(Exception ex) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
    }

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