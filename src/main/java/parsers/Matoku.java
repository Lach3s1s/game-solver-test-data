package parsers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Matoku extends AreaBased {
    private static final String propertiesFileName = "matoku.properties";

    private static final String intercells = "separator.inter-cells";
    private static final String keyvalues = "separator.key-values";

    public Matoku(String pathName) {
        super(pathName, propertiesFileName);
    }

    public static void main(String[] args) {
        new Matoku("matoku/").run();
    }

    protected List<String> extractCells(List<String> rules) {
        return rules.stream()
                .map(line -> Arrays.stream(line.split(properties.getProperty(keyvalues))[0].split(properties.getProperty(intercells))))
                .flatMap(Function.identity())
                .collect(Collectors.toList());
    }

    protected void processSourceData(List<String> rules, List<String> errors) {
        rules.forEach(rule -> {
            String[] split = rule.split(properties.getProperty(keyvalues));
            String[] operands = split[0].split(properties.getProperty(intercells));
            try {
                Operation operation = Operation.ofSymbol(split[1].substring(0, 1));
                Integer.parseInt(split[1].substring(1)); // for potential exception

                if(operands.length == 1) {
                    if (operation != Operation.EQUAL)
                        throw new RuntimeException("Bad config: " + rule);
                }
            } catch (Exception e) {
                errors.add(e.getMessage());
            }
        });
    }

    protected void extraChecksOnResults(List<String> lines, Integer[][] values, List<String> errors) {
        Arrays.stream(values).forEach(row -> {
            Map<Integer, List<Integer>> collect = Arrays.stream(row).collect(Collectors.groupingBy(Function.identity()));
            IntStream.rangeClosed(1, 5).forEach(value -> {
                if(collect.get(value) == null || collect.get(value).size() > 1)
                    errors.add("Invalid row: missing or too many '"+value+"' in it: "+ Arrays.toString(row));
            });
        });
        IntStream.range(0, values[0].length).mapToObj(colNumber -> values[colNumber]).forEach(column -> {
            Map<Integer, List<Integer>> collect = Arrays.stream(column).collect(Collectors.groupingBy(Function.identity()));
            IntStream.rangeClosed(1, 5).forEach(value -> {
                if(collect.get(value) == null || collect.get(value).size() > 1)
                    errors.add("Invalid column: missing or too many '"+value+"' in it: "+ Arrays.toString(column));
            });
        });
    }

    protected void specificCheckOnAreaFilling(String rule, Integer[][] results, List<String> errors) {
        String[] split = rule.split(properties.getProperty(keyvalues));
        String[] operands = split[0].split(properties.getProperty(intercells));
        Operation operation = Operation.ofSymbol(split[1].substring(0, 1));
        int value = Integer.parseInt(split[1].substring(1));

        Integer foundValue;

        if(operands.length == 1) {
            if (operation != Operation.EQUAL)
                throw new RuntimeException("Bad config: " + rule);
            String cellId = operands[0];
            foundValue = results[findRowNumber.apply(cellId)][findColumnNumber.apply(cellId)];
        }
        else {
            foundValue = Arrays.stream(operands)
                    .map(cellId -> results[findRowNumber.apply(cellId)][findColumnNumber.apply(cellId)])
                    .reduce(operation.base, operation.accumulator);
        }

        if(!foundValue.equals(value))
            errors.add("Invalid area computed: "+rule+" (found="+foundValue+" v. expected="+value+")");
    }

    private enum Operation {
        PLUS("+", 0, Integer::sum), MINUS("-", 0, (one, two) -> Math.abs(one - two)), MULTIPLY("x", 1, (one, two) -> one * two), DIVIDE("/", 1, (one, two) -> Math.max(one, two) / Math.min(one, two)),
        EQUAL("~", 0, (one, two) -> one);

        final String symbol;
        private final Integer base;
        private final BinaryOperator<Integer> accumulator;

        Operation(String symbol, Integer base, BinaryOperator<Integer> accumulator) {
            this.symbol = symbol;
            this.base = base;
            this.accumulator = accumulator;
        }

        static Operation ofSymbol(String symbol) {
            return Arrays.stream(Operation.values())
                    .filter(operation -> operation.symbol.equals(symbol))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Unknown symbol: '"+symbol+"'"));
        }
    }
}
