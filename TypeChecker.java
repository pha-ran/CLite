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
        //prog.display();
        TypeMap tm = prog.typing(prog.decpart);
        try {
            prog.V(tm);
            System.out.println("Program is Valid\n");
            prog = prog.T(tm);
            prog.display();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    } //main
}
