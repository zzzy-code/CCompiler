package GUI;

import AST.ProgramNode;
import AST.RecursiveDescentASTParser;
import AST.TACContext;
import AssemblyGenerator.AssemblyGenerator;
import Lexer.Lexer;
import Lexer.Token;
import Parser.SimplePrecedenceParser;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream; // 用于指定编码保存
import java.io.OutputStreamWriter; // 用于指定编码保存
import java.io.Writer;           // 用于指定编码保存
import java.nio.charset.Charset;   // 用于指定编码保存
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
    private JButton loadExampleButton;

    private List<Token> currentTokens = null;
    private ProgramNode currentAstRoot = null;
    private List<String> currentTac = null;
    private SimplePrecedenceParser simpleParserInstance;

    public MainGUI() {
        setTitle("C 编译器 (词法 -> 优先分析 -> AST -> TAC -> ASM)");
        setSize(900, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initComponents();
        loadExampleFile();
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

        loadExampleButton = new JButton("加载 example.txt");
        loadExampleButton.addActionListener(e -> loadExampleFile());
        JPanel inputButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        inputButtonPanel.add(loadExampleButton);
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
        splitPane.setResizeWeight(0.4);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private String readFileToString(String filePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        return content.replaceAll("\\\\s*", "").trim();
    }

    private void loadExampleFile() {
        File exampleFile = new File("example.txt");
        if (!exampleFile.exists()) {
            exampleFile = new File("src/main/java/example.txt");
        }

        if (exampleFile.exists()) {
            try {
                String sourceCode = readFileToString(exampleFile.getAbsolutePath());
                inputArea.setText(sourceCode);
                outputArea.setText("example.txt 已加载。\n路径: " + exampleFile.getAbsolutePath() + "\n");
                resetCompilationState(true);
            } catch (IOException ex) {
                outputArea.setText("加载 example.txt 失败: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "加载 example.txt 失败: " + ex.getMessage(),
                        "文件错误", JOptionPane.ERROR_MESSAGE);
                resetCompilationState(true);
            }
        } else {
            outputArea.setText("未找到 example.txt。\n请确保文件存在于以下任一路径：\n" +
                    new File("example.txt").getAbsolutePath() + "\n或\n" +
                    new File("src/main/java/example.txt").getAbsolutePath() + "\n或手动粘贴代码。");
            resetCompilationState(true);
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
            JOptionPane.showMessageDialog(this, "请输入源代码！", "错误", JOptionPane.ERROR_MESSAGE);
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

            JOptionPane.showMessageDialog(this, "词法分析完成！", "成功", JOptionPane.INFORMATION_MESSAGE); // 添加成功对话框

        } catch (Exception ex) {
            outputArea.setText("词法分析错误: " + ex.getMessage());
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
            boolean success = simpleParserInstance.parse(tokenString);

            StringBuilder sb = new StringBuilder("=== 简单优先语法分析过程 ===\n");
            // 假设 SimplePrecedenceParser 有一个 getParseSteps() 方法
            List<String> parseSteps = simpleParserInstance.getParseSteps(); // 需要您在SimplePrecedenceParser中实现此方法
            if (parseSteps != null) {
                for (String step : parseSteps) {
                    sb.append(step).append("\n");
                }
            } else {
                sb.append("（未能获取详细分析步骤，请确保 SimplePrecedenceParser.getParseSteps() 可用且返回了步骤）\n");
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
                // 假设 ASTNode 的 printTree 方法已修改为返回 String
                String astStringRepresentation = currentAstRoot.printTree("", true);
                outputArea.setText("=== AST 结构展示 ===\n" + astStringRepresentation);

                tacButton.setEnabled(true);
                asmButton.setEnabled(false);
                currentTac = null;
                JOptionPane.showMessageDialog(this, "AST 构建完成！", "成功", JOptionPane.INFORMATION_MESSAGE); // 添加成功对话框
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
            JOptionPane.showMessageDialog(this, "三地址码生成完成！", "成功", JOptionPane.INFORMATION_MESSAGE); // 添加成功对话框

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
            JOptionPane.showMessageDialog(this, "汇编代码生成完成！", "成功", JOptionPane.INFORMATION_MESSAGE); // 通用成功信息

            JFileChooser fileChooser = new JFileChooser(".");
            fileChooser.setDialogTitle("保存汇编代码");
            fileChooser.setSelectedFile(new File("output.asm"));
            int userSelection = fileChooser.showSaveDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                try (Writer writer = new OutputStreamWriter(new FileOutputStream(fileToSave), Charset.forName("GBK"))) { // ANSI
                    for (String asmLine : assemblyCode) {
                        writer.write(asmLine + System.lineSeparator());
                    }
                    JOptionPane.showMessageDialog(this,
                            "汇编代码已以 ANSI (GBK) 编码保存到: " + fileToSave.getAbsolutePath(),
                            "保存成功", JOptionPane.INFORMATION_MESSAGE); // 明确保存成功信息
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