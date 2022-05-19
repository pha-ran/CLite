// Abstract syntax for the language C++Lite,
// exactly as it appears in Appendix B.

import java.util.*;

class Program {
    // Program = Declarations decpart ; Block body
    Declarations decpart;
    Block body;

    Program (Declarations d, Block b) {
        decpart = d;
        body = b;
    }

	public void display() {
        System.out.println("[ Program AST ]");
        decpart.display(1);
        body.display(1);
    }

    public TypeMap typing (Declarations ds) {
        TypeMap tm = new TypeMap();

        for (Declaration d : ds) {
            tm.put(d.v, d.t);
        }

        return tm;
    }

    public void V() {
        decpart.V();
        TypeMap tm = typing(decpart);
        System.out.println("[ Type Map ]");
        tm.display();
        body.V(tm);
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
    // Type = int | bool | char | float 
    final static Type INT = new Type("int");
    final static Type BOOL = new Type("bool");
    final static Type CHAR = new Type("char");
    final static Type FLOAT = new Type("float");
    // final static Type UNDEFINED = new Type("undef");
    
    private String id;

    private Type (String t) { id = t; }

    public String toString ( ) { return id; }
}

abstract class Statement {
    // Statement = Skip | Block | Assignment | Conditional | Loop

    public void display(int i) {

    }

    protected void check(boolean b, String s) {
        if (b) {
            return;
        }
        else {
            throw new IllegalArgumentException(s);
        }
    }

    abstract public void V(TypeMap tm);
}

class Skip extends Statement {
    @Override
    public void V(TypeMap tm) {
        return;
    }
}

class Block extends Statement {
    // Block = Statement*
    //         (a Vector of members)
    public ArrayList<Statement> members = new ArrayList<Statement>();

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
    public void V(TypeMap tm) {
        for (Statement s : members) {
            s.V(tm);
        }
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
    public void V(TypeMap tm) {
        check(tm.containsKey(target), "assignment target error (undeclared variable) : " + target);
        source.V(tm);

        Type ttype = tm.get(target);
        Type stype = source.typeOf(tm);
        if (ttype != stype) {
            if (ttype == Type.FLOAT) check(stype == Type.INT, "assignment type error : " + target);
            else if (ttype == Type.INT) check(stype == Type.CHAR, "assignment type error : " + target);
            else check(false, "assignment type error : " + target);
        }
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
    public void V(TypeMap tm) {
        test.V(tm);

        if (test.typeOf(tm) == Type.BOOL) {
            thenbranch.V(tm);
            if (elsebranch != null) elsebranch.V(tm);
        }
        else {
            check(false, "conditional type error : " + test);
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
    public void V(TypeMap tm) {
        test.V(tm);

        if (test.typeOf(tm) == Type.BOOL) body.V(tm);
        else check(false, "loop type error : " + test);
    }
}

abstract class Expression {
    // Expression = Variable | Value | Binary | Unary

    public void display(int i) {

    }

    protected void check(boolean b, String s) {
        if (b) {
            return;
        }
        else {
            throw new IllegalArgumentException(s);
        }
    }

    abstract protected Type typeOf(TypeMap tm);

    abstract public void V(TypeMap tm);
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
    protected Type typeOf(TypeMap tm) {
        check(tm.containsKey(this), "undefined variable : " + id);
        return tm.get(this);
    }

    @Override
    public void V(TypeMap tm) {
        check(tm.containsKey(this), "undeclared variable : " + id);
        return;
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

    public void display(int i) {

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
    protected Type typeOf(TypeMap tm) {
        return type;
    }

    @Override
    public void V(TypeMap tm) {
        return;
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
    protected Type typeOf(TypeMap tm) {
        return type;
    }

    @Override
    public void V(TypeMap tm) {
        return;
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
    protected Type typeOf(TypeMap tm) {
        return type;
    }

    @Override
    public void V(TypeMap tm) {
        return;
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
    protected Type typeOf(TypeMap tm) {
        return type;
    }

    @Override
    public void V(TypeMap tm) {
        return;
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
    protected Type typeOf(TypeMap tm) {
        if (op.ArithmeticOp()) {
            return term1.typeOf(tm);
        }
        else if (op.RelationalOp() || op.BooleanOp()) {
            return Type.BOOL;
        }
        else {
            throw new IllegalArgumentException("binary type error : " + op);
        }
    }

    @Override
    public void V(TypeMap tm) {
        term1.V(tm);
        term2.V(tm);

        Type tp1 = term1.typeOf(tm);
        Type tp2 = term2.typeOf(tm);
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
    protected Type typeOf(TypeMap tm) {
        if (op.NotOp()) {
            return Type.BOOL;
        }
        else if (op.NegateOp()) {
            return term.typeOf(tm);
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
    public void V(TypeMap tm) {
        term.V(tm);

        Type tp = term.typeOf(tm);
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
            || val.equals(TIMES) || val.equals(DIV);
    }
    boolean NotOp ( ) { return val.equals(NOT) ; }
    boolean NegateOp ( ) { return val.equals(NEG) ; }
    boolean intOp ( ) { return val.equals(INT); }
    boolean floatOp ( ) { return val.equals(FLOAT); }
    boolean charOp ( ) { return val.equals(CHAR); }

    final static String intMap[ ] [ ] = {
        {PLUS, INT_PLUS}, {MINUS, INT_MINUS},
        {TIMES, INT_TIMES}, {DIV, INT_DIV},
        {EQ, INT_EQ}, {NE, INT_NE}, {LT, INT_LT},
        {LE, INT_LE}, {GT, INT_GT}, {GE, INT_GE},
        {NEG, INT_NEG}, {FLOAT, I2F}, {CHAR, I2C}
    };

    final static String floatMap[ ] [ ] = {
        {PLUS, FLOAT_PLUS}, {MINUS, FLOAT_MINUS},
        {TIMES, FLOAT_TIMES}, {DIV, FLOAT_DIV},
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