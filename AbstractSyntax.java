// Abstract syntax for the language C++Lite,
// exactly as it appears in Appendix B.

import java.util.*;

class Program {
    // Program = Declarations globals ; Functions functions
    Declarations globals;
    Functions functions;

    Program (Declarations d, Functions f) {
        globals = d;
        functions = f;
    }

	public void display() {
        System.out.println("[ Program AST ]");
        System.out.println("\tGlobals :");
        globals.display(2);
        functions.display(1);
        System.out.println("");
    }

    public void V(TypeMap gm) {
        globals.V();
        System.out.println("[ Globals Type Map ]");
        globals.display(1);
        System.out.println("");
        functions.V(gm);
    }

    public Program T(TypeMap gm) {
        functions = functions.T(gm);
        return new Program(globals, functions);
    }

    public State M() {
        Variable main = new Variable("main");
        ArrayList<Value> params = new ArrayList<>();

        return functions.M_S(main, Semantics.initialState(globals), params);
    }
}

class Functions extends ArrayList<Function> {
    public void display(int i) {
        for (int j = 0; j < i; j++) {
            System.out.print("\t");
        }
        System.out.println("Functions :");
        for (Function f : this) {
            f.display(i + 1);
        }
    }

    public void V(TypeMap gm) {
        for (int i = 0; i < this.size() - 1; i++) {
            Function fi = this.get(i);

            for (int j = i + 1; j < this.size(); j++) {
                Function fj = this.get(j);

                if (fi.name.equals(fj.name)) {
                    throw new IllegalArgumentException("duplicate function name : " + fj.name);
                }
            }
        }

        boolean m = false;
        Variable v = new Variable("main");
        for (Function f : this) {
            if (f.name.equals(v) && f.type.equals(Type.INT)) {
                m = true;
            }
        }

        if (!m) {
            throw new IllegalArgumentException("int main function not found : ");
        }

        for (Function f : this) {
            TypeMap tm = new TypeMap();
            tm.putAll(gm);

            for (int i = 0; i < f.params.size() - 1; i++) {
                Declaration di = f.params.get(i);

                for (int j = i + 1; j < f.params.size(); j++) {
                    Declaration dj = f.params.get(j);

                    if (di.v.equals(dj.v)) {
                        throw new IllegalArgumentException("params duplicate declaration : " + dj.v);
                    }
                }
            }

            for (int i = 0; i < f.locals.size() - 1; i++) {
                Declaration di = f.locals.get(i);

                for (int j = i + 1; j < f.locals.size(); j++) {
                    Declaration dj = f.locals.get(j);

                    if (di.v.equals(dj.v)) {
                        throw new IllegalArgumentException("locals duplicate declaration : " + dj.v);
                    }
                }
            }

            for (Declaration di : f.params) {
                for (Declaration dj : f.locals) {
                    if (di.v.equals(dj.v)) {
                        throw new IllegalArgumentException("function duplicate declaration : " + dj.v);
                    }
                }
            }

            f.params.V();
            f.locals.V();

            tm.putAll(TypeChecker.typing(f.params));
            tm.putAll(TypeChecker.typing(f.locals));

            if (f.type.equals(Type.VOID)) {
                for (Statement s : f.body.members) {
                    if (s instanceof Return) throw new IllegalArgumentException("void function cannot have a return value : " + f.name);
                }
            }
            else {
                boolean b = false;

                for (Statement s : f.body.members) {
                    if (s instanceof Return) b = true;
                }

                if (!b) throw new IllegalArgumentException("no return value : " + f.name);
            }

            System.out.println("[ Function Type Map -> " + f.name +  " ]");
            tm.display();

            f.body.V(this, tm);
        }
    }

    public Functions T(TypeMap gm) {
        Functions fs = new Functions();

        for (Function f : this) {
            TypeMap tm = new TypeMap();
            tm.putAll(gm);
            tm.putAll(TypeChecker.typing(f.params));
            tm.putAll(TypeChecker.typing(f.locals));

            Function nf = new Function(f.name, f.type, f.params, f.locals, f.body.T(this, tm));

            fs.add(nf);
        }

        return fs;
    }

    public State M_S(Variable fun, State globals, ArrayList<Value> params) {
        for (int i = 0; i < this.size(); i++) {
            if (this.get(i).name.equals(fun)) { // find function
                State locals = new State();
                Function f = new Function(
                        this.get(i).name, this.get(i).type, this.get(i).params, this.get(i).locals, this.get(i).body
                );
                Semantics.callStack.push(f);

                for (int j = 0; j < f.params.size(); j++) {
                    locals.put(f.params.get(j).v, params.get(j));
                }
                locals.putAll(Semantics.initialState(f.locals));

                f.body.M(this, globals, locals);

                Semantics.callStack.pop();
                break;
            }
        }

        return globals;
    }

    public Value M_V(Variable fun, State globals, ArrayList<Value> params) {
        Value result = null;

        for (int i = 0; i < this.size(); i++) {
            if (this.get(i).name.equals(fun)) { // find function
                State locals = new State();
                Function f = new Function(
                        this.get(i).name, this.get(i).type, this.get(i).params, this.get(i).locals, this.get(i).body
                );
                Semantics.callStack.push(f);

                for (int j = 0; j < f.params.size(); j++) {
                    locals.put(f.params.get(j).v, params.get(j));
                }
                locals.putAll(Semantics.initialState(f.locals));

                f.body.M(this, globals, locals);

                result = f.value;

                Semantics.callStack.pop();
                break;
            }
        }

        return result;
    }
}

class Function {
    Variable name;
    Type type;
    Value value;
    Declarations params, locals;
    Block body;

    Function(Variable n, Type t, Declarations p, Declarations l, Block b) {
        name = n;
        type = t;
        params = p;
        locals = l;
        body = b;

        if (type.equals(Type.INT))
            value = new IntValue();
        else if (type.equals(Type.FLOAT))
            value = new FloatValue();
        else if (type.equals(Type.CHAR))
            value = new CharValue();
        else if (type.equals(Type.BOOL))
            value = new BoolValue();
        else
            value = new VoidValue();
    }

