import java.util.HashMap;

class State extends HashMap<Variable, Value> {
    public State onion(Variable key, Value val) {
        put(key, val);
        return this;
    }

    public void display() {
        System.out.println(this.entrySet());
        System.out.println("");
    }
}

public class Semantics {
    public static void main(String[] args) {
        try {
            Parser parser  = new Parser(new Lexer(args[0]));
            Program prog = parser.program();
            //prog.display();
            TypeMap tm = prog.typing();

            prog.V(tm);
            System.out.println("[ Program is Valid ]\n");

            prog = prog.T(tm);
            prog.display();

            System.out.println("[ Program Meaning ]");
            State res = prog.M();
            System.out.println("");
            //res.display();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    } //main
}
