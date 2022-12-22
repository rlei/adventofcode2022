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
        public Optional<Long> eval();

        /**
         * Solve the X in this expr. Throw a RuntimeException if the expr tree doesn't
         * contain an X.
         */
        public long solve(long fromResult);

        default Optional<Long> apply2(Expr a, Expr b, BiFunction<Long, Long, Long> op) {
            return a.eval().flatMap(aRes -> b.eval().map(bRes -> op.apply(aRes, bRes)));
        }

        default long solveEither(Expr a, Expr b, Function<Long, Long> opA, Function<Long, Long> opB) {
            return a.eval().map(aRes -> b.solve(opA.apply(aRes))).orElseGet(() -> a.solve(opB.apply(b.eval().get())));
        }
    }

    record Constant(long n) implements Expr {
        public Optional<Long> eval() {
            return Optional.of(n);
        }

        public long solve(long fromResult) {
            throw new IllegalStateException("nothing to solve");
        };
    }

    record X() implements Expr {
        public Optional<Long> eval() {
            return Optional.empty();
        }

        public long solve(long fromResult) {
            return fromResult;
        };
    }

    record Root(Expr a, Expr b) implements Expr {
        public Optional<Long> eval() {
            // The root expr is a bit special: either a or b must eval to a constant,
            // solve the X in the other one and return its value as root's eval result.
            return a.eval().map(aResult -> b.solve(aResult)).or(() -> Optional.of(a.solve(b.eval().get())));
        }

        public long solve(long fromResult) {
            throw new IllegalStateException("Root doesn't solve this way");
        };
    }

    record Add(Expr a, Expr b) implements Expr {
        public Optional<Long> eval() {
            return apply2(a, b, (a1, b1) -> a1 + b1);
        }

        public long solve(long fromResult) {
            return solveEither(a, b, a1 -> fromResult - a1, b1 -> fromResult - b1);
        };
    }

    record Sub(Expr a, Expr b) implements Expr {
        public Optional<Long> eval() {
            return apply2(a, b, (a1, b1) -> a1 - b1);
        }

        public long solve(long fromResult) {
            return solveEither(a, b, a1 -> a1 - fromResult, b1 -> b1 + fromResult);
        };
    }

    record Multiply(Expr a, Expr b) implements Expr {
        public Optional<Long> eval() {
            return apply2(a, b, (a1, b1) -> a1 * b1);
        }

        public long solve(long fromResult) {
            return solveEither(a, b, a1 -> fromResult / a1, b1 -> fromResult / b1);
        };
    }

    record Divide(Expr a, Expr b) implements Expr {
        public Optional<Long> eval() {
            return apply2(a, b, (a1, b1) -> a1 / b1);
        }

        public long solve(long fromResult) {
            return solveEither(a, b, a1 -> a1 / fromResult, b1 -> fromResult * b1);
        };
    }

    record BiExprByName(String a, String b, Map<String, Expr> lookup, BiFunction<Expr, Expr, Expr> builder)
            implements Expr {
        public Optional<Long> eval() {
            return builder.apply(lookup.get(a), lookup.get(b)).eval();
        }

        public long solve(long fromResult) {
            return builder.apply(lookup.get(a), lookup.get(b)).solve(fromResult);
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
                        case "root":
                            lookupTable.put(name, new BiExprByName(tokens[0], tokens[2], lookupTable, Root::new));
                            break;
                        case "humn":
                            lookupTable.put(name, new X());
                            break;
                        default:
                            if (Character.isDigit(exprStr.charAt(0))) {
                                lookupTable.put(name, new Constant(Long.parseLong(exprStr)));
                            } else {
                                lookupTable.put(name,
                                        new BiExprByName(tokens[0], tokens[2], lookupTable, opToExpr.get(tokens[1])));
                            }
                    }
                });

        System.out.println(lookupTable.get("root").eval().get());
    }
}