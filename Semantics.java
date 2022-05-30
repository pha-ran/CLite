import java.util.HashMap;
import java.util.Stack;

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
    public static Stack<Function> callStack = new Stack<>();

    public static State initialState(Declarations declarations) {
        State state = new State();

        for (Declaration d : declarations) {
            state.put(d.v, Value.mkValue(d.t));
        }

        return state;
    }
    public static void main(String[] args) {
        try {
            Parser parser  = new Parser(new Lexer(args[0]));
            Program prog = parser.program();
            //prog.display();

            TypeMap gm = TypeChecker.typing(prog.globals);
            prog.V(gm);
            System.out.println("[ Program is Valid ]\n");

            prog = prog.T(gm);
            prog.display();

            System.out.println("[ Program Meaning ]");
            State res = prog.M();

            System.out.println("\n\n[ Final Globals ]");
            res.display();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    } //main
}
