import java.util.*;

public class Parser {
    // Recursive descent parser that inputs a C++Lite program and 
    // generates its abstract syntax.  Each method corresponds to
    // a concrete syntax grammar rule, which appears as a comment
    // at the beginning of the method.
  
    Token token;          // current token from the input stream
    Lexer lexer;
  
    public Parser(Lexer ts) { // Open the C++Lite source program
        lexer = ts;                          // as a token stream, and
        token = lexer.next();            // retrieve its first Token
    }
  
    private String match (TokenType t) { // * return the string of a token if it matches with t *
        String value = token.value();
        if (token.type().equals(t))
            token = lexer.next();
        else
            error(t);
        return value;
    }

    private void error(TokenType tok) {
        System.err.println("Syntax error -> expecting: " + tok
                           + "; saw: " + token);
        System.exit(1);
    }
  
    private void error(String tok) {
        System.err.println("Syntax error -> expecting: " + tok
                           + "; saw: " + token);
        System.exit(1);
    }
  
    public Program program() {
        // Program --> int main ( ) '{' Declarations Statements '}'
        TokenType[ ] header = {TokenType.Int, TokenType.Main,
                          TokenType.LeftParen, TokenType.RightParen};
        for (int i=0; i<header.length; i++)   // bypass "int main ( )"
            match(header[i]);
        match(TokenType.LeftBrace);

        // student exercise
        Declarations ds = declarations();

        Block b = new Block();
        while (token.type().equals(TokenType.Semicolon) ||
                token.type().equals(TokenType.LeftBrace) ||
                token.type().equals(TokenType.Identifier) ||
                token.type().equals(TokenType.If) ||
                token.type().equals(TokenType.While) ||
                token.type().equals(TokenType.Print))
        {
            Statement s = statement();
            b.members.add(s);
        }

        match(TokenType.RightBrace);

        return new Program(ds, b);  // student exercise
    }
  
    private Declarations declarations () {
        // Declarations --> { Declaration }
        Declarations ds = new Declarations();

        while (isType()) {
            declaration(ds);
        }

        return ds;  // student exercise
    }
  
    private void declaration (Declarations ds) {
        // Declaration  --> Type Identifier { , Identifier } ;
        // student exercise
        Type t = type();
        Variable v = new Variable(match(TokenType.Identifier));
        Declaration d = new Declaration(v, t);
        ds.add(d);

        while (token.type().equals(TokenType.Comma)) {
            token = lexer.next();
            v = new Variable(match(TokenType.Identifier));
            d = new Declaration(v, t);
            ds.add(d);
        }

        match(TokenType.Semicolon);
    }
  
    private Type type () {
        // Type  -->  int | bool | float | char 
        Type t = null;

        // student exercise
        if(token.type().equals(TokenType.Int))
            t = Type.INT;
        else if(token.type().equals(TokenType.Bool))
            t= Type.BOOL;
        else if(token.type().equals(TokenType.Float))
            t= Type.FLOAT;
        else if(token.type().equals(TokenType.Char))
            t= Type.CHAR;

        token = lexer.next();

        return t;
    }
  
    private Statement statement() {
        // Statement --> ; | Block | Assignment | IfStatement | WhileStatement | PrintStatement
        Statement s = new Skip();

        // student exercise
        if(token.type().equals(TokenType.LeftBrace))
            s = statements();
        else if(token.type().equals(TokenType.Identifier))
            s = assignment();
        else if(token.type().equals(TokenType.If))
            s = ifStatement();
        else if(token.type().equals(TokenType.While))
            s = whileStatement();
        else if(token.type().equals(TokenType.Print))
            s = printStatement();
        else if(token.type().equals(TokenType.Semicolon))
            token = lexer.next();

        return s;
    }
  
    private Block statements () {
        // Block --> '{' Statements '}'
        Block b = new Block();

        // student exercise
        match(TokenType.LeftBrace);

        while(token.type().equals(TokenType.Semicolon) ||
                token.type().equals(TokenType.LeftBrace) ||
                token.type().equals(TokenType.Identifier) ||
                token.type().equals(TokenType.If) ||
                token.type().equals(TokenType.While) ||
                token.type().equals(TokenType.Print))
        {
            Statement s = statement();
            b.members.add(s);
        }

        match(TokenType.RightBrace);

        return b;
    }
  
    private Assignment assignment () {
        // Assignment --> Identifier = Expression ;
        Variable v = new Variable(match(TokenType.Identifier));

        match(TokenType.Assign);

        Expression e = expression();

        match(TokenType.Semicolon);

        return new Assignment(v, e);  // student exercise
    }
  
    private Conditional ifStatement () {
        // IfStatement --> if ( Expression ) Statement [ else Statement ]
        match(TokenType.If);

        match(TokenType.LeftParen);
        Expression e = expression();
        match(TokenType.RightParen);

        Statement s = statement();

        Conditional c;

        if (token.type().equals(TokenType.Else)) { // match X
            token = lexer.next();
            Statement es = statement();
            c = new Conditional(e, s, es);
        }
        else {
            c = new Conditional(e, s);
        }

        return c;  // student exercise
    }
  
    private Loop whileStatement () {
        // WhileStatement --> while ( Expression ) Statement
        match(TokenType.While);

        match(TokenType.LeftParen);
        Expression e = expression();
        match(TokenType.RightParen);

        Statement s = statement();

        return new Loop(e, s);  // student exercise
    }

