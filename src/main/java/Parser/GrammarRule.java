package Parser;

public class GrammarRule {
    public String left;
    public String[] right;

    public GrammarRule(String left, String... right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return left + " -> " + String.join(" ", right);
    }
}