    public void display(int i) {
        for (int j = 0; j < i; j++) {
            System.out.print("\t");
        }
        System.out.println("Function : " + name + "; Return type : " + type);

        i++;

        for (int j = 0; j < i; j++) {
            System.out.print("\t");
        }
        System.out.println("params :");
        params.display(i + 1);

        for (int j = 0; j < i; j++) {
            System.out.print("\t");
        }
        System.out.println("locals :");
        locals.display(i + 1);

        for (int j = 0; j < i; j++) {
            System.out.print("\t");
        }
        System.out.println("body :");
        body.display(i + 1);

        System.out.println("");
    }
}

class Declarations extends ArrayList<Declaration> {

	public void display(int i) {
        for (int j = 0; j < i; j++) {
            System.out.print("\t");
        }
        System.out.println("Declarations :");
		for (Declaration d : this) {
            d.display(i + 1);
        }
	}
    // Declarations = Declaration*
    // (a list of declarations d1, d2, ..., dn)

    public void V() {
        for (int i = 0; i < this.size() - 1; i++) {
            Declaration di = this.get(i);

            for (int j = i + 1; j < this.size(); j++) {
                Declaration dj = this.get(j);

                if (di.v.equals(dj.v)) {
                    throw new IllegalArgumentException("duplicate declaration : " + dj.v);
                }
            }
        }

        for (Declaration d : this) {
            if (d.t.equals(Type.VOID)) throw new IllegalArgumentException("Cannot declare void variable : " + d.v);
        }
    }
}

class Declaration {
// Declaration = Variable v; Type t
    Variable v;
    Type t;

    Declaration (Variable var, Type type) {
        v = var; t = type;
    } // declaration */

    public void display(int i) {
        for (int j = 0; j < i; j++) {
            System.out.print("\t");
        }
        System.out.println(t + " " + v);
    }
}

class Type {
    // Type = int | bool | char | float | void
    final static Type INT = new Type("int");
    final static Type BOOL = new Type("bool");
    final static Type CHAR = new Type("char");
    final static Type FLOAT = new Type("float");
    // final static Type UNDEFINED = new Type("undef");
    final static Type VOID = new Type("void");
    
    private String id;

    private Type (String t) { id = t; }

    public String toString ( ) { return id; }
}

abstract class Statement {
    // Statement = Skip | Block | Assignment | Conditional | Loop | Print | StatementCall | Return

    abstract public void display(int i);

    protected void check(boolean b, String s) {
        if (b) {
            return;
        }
        else {
            throw new IllegalArgumentException(s);
        }
    }

    abstract public void V(Functions fs, TypeMap tm);

    abstract public Statement T(Functions fs, TypeMap tm);

    abstract public State M(Functions fs, State globals, State locals);
}

class Skip extends Statement {
    @Override
    public void display(int i) { return; }

    @Override
    public void V(Functions fs, TypeMap tm) {
        return;
    }

    @Override
    public Skip T(Functions fs, TypeMap tm) {
        return new Skip();
    }

    @Override
    public State M(Functions fs, State globals, State locals) {
        return locals;
    }
}

class Block extends Statement {
    // Block = Statement*
    //         (a Vector of members)
    public ArrayList<Statement> members = new ArrayList<>();

    @Override
    public void display(int i) {
        for (int j = 0; j < i; j++) {
            System.out.print("\t");
        }
        System.out.println("Block :");
        for (Statement s : members) {
            s.display(i + 1);
        }
    }

    @Override
    public void V(Functions fs, TypeMap tm) {
        for (Statement s : members) {
            s.V(fs, tm);
        }
    }

    @Override
    public Block T(Functions fs, TypeMap tm) {
        Block b = new Block();

        for (Statement s : members) {
            b.members.add(s.T(fs, tm));
        }

        return b;
    }

    @Override
    public State M(Functions fs, State globals, State locals) {
        State state = new State();

        for (Statement s : members) {
            if (!(Semantics.callStack.get(Semantics.callStack.size() - 1).value.isUndef())) {
                return state;
            }

            state = s.M(fs, globals, locals);
        }

        return state;
    }
}

class Assignment extends Statement {
    // Assignment = Variable target; Expression source
    Variable target;
    Expression source;

    Assignment (Variable t, Expression e) {
        target = t;
        source = e;
    }

    @Override
    public void display(int i) {
        for (int j = 0; j < i; j++) {
            System.out.print("\t");
        }
        System.out.println("Assignment :");
        target.display(i + 1);
        source.display(i + 1);
    }

    @Override
    public void V(Functions fs, TypeMap tm) {
        check(tm.containsKey(target), "assignment target error (undeclared variable) : " + target);
        source.V(fs, tm);

        Type ttype = tm.get(target);
        Type stype = source.typeOf(fs, tm);
        if (ttype != stype) {
            if (ttype == Type.FLOAT) check(stype == Type.INT, "assignment type error : " + target);
            else if (ttype == Type.INT) check(stype == Type.CHAR, "assignment type error : " + target);
            else check(false, "assignment type error : " + target);
        }
    }

    @Override
    public Assignment T(Functions fs, TypeMap tm) {
        Expression e = source.T(fs, tm);

        Type ttype = tm.get(target);
        Type stype = source.typeOf(fs, tm);

        if (ttype == Type.FLOAT) {
            if (stype == Type.INT) {
                e = new Unary(new Operator(Operator.I2F), e);
                stype = Type.FLOAT;
            }
        }
        else if (ttype == Type.INT) {
            if (stype == Type.CHAR) {
                e = new Unary(new Operator(Operator.C2I), e);
                stype = Type.INT;
            }
        }
        check(ttype == stype, "type transform error : " + target);

        return new Assignment(target, e);
    }

