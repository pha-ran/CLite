import java.util.HashMap;

class TypeMap extends HashMap<Variable, Type> {
    public void display() {
        System.out.println(this.entrySet());
        System.out.println("");
    }
}

public class TypeChecker {
    public static void main(String[] args) {
        Parser parser  = new Parser(new Lexer(args[0]));
        Program prog = parser.program();
        try {
            prog.V();
            System.out.println("Program is Valid");
        } catch (Exception e) {
            System.err.println("Type Error\n" + e.getMessage());
            System.exit(1);
        }
    } //main
}
