import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

class prob1 {
    sealed interface Expr {
        long eval();
    }

    record Constant(long n) implements Expr {
        public long eval() {
            return n;
        }
    }

    record Add(Expr a, Expr b) implements Expr {
        public long eval() {
            return a.eval() + b.eval();
        }
    }

    record Sub(Expr a, Expr b) implements Expr {
        public long eval() {
            return a.eval() - b.eval();
        }
    }

    record Multiply(Expr a, Expr b) implements Expr {
        public long eval() {
            return a.eval() * b.eval();
        }
    }

    record Divide(Expr a, Expr b) implements Expr {
        public long eval() {
            return a.eval() / b.eval();
        }
    }

    record BiExprByName(String a, String b, Map<String, Expr> lookup, BiFunction<Expr, Expr, Expr> builder)
            implements Expr {
        public long eval() {
            return builder.apply(lookup.get(a), lookup.get(b)).eval();
        }
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
                    if (Character.isDigit(exprStr.charAt(0))) {
                        lookupTable.put(name, new Constant(Long.parseLong(exprStr)));
                    } else {
                        var tokens = exprStr.split(" ");
                        lookupTable.put(name, new BiExprByName(tokens[0], tokens[2], lookupTable, opToExpr.get(tokens[1])));
                    }
                });

        System.out.println(lookupTable.get("root").eval());
    }
}