    @Override
    public State M(Functions fs, State globals, State locals) {
        if (!(Semantics.callStack.get(Semantics.callStack.size() - 1).value.isUndef())) {
            return locals;
        }

        if (locals.containsKey(target))
            return locals.onion(target, source.M(fs, globals, locals));
        else
            return globals.onion(target, source.M(fs, globals, locals));
    }
}

class Conditional extends Statement {
// Conditional = Expression test; Statement thenbranch, elsebranch
    Expression test;
    Statement thenbranch, elsebranch;
    // elsebranch == null means "if... then"
    
    Conditional (Expression t, Statement tp) {
        test = t; thenbranch = tp; elsebranch = null;
    }
    
    Conditional (Expression t, Statement tp, Statement ep) {
        test = t; thenbranch = tp; elsebranch = ep;
    }

    @Override
    public void display(int i) {
        for (int j = 0; j < i; j++) {
            System.out.print("\t");
        }
        System.out.println("Conditional :");

        for (int j = 0; j < i + 1; j++) {
            System.out.print("\t");
        }
        System.out.println("If :");

        test.display(i + 2);
        thenbranch.display(i + 2);

        if (elsebranch != null) {
            for (int j = 0; j < i + 1; j++) {
                System.out.print("\t");
            }
            System.out.println("Else :");

            elsebranch.display(i + 2);
        }
    }

    @Override
    public void V(Functions fs, TypeMap tm) {
        test.V(fs, tm);

        if (test.typeOf(fs, tm) == Type.BOOL) {
            thenbranch.V(fs, tm);
            if (elsebranch != null) elsebranch.V(fs, tm);
        }
        else {
            check(false, "conditional type error : " + test);
        }
    }

    @Override
    public Conditional T(Functions fs, TypeMap tm) {
        Expression e = test.T(fs, tm);
        Statement st = thenbranch.T(fs, tm);
        Statement se = null;

        if (elsebranch != null) {
            se = elsebranch.T(fs, tm);
        }

        return new Conditional(e, st, se);
    }

    @Override
    public State M(Functions fs, State globals, State locals) {
        if (!(Semantics.callStack.get(Semantics.callStack.size() - 1).value.isUndef())) {
            return locals;
        }

        if(test.M(fs, globals, locals).boolValue())
            return thenbranch.M(fs, globals, locals);
        else
            if (elsebranch != null) {
                return elsebranch.M(fs, globals, locals);
            }
            else {
                return locals;
            }
    }
}

class Loop extends Statement {
// Loop = Expression test; Statement body
    Expression test;
    Statement body;

    Loop (Expression t, Statement b) {
        test = t; body = b;
    }

    @Override
    public void display(int i) {
        for (int j = 0; j < i; j++) {
            System.out.print("\t");
        }
        System.out.println("Loop :");
        test.display(i + 1);
        body.display(i + 1);
    }

    @Override
    public void V(Functions fs, TypeMap tm) {
        test.V(fs, tm);

        if (test.typeOf(fs, tm) == Type.BOOL) body.V(fs, tm);
        else check(false, "loop type error : " + test);
    }

    @Override
    public Loop T(Functions fs, TypeMap tm) {
        Expression e = test.T(fs, tm);
        Statement s = body.T(fs, tm);

        return new Loop(e, s);
    }

    @Override
    public State M(Functions fs, State globals, State locals) {
        if (!(Semantics.callStack.get(Semantics.callStack.size() - 1).value.isUndef())) {
            return locals;
        }

        if (test.M(fs, globals, locals).boolValue())
            return this.M(fs, body.M(fs, globals, locals), locals);
        else
            return locals;
    }
}

class Print extends Statement {
    Expression expression;

    Print(Expression e) {
        expression = e;
    }

    @Override
    public void display(int i) {
        for (int j = 0; j < i; j++) {
            System.out.print("\t");
        }
        System.out.println("Print :");

        expression.display(i + 1);
    }

    @Override
    public void V(Functions fs, TypeMap tm) {
        expression.V(fs, tm);
    }

    @Override
    public Print T(Functions fs, TypeMap tm) {
        Expression e = expression.T(fs, tm);

        return new Print(e);
    }

    @Override
    public State M(Functions fs, State globals, State locals) {
        if (!(Semantics.callStack.get(Semantics.callStack.size() - 1).value.isUndef())) {
            return locals;
        }

        Value v = expression.M(fs, globals, locals);

        System.out.print(v);

        return locals;
    }
}

class StatementCall extends Statement {
    Variable name;
    ArrayList<Expression> params;

    StatementCall(Variable n, ArrayList<Expression> p) {
        name = n;
        params = p;
    }

    public void display(int i) {
        for (int j = 0; j < i; j++) {
            System.out.print("\t");
        }
        System.out.println("StatementCall : " + name);

        for (int j = 0; j < i + 1; j++) {
            System.out.print("\t");
        }
        System.out.println("Args :");

        for (Expression e : params) {
            e.display(i + 2);
        }
    }

    @Override
    public void V(Functions fs, TypeMap tm) {
        boolean c = false;
        Function function = null;

        for (Function f : fs) {
            if (f.name.equals(this.name)) {
                c = true;
                function = f;
            }
        }
        check(c, "undefined function StatementCall : " + name);

        for (Expression e : params) {
            e.V(fs, tm);
        }

        check(function.params.size() == params.size(), "different number of parameters : " + name);

        for (int i = 0; i < params.size(); i++) {
            check(function.params.get(i).t == params.get(i).typeOf(fs, tm), "different type of parameters : " + name);
        }
    }

    @Override
    public StatementCall T(Functions fs, TypeMap tm) {
        ArrayList<Expression> es = new ArrayList<>();

        for (Expression e : params) {
            es.add(e.T(fs, tm));
        }

        return new StatementCall(name, es);
    }

