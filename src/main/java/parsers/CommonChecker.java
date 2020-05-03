package parsers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class CommonChecker {

    interface BiSupplier<T, U> {
        T getOne();
        U getTwo();
    }

    static <T, U> BiSupplier<T, U> buildBiSupplier(T one, U two) {
        return new BiSupplier<>() {
            public T getOne() {
                return one;
            }

            public U getTwo() {
                return two;
            }
        };
    }

    static <T, K, U, M extends Map<K, U>> Collector<T, ?, M> collectorToMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper, Supplier<M> mapFactory) {
        return Collectors.toMap(keyMapper, valueMapper, (x, y) -> x, mapFactory);
    }

    static void crossStreams(Supplier<IntStream> stream1, Supplier<IntStream> stream2, BiConsumer<Integer, Integer> biConsumer) {
        stream1.get().forEach(first -> stream2.get().forEach(second -> biConsumer.accept(first, second)));
    }

    static <K, V> Map<Map.Entry<K, V>, Optional<V>> extractMappedData(Map<K, V> inputMap, Predicate<K> keeper, Function<K, K> replacer) {
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

    static <K, V1, V2> Map<Map.Entry<K, V1>, Optional<V2>> extractMappedData(Map<K, V1> srcMap, Map<K, V2> resMap, BiPredicate<K, K> matcher) {
        return srcMap
                .entrySet()
                .stream()
                .collect(
                        collectorToMap(
                                Function.identity(),
                                entry -> resMap.entrySet().stream().filter(e -> matcher.test(entry.getKey(), e.getKey())).findFirst().map(Map.Entry::getValue),
                                LinkedHashMap::new)
                );
    }

    static String subString(String input, int dropFromTheEnd) {
        if(dropFromTheEnd > 0)
            throw new IllegalArgumentException("Second argument 'dropFromTheEnd' expects a negative value");
        return input.substring(0, input.length() + dropFromTheEnd);
    }


    static <R> List<R> crossStreamsWithResult(Supplier<IntStream> stream1, Supplier<IntStream> stream2, BiFunction<Integer, Integer, Optional<R>> biFunction) {
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

}
