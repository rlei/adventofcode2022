class TaggedData {
    sealed interface Expr {}
    record Lit(long n) implements Expr {}
    record Add(Expr a, Expr b) implements Expr {}
    record Sub(Expr a, Expr b) implements Expr {}
    record Mul(Expr a, Expr b) implements Expr {}
    record Div(Expr a, Expr b) implements Expr {}

    static long eval(Expr e) {
        // Java 18+: Pattern Matching for switch Expressions and Statements
        return switch (e) {
            case Lit lit -> lit.n;
            case Add expr -> eval(expr.a) + eval(expr.b);
            case Sub expr -> eval(expr.a) - eval(expr.b);
            case Mul expr -> eval(expr.a) * eval(expr.b);
            case Div expr -> eval(expr.a) / eval(expr.b);
        };
    }

    /*
    // won't work
    static long lazyEval(Expr e) {
        return switch (e) {
            case ExprProxy proxy -> eval(proxy.resolve());
            default -> eval(e);
        };
    }
     */

    static String print(Expr e) {
        return switch (e) {
            case Lit lit -> String.valueOf(lit.n);
            case Add expr -> "(" + print(expr.a) + "+" + print(expr.b) + ")";
            case Sub expr -> "(" + print(expr.a) + "-" + print(expr.b) + ")";
            case Mul expr -> "(" + print(expr.a) + "*" + print(expr.b) + ")";
            case Div expr -> "(" + print(expr.a) + "/" + print(expr.b) + ")";
        };
    }

    /*
    record ExprProxy(String a, String b, Map<String, Expr> lookup, BiFunction<Expr, Expr, Expr> opToExpr) implements Expr {
        public Expr resolve() {
            return opToExpr.apply(lookup.get(a), lookup.get(b));
        }
    }
     */

    static Expr program =
        new Add(
                new Mul(
                        new Lit(2), new Lit(3)
                ),
                new Sub(
                        new Lit(4), new Lit(6)
                )
        );

    public static void main(String[] args) {
        System.out.println(print(program));
        System.out.println(eval(program));
    }
}