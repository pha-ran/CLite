int main() {
    int x, i, max, sum;
    bool isp;

    sum = 0;
    x = 1000;
    max = 2000;

    while (x < max) {
        isp = true;
        i = 2;

        while (i < x) {
            if (x % i == 0) {
                isp = false;
                i = max;
            }
            i = i + 1;
        }

        if (isp) {
            print x;
            print '\n';
            sum = sum + 1;
        }

        x = x + 1;
    }

    print '\n';
    print 's';print 'u';print 'm';print ':';
    print sum;
    print '\n';
}