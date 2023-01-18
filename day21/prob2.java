import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

class prob2 {
    sealed interface Expr {
        /** Optional.empty() means this expr has an X and can't be evaluated. */
        Optional<Long> tryEval();

        /** Solve the X in this expr. Optional.empty() means this expr has no X. */
        Optional<Long> trySolve(long fromResult);

        default Optional<Long> apply2(Expr a, Expr b, BiFunction<Long, Long, Long> op) {
            return a.tryEval().flatMap(aRes -> b.tryEval().map(bRes -> op.apply(aRes, bRes)));
        }

        default Optional<Long> solveEither(Expr a, Expr b, Function<Long, Long> opA, Function<Long, Long> opB) {
            return a.tryEval().map(opA).flatMap(b::trySolve).or(() -> b.tryEval().map(opB).flatMap(a::trySolve));
        }
    }

    record Constant(long n) implements Expr {
        public Optional<Long> tryEval() {
            return Optional.of(n);
        }

        public Optional<Long> trySolve(long fromResult) {
            throw new IllegalStateException("nothing to trySolve");
        };
    }

    record X() implements Expr {
        public Optional<Long> tryEval() {
            return Optional.empty();
        }

        public Optional<Long> trySolve(long fromResult) {
            return Optional.of(fromResult);
        };
    }

    record Root(Expr a, Expr b) implements Expr {
        public Optional<Long> tryEval() {
            // The root expr is a bit special: either a or b must tryEval to a constant,
            // trySolve the X in the other one and return its value as root's tryEval result.
            return a.tryEval().flatMap(b::trySolve).or(() -> b.tryEval().flatMap(a::trySolve));
        }

        public Optional<Long> trySolve(long fromResult) {
            return Optional.empty();
        };
    }

    record Add(Expr a, Expr b) implements Expr {
        public Optional<Long> tryEval() {
            return apply2(a, b, Long::sum);
        }

        public Optional<Long> trySolve(long fromResult) {
            return solveEither(a, b, a1 -> fromResult - a1, b1 -> fromResult - b1);
        };
    }

    record Sub(Expr a, Expr b) implements Expr {
        public Optional<Long> tryEval() {
            return apply2(a, b, (a1, b1) -> a1 - b1);
        }

        public Optional<Long> trySolve(long fromResult) {
            return solveEither(a, b, a1 -> a1 - fromResult, b1 -> b1 + fromResult);
        };
    }

    record Multiply(Expr a, Expr b) implements Expr {
        public Optional<Long> tryEval() {
            return apply2(a, b, (a1, b1) -> a1 * b1);
        }

        public Optional<Long> trySolve(long fromResult) {
            return solveEither(a, b, a1 -> fromResult / a1, b1 -> fromResult / b1);
        };
    }

    record Divide(Expr a, Expr b) implements Expr {
        public Optional<Long> tryEval() {
            return apply2(a, b, (a1, b1) -> a1 / b1);
        }

        public Optional<Long> trySolve(long fromResult) {
            return solveEither(a, b, a1 -> a1 / fromResult, b1 -> fromResult * b1);
        };
    }

    record BiExprByName(String a, String b, Map<String, Expr> lookup, BiFunction<Expr, Expr, Expr> builder)
            implements Expr {
        public Optional<Long> tryEval() {
            return builder.apply(lookup.get(a), lookup.get(b)).tryEval();
        }

        public Optional<Long> trySolve(long fromResult) {
            return builder.apply(lookup.get(a), lookup.get(b)).trySolve(fromResult);
        };
    }

    public static void main(String[] args) {

        final var lookupTable = new HashMap<String, Expr>();
        final Map<String, BiFunction<Expr, Expr, Expr>> opToExpr = Map.of(
            "+", Add::new,
            "-", Sub::new,
            "*", Multiply::new,
            "/", Divide::new);

        new BufferedReader(new InputStreamReader(System.in)).lines()
                .forEach(line -> {
                    var strs = line.split(":");
                    var name = strs[0];
                    var exprStr = strs[1].trim();
                    var tokens = exprStr.split(" ");

                    switch (name) {
                        case "root" ->
                                lookupTable.put(name, new BiExprByName(tokens[0], tokens[2], lookupTable, Root::new));
                        case "humn" -> lookupTable.put(name, new X());
                        default -> {
                            if (Character.isDigit(exprStr.charAt(0))) {
                                lookupTable.put(name, new Constant(Long.parseLong(exprStr)));
                            } else {
                                lookupTable.put(name,
                                        new BiExprByName(tokens[0], tokens[2], lookupTable, opToExpr.get(tokens[1])));
                            }
                        }
                    }
                });

        System.out.println(lookupTable.get("root").tryEval().get());
    }
}