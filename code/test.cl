int x, y;

int fibonacci (int n) {
   if (n < 2) return n;
   return fibonacci(n-1) + fibonacci(n-2);
}

int main ( ) {
    int n, i, result, j;
    float a, b, c;
    char d, e, f;
    bool g, h;

    ;
    i = 1;
    n = 8;
    i = 1;
    result = 1;
    j = 0;
    a = 0.35;
    b = 99.125;
    c = 80;
    d = 'd';
    f = 'f';
    g = true;
    h = false;

    if (g && !h) {
        a = float(n);
        j = char(n * 6);
        j = j + int(f);
        j = j - int(a * c);
        j = (-j) % n;
    }
    else {
        a = float(n + 1);
        if (a <= c) b = b % a;
    }
    while (i < n) {
        i = i + 1;
        result = result * i;
    }

    d = '\n';

    print f;
    print ' ';
    print j;
    print '\n';
    print e;
    print '\n';
    print 'H';print 'e';print 'l';print 'l';print 'o';print ' ';print 'W';print 'o';print 'r';print 'l';print 'd';print '!';
    print '\n';

    print 1 + 5 + 10 - 10 - int(5.000000);
    print '\n';
    print (3+5)*9;
    print '\n';
    print true;

    print '\n';
    print fibonacci(5);

    return 0;
}
