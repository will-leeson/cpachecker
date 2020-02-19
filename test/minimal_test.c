typedef _Bool bool;

extern void __VERIFIER_error() __attribute__((noreturn));
extern void __VERIFIER_assume(int cond);

void __VERIFIER_assert(int cond) {
    if(!cond) __VERIFIER_error();
}

_Bool __CPROVER_overflow_plus(unsigned long a, unsigned long b) {
    unsigned long c;
    return __builtin_uaddl_overflow(a, b, &c);
}

int main(){
    unsigned long a = 3UL;
    unsigned long b = 0UL;

    int r = __CPROVER_overflow_plus(a, b);

    r = 1;

    __VERIFIER_assert(!(r));

}