    @Override
    public State M(Functions fs, State globals, State locals) {
        if (!(Semantics.callStack.get(Semantics.callStack.size() - 1).value.isUndef())) {
            return locals;
        }

        ArrayList<Value> p = new ArrayList<>();

        for (Expression e : params) {
            p.add(e.M(fs, globals, locals));
        }

        return fs.M_S(name, globals, p);
    }
}

class ExpressionCall extends Expression {
    Variable name;
    ArrayList<Expression> params;

    ExpressionCall(Variable n, ArrayList<Expression> p) {
        name = n;
        params = p;
    }

    public void display(int i) {
        for (int j = 0; j < i; j++) {
            System.out.print("\t");
        }
        System.out.println("ExpressionCall : " + name);

        for (int j = 0; j < i + 1; j++) {
            System.out.print("\t");
        }
        System.out.println("Args :");

        for (Expression e : params) {
            e.display(i + 2);
        }
    }

    @Override
    protected Type typeOf(Functions fs, TypeMap tm) {
        Type t = null;

        for (Function f : fs) {
            if (f.name.equals(this.name)) {
                t = f.type;
            }
        }

        if (t == null) check(false, "undefined function StatementCall typeOf : " + name);

        return t;
    }

    @Override
    public void V(Functions fs, TypeMap tm) {
        boolean c = false;
        Function function = null;

        for (Function f : fs) {
            if (f.name.equals(this.name)) {
                c = true;
                function = f;
            }
        }
        check(c, "undefined function ExpressionCall : " + name);

        for (Expression e : params) {
            e.V(fs, tm);
        }

        check(function.params.size() == params.size(), "different number of parameters : " + name);

        for (int i = 0; i < params.size(); i++) {
            check(function.params.get(i).t == params.get(i).typeOf(fs, tm), "different type of parameters : " + name);
        }
    }

    @Override
    public ExpressionCall T(Functions fs, TypeMap tm) {
        ArrayList<Expression> es = new ArrayList<>();

        for (Expression e : params) {
            es.add(e.T(fs, tm));
        }

        return new ExpressionCall(name, es);
    }

    @Override
    public Value M(Functions fs, State globals, State locals) {
        for (int i = 0; i < fs.size(); i++) {
            if (fs.get(i).name.equals(name)) {
                ArrayList<Value> p = new ArrayList<>();

                for (Expression e : params) {
                    p.add(e.M(fs, globals, locals));
                }

                return fs.M_V(name, globals, p);
            }
        }

        throw new IllegalArgumentException("ExpressionCall M error");
    }
}

class Return extends Statement {
    Variable name;
    Expression result;

    Return(Variable n, Expression r) {
        name = n;
        result = r;
    }

    @Override
    public void display(int i) {
        for (int j = 0; j < i; j++) {
            System.out.print("\t");
        }
        System.out.println("Return :");
        result.display(i + 1);
    }

    @Override
    public void V(Functions fs, TypeMap tm) {
        boolean c = false;
        Type t = null;
        for (Function f : fs) {
            if (f.name.equals(this.name)) {
                c = true;
                t = f.type;
            }
        }
        check(c, "undefined function Return : " + name);

        result.V(fs, tm);

        check(result.typeOf(fs, tm).equals(t), "function return type error : " + name);
    }

    @Override
    public Return T(Functions fs, TypeMap tm) {
        return new Return(name, result.T(fs, tm));
    }

    @Override
    public State M(Functions fs, State globals, State locals) {
        if (!(Semantics.callStack.get(Semantics.callStack.size() - 1).value.isUndef())) {
            return locals;
        }

        Semantics.callStack.get(Semantics.callStack.size() - 1).value = result.M(fs, globals, locals);

        return locals;
    }
}

abstract class Expression {
    // Expression = Variable | Value | Binary | Unary | ExpressionCall

    abstract public void display(int i);

    protected void check(boolean b, String s) {
        if (b) {
            return;
        }
        else {
            throw new IllegalArgumentException(s);
        }
    }

    abstract protected Type typeOf(Functions fs, TypeMap tm);

    abstract public void V(Functions fs, TypeMap tm);

    abstract public Expression T(Functions fs, TypeMap tm);

    abstract public Value M(Functions fs, State globals, State locals);
}

class Variable extends Expression {
    // Variable = String id
    private String id;

    Variable (String s) { id = s; }

    public String toString( ) { return id; }
    
    public boolean equals (Object obj) {
        String s = ((Variable) obj).id;
        return id.equals(s); // case-sensitive identifiers
    }
    
    public int hashCode ( ) { return id.hashCode( ); }

    @Override
    public void display(int i) {
        for (int j = 0; j < i; j++) {
            System.out.print("\t");
        }
        System.out.println("Var " + id);
    }

    @Override
    protected Type typeOf(Functions fs, TypeMap tm) {
        check(tm.containsKey(this), "undefined variable : " + id);
        return tm.get(this);
    }

    @Override
    public void V(Functions fs, TypeMap tm) {
        check(tm.containsKey(this), "undeclared variable : " + id);
        return;
    }

    @Override
    public Variable T(Functions fs, TypeMap tm) {
        return this;
    }

    @Override
    public Value M(Functions fs, State globals, State locals) {
        if (locals.containsKey(this))
            return locals.get(this);
        else
            return globals.get(this);
    }
}

abstract class Value extends Expression {
    // Value = IntValue | BoolValue |
    //         CharValue | FloatValue
    protected Type type;
    protected boolean undef = true;

    int intValue ( ) {
        assert false : "should never reach here";
        return 0;
    } // implementation of this function is unnecessary can can be removed.
    
    boolean boolValue ( ) {
        assert false : "should never reach here";
        return false;
    }
    
    char charValue ( ) {
        assert false : "should never reach here";
        return ' ';
    }
    
    float floatValue ( ) {
        assert false : "should never reach here";
        return 0.0f;
    }

