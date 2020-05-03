package parsers;

import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class Kemaru extends AreaBased {

    public Kemaru(String pathName) {
        super(pathName);
    }

    public static void main(String[] args) {
        new Kemaru("kemaru/").run();
    }

    protected List<String> extractCells(List<String> rules) {
        return rules.stream()
                .map(line -> Arrays.stream(line.split(",")))
                .flatMap(Function.identity())
                .collect(Collectors.toList());
    }

    @Override
    protected UnaryOperator<String> cellIdExtractor() {
        return cellId -> cellId.split("=")[0];
    }

    protected void processSourceData(List<String> rules, List<String> errors) {
        rules.forEach(rule -> {
            String[] items = rule.split(",");
            Arrays.stream(items).map(findValue).filter(Optional::isPresent).map(Optional::get).filter(v -> v < 1 || v > items.length).forEach(v ->
                errors.add("Found a bad prefilled value ("+v+") for block: "+Arrays.toString(items))
            );
        });
    }

    protected void extraChecksOnResults(List<String> lines, Integer[][] values, List<String> errors) {
        // do nothing here
    }

    protected void specificCheckOnAreaFilling(String rule, Integer[][] results, List<String> errors) {
        String[] items = rule.split(",");
        IntSummaryStatistics statistics = Arrays.stream(items).mapToInt(item -> results[findRowNumber.apply(item)][findColumnNumber.apply(item)]).summaryStatistics();
        if(statistics.getCount() != items.length || statistics.getMin() != 1 || statistics.getMax() != items.length)
            errors.add("Wrong filling block "+Arrays.toString(items)+" (check stats="+statistics+")");
    }

    private static final Function<String, Optional<Integer>> findValue = cellId -> {
        String[] split = cellId.split("=");
        if(split.length == 1)
            return Optional.empty();
        else
            return Optional.of(Integer.valueOf(split[1]));
    };


}
