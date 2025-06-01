package AST;

public class ProgramNode implements ASTNode {
    BlockNode block;

    public ProgramNode(BlockNode block) {
        this.block = block;
    }

    @Override
    public String generateTAC(TACContext context) {
        context.emit("START_PROGRAM");
        if (block != null) {
            block.generateTAC(context);
        }
        context.emit("END_PROGRAM");
        return null;
    }

    @Override
    public String printTree(String indent, boolean isLast) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append(isLast ? "└── " : "├── ").append("ProgramNode\n");
        if (block != null) {
            sb.append(block.printTree(indent + (isLast ? "    " : "│   "), true));
        }
        return sb.toString();
    }
}
