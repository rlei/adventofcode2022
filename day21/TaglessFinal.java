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

    static class Lazy<T> implements Expr<Supplier<T>> {
        private final Expr<T> eval;

        Lazy(Expr<T> eval) {
            this.eval = eval;
        }

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

    static class LazyEquation<T> implements Equation<Supplier<T>> {
        private final Equation<T> equationDsl;

        // Only root() and X() need to be implemented for LazyEquation. The rest are simply delegated to Lazy<>.
        private final Lazy<T> lazyDelegate;

        LazyEquation(Equation<T> equationDsl) {
            this.equationDsl = equationDsl;
            this.lazyDelegate = new Lazy<>(equationDsl);
        }

        @Override
        public Supplier<T> root(Supplier<T> a, Supplier<T> b) {
            return () -> equationDsl.root(a.get(), b.get());
        }

        @Override
        public Supplier<T> X() {
            return equationDsl::X;
        }

        @Override
        public Supplier<T> lit(long n) {
            return lazyDelegate.lit(n);
        }

        @Override
        public Supplier<T> add(Supplier<T> a, Supplier<T> b) {
            return lazyDelegate.add(a, b);
        }

        @Override
        public Supplier<T> sub(Supplier<T> a, Supplier<T> b) {
            return lazyDelegate.sub(a, b);
        }

        @Override
        public Supplier<T> mul(Supplier<T> a, Supplier<T> b) {
            return lazyDelegate.mul(a, b);
        }

        @Override
        public Supplier<T> div(Supplier<T> a, Supplier<T> b) {
            return lazyDelegate.div(a, b);
        }
    }

    interface Equation<T> extends Expr<T> {
        T root(T a, T b);
        T X();
    }

    interface Solvable<T> {
        Optional<T> tryEval();
        Optional<T> trySolve(T fromResult);

        default Optional<T> apply2(Solvable<T> a, Solvable<T> b, BiFunction<T, T, T> op) {
            return a.tryEval().flatMap(aRes -> b.tryEval().map(bRes -> op.apply(aRes, bRes)));
        }

        default Optional<T> solveEither(Solvable<T> a, Solvable<T> b, Function<T, T> opA, Function<T, T> opB) {
            return a.tryEval().map(opA).flatMap(b::trySolve).or(() -> b.tryEval().map(opB).flatMap(a::trySolve));
        }
    }

    record Solver<T>(Expr<T> eval) implements Equation<Solvable<T>> {

        @Override
        public Solvable<T> lit(long n) {
            return new Solvable<>() {
                @Override
                public Optional<T> tryEval() {
                    return Optional.of(eval.lit(n));
                }

                @Override
                public Optional<T> trySolve(T fromResult) {
                    return Optional.empty();
                }
            };
        }

        @Override
        public Solvable<T> add(Solvable<T> a, Solvable<T> b) {
            return new Solvable<>() {
                @Override
                public Optional<T> tryEval() {
                    return apply2(a, b, eval::add);
                }

                @Override
                public Optional<T> trySolve(T fromResult) {
                    return solveEither(a, b, a1 -> eval.sub(fromResult, a1), b1 -> eval.sub(fromResult, b1));
                }
            };
        }

        @Override
        public Solvable<T> sub(Solvable<T> a, Solvable<T> b) {
            return new Solvable<>() {
                @Override
                public Optional<T> tryEval() {
                    return apply2(a, b, eval::sub);
                }

                @Override
                public Optional<T> trySolve(T fromResult) {
                    return solveEither(a, b, a1 -> eval.sub(a1, fromResult), b1 -> eval.add(b1, fromResult));
                }
            };
        }

        @Override
        public Solvable<T> mul(Solvable<T> a, Solvable<T> b) {
            return new Solvable<>() {
                @Override
                public Optional<T> tryEval() {
                    return apply2(a, b, eval::mul);
                }

                @Override
                public Optional<T> trySolve(T fromResult) {
                    return solveEither(a, b, a1 -> eval.div(fromResult, a1), b1 -> eval.div(fromResult, b1));
                }
            };
        }

        @Override
        public Solvable<T> div(Solvable<T> a, Solvable<T> b) {
            return new Solvable<>() {
                @Override
                public Optional<T> tryEval() {
                    return apply2(a, b, eval::div);
                }

                @Override
                public Optional<T> trySolve(T fromResult) {
                    return solveEither(a, b, a1 -> eval.div(a1, fromResult), b1 -> eval.mul(fromResult, b1));
                }
            };
        }

        @Override
        public Solvable<T> root(Solvable<T> a, Solvable<T> b) {
            return new Solvable<>() {
                @Override
                public Optional<T> tryEval() {
                    // The root expr is a bit special: either a or b must tryEval to a constant,
                    // trySolve the X in the other one and return its value as root's tryEval result.
                    return a.tryEval().flatMap(b::trySolve).or(() -> b.tryEval().flatMap(a::trySolve));
                }

                @Override
                public Optional<T> trySolve(T fromResult) {
                    return Optional.empty();
                }
            };
        }

        @Override
        public Solvable<T> X() {
            return new Solvable<>() {
                @Override
                public Optional<T> tryEval() {
                    return Optional.empty();
                }

                @Override
                public Optional<T> trySolve(T fromResult) {
                    return Optional.of(fromResult);
                }
            };
        }
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

        final var lazyEval = new LazyEval();
        System.out.println(program(lazyEval).get());
        System.out.println(program(new LazyEval2(evalDsl)).get());
        System.out.println(program(new Lazy<>(printer)).get());

        var lookupTable = loadLazyProgram(new LazyEquation<>(new Solver<>(evalDsl)));

        System.out.println(lookupTable.get("root").get().tryEval().get());
    }

    // AoC22 Day 21 problem #2
    private static <T> HashMap<String, Supplier<T>> loadLazyProgram(Equation<Supplier<T>> lazyDsl) {
        final var lookupTable = new HashMap<String, Supplier<T>>();
        final Map<String, BiFunction<Supplier<T>, Supplier<T>, Supplier<T>>> opToExpr = Map.of(
                "+", lazyDsl::add,
                "-", lazyDsl::sub,
                "*", lazyDsl::mul,
                "/", lazyDsl::div);

        new BufferedReader(new InputStreamReader(System.in)).lines()
                .forEach(line -> {
                    var strs = line.split(":");
                    var name = strs[0];
                    var exprStr = strs[1].trim();

                    var tokens = exprStr.split(" ");
                    switch (name) {
                        // Comment out the root and humn branches for solving problem #1
                        case "root" ->
                                lookupTable.put(name, exprProxy(tokens[0], tokens[2], lookupTable, lazyDsl::root));
                        case "humn" ->
                                lookupTable.put(name, lazyDsl.X());
                        default -> {
                            if (Character.isDigit(exprStr.charAt(0))) {
                                lookupTable.put(name, lazyDsl.lit(Long.parseLong(exprStr)));
                            } else {
                                lookupTable.put(name, exprProxy(tokens[0], tokens[2], lookupTable, opToExpr.get(tokens[1])));
                            }
                        }
                    }
                });
        return lookupTable;
    }

    static <T> Supplier<T> exprProxy(String a, String b, Map<String, Supplier<T>> lookup,
                                     BiFunction<Supplier<T>, Supplier<T>, Supplier<T>> builder) {
        // Note the delayed resolving with the lookup table
        return builder.apply(() -> lookup.get(a).get(), () -> lookup.get(b).get());
    }

    interface ArithmeticExpr<T, N> {
        T lit(N n);
        T add(T a, T b);
        T sub(T a, T b);
        T mul(T a, T b);
        T div(T a, T b);
    }
}