    private Print printStatement () {
        match(TokenType.Print);

        Value val = null;
        Variable var = null;

        if (token.type().equals(TokenType.Identifier)) {
            var = new Variable(match(TokenType.Identifier));
        } else if (isLiteral()) {
            val = literal();
        } else error("Identifier | Literal");

        match(TokenType.Semicolon);

        return new Print(val, var);
    }

    private Expression expression () {
        // Expression --> Conjunction { || Conjunction }
        Expression c = conjunction();

        while(token.type().equals(TokenType.Or)) {
            Operator o = new Operator(match(token.type()));
            Expression e = expression();
            c = new Binary(o, c, e);
        }

        return c;  // student exercise
    }
  
    private Expression conjunction () {
        // Conjunction --> Equality { && Equality }
        Expression e = equality();

        while (token.type().equals(TokenType.And)) {
            Operator o = new Operator(match(token.type()));
            Expression c = conjunction();
            e = new Binary(o, e, c);
        }

        return e;  // student exercise
    }
  
    private Expression equality () {
        // Equality --> Relation [ EquOp Relation ]
        Expression r = relation();

        if (isEqualityOp()) {
            Operator o = new Operator(match(token.type()));
            Expression r2 = relation();
            r = new Binary(o, r, r2);
        }

        return r;  // student exercise
    }

    private Expression relation (){
        // Relation --> Addition [RelOp Addition]
        Expression a = addition();

        if (isRelationalOp()) {
            Operator o = new Operator(match(token.type()));
            Expression a2 = addition();
            a = new Binary(o, a, a2);
        }

        return a;  // student exercise
    }
  
    private Expression addition () {
        // Addition --> Term { AddOp Term }
        Expression e = term();
        while (isAddOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term2 = term();
            e = new Binary(op, e, term2);
        }
        return e;
    }
  
    private Expression term () {
        // Term --> Factor { MultiplyOp Factor }
        Expression e = factor();
        while (isMultiplyOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term2 = factor();
            e = new Binary(op, e, term2);
        }
        return e;
    }
  
    private Expression factor() {
        // Factor --> [ UnaryOp ] Primary 
        if (isUnaryOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term = primary();
            return new Unary(op, term);
        }
        else return primary();
    }
  
    private Expression primary () {
        // Primary --> Identifier | Literal | ( Expression )
        //             | Type ( Expression )
        Expression e = null;
        if (token.type().equals(TokenType.Identifier)) {
            e = new Variable(match(TokenType.Identifier));
        } else if (isLiteral()) {
            e = literal();
        } else if (token.type().equals(TokenType.LeftParen)) {
            token = lexer.next();
            e = expression();       
            match(TokenType.RightParen);
        } else if (isType( )) {
            Operator op = new Operator(match(token.type()));
            match(TokenType.LeftParen);
            Expression term = expression();
            match(TokenType.RightParen);
            e = new Unary(op, term);
        } else error("Identifier | Literal | ( | Type");
        return e;
    }

    private Value literal( ) {
        String s = token.value();
        Value v = null;

        if (token.type().equals(TokenType.IntLiteral)) {
            v = new IntValue(Integer.parseInt(s));
        }
        else if (token.type().equals(TokenType.FloatLiteral)) {
            v = new FloatValue(Float.parseFloat(s));
        }
        else if (token.type().equals(TokenType.True)) {
            v = new BoolValue(true);
        }
        else if (token.type().equals(TokenType.False)) {
            v = new BoolValue(false);
        }
        else if (token.type().equals(TokenType.CharLiteral)) {
            v = new CharValue(s.charAt(0));
        }
        else {
            error("Literal error");
        }

        token = lexer.next();

        return v;  // student exercise
    }

    private boolean isAddOp( ) {
        return token.type().equals(TokenType.Plus) ||
               token.type().equals(TokenType.Minus);
    }
    
    private boolean isMultiplyOp( ) {
        return token.type().equals(TokenType.Multiply) ||
               token.type().equals(TokenType.Divide) ||
               token.type().equals(TokenType.Remain);
    }
    
    private boolean isUnaryOp( ) {
        return token.type().equals(TokenType.Not) ||
               token.type().equals(TokenType.Minus);
    }
    
    private boolean isEqualityOp( ) {
        return token.type().equals(TokenType.Equals) ||
            token.type().equals(TokenType.NotEqual);
    }
    
    private boolean isRelationalOp( ) {
        return token.type().equals(TokenType.Less) ||
               token.type().equals(TokenType.LessEqual) || 
               token.type().equals(TokenType.Greater) ||
               token.type().equals(TokenType.GreaterEqual);
    }
    
    private boolean isType( ) {
        return token.type().equals(TokenType.Int)
            || token.type().equals(TokenType.Bool) 
            || token.type().equals(TokenType.Float)
            || token.type().equals(TokenType.Char);
    }
    
    private boolean isLiteral( ) {
        return token.type().equals(TokenType.IntLiteral) ||
            isBooleanLiteral() ||
            token.type().equals(TokenType.FloatLiteral) ||
            token.type().equals(TokenType.CharLiteral);
    }
    
    private boolean isBooleanLiteral( ) {
        return token.type().equals(TokenType.True) ||
            token.type().equals(TokenType.False);
    }

} // Parser