    boolean isUndef( ) { return undef; }

    Type type ( ) { return type; }

    static Value mkValue (Type type) {
        if (type == Type.INT) return new IntValue( );
        if (type == Type.BOOL) return new BoolValue( );
        if (type == Type.CHAR) return new CharValue( );
        if (type == Type.FLOAT) return new FloatValue( );
        throw new IllegalArgumentException("Illegal type in mkValue");
    }
}

class IntValue extends Value {
    private int value = 0;

    IntValue ( ) { type = Type.INT; }

    IntValue (int v) { this( ); value = v; undef = false; }

    int intValue ( ) {
        assert !undef : "reference to undefined int value";
        return value;
    }

    public String toString( ) {
        if (undef)  return "undef";
        return "" + value;
    }

    @Override
    public void display(int i) {
        for (int j = 0; j < i; j++) {
            System.out.print("\t");
        }
        System.out.println("Int " + value);
    }

    @Override
    protected Type typeOf(Functions fs, TypeMap tm) {
        return type;
    }

    @Override
    public void V(Functions fs, TypeMap tm) {
        return;
    }

    @Override
    public IntValue T(Functions fs, TypeMap tm) {
        return this;
    }

    @Override
    public Value M(Functions fs, State globals, State locals) {
        return this;
    }
}

class BoolValue extends Value {
    private boolean value = false;

    BoolValue ( ) { type = Type.BOOL; }

    BoolValue (boolean v) { this( ); value = v; undef = false; }

    boolean boolValue ( ) {
        assert !undef : "reference to undefined bool value";
        return value;
    }

    int intValue ( ) {
        assert !undef : "reference to undefined bool value";
        return value ? 1 : 0;
    }

    public String toString( ) {
        if (undef)  return "undef";
        return "" + value;
    }

    @Override
    public void display(int i) {
        for (int j = 0; j < i; j++) {
            System.out.print("\t");
        }
        System.out.println("Bool " + value);
    }

    @Override
    protected Type typeOf(Functions fs, TypeMap tm) {
        return type;
    }

    @Override
    public void V(Functions fs, TypeMap tm) {
        return;
    }

    @Override
    public BoolValue T(Functions fs, TypeMap tm) {
        return this;
    }

    @Override
    public Value M(Functions fs, State globals, State locals) {
        return this;
    }
}

class CharValue extends Value {
    private char value = ' ';

    CharValue ( ) { type = Type.CHAR; }

    CharValue (char v) { this( ); value = v; undef = false; }

    char charValue ( ) {
        assert !undef : "reference to undefined char value";
        return value;
    }

    public String toString( ) {
        if (undef)  return "undef";
        return "" + value;
    }

    @Override
    public void display(int i) {
        for (int j = 0; j < i; j++) {
            System.out.print("\t");
        }
        System.out.println("Char " + value);
    }

    @Override
    protected Type typeOf(Functions fs, TypeMap tm) {
        return type;
    }

    @Override
    public void V(Functions fs, TypeMap tm) {
        return;
    }

    @Override
    public CharValue T(Functions fs, TypeMap tm) {
        return this;
    }

    @Override
    public Value M(Functions fs, State globals, State locals) {
        return this;
    }
}

class FloatValue extends Value {
    private float value = 0;

    FloatValue ( ) { type = Type.FLOAT; }

    FloatValue (float v) { this( ); value = v; undef = false; }

    float floatValue ( ) {
        assert !undef : "reference to undefined float value";
        return value;
    }

    public String toString( ) {
        if (undef)  return "undef";
        return "" + value;
    }

    @Override
    public void display(int i) {
        for (int j = 0; j < i; j++) {
            System.out.print("\t");
        }
        System.out.println("Float " + value);
    }

    @Override
    protected Type typeOf(Functions fs, TypeMap tm) {
        return type;
    }

    @Override
    public void V(Functions fs, TypeMap tm) {
        return;
    }

    @Override
    public FloatValue T(Functions fs, TypeMap tm) {
        return this;
    }

    @Override
    public Value M(Functions fs, State globals, State locals) {
        return this;
    }
}

class VoidValue extends Value {
    VoidValue() {
        type = Type.VOID;
    }

    @Override
    public String toString() {
        return "undef";
    }

    @Override
    public void display(int i) {
        for (int j = 0; j < i; j++) {
            System.out.print("\t");
        }
        System.out.println("Void");
    }

    @Override
    protected Type typeOf(Functions fs, TypeMap tm) {
        return type;
    }

    @Override
    public void V(Functions fs, TypeMap tm) {
        return;
    }

    @Override
    public Expression T(Functions fs, TypeMap tm) {
        return this;
    }

    @Override
    public Value M(Functions fs, State globals, State locals) {
        return this;
    }
}

class Binary extends Expression {
// Binary = Operator op; Expression term1, term2
    Operator op;
    Expression term1, term2;

    Binary (Operator o, Expression l, Expression r) {
        op = o; term1 = l; term2 = r;
    } // binary

    @Override
    public void display(int i) {
        for (int j = 0; j < i; j++) {
            System.out.print("\t");
        }
        System.out.println("Binary :");
        op.display(i + 1);
        term1.display(i + 1);
        term2.display(i + 1);
    }

    @Override
    protected Type typeOf(Functions fs, TypeMap tm) {
        if (op.ArithmeticOp()) {
            return term1.typeOf(fs, tm);
        }
        else if (op.RelationalOp() || op.BooleanOp()) {
            return Type.BOOL;
        }
        else {
            throw new IllegalArgumentException("binary type error : " + op);
        }
    }

