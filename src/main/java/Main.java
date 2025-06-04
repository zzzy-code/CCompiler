import AST.ProgramNode;
import AST.RecursiveDescentASTParser;
import AST.TACContext;
import AssemblyGenerator.AssemblyGenerator;
import Lexer.Lexer;
import Lexer.Token;
import Parser.SimplePrecedenceParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Main 类是编译器的命令行驱动程序。
 * 它按顺序执行编译的各个阶段：
 * 1. 从文件读取源代码。
 * 2. 词法分析 (Lexer)。
 * 3. 语法分析 (SimplePrecedenceParser, 用于演示，实际构建AST使用RecursiveDescentASTParser)。
 * 4. 构建抽象语法树 (AST) (RecursiveDescentASTParser)。
 * 5. 从 AST 生成三地址码 (TAC)。
 * 6. 从 TAC 生成汇编代码。
 */
public class Main {
    /**
     * 从指定文件路径读取内容到字符串。
     *
     * @param filePath 文件路径。
     * @return 文件内容字符串。
     * @throws IOException 如果读取文件发生错误。
     */
    public static String readFileToString(String filePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        return content.replaceAll("\\\\s*", "").trim();
    }

    /**
     * 编译器的主入口点。
     *
     * @param args 命令行参数 (未使用)。
     */
    public static void main(String[] args) {
        String filePath = "D:\\computerPracticalTraining\\CCompiler\\src\\main\\java\\example.txt";

        try {
            String sourceCode = readFileToString(filePath);
            System.out.println("--- 源文件内容 (来自 " + filePath + ") ---");
            System.out.println(sourceCode);
            System.out.println("-------------------------------------\n");

            // 1. 词法分析
            List<Token> tokens = Lexer.lex(sourceCode);
            System.out.println("--- 词法单元 ---");
            for (Token token : tokens) {
                System.out.print(token + " ");
            }

            boolean hasEOF = false;
            if (!tokens.isEmpty()) {
                Token lastToken = tokens.get(tokens.size() - 1);
                if (lastToken.type.equals("EOF") || lastToken.type.equals("$")) {
                    hasEOF = true;
                }
            }
            if (!hasEOF) {
                tokens.add(new Token("EOF", "$"));
            }
            System.out.println("\n---------------\n");

            // 2. 语法分析 (简单优先法)
            String tokenString = convertTokensToString(tokens);
            SimplePrecedenceParser parser = new SimplePrecedenceParser();
            boolean success = parser.parse(tokenString);

            System.out.println("\n=== 最终结果 ===");
            System.out.println("语法分析" + (success ? "成功" : "失败"));


            // 3. 语法制导分析并构建 AST (使用新的递归下降解析器)
            RecursiveDescentASTParser astParser = new RecursiveDescentASTParser(tokens);
            ProgramNode astRoot = astParser.parseProgram();

            if (astRoot != null) {
                System.out.println("\n--- AST 结构展示 ---");
                astRoot.printTree("", true);
                System.out.println("----------------------\n");

                // 4. 从 AST 生成三地址码
                TACContext tacContext = new TACContext();
                astRoot.generateTAC(tacContext);

                System.out.println("=== 生成的三地址码 (来自文件 -> AST -> TAC) ===");
                for (String instruction : tacContext.instructions) {
                    System.out.println(instruction);
                }

                // 5. 从三地址码 (TAC) 生成汇编代码
                System.out.println("\n=== 生成的汇编代码 (来自三地址码) ===");
                AssemblyGenerator asmGenerator = new AssemblyGenerator();
                List<String> assemblyCode = asmGenerator.generate(tacContext.instructions);
                for (String asmLine : assemblyCode) {
                    System.out.println(asmLine);
                }
            } else {
                System.err.println("AST 构建失败。解析器返回了 null。");
            }

        } catch (IOException e) {
            System.err.println("错误: 读取文件 " + filePath + " 失败: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("处理过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 将 Token 列表转换为 SimplePrecedenceParser 所期望的特定格式的字符串。
     * 格式: "(类型1, 值1) (类型2, 值2) ..."
     *
     * @param tokens Token 列表。
     * @return 格式化后的 Token 字符串。
     */
    private static String convertTokensToString(List<Token> tokens) {
        StringBuilder sb = new StringBuilder();
        for (Token token : tokens) {
            sb.append("(").append(token.type).append(", ").append(token.value).append(") ");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

}