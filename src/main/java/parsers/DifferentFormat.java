package parsers;

import java.io.*;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.IntStream;

public abstract class DifferentFormat<Source> extends CommonChecker implements Runnable {

    private final String pathName;

    DifferentFormat(String pathName) {
        this.pathName = pathName;
    }

    public void run() {
        Map<String, BiSupplier<List<String>, Source>> fileNameToData = Arrays.stream(new File(pathName).listFiles())
                .map(File::getPath)
                .filter(fileName -> !fileName.endsWith("_res.txt"))
                .collect(collectorToMap(Function.identity(), this::checkSourceFile, LinkedHashMap::new));

        Map<String, BiSupplier<List<String>, Integer[][]>> fileNameToDataRes = Arrays.stream(new File(pathName).listFiles())
                .map(File::getPath)
                .filter(fileName -> fileName.endsWith("_res.txt"))
                .collect(collectorToMap(Function.identity(), this::checkResultFile, LinkedHashMap::new));

        System.out.println("\n### Consistency checks ###\n");

        fileNameToData.forEach((fileName, data) -> {
            if (!data.getOne().isEmpty()) {
                System.err.println(fileName);
                data.getOne().forEach(problem -> System.err.println("\t" + problem));
            } else
                System.out.println(fileName + " --> OK");
        });

        Map<Map.Entry<String, BiSupplier<List<String>, Source>>, Optional<BiSupplier<List<String>, Integer[][]>>> srcToRes =
                extractMappedData(
                        fileNameToData,
                        fileNameToDataRes,
                        matcherSrcToRes()
                );

        Map<Map.Entry<String, BiSupplier<List<String>, Integer[][]>>, Optional<BiSupplier<List<String>, Source>>> resToSrc =
                extractMappedData(
                        fileNameToDataRes,
                        fileNameToData,
                        matcherResToSrc()
                );

        System.out.println("\n### Cross-files consistency checks ###\n");

        srcToRes.forEach((entryDataForSourceFile, optionalMatchedDataForResultFile) ->
                optionalMatchedDataForResultFile.ifPresentOrElse(matchedDataForResultFile -> {
                    List<String> errorsFound = compareData(entryDataForSourceFile.getValue().getTwo(), matchedDataForResultFile.getTwo());
                    if (!errorsFound.isEmpty()) {
                        System.err.println("Issues found when comparing source and result files: " + entryDataForSourceFile.getKey());
                        errorsFound.forEach(problem -> System.err.println("\t" + problem));
                    }
                }, () -> {
                    System.err.println("No result file for source file: " + entryDataForSourceFile.getKey());
                })
        );

        System.out.println("\n### Gap information ###\n");
        resToSrc.entrySet().stream().filter(e -> !e.getValue().isPresent()).forEach(entryDataForResultFile ->
                System.out.println("No source file for result: " + entryDataForResultFile.getKey().getKey())
        );
    }

    protected abstract BiSupplier<List<String>, Source> checkSourceFile(String fileName);

    /**
     * Check errors in result files and return the corresponding data
     * @param fileName input file name
     * @return composite: the list of errors found and the matrix data
     */
    private BiSupplier<List<String>, Integer[][]> checkResultFile(String fileName) {
        List<String> errors = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        String lineRead = null;
        try {
            FileInputStream stream = new FileInputStream(fileName);
            InputStreamReader isr = new InputStreamReader(stream);
            BufferedReader reader = new BufferedReader(isr);
            while ((lineRead = reader.readLine()) != null) {
                lines.add(lineRead);
            }

            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Line : " + lineRead);
        }

        IntSummaryStatistics statistics = lines.stream().mapToInt(String::length).summaryStatistics();
        if (statistics.getMin() != statistics.getMax())
            errors.add("Not all the rows have the same length: min=" + statistics.getMin() + ", max=" + statistics.getMax());


        Integer[][] values = new Integer[lines.size()][statistics.getMin()];
        crossStreams(
                () -> IntStream.range(0, lines.size()),
                () -> IntStream.range(0, statistics.getMin()),
                (row, col) -> values[row][col] = Integer.valueOf(String.valueOf(lines.get(row).charAt(col)))
        );

        extraChecksOnResults(lines, values, errors);

        return buildBiSupplier(errors, values);
    }

    protected abstract void extraChecksOnResults(List<String> lines, Integer[][] values, List<String> errors);

    // -4 because .txt
    // -8 because _res.txt
    protected BiPredicate<String, String> matcherSrcToRes() {
        return (src, res) -> subString(src, -4).equals(subString(res, -8));
    }

    protected BiPredicate<String, String> matcherResToSrc() {
        return (res, src) -> subString(res, -8).equals(subString(src, -4));
    }

    protected abstract List<String> compareData(Source source, Integer[][] results);

}