    @Override
    public void V(Functions fs, TypeMap tm) {
        term1.V(fs, tm);
        term2.V(fs, tm);

        Type tp1 = term1.typeOf(fs, tm);
        Type tp2 = term2.typeOf(fs, tm);
        if(op.ArithmeticOp()) {
            check((tp1 == tp2) && (tp1 == Type.INT || tp1 == Type.FLOAT), "binary type error : " + op);
        }
        else if (op.RelationalOp()) {
            check(tp1 == tp2, "binary type error : " + op);
        }
        else if (op.BooleanOp()) {
            check((tp1 == Type.BOOL) && (tp2 == Type.BOOL), "binary type error : " + op);
        }
        else {
            throw new IllegalArgumentException("binary type error : " + op);
        }
    }

    @Override
    public Binary T(Functions fs, TypeMap tm) {
        Type tp1 = term1.typeOf(fs, tm);
        Type tp2 = term2.typeOf(fs, tm);

        Expression t1 = term1.T(fs, tm);
        Expression t2 = term2.T(fs, tm);

        if(op.ArithmeticOp()) {
            if (tp1 == Type.INT) {
                return new Binary(Operator.intMap(op.val), t1, t2);
            }
            else if (tp1 == Type.FLOAT) {
                return new Binary(Operator.floatMap(op.val), t1, t2);
            }
            else {
                throw new IllegalArgumentException("type transform error : " + op);
            }
        }
        else if (op.RelationalOp()) {
            if (tp1 == Type.INT) {
                return new Binary(Operator.intMap(op.val), t1, t2);
            }
            else if (tp1 == Type.FLOAT) {
                return new Binary(Operator.floatMap(op.val), t1, t2);
            }
            else if (tp1 == Type.CHAR) {
                return new Binary(Operator.charMap(op.val), t1, t2);
            }
            else if (tp1 == Type.BOOL) {
                return new Binary(Operator.boolMap(op.val), t1, t2);
            }
            else {
                throw new IllegalArgumentException("type transform error : " + op);
            }
        }
        else if (op.BooleanOp()) {
            return new Binary(new Operator(op.val), t1, t2);
        }
        else {
            throw new IllegalArgumentException("type transform error : " + op);
        }
    }

    @Override
    public Value M(Functions fs, State globals, State locals) {
        return applyBinary(term1.M(fs, globals, locals), term2.M(fs, globals, locals));
    }

    private Value applyBinary(Value v1, Value v2) {
        check(!v1.isUndef() && !v2.isUndef(), "undef value error : " + op);

        if (op.val.equals(Operator.INT_PLUS))
            return new IntValue(v1.intValue( ) + v2.intValue( ));
        if (op.val.equals(Operator.INT_MINUS))
            return new IntValue(v1.intValue( ) - v2.intValue( ));
        if (op.val.equals(Operator.INT_TIMES))
            return new IntValue(v1.intValue( ) * v2.intValue( ));
        if (op.val.equals(Operator.INT_DIV))
            return new IntValue(v1.intValue( ) / v2.intValue( ));
        if (op.val.equals(Operator.INT_REM))
            return new IntValue(v1.intValue( ) % v2.intValue( ));

        if (op.val.equals(Operator.FLOAT_PLUS))
            return new FloatValue(v1.floatValue( ) + v2.floatValue( ));
        if (op.val.equals(Operator.FLOAT_MINUS))
            return new FloatValue(v1.floatValue( ) - v2.floatValue( ));
        if (op.val.equals(Operator.FLOAT_TIMES))
            return new FloatValue(v1.floatValue( ) * v2.floatValue( ));
        if (op.val.equals(Operator.FLOAT_DIV))
            return new FloatValue(v1.floatValue( ) / v2.floatValue( ));
        if (op.val.equals(Operator.FLOAT_REM))
            return new FloatValue(v1.floatValue( ) % v2.floatValue( ));

        if (op.val.equals(Operator.INT_LT))
            return new BoolValue(v1.intValue( ) < v2.intValue( ));
        if (op.val.equals(Operator.INT_LE))
            return new BoolValue(v1.intValue( ) <= v2.intValue( ));
        if (op.val.equals(Operator.INT_EQ))
            return new BoolValue(v1.intValue( ) == v2.intValue( ));
        if (op.val.equals(Operator.INT_NE))
            return new BoolValue(v1.intValue( ) != v2.intValue( ));
        if (op.val.equals(Operator.INT_GT))
            return new BoolValue(v1.intValue( ) > v2.intValue( ));
        if (op.val.equals(Operator.INT_GE))
            return new BoolValue(v1.intValue( ) >= v2.intValue( ));

        if (op.val.equals(Operator.FLOAT_LT))
            return new BoolValue(v1.floatValue( ) <  v2.floatValue( ));
        if (op.val.equals(Operator.FLOAT_LE))
            return new BoolValue(v1.floatValue( ) <= v2.floatValue( ));
        if (op.val.equals(Operator.FLOAT_EQ))
            return new BoolValue(v1.floatValue( ) == v2.floatValue( ));
        if (op.val.equals(Operator.FLOAT_NE))
            return new BoolValue(v1.floatValue( ) != v2.floatValue( ));
        if (op.val.equals(Operator.FLOAT_GT))
            return new BoolValue(v1.floatValue( ) >  v2.floatValue( ));
        if (op.val.equals(Operator.FLOAT_GE))
            return new BoolValue(v1.floatValue( ) >= v2.floatValue( ));

        if (op.val.equals(Operator.CHAR_LT))
            return new BoolValue(v1.charValue( ) <  v2.charValue( ));
        if (op.val.equals(Operator.CHAR_LE))
            return new BoolValue(v1.charValue( ) <= v2.charValue( ));
        if (op.val.equals(Operator.CHAR_EQ))
            return new BoolValue(v1.charValue( ) == v2.charValue( ));
        if (op.val.equals(Operator.CHAR_NE))
            return new BoolValue(v1.charValue( ) != v2.charValue( ));
        if (op.val.equals(Operator.CHAR_GT))
            return new BoolValue(v1.charValue( ) >  v2.charValue( ));
        if (op.val.equals(Operator.CHAR_GE))
            return new BoolValue(v1.charValue( ) >= v2.charValue( ));

        if (op.val.equals(Operator.BOOL_EQ))
            return new BoolValue(v1.boolValue( ) == v2.boolValue( ));
        if (op.val.equals(Operator.BOOL_NE))
            return new BoolValue(v1.boolValue( ) != v2.boolValue( ));
//        // Unable to compare boolean in Java
//        if (op.val.equals(Operator.BOOL_LT))
//            return new BoolValue(v1.boolValue( ) < v2.boolValue( ));
//        if (op.val.equals(Operator.BOOL_LE))
//            return new BoolValue(v1.boolValue( ) <= v2.boolValue( ));
//        if (op.val.equals(Operator.BOOL_GT))
//            return new BoolValue(v1.boolValue( ) > v2.boolValue( ));
//        if (op.val.equals(Operator.BOOL_GE))
//            return new BoolValue(v1.boolValue( ) >= v2.boolValue( ));

        if (op.val.equals(Operator.AND))
            return new BoolValue(v1.boolValue( ) && v2.boolValue( ));
        if (op.val.equals(Operator.OR))
            return new BoolValue(v1.boolValue( ) || v2.boolValue( ));

        throw new IllegalArgumentException("apply binary error : " + op);
    }
}

