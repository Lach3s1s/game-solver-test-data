package parsers;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Nonogram extends DifferentFormat<CommonChecker.BiSupplier<List<List<Integer>>, List<List<Integer>>>> {

    private Nonogram(String pathName) {
        super(pathName);
    }

    public static void main(String[] args) {
        new Nonogram("nonogram/").run();
    }

    private static List<List<Integer>> parseArray(JSONArray jsonArray) {
        return Arrays.stream(jsonArray.toArray())
                .map(o -> (JSONObject) o)
                .map(jsonObj -> (JSONArray) jsonObj.get("values"))
                .map(ArrayList::toArray)
                .map(values ->
                        Arrays.stream(values)
                                .map(o -> ((Long) o).intValue())
                                .collect(Collectors.toList())
                )
                .collect(Collectors.toList());
    }

    protected BiSupplier<List<String>, BiSupplier<List<List<Integer>>, List<List<Integer>>>> checkSourceFile(String fileName) {
        List<String> errors = new ArrayList<>();

        JSONParser jsonParser = new JSONParser();
        List<List<Integer>> rows = null;
        List<List<Integer>> columns = null;

        try (FileReader reader = new FileReader(fileName)) {
            JSONObject rootJSONObj = (JSONObject) jsonParser.parse(reader);
            rows = parseArray((JSONArray) rootJSONObj.get("rows"));
            columns = parseArray((JSONArray) rootJSONObj.get("columns"));

            if (rows.size() != columns.size())
                errors.add("Bad JSON content: must contain as many rows (got " + rows.size() + ") as columns (got " + columns.size() + ")");
            else {
                Integer sumValuesRows = rows.stream().flatMap(Collection::stream).reduce(Integer::sum).orElseThrow();
                Integer sumValuesColumns = columns.stream().flatMap(Collection::stream).reduce(Integer::sum).orElseThrow();
                if (!Objects.equals(sumValuesRows, sumValuesColumns))
                    errors.add("Different global sum between rows (" + sumValuesRows + ") and columns (" + sumValuesColumns + ")");

                analyzeStructure(rows, "row", errors);
                analyzeStructure(columns, "column", errors);
            }

        } catch (IOException | ParseException e) {
            errors.add("FATAL ERROR with file: " + fileName + " => " + e.getMessage());
            //throw new RuntimeException(e);
        }

        return buildBiSupplier(errors, rows == null ? null : buildBiSupplier(rows, columns));
    }

    private static void analyzeStructure(List<List<Integer>> structure, String name, List<String> errors) {
        int size = structure.size();
        structure.forEach(struct -> {
            int countOnesAndZeros = struct.stream().reduce(Integer::sum).orElseThrow() + (struct.size() - 1);
            if (countOnesAndZeros > size)
                errors.add("Too many values (" + countOnesAndZeros + " > " + size + " in " + name + ": " + struct);
            struct.stream().filter(v -> v > size).forEach(v -> errors.add("A too big value (" + v + ") in " + name + ": " + struct));
        });
    }

    protected void extraChecksOnResults(List<String> lines, Integer[][] values, List<String> errors) {
        Arrays.stream(values).flatMap(Stream::of).filter(v -> !(v == 0 || v == 1)).forEach(v -> errors.add("Found bad value in result file: " + v));
    }

    // -5 because .json
    // -8 because _res.txt
    protected BiPredicate<String, String> matcherSrcToRes() {
        return (src, res) -> subString(src, -5).equals(subString(res, -8));
    }

    protected BiPredicate<String, String> matcherResToSrc() {
        return (res, src) -> subString(res, -8).equals(subString(src, -5));
    }

    protected List<String> compareData(BiSupplier<List<List<Integer>>, List<List<Integer>>> rowsAndColumnsSupplier, Integer[][] results) {
        List<String> errors = new ArrayList<>();
        List<List<Integer>> rows = rowsAndColumnsSupplier.getOne();
        List<List<Integer>> columns = rowsAndColumnsSupplier.getTwo();
        IntStream.range(0, rows.size()).forEach(rowId -> checkConstraint(errors, rows.get(rowId), results[rowId], "in row #"+(rowId+1)));
        IntStream.range(0, columns.size()).forEach(columnId -> checkConstraint(errors, columns.get(columnId), Arrays.stream(results).map(row -> row[columnId]).toArray(Integer[]::new), "in column #"+(columnId+1)));
        return errors;
    }

    private static void checkConstraint(List<String> errors, List<Integer> constraints, Integer[] data, String forLog) {
        int[] sums = new int[data.length];
        int counter = 0;
        for (Integer datum : data) {
            if(datum == 0) {
                if(sums[counter] > 0)
                    counter++;
            }
            else { // 1
                sums[counter]++;
            }
        }
        int[] collect = Arrays.stream(sums).filter(v -> v > 0).toArray();
        if(constraints.size() != collect.length)
            errors.add("Different number of constraints "+forLog+": expected="+constraints.size()+", found="+collect.length);

        for (int i = 0; i < constraints.size(); i++) {
            if(constraints.get(i) != collect[i])
                errors.add("Different value for constraint #"+(i+1)+" "+forLog+": expected="+constraints.get(i)+", found="+collect[i]);
        }
    }


}
