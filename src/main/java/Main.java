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

public class Main {
    public static String readFileToString(String filePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        // 移除 example.txt 中类似 的标记
        return content.replaceAll("\\\\s*", "").trim();
    }

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
            // 为递归下降解析器添加一个明确的结束标记 (如果您的词法分析器不自动添加)
            // 我们的 RecursiveDescentASTParser 期望在列表末尾有一个 EOF 类型的 token
            // 如果您的 Lexer.lex 返回的列表不包含它，我们在这里添加。
            // 检查最后一个 token 是否已经是某种形式的 EOF
            boolean hasEOF = false;
            if (!tokens.isEmpty()) {
                Token lastToken = tokens.get(tokens.size() - 1);
                if (lastToken.type.equals("EOF") || lastToken.type.equals("$")) { // 假设 $ 也是EOF
                    hasEOF = true;
                }
            }
            if (!hasEOF) {
                tokens.add(new Token("EOF", "$")); // 使用 "$" 作为 EOF 的值
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
            ProgramNode astRoot = astParser.parseProgram(); // 解析器返回 AST 的根节点

            if (astRoot != null) {
                System.out.println("\n--- AST 结构展示 ---");
                astRoot.printTree("", true); // 调用打印 AST 的方法
                System.out.println("----------------------\n");

                // 4. 从 AST 生成三地址码
                TACContext tacContext = new TACContext();
                astRoot.generateTAC(tacContext); // 此方法会填充 tacContext.instructions

                System.out.println("=== 生成的三地址码 (来自文件 -> AST -> TAC) ===");
                for (String instruction : tacContext.instructions) {
                    System.out.println(instruction);
                }

                // 5. 从 AST 根据三地址码生成汇编代码
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
        } catch (Exception e) { // 捕获词法分析或语法分析可能抛出的 RuntimeException
            System.err.println("处理过程中发生错误: " + e.getMessage());
            e.printStackTrace(); // 打印详细错误信息，便于调试
        }
    }

    /**
     * 将List<Token>转换为SimplePrecedenceParser期望的token字符串格式
     */
    private static String convertTokensToString(List<Token> tokens) {
        StringBuilder sb = new StringBuilder();
        for (Token token : tokens) {
            sb.append("(").append(token.type).append(", ").append(token.value).append(") ");
        }
        // 移除末尾多余的空格
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

}