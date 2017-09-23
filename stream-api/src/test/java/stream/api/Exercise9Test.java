package stream.api;

import common.test.tool.annotation.Difficult;
import common.test.tool.annotation.Easy;
import common.test.tool.dataset.ClassicOnlineStore;
import common.test.tool.entity.Customer;
import common.test.tool.util.CollectorImpl;
import org.junit.Test;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class Exercise9Test extends ClassicOnlineStore {

    @Easy
    @Test
    public void simplestStringJoin() {
        List<Customer> customerList = this.mall.getCustomerList();
        /**
         * Implement a {@link Collector} which can create a String with comma separated names shown in the assertion.
         * The collector will be used by serial stream.
         */
        Supplier<StringBuilder> supplier = () -> new StringBuilder();
        BiConsumer<StringBuilder, String> accumulator = (sb, s) -> sb.append(s)
                .append(',');
        BinaryOperator<StringBuilder> combiner = (sb1, sb2) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(sb1).append(sb2);
            return sb;
        };
        Function<StringBuilder, String> finisher = sb -> sb.toString()
                .substring(0, sb.length() - 1);

        Collector<String, StringBuilder, String> toCsv =
                new CollectorImpl<>(supplier, accumulator, combiner, finisher, Collections.emptySet());
        String nameAsCsv = customerList.stream().map(Customer::getName).collect(toCsv);
        assertThat(nameAsCsv, is("Joe,Steven,Patrick,Diana,Chris,Kathy,Alice,Andrew,Martin,Amy"));
    }

    @Easy
    @Test
    public void simplestStringJoin2() {
        List<Customer> customerList = this.mall.getCustomerList();

        /**
         * Implement a {@link Collector} which can create a String with comma separated names shown in the assertion.
         * The collector will be used by serial stream.
         */
        Supplier<List<String>> supplier = () -> new ArrayList<>();
        BiConsumer<List<String>, String> accumulator = (list, s) -> list.add(s);
        BinaryOperator<List<String>> combiner = (list1, list2) -> {
            ArrayList<String> list = new ArrayList<>();
            list.addAll(list1);
            list.addAll(list2);
            return list;
        };
        Function<List<String>, String> finisher = (list) -> {
            StringJoiner joiner = new StringJoiner(",");
            list.forEach(s -> joiner.add(s));
            return joiner.toString();
        };

        Collector<String, List<String>, String> toCsv =
                new CollectorImpl<>(supplier, accumulator, combiner, finisher, Collections.emptySet());
        String nameAsCsv = customerList.stream().map(Customer::getName).collect(toCsv);
        assertThat(nameAsCsv, is("Joe,Steven,Patrick,Diana,Chris,Kathy,Alice,Andrew,Martin,Amy"));
    }


    @Easy
    @Test
    public void simplestStringJoin3() {
        List<Customer> customerList = this.mall.getCustomerList();

        /**
         * Implement a {@link Collector} which can create a String with comma separated names shown in the assertion.
         * The collector will be used by serial stream.
         */
        Supplier<StringJoiner> supplier = () -> new StringJoiner(",");
        BiConsumer<StringJoiner, String> accumulator = (joiner, s) -> joiner.add(s);
        BinaryOperator<StringJoiner> combiner = (joiner1, joiner2) -> joiner1.merge(joiner2);
        Function<StringJoiner, String> finisher = joiner -> joiner.toString();

        Collector<String, StringJoiner, String> toCsv =
                new CollectorImpl<>(supplier, accumulator, combiner, finisher, Collections.emptySet());
        String nameAsCsv = customerList.stream().map(Customer::getName).collect(toCsv);
        assertThat(nameAsCsv, is("Joe,Steven,Patrick,Diana,Chris,Kathy,Alice,Andrew,Martin,Amy"));
    }

    @Difficult
    @Test
    public void mapKeyedByItems() {
        List<Customer> customerList = this.mall.getCustomerList();

        /**
         * Implement a {@link Collector} which can create a {@link Map} with keys as item and
         * values as {@link Set} of customers who are wanting to buy that item.
         * The collector will be used by parallel stream.
         */
        Supplier<Map<String, Set<String>>> supplier = () -> new HashMap<>();
        BiConsumer<Map<String, Set<String>>, Customer> accumulator =
                (map, customer) -> customer.getWantToBuy().stream()
                .map(item -> item.getName())
                .forEach(name -> {
                    map.putIfAbsent(name, new HashSet<>());
                    map.get(name).add(customer.getName());
                });
        BinaryOperator<Map<String, Set<String>>> combiner = (map1, map2) -> {
            map2.entrySet().stream()
                    .forEach(e -> map1.merge(e.getKey(), e.getValue(),
                            (set1, set2) -> {
                                Set<String> result = new HashSet<>();
                                result.addAll(set1);
                                result.addAll(set2);
                                return result;
                            }));
            return map1;
        };
        Function<Map<String, Set<String>>, Map<String, Set<String>>> finisher
                = x -> x;

        Collector<Customer, ?, Map<String, Set<String>>> toItemAsKey =
                new CollectorImpl<>(supplier, accumulator, combiner, finisher, EnumSet.of(
                        Collector.Characteristics.CONCURRENT,
                        Collector.Characteristics.IDENTITY_FINISH));
        Map<String, Set<String>> itemMap = customerList.stream().parallel().collect(toItemAsKey);
        assertThat(itemMap.get("plane"), containsInAnyOrder("Chris"));
        assertThat(itemMap.get("onion"), containsInAnyOrder("Patrick", "Amy"));
        assertThat(itemMap.get("ice cream"), containsInAnyOrder("Patrick", "Steven"));
        assertThat(itemMap.get("earphone"), containsInAnyOrder("Steven"));
        assertThat(itemMap.get("plate"), containsInAnyOrder("Joe", "Martin"));
        assertThat(itemMap.get("fork"), containsInAnyOrder("Joe", "Martin"));
        assertThat(itemMap.get("cable"), containsInAnyOrder("Diana", "Steven"));
        assertThat(itemMap.get("desk"), containsInAnyOrder("Alice"));
    }


    @Difficult
    @Test
    public void mapKeyedByItems2() {
        List<Customer> customerList = this.mall.getCustomerList();

        /**
         * Implement a {@link Collector} which can create a {@link Map} with keys as item and
         * values as {@link Set} of customers who are wanting to buy that item.
         * The collector will be used by parallel stream.
         */
        Supplier<MultiMap<String, String>> supplier = () -> new MultiMap<>();

        BiConsumer<MultiMap<String, String>, Customer> accumulator =
                (map, customer) -> customer.getWantToBuy().stream()
                        .map(item -> item.getName())
                        .forEach(name -> map.put(name, customer.getName()));

        BinaryOperator<MultiMap<String, String>> combiner =
                (map1, map2) -> map1.merge(map2);

        Function<MultiMap<String, String>, Map<String, Set<String>>> finisher
                = map -> map.toMap();

        Collector<Customer, MultiMap<String, String>, Map<String, Set<String>>> toItemAsKey =
                new CollectorImpl<>(supplier, accumulator, combiner, finisher, EnumSet.of(
                        Collector.Characteristics.CONCURRENT,
                        Collector.Characteristics.IDENTITY_FINISH));
        Map<String, Set<String>> itemMap = customerList.stream().parallel().collect(toItemAsKey);
        assertThat(itemMap.get("plane"), containsInAnyOrder("Chris"));
        assertThat(itemMap.get("onion"), containsInAnyOrder("Patrick", "Amy"));
        assertThat(itemMap.get("ice cream"), containsInAnyOrder("Patrick", "Steven"));
        assertThat(itemMap.get("earphone"), containsInAnyOrder("Steven"));
        assertThat(itemMap.get("plate"), containsInAnyOrder("Joe", "Martin"));
        assertThat(itemMap.get("fork"), containsInAnyOrder("Joe", "Martin"));
        assertThat(itemMap.get("cable"), containsInAnyOrder("Diana", "Steven"));
        assertThat(itemMap.get("desk"), containsInAnyOrder("Alice"));
    }


    @Difficult
    @Test
    public void bitList2BitString() {
        String bitList = "22-24,9,42-44,11,4,46,14-17,5,2,38-40,33,50,48";

        /**
         * Create a {@link String} of "n"th bit ON.
         * for example
         * "3" will be "001"
         * "1,3,5" will be "10101"
         * "1-3" will be "111"
         * "7,1-3,5" will be "1110101"
         */
        Collector<String, ?, String> toBitString = null;

        String bitString = Arrays.stream(bitList.split(",")).collect(toBitString);
        assertThat(bitString, is("01011000101001111000011100000000100001110111010101")

        );
    }
}

class MultiMap<K, V> {
    private Map<K, Set<V>> map = new HashMap<>();

    void put(K key, V value){
        map.putIfAbsent(key, new HashSet<>());
        map.get(key).add(value);
    }
//
//    void put(K key, Set<V> values){
//        values.forEach(v -> put(key, v));
//    }

    void put(K key, Set<V> values){
        map.putIfAbsent(key, new HashSet<>());
        map.get(key).addAll(values);
    }

    Set<V> get(K key){
        return map.getOrDefault(key, Collections.emptySet());
    }

    MultiMap<K, V> merge(MultiMap<K,V> other){
        other.map.entrySet().forEach(e -> put(e.getKey(), e.getValue()));
        return this;
    }

    public Map<K, Set<V>> toMap() {
        return map;
    }
}