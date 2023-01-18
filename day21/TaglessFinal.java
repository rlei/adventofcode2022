import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

class TaglessFinal {
    interface Expr<T> {
        T lit(long n);
        T add(T a, T b);
        T sub(T a, T b);
        T mul(T a, T b);
        T div(T a, T b);
    }

    record Eval() implements Expr<Long> {
        @Override
        public Long lit(long n) {
            return n;
        }

        @Override
        public Long add(Long a, Long b) {
            return a + b;
        }

        @Override
        public Long sub(Long a, Long b) {
            return a - b;
        }

        @Override
        public Long mul(Long a, Long b) {
            return a * b;
        }

        @Override
        public Long div(Long a, Long b) {
            return a / b;
        }
    }

    record Print() implements Expr<String> {
        @Override
        public String lit(long n) {
            return String.valueOf(n);
        }

        @Override
        public String add(String a, String b) {
            return build(a, "+", b);
        }

        private static String build(String a, String op, String b) {
            return "(" + a + " " + op + " " + b + ")";
        }

        @Override
        public String sub(String a, String b) {
            return build(a, "-", b);
        }

        @Override
        public String mul(String a, String b) {
            return build(a, "*", b);
        }

        @Override
        public String div(String a, String b) {
            return build(a, "/", b);
        }
    }

    record LazyEval() implements Expr<Supplier<Long>> {
        @Override
        public Supplier<Long> lit(long n) {
            return () -> n;
        }

        @Override
        public Supplier<Long> add(Supplier<Long> a, Supplier<Long> b) {
            return () -> a.get() + b.get();
        }

        @Override
        public Supplier<Long> sub(Supplier<Long> a, Supplier<Long> b) {
            return () -> a.get() - b.get();
        }

        @Override
        public Supplier<Long> mul(Supplier<Long> a, Supplier<Long> b) {
            return () -> a.get() * b.get();
        }

        @Override
        public Supplier<Long> div(Supplier<Long> a, Supplier<Long> b) {
            return () -> a.get() / b.get();
        }
    }

    record LazyEval2(Eval eval) implements Expr<Supplier<Long>> {
        @Override
        public Supplier<Long> lit(long n) {
            return () -> eval.lit(n);
        }

        @Override
        public Supplier<Long> add(Supplier<Long> a, Supplier<Long> b) {
            return () -> eval.add(a.get(), b.get());
        }

        @Override
        public Supplier<Long> sub(Supplier<Long> a, Supplier<Long> b) {
            return () -> eval.sub(a.get(), b.get());
        }

        @Override
        public Supplier<Long> mul(Supplier<Long> a, Supplier<Long> b) {
            return () -> eval.mul(a.get(), b.get());
        }

        @Override
        public Supplier<Long> div(Supplier<Long> a, Supplier<Long> b) {
            return () -> eval.div(a.get(), b.get());
        }
    }

    record Lazy<T>(Expr<T> eval) implements Expr<Supplier<T>> {
        @Override
        public Supplier<T> lit(long n) {
            return () -> eval.lit(n);
        }

        @Override
        public Supplier<T> add(Supplier<T> a, Supplier<T> b) {
            return () -> eval.add(a.get(), b.get());
        }

        @Override
        public Supplier<T> sub(Supplier<T> a, Supplier<T> b) {
            return () -> eval.sub(a.get(), b.get());
        }

        @Override
        public Supplier<T> mul(Supplier<T> a, Supplier<T> b) {
            return () -> eval.mul(a.get(), b.get());
        }

        @Override
        public Supplier<T> div(Supplier<T> a, Supplier<T> b) {
            return () -> eval.div(a.get(), b.get());
        }
    }

    interface Equation<T> extends Expr<T> {
        T root(T a, T b);
        T X();
    }

    interface Solvable {
        Optional<Long> tryEval();
        Optional<Long> trySolve(long fromResult);

        default Optional<Long> apply2(Solvable a, Solvable b, BiFunction<Long, Long, Long> op) {
            return a.tryEval().flatMap(aRes -> b.tryEval().map(bRes -> op.apply(aRes, bRes)));
        }

        default Optional<Long> solveEither(Solvable a, Solvable b, Function<Long, Long> opA, Function<Long, Long> opB) {
            return a.tryEval().map(opA).flatMap(b::trySolve).or(() -> b.tryEval().map(opB).flatMap(a::trySolve));
        }
    }

    record Solver() implements Equation<Solvable> {

        @Override
        public Solvable lit(long n) {
            return new Solvable() {
                @Override
                public Optional<Long> tryEval() {
                    return Optional.of(n);
                }

                @Override
                public Optional<Long> trySolve(long fromResult) {
                    return Optional.empty();
                }
            };
        }

        @Override
        public Solvable add(Solvable a, Solvable b) {
            return new Solvable() {
                @Override
                public Optional<Long> tryEval() {
                    return apply2(a, b, Long::sum);
                }

                @Override
                public Optional<Long> trySolve(long fromResult) {
                    return solveEither(a, b, a1 -> fromResult - a1, b1 -> fromResult - b1);
                }
            };
        }

        @Override
        public Solvable sub(Solvable a, Solvable b) {
            return new Solvable() {
                @Override
                public Optional<Long> tryEval() {
                    return apply2(a, b, (a1, b1) -> a1 - b1);
                }

                @Override
                public Optional<Long> trySolve(long fromResult) {
                    return solveEither(a, b, a1 -> a1 - fromResult, b1 -> b1 + fromResult);
                }
            };
        }

