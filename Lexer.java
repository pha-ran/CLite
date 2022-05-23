import java.io.*;

public class Lexer {

    private boolean isEof = false;
    private char ch = ' '; 
    private BufferedReader input;
    private String line = "";
    private int lineno = 0;
    private int col = 1;
    private final String letters = "abcdefghijklmnopqrstuvwxyz"
        + "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final String digits = "0123456789";
    private final char eolnCh = '\n';
    private final char eofCh = '\004'; // 전송 종료 문자
    

    public Lexer (String fileName) { // source filename
        try {
            input = new BufferedReader (new FileReader(fileName));
        }
        catch (FileNotFoundException e) {
            System.out.println("File not found: " + fileName);
            System.exit(1);
        }
    }

    private char nextChar() { // Return next char
        if (ch == eofCh)
            error("Attempt to read past end of file");
        col++;
        if (col >= line.length()) { // 한 라인이 끝나면
            try {
                line = input.readLine( ); // 다음 라인 읽기
            } catch (IOException e) {
                System.err.println(e);
                System.exit(1);
            } // try
            if (line == null) // at end of file
                line = "" + eofCh;
            else {
                // System.out.println(lineno + ":\t" + line);
                lineno++;
                line += eolnCh; // 라인의 끝 삽입
            } // if line
            col = 0; // 열 초기화
        } // if col
        return line.charAt(col); // 열 번호에 해당하는 문자 반환
    }
            

    public Token next( ) { // Return next token
        do {
            if (isLetter(ch)) { // ident or keyword
                String spelling = concat(letters + digits);
                return Token.keyword(spelling);
            } else if (isDigit(ch)) { // int or float literal
                String number = concat(digits);
                if (ch != '.')  // int Literal
                    return Token.mkIntLiteral(number);
                number += concat(digits);
                return Token.mkFloatLiteral(number);
            } else switch (ch) {
            case ' ': case '\t': case '\r': case eolnCh:
                ch = nextChar();
                break;
            
            case '/':  // divide or comment
                ch = nextChar();
                if (ch != '/')  return Token.divideTok;
                // comment
                do {
                    ch = nextChar();
                } while (ch != eolnCh);
                ch = nextChar();
                break;
            
            case '\'':  // char literal
                char ch1 = nextChar();
                nextChar(); // get '
                ch = nextChar();
                return Token.mkCharLiteral("" + ch1);
                
            case eofCh: return Token.eofTok;
            
            case '+': ch = nextChar();
                return Token.plusTok;

                // - * ( ) { } ; ,  student exercise
            case '-': ch = nextChar();
            	return Token.minusTok;
            case '*': ch = nextChar();
            	return Token.multiplyTok;
            case '%': ch = nextChar();
                return Token.remainTok;

            case '(': ch = nextChar();
            	return Token.leftParenTok;
            case ')': ch = nextChar();
            	return Token.rightParenTok;
            	
            case '{': ch = nextChar();
            	return Token.leftBraceTok;
            case '}': ch = nextChar();
            	return Token.rightBraceTok;
            	
            case ';': ch = nextChar();
            	return Token.semicolonTok;
            	
            case ',': ch = nextChar();
            	return Token.commaTok;
                
            case '&': check('&'); return Token.andTok;
            case '|': check('|'); return Token.orTok;

            case '=':
                return chkOpt('=', Token.assignTok,
                                   Token.eqeqTok);
                // < > !  student exercise
            case '<':
            	return chkOpt('=', Token.ltTok,
            					   Token.lteqTok);
            case '>':
            	return chkOpt('=', Token.gtTok,
            					   Token.gteqTok);
            case '!':
            	return chkOpt('=', Token.notTok,
            					   Token.noteqTok);

            default:  error("Illegal character " + ch); 
            } // switch
        } while (true);
    } // next


    private boolean isLetter(char c) {
        return (c>='a' && c<='z' || c>='A' && c<='Z');
    }
  
    private boolean isDigit(char c) {
        return (c >= '0' && c <= '9');  // student exercise
    }

    private void check(char c) {
        ch = nextChar();
        if (ch != c) 
            error("Illegal character, expecting " + c);
        ch = nextChar();
    }

    private Token chkOpt(char c, Token one, Token two) {
    	ch = nextChar();
    	if (ch == c) {
    		ch = nextChar();
    		return two;
    	}
        return one;  // student exercise
    }

    private String concat(String set) {
        String r = "";
        do {
            r += ch;
            ch = nextChar();
        } while (set.indexOf(ch) >= 0);
        return r;
    }

    public void error (String msg) {
        System.err.print(line);
        System.err.println("Error: column " + col + " " + msg);
        System.exit(1);
    }
}