class Unary extends Expression {
    // Unary = Operator op; Expression term
    Operator op;
    Expression term;

    Unary (Operator o, Expression e) {
        op = o; term = e;
    } // unary

    @Override
    public void display(int i) {
        for (int j = 0; j < i; j++) {
            System.out.print("\t");
        }
        System.out.println("Unary :");
        op.display(i + 1);
        term.display(i + 1);
    }

    @Override
    protected Type typeOf(Functions fs, TypeMap tm) {
        if (op.NotOp()) {
            return Type.BOOL;
        }
        else if (op.NegateOp()) {
            return term.typeOf(fs, tm);
        }
        else if (op.intOp()) {
            return Type.INT;
        }
        else if (op.floatOp()) {
            return Type.FLOAT;
        }
        else if (op.charOp()) {
            return Type.CHAR;
        }
        else {
            throw new IllegalArgumentException("unary type error : " + op);
        }
    }

    @Override
    public void V(Functions fs, TypeMap tm) {
        term.V(fs, tm);

        Type tp = term.typeOf(fs, tm);
        if (op.NotOp()) {
            check(tp == Type.BOOL, "unary type error : " + op);
        }
        else if (op.NegateOp()) {
            check((tp == Type.INT) || (tp == Type.FLOAT), "unary type error : " + op);
        }
        else if (op.intOp()) {
            check((tp == Type.FLOAT) || (tp == Type.CHAR), "unary type error : " + op);
        }
        else if (op.floatOp()) {
            check(tp == Type.INT, "unary type error : " + op);
        }
        else if (op.charOp()) {
            check(tp == Type.INT, "unary type error : " + op);
        }
        else {
            throw new IllegalArgumentException("unary type error : " + op);
        }
    }

    @Override
    public Unary T(Functions fs, TypeMap tm) {
        Type t = term.typeOf(fs, tm);
        Expression e = term.T(fs, tm);

        if ((t == Type.BOOL) && (op.NotOp())) {
            return new Unary(new Operator(Operator.NOT), e);
        }
        else if (op.NegateOp()) {
            if (t == Type.FLOAT) {
                return new Unary(new Operator(Operator.FLOAT_NEG), e);
            }
            else if (t == Type.INT) {
                return new Unary(new Operator(Operator.INT_NEG), e);
            }
            else {
                throw new IllegalArgumentException("type transform error : " + op);
            }
        }
        else if (op.intOp()) {
            if (t == Type.FLOAT) {
                return new Unary(new Operator(Operator.F2I), e);
            }
            else if (t == Type.CHAR) {
                return new Unary(new Operator(Operator.C2I), e);
            }
            else {
                throw new IllegalArgumentException("type transform error : " + op);
            }
        }
        else if ((t == Type.INT) && op.floatOp()) {
            return new Unary(new Operator(Operator.I2F), e);
        }
        else if ((t == Type.INT) && op.charOp()) {
            return new Unary(new Operator(Operator.I2C), e);
        }
        else {
            throw new IllegalArgumentException("type transform error : " + op);
        }
    }

    @Override
    public Value M(Functions fs, State globals, State locals) {
        return applyUnary(term.M(fs, globals, locals));
    }

    private Value applyUnary(Value v) {
        check(!v.isUndef(), "undef value error : " + op);

        if (op.val.equals(Operator.NOT))
            return new BoolValue(!v.boolValue( ));
        if (op.val.equals(Operator.INT_NEG))
            return new IntValue(-v.intValue( ));
        if (op.val.equals(Operator.FLOAT_NEG))
            return new FloatValue(-v.floatValue( ));
        if (op.val.equals(Operator.I2F))
            return new FloatValue((float)(v.intValue( )));
        if (op.val.equals(Operator.F2I))
            return new IntValue((int)(v.floatValue( )));
        if (op.val.equals(Operator.C2I))
            return new IntValue((int)(v.charValue( )));
        if (op.val.equals(Operator.I2C))
            return new CharValue((char)(v.intValue( )));

        throw new IllegalArgumentException("apply binary error : " + op);
    }
}

