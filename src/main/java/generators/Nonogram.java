package generators;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Nonogram {
    private final int size;
    private Data data;

    public static void main(String[] args) {
        new Nonogram(10);
    }

    private Nonogram(int size, String fileName) {
        this.size = size;
        data = new Data(size);
        generate();
        print();
        extractTips();
        printTips();
        writeJSON(fileName);
    }

    private Nonogram(int size) {
        this(size, "nonogram/generated.json");
    }

    private static int generateRandom() {
        double random = Math.random();
        if(random < 0.5)
            return 0;
        return 1;
    }

    private void generate() {
        IntStream.range(0, size).forEach(row ->
            IntStream.range(0, size).forEach(column ->
                data.solution[row][column] = generateRandom()
            )
        );
        Arrays.stream(data.solution)
                .filter(row -> Arrays.stream(row).noneMatch(v -> v == 1))
                .findFirst()
                .ifPresent(x -> generate());
        IntStream.range(0, size)
                .mapToObj(col -> Arrays.stream(data.solution).mapToInt(row -> row[col]).toArray())
                .filter(column -> Arrays.stream(column).noneMatch(v -> v == 1))
                .findFirst()
                .ifPresent(x -> generate());
    }

    private void print() {
        for (int[] row : data.solution) {
            System.out.println(Arrays.toString(row));
        }
    }

    private static List<Integer> extract(int[] structure) {
        List<Integer> result = new ArrayList<>();
        int c = 0;
        while(c < structure.length) {
            if(structure[c] == 1) {
                if(result.isEmpty())
                    result.add(0);
                result.set(result.size()-1, result.get(result.size()-1) + 1);
            }
            else {
                if(!result.isEmpty() && result.get(result.size()-1) > 0)
                    result.add(0);
            }
            c++;
        }
        if(result.get(result.size()-1) == 0)
            return result.subList(0, result.size() - 1);
        return result;
    }

    private void extractTips() {
        data.rows = Arrays.stream(data.solution)
                .map(Nonogram::extract)
                .collect(Collectors.toList());

        data.columns = IntStream.range(0, size)
                .mapToObj(col -> extract(
                                    Arrays.stream(data.solution)
                                    .mapToInt(row -> row[col])
                                    .toArray()
                                )
                )
                .collect(Collectors.toList());
    }

    private void printTips() {
        System.out.println(data.rows);
        System.out.println(data.columns);
    }

    private static JSONArray writeStructure(List<List<Integer>> structure) {
        JSONArray structList = new JSONArray();
        structure.stream().map(valuesList -> {
            JSONArray values = new JSONArray();
            values.addAll(valuesList);
            return values;
        }).map(values -> {
            JSONObject obj = new JSONObject();
            obj.put("values", values);
            return obj;
        }).forEach(structList::add);
        return structList;
    }

    private void writeJSON(String fileName) {
        JSONObject rootObj = new JSONObject();

        rootObj.put("rows", writeStructure(data.rows));
        rootObj.put("columns", writeStructure(data.columns));

        try (FileWriter file = new FileWriter(fileName)) {
            file.write(rootObj.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.print(rootObj);
    }

    private class Data {
        List<List<Integer>> rows;
        List<List<Integer>> columns;
        int[][] solution;

        Data(int size) {
            solution = new int[size][size];
            rows = new ArrayList<>(size);
            columns = new ArrayList<>(size);
        }
    }
}
