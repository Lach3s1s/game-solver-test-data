package parsers;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

public abstract class AreaBased extends DifferentFormat<List<String>> {

    AreaBased(String pathName) {
        super(pathName);
    }

    /**
     * Check errors in input files
     *
     * @param fileName the name of the file to check
     * @return composite: list of errors found and data: list of rules read
     */
    public BiSupplier<List<String>, List<String>> checkSourceFile(String fileName) {
        List<String> errors = new ArrayList<>();

        List<String> rules = new ArrayList<>();
        String lineRead = null;
        try {
            FileInputStream stream = new FileInputStream(fileName);
            InputStreamReader isr = new InputStreamReader(stream);
            BufferedReader reader = new BufferedReader(isr);
            while ((lineRead = reader.readLine()) != null) {
                rules.add(lineRead);
            }

            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Line : " + lineRead);
        }

        List<String> cells = extractCells(rules);

        Integer maxRowNumber = cells.stream().map(findRowNumber).max(Integer::compareTo).orElseThrow();
        Integer maxColNumber = cells.stream().map(findColumnNumber).max(Integer::compareTo).orElseThrow();

        int[][] counts = new int[maxRowNumber + 1][maxColNumber + 1];
        crossStreams(
                () -> IntStream.rangeClosed(0, maxRowNumber),
                () -> IntStream.rangeClosed(0, maxColNumber),
                (row, col) -> counts[row][col] = 0
        );

        cells.forEach(item -> counts[findRowNumber.apply(item)][findColumnNumber.apply(item)]++ );

        crossStreams(() -> IntStream.rangeClosed(0, maxRowNumber), () -> IntStream.rangeClosed(0, maxColNumber), (row, col) -> {
            if (counts[row][col] > 1)
                errors.add("Found the same cell several times (" + counts[row][col] + "): " + LineName.byIndex(row + 1) + (col + 1));
            else if (counts[row][col] == 0)
                errors.add("Didn't find the cell: " + LineName.byIndex(row + 1) + (col + 1));
        });

        processSourceData(rules, errors);

        return buildBiSupplier(errors, rules);
    }

    protected UnaryOperator<String> cellIdExtractor() {
        return UnaryOperator.identity();
    }

    protected abstract List<String> extractCells(List<String> rules);

    private static final Function<String, Integer> extractRowNumber = cellId -> LineName.valueOf(cellId.substring(0, 1)).index - 1;
    private static final Function<String, Integer> extractColumnNumber = cellId -> Integer.valueOf(cellId.substring(1)) - 1;

    final Function<String, Integer> findRowNumber = cellIdExtractor().andThen(extractRowNumber);
    final Function<String, Integer> findColumnNumber = cellIdExtractor().andThen(extractColumnNumber);

    protected abstract void processSourceData(List<String> rules, List<String> errors);

    public List<String> compareData(List<String> rules, Integer[][] results) {
        List<String> errors = new ArrayList<>();
        rules.forEach(rule -> specificCheckOnAreaFilling(rule, results, errors));
        return errors;
    }

    protected abstract void specificCheckOnAreaFilling(String rule, Integer[][] results, List<String> errors);

    protected enum LineName {
        A(1), B(2), C(3), D(4), E(5), F(6);

        private final int index;

        LineName(int index) {
            this.index = index;
        }

        static LineName byIndex(int index) {
            return Arrays.stream(values()).filter(v -> v.index == index).findFirst().orElseThrow();
        }
    }
}