class Operator {
    // Operator = BooleanOp | RelationalOp | ArithmeticOp | UnaryOp
    // BooleanOp = && | ||
    final static String AND = "&&";
    final static String OR = "||";
    // RelationalOp = < | <= | == | != | >= | >
    final static String LT = "<";
    final static String LE = "<=";
    final static String EQ = "==";
    final static String NE = "!=";
    final static String GT = ">";
    final static String GE = ">=";
    // ArithmeticOp = + | - | * | /
    final static String PLUS = "+";
    final static String MINUS = "-";
    final static String TIMES = "*";
    final static String DIV = "/";
    final static String REM = "%";
    // UnaryOp = !    
    final static String NOT = "!";
    final static String NEG = "-";
    // CastOp = int | float | char
    final static String INT = "int";
    final static String FLOAT = "float";
    final static String CHAR = "char";
    // Typed Operators
    // RelationalOp = < | <= | == | != | >= | >
    final static String INT_LT = "INT<";
    final static String INT_LE = "INT<=";
    final static String INT_EQ = "INT==";
    final static String INT_NE = "INT!=";
    final static String INT_GT = "INT>";
    final static String INT_GE = "INT>=";
    // ArithmeticOp = + | - | * | /
    final static String INT_PLUS = "INT+";
    final static String INT_MINUS = "INT-";
    final static String INT_TIMES = "INT*";
    final static String INT_DIV = "INT/";
    final static String INT_REM = "INT%";
    // UnaryOp = !    
    final static String INT_NEG = "-";
    // RelationalOp = < | <= | == | != | >= | >
    final static String FLOAT_LT = "FLOAT<";
    final static String FLOAT_LE = "FLOAT<=";
    final static String FLOAT_EQ = "FLOAT==";
    final static String FLOAT_NE = "FLOAT!=";
    final static String FLOAT_GT = "FLOAT>";
    final static String FLOAT_GE = "FLOAT>=";
    // ArithmeticOp = + | - | * | /
    final static String FLOAT_PLUS = "FLOAT+";
    final static String FLOAT_MINUS = "FLOAT-";
    final static String FLOAT_TIMES = "FLOAT*";
    final static String FLOAT_DIV = "FLOAT/";
    final static String FLOAT_REM = "FLOAT%";
    // UnaryOp = !    
    final static String FLOAT_NEG = "-";
    // RelationalOp = < | <= | == | != | >= | >
    final static String CHAR_LT = "CHAR<";
    final static String CHAR_LE = "CHAR<=";
    final static String CHAR_EQ = "CHAR==";
    final static String CHAR_NE = "CHAR!=";
    final static String CHAR_GT = "CHAR>";
    final static String CHAR_GE = "CHAR>=";
    // RelationalOp = < | <= | == | != | >= | >
    final static String BOOL_LT = "BOOL<";
    final static String BOOL_LE = "BOOL<=";
    final static String BOOL_EQ = "BOOL==";
    final static String BOOL_NE = "BOOL!=";
    final static String BOOL_GT = "BOOL>";
    final static String BOOL_GE = "BOOL>=";
    // Type specific cast
    final static String I2F = "I2F";
    final static String F2I = "F2I";
    final static String C2I = "C2I";
    final static String I2C = "I2C";
    
    String val;
    
    Operator (String s) { val = s; }

    public String toString( ) { return val; }
    public boolean equals(Object obj) { return val.equals(obj); }
    
    boolean BooleanOp ( ) { return val.equals(AND) || val.equals(OR); }
    boolean RelationalOp ( ) {
        return val.equals(LT) || val.equals(LE) || val.equals(EQ)
            || val.equals(NE) || val.equals(GT) || val.equals(GE);
    }
    boolean ArithmeticOp ( ) {
        return val.equals(PLUS) || val.equals(MINUS)
            || val.equals(TIMES) || val.equals(DIV) || val.equals(REM);
    }
    boolean NotOp ( ) { return val.equals(NOT) ; }
    boolean NegateOp ( ) { return val.equals(NEG) ; }
    boolean intOp ( ) { return val.equals(INT); }
    boolean floatOp ( ) { return val.equals(FLOAT); }
    boolean charOp ( ) { return val.equals(CHAR); }

    final static String intMap[ ] [ ] = {
        {PLUS, INT_PLUS}, {MINUS, INT_MINUS},
        {TIMES, INT_TIMES}, {DIV, INT_DIV}, {REM, INT_REM},
        {EQ, INT_EQ}, {NE, INT_NE}, {LT, INT_LT},
        {LE, INT_LE}, {GT, INT_GT}, {GE, INT_GE},
        {NEG, INT_NEG}, {FLOAT, I2F}, {CHAR, I2C}
    };

    final static String floatMap[ ] [ ] = {
        {PLUS, FLOAT_PLUS}, {MINUS, FLOAT_MINUS},
        {TIMES, FLOAT_TIMES}, {DIV, FLOAT_DIV}, {REM, FLOAT_REM},
        {EQ, FLOAT_EQ}, {NE, FLOAT_NE}, {LT, FLOAT_LT},
        {LE, FLOAT_LE}, {GT, FLOAT_GT}, {GE, FLOAT_GE},
        {NEG, FLOAT_NEG}, {INT, F2I}
    };

    final static String charMap[ ] [ ] = {
        {EQ, CHAR_EQ}, {NE, CHAR_NE}, {LT, CHAR_LT},
        {LE, CHAR_LE}, {GT, CHAR_GT}, {GE, CHAR_GE},
        {INT, C2I}
    };

    final static String boolMap[ ] [ ] = {
        {EQ, BOOL_EQ}, {NE, BOOL_NE}, {LT, BOOL_LT},
        {LE, BOOL_LE}, {GT, BOOL_GT}, {GE, BOOL_GE},
    };

    final static private Operator map (String[][] tmap, String op) {
        for (int i = 0; i < tmap.length; i++)
            if (tmap[i][0].equals(op))
                return new Operator(tmap[i][1]);
        assert false : "should never reach here";
        return null;
    }

    final static public Operator intMap (String op) {
        return map (intMap, op);
    }

    final static public Operator floatMap (String op) {
        return map (floatMap, op);
    }

    final static public Operator charMap (String op) {
        return map (charMap, op);
    }

    final static public Operator boolMap (String op) {
        return map (boolMap, op);
    }

    public void display(int i) {
        for (int j = 0; j < i; j++) {
            System.out.print("\t");
        }
        System.out.println(val);
    }
}