        @Override
        public Solvable mul(Solvable a, Solvable b) {
            return new Solvable() {
                @Override
                public Optional<Long> tryEval() {
                    return apply2(a, b, (a1, b1) -> a1 * b1);
                }

                @Override
                public Optional<Long> trySolve(long fromResult) {
                    return solveEither(a, b, a1 -> fromResult / a1, b1 -> fromResult / b1);
                }
            };
        }

        @Override
        public Solvable div(Solvable a, Solvable b) {
            return new Solvable() {
                @Override
                public Optional<Long> tryEval() {
                    return apply2(a, b, (a1, b1) -> a1 / b1);
                }

                @Override
                public Optional<Long> trySolve(long fromResult) {
                    return solveEither(a, b, a1 -> a1 / fromResult, b1 -> fromResult * b1);
                }
            };
        }

        @Override
        public Solvable root(Solvable a, Solvable b) {
            return new Solvable() {
                @Override
                public Optional<Long> tryEval() {
                    // The root expr is a bit special: either a or b must tryEval to a constant,
                    // trySolve the X in the other one and return its value as root's tryEval result.
                    return a.tryEval().flatMap(b::trySolve).or(() -> b.tryEval().flatMap(a::trySolve));
                }

                @Override
                public Optional<Long> trySolve(long fromResult) {
                    return Optional.empty();
                }
            };
        }

        @Override
        public Solvable X() {
            return new Solvable() {
                @Override
                public Optional<Long> tryEval() {
                    return Optional.empty();
                }

                @Override
                public Optional<Long> trySolve(long fromResult) {
                    return Optional.of(fromResult);
                }
            };
        }
    }

    static <T> T exprProxy(String a, String b, Map<String, T> lookup, BiFunction<T, T, T> builder) {
        return builder.apply(lookup.get(a), lookup.get(b));
    }

    static <T> T program(Expr<T> dsl) {
        return dsl.add(
                dsl.mul(
                        dsl.lit(2), dsl.lit(3)
                ),
                dsl.sub(
                        dsl.lit(4), dsl.lit(6)
                )
        );
    }

    public static void main(String[] args) {
        final var evalDsl = new Eval();
        final var printer = new Print();

        System.out.println(
                printer.add(
                        printer.mul(
                                printer.lit(2), printer.lit(3)
                        ),
                        printer.sub(
                                printer.lit(4), printer.lit(6)
                        )
                ));
        System.out.println(
                evalDsl.add(
                        evalDsl.mul(
                                evalDsl.lit(2), evalDsl.lit(3)
                        ),
                        evalDsl.sub(
                                evalDsl.lit(4), evalDsl.lit(6)
                        )
                ));

        System.out.println(program(printer));
        System.out.println(program(evalDsl));

        final var lazyDsl = new LazyEval();
        System.out.println(program(lazyDsl).get());
        System.out.println(program(new LazyEval2(evalDsl)).get());

        final HashMap<String, Supplier<Long>> lookupTable = loadProgram(lazyDsl);

        System.out.println(lookupTable.get("root").get());
    }

    private static <T> HashMap<String, T> loadProgram(Expr<T> dsl) {
        final var lookupTable = new HashMap<String, T>();
        final Map<String, BiFunction<T, T, T>> opToExpr = Map.of(
            "+", dsl::add,
            "-", dsl::sub,
            "*", dsl::mul,
            "/", dsl::div);
        new BufferedReader(new InputStreamReader(System.in)).lines()
                .forEach(line -> {
                    var strs = line.split(":");
                    var name = strs[0];
                    var exprStr = strs[1].trim();
                    if (Character.isDigit(exprStr.charAt(0))) {
                        lookupTable.put(name, dsl.lit(Long.parseLong(exprStr)));
                    } else {
                        var tokens = exprStr.split(" ");
                        lookupTable.put(name, exprProxy(tokens[0], tokens[2], lookupTable, opToExpr.get(tokens[1])));
                    }
                });
        return lookupTable;
    }

    private static <T> HashMap<String, Supplier<T>> loadLazyProgram(Lazy<T> dsl) {
        final var lookupTable = new HashMap<String, Supplier<T>>();
        final Map<String, BiFunction<Supplier<T>, Supplier<T>, Supplier<T>>> opToExpr = Map.of(
                "+", dsl::add,
                "-", dsl::sub,
                "*", dsl::mul,
                "/", dsl::div);
        new BufferedReader(new InputStreamReader(System.in)).lines()
                .forEach(line -> {
                    var strs = line.split(":");
                    var name = strs[0];
                    var exprStr = strs[1].trim();
                    if (Character.isDigit(exprStr.charAt(0))) {
                        lookupTable.put(name, dsl.lit(Long.parseLong(exprStr)));
                    } else {
                        var tokens = exprStr.split(" ");
                        lookupTable.put(name, exprProxy(tokens[0], tokens[2], lookupTable, opToExpr.get(tokens[1])));
                    }
                });
        return lookupTable;
    }

    interface ArithmeticExpr<T, N> {
        T lit(N n);
        T add(T a, T b);
        T sub(T a, T b);
        T mul(T a, T b);
        T div(T a, T b);
    }
}