import java.util.HashMap;

class TypeMap extends HashMap<Variable, Type> {
    public void display() {
        System.out.println(this.entrySet());
        System.out.println("");
    }
}

public class TypeChecker {
    public static TypeMap typing (Declarations declarations) {
        TypeMap tm = new TypeMap();

        for (Declaration d : declarations) {
            tm.put(d.v, d.t);
        }

        return tm;
    }
}
