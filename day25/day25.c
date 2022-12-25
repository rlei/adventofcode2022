#include <stdio.h>

int main(int argc, char* argv[]) {
    char buf[256];
    int64_t num = 0;
    int64_t sum = 0;
    while (fgets(buf, sizeof(buf), stdin) != NULL) {
        for (const char *p = buf; *p; p++) {
            char ch = *p;
            switch (ch) {
            case '-':
                num = num * 5 - 1;
                break;
            case '=':
                num = num * 5 - 2;
                break;
            default:
                if (ch >= '0' && ch <= '9') {
                    num = num * 5 + ch - '0';
                }
            }
        }
        // printf("%lld\n", num);
        sum += num;
        num = 0;
    }
    printf ("sum %lld\n", sum);
    char *out = buf;
    do {
        int rem5 = sum % 5;
        switch (rem5) {
        case 4:
            *out++ = '-';
            sum = (sum + 1) / 5;
            break;
        case 3:
            *out++ = '=';
            sum = (sum + 2) / 5;
            break;
        default:
            *out++ = rem5 + '0';
            sum /= 5;
            break;
        }
    } while (sum != 0);
    out--;
    while (out >= buf) fputc(*out--, stdout);
    fputc('\n', stdout);

    return 0;
}