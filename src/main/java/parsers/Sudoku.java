package parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Sudoku {
	public static void main(String[] args) {
		Map<String, BiSupplier<List<Problem>, Integer[][]>> fileNameToData = Arrays.stream(new File("sudoku/").listFiles())
				.map(File::getPath)
				.collect(collectorToMap(Function.identity(), Sudoku::checkFile, LinkedHashMap::new));
		
		System.out.println("\n### Consistency checks ###\n");
		
		fileNameToData.forEach((fileName, data) -> {
			if (!data.getOne().isEmpty()) {
				System.out.println(fileName);
				data.getOne().forEach(problem -> System.err.println("\t" + problem));
			} else
				System.out.println(fileName + " --> OK");
		});
		
		Map<Map.Entry<String, BiSupplier<List<Problem>, Integer[][]>>, Optional<BiSupplier<List<Problem>, Integer[][]>>> resToSrc =
				extractMappedData(
						fileNameToData,
						fileName -> fileName.endsWith("_res.txt"),
						fileName -> fileName.replace("_res.txt", ".txt")
				);
		
		Map<Map.Entry<String, BiSupplier<List<Problem>, Integer[][]>>, Optional<BiSupplier<List<Problem>, Integer[][]>>> srcToRes =
				extractMappedData(
						fileNameToData,
						fileName -> !fileName.endsWith("_res.txt"),
						fileName -> fileName.replace(".txt", "_res.txt")
				);
		
		System.out.println("\n### Cross-files consistency checks ###\n");
		
		resToSrc.forEach((entryDataForResultFile, optionalMatchedDataForSourceFile) ->
				optionalMatchedDataForSourceFile.ifPresentOrElse(matchedDataForSourceFile -> {
					List<BiSupplier<Integer, Integer>> biSuppliers = compareData(entryDataForResultFile.getValue().getTwo(), matchedDataForSourceFile.getTwo());
					if (!biSuppliers.isEmpty()) {
						System.err.println("Differences on provided inputs for: " + entryDataForResultFile.getKey());
						System.err.println("\t" + collectionToString(biSuppliers));
					}
				}, () -> {
					System.err.println("No source file for result file: " + entryDataForResultFile.getKey());
				})
		);
		
		System.out.println("\n### Gap information ###\n");
		srcToRes.entrySet().stream().filter(e -> !e.getValue().isPresent()).forEach(entryDataForSourceFile ->
				System.out.println("No result file for source: "+entryDataForSourceFile.getKey().getKey())
		);
		
	}
	
	private static final Pattern pattern = Pattern.compile("[0-9_]");
	
	private static BiSupplier<List<Problem>, Integer[][]> checkFile(String fileName) {
		List<Problem> problems = new ArrayList<>();
		Integer[][] data = new Integer[9][9];
		
		String lineRead = null;
		try {
			FileInputStream stream = new FileInputStream(fileName);
			InputStreamReader isr = new InputStreamReader(stream);
			BufferedReader reader = new BufferedReader(isr);
			int rowNumber = 0;
			while ((lineRead = reader.readLine()) != null) {
				if (lineRead.length() != 9) {
					Problem problem = new Problem();
					problem.rowNumber = OptionalInt.of(rowNumber);
					problem.description = "Not the right characters count on this row (found=" + lineRead.length() + ")";
					problems.add(problem);
				}
				for (int columnIndex = 0; columnIndex < lineRead.length(); columnIndex++) {
					char c = lineRead.charAt(columnIndex);
					String s = c + "";
					if (pattern.matcher(s).matches())
						data[rowNumber][columnIndex] = "_".equals(s) ? null : Integer.parseInt(s);
					else {
						Problem problem = new Problem();
						problem.rowNumber = OptionalInt.of(rowNumber);
						problem.colNumber = OptionalInt.of(columnIndex);
						problem.description = "Not an acceptable character (found=" + s + ")";
						problems.add(problem);
					}
				}
				rowNumber++;
			}
			
			if (rowNumber != 9) {
				Problem problem = new Problem();
				problem.description = "Not the right rows count (found=" + rowNumber + ")";
				problems.add(problem);
			}
			reader.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Line : " + lineRead);
		}
		
		BiSupplier<List<Problem>, Integer[][]> result = new BiSupplier<>() {
			public List<Problem> getOne() {
				return problems;
			}
			
			public Integer[][] getTwo() {
				return data;
			}
		};
		
		if (!problems.isEmpty())
			return result;
		
		for (int row = 0; row < 9; row++) {
			int finalRow = row;
			
			analyzeStructure(data, problems,
					() -> IntStream.of(finalRow),
					() -> IntStream.range(0, 9),
					() -> "the row"
			);
			
			/*Map<Integer, List<Integer>> collect = Arrays.stream(data[row]).filter(Objects::nonNull).collect(Collectors.groupingBy(Function.identity()));
			collect.entrySet().stream().filter(e -> e.getValue().size() > 1).forEach(e -> {
				Problem problem = new Problem();
				problem.rowNumber = OptionalInt.of(finalRow);
				problem.value = OptionalInt.of(e.getKey());
				problem.description = "Too many of the same value in the row (found = "+e.getValue().size()+")";
				problems.add(problem);
			});*/
		}
		
		for (int column = 0; column < 9; column++) {
			int finalColumn = column;
			analyzeStructure(data, problems,
					() -> IntStream.range(0, 9),
					() -> IntStream.of(finalColumn),
					() -> "the column"
			);
		
			/*Map<Integer, List<Integer>> collect = Arrays.stream(data).map(row -> row[finalColumn]).filter(Objects::nonNull).collect(Collectors.groupingBy(Function.identity()));
			collect.entrySet().stream().filter(e -> e.getValue().size() > 1).forEach(e -> {
				Problem problem = new Problem();
				problem.colNumber = OptionalInt.of(finalColumn);
				problem.value = OptionalInt.of(e.getKey());
				problem.description = "Too many of the same value in the column (found = "+e.getValue().size()+")";
				problems.add(problem);
			});*/
		}
		
		for (int square = 0; square < 9; square++) {
			int finalSquare = square;
			analyzeStructure(data, problems,
					() -> generateStream(3 * (finalSquare / 3)),
					() -> generateStream(3 * (finalSquare % 3)),
					() -> "square"
			);
		}
		
		return result;
	}
	
	private static class Problem {
		OptionalInt rowNumber = OptionalInt.empty();
		OptionalInt colNumber = OptionalInt.empty();
		OptionalInt value = OptionalInt.empty();
		String description;
		
		@Override
		public String toString() {
			return "Problem{" +
					(rowNumber.isPresent() ? "rowNumber=" + (rowNumber.getAsInt() + 1) + ", " : "") +
					(colNumber.isPresent() ? "colNumber=" + (colNumber.getAsInt() + 1) + ", " : "") +
					(value.isPresent() ? "value=" + value.getAsInt() + ", " : "") +
					"description='" + description + '\'' +
					'}';
		}
	}
	
	private interface BiSupplier<T, U> {
		T getOne();
		U getTwo();
	}
	
	private static void analyzeStructure(Integer[][] data, List<Problem> problems,
			Supplier<IntStream> rowGenerator, Supplier<IntStream> columnGenerator,
			Supplier<String> forDescription) {
		
		Map<Integer, List<BiSupplier<Integer, Integer>>> collect = new HashMap<>();
		
		crossStreams(rowGenerator, columnGenerator,
				(row, column) -> {
					Integer value = data[row][column];
					if (value == null)
						return;
					List<BiSupplier<Integer, Integer>> biConsumers = collect.computeIfAbsent(value, x -> new ArrayList<>());
					biConsumers.add(new BiSupplier<>() {
						public Integer getOne() {
							return row;
						}
						
						public Integer getTwo() {
							return column;
						}
					});
				}
		);
		
		collect.entrySet().stream().filter(e -> e.getValue().size() > 1).forEach(e -> {
			Problem problem = new Problem();
			problem.rowNumber = OptionalInt.of(e.getValue().get(0).getOne());
			problem.colNumber = OptionalInt.of(e.getValue().get(0).getTwo());
			problem.value = OptionalInt.of(e.getKey());
			problem.description = "Too many of the same value in " + forDescription.get() + " (found = " + e.getValue().size() + ") -- " + collectionToString(e.getValue());
			problems.add(problem);
		});
	}
	
	private static String collectionToString(Collection<BiSupplier<Integer, Integer>> collection) {
		return collection
				.stream()
				.map(v -> "[" + (v.getOne() + 1) + "," + (v.getTwo() + 1) + "]")
				.collect(Collectors.joining(", "));
	}
	
	private static IntStream generateStream(int initial) {
		return IntStream.range(initial, initial + 3); // 3 values per dimension in square
	}
	
	private static void crossStreams(Supplier<IntStream> stream1, Supplier<IntStream> stream2, BiConsumer<Integer, Integer> biConsumer) {
		stream1.get().forEach(first -> stream2.get().forEach(second -> biConsumer.accept(first, second)));
	}
	
	private static <R> List<R> crossStreamsWithResult(Supplier<IntStream> stream1, Supplier<IntStream> stream2, BiFunction<Integer, Integer, Optional<R>> biFunction) {
		return stream1.get()
				.mapToObj(first ->
						stream2.get()
								.mapToObj(second -> biFunction.apply(first, second))
								.filter(Optional::isPresent)
								.map(Optional::get)
								.collect(Collectors.toList())
				)
				.flatMap(List::stream)
				.collect(Collectors.toList());
	}
	
	private static List<BiSupplier<Integer, Integer>> compareData(Integer[][] resData, Integer[][] srcData) {
		return
			crossStreamsWithResult(
				() -> IntStream.range(0, 9),
				() -> IntStream.range(0, 9),
				(row, column) -> {
					if (srcData[row][column] != null) {
						if (!resData[row][column].equals(srcData[row][column])) {
							return Optional.of(
									new BiSupplier<Integer, Integer>() {
										public Integer getOne() {
											return row;
										}
										
										public Integer getTwo() {
											return column;
										}
									}
							);
						}
					}
					return Optional.<BiSupplier<Integer, Integer>>empty();
				});
	}
	
	private static <T, K, U, M extends Map<K, U>>	Collector<T, ?, M> collectorToMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper, Supplier<M> mapFactory) {
		return Collectors.toMap(keyMapper, valueMapper, (x, y) -> x, mapFactory);
	}
	
	
	private static <K, V> Map<Map.Entry<K, V>, Optional<V>> extractMappedData(Map<K, V> inputMap, Predicate<K> keeper, Function<K, K> replacer) {
		return inputMap
				.entrySet()
				.stream()
				.filter(e -> keeper.test(e.getKey()))
				.collect(
						collectorToMap(
								Function.identity(),
								entry -> Optional.ofNullable(inputMap.get(replacer.apply(entry.getKey()))),
								LinkedHashMap::new)
				);
	}
}
