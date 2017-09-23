package stream.api;

import common.test.tool.annotation.Difficult;
import common.test.tool.dataset.ClassicOnlineStore;
import common.test.tool.entity.Customer;
import common.test.tool.entity.Item;
import common.test.tool.entity.Shop;

import org.junit.Test;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

public class Exercise8Test extends ClassicOnlineStore {

    @Difficult @Test
    public void itemsNotOnSale() {
        Stream<Customer> customerStream = this.mall.getCustomerList().stream();
        Stream<Shop> shopStream = this.mall.getShopList().stream();

        /**
         * Create a set of item names that are in {@link Customer.wantToBuy} but not on sale in any shop.
         */
        // co jest do kupienia
        List<String> itemListOnSale = shopStream
                .flatMap(shop -> shop.getItemList().stream())
                .map(item -> item.getName())
                .collect(Collectors.toList());

        // co ludzie chcą kupić
        Set<String> itemWantedByCustomers = customerStream
                .flatMap(customer -> customer.getWantToBuy().stream())
                .map(item -> item.getName())
                .collect(Collectors.toSet());
        // różnica tych dwóch zbiorów
        Set<String> itemSetNotOnSale = new HashSet<>(itemWantedByCustomers);

        itemSetNotOnSale.removeAll(itemListOnSale);

        assertThat(itemSetNotOnSale, hasSize(3));
        assertThat(itemSetNotOnSale, hasItems("bag", "pants", "coat"));
    }

    @Difficult @Test
    public void havingEnoughMoney() {
        Stream<Customer> customerStream = this.mall.getCustomerList().stream();
        Stream<Shop> shopStream = this.mall.getShopList().stream();

        /**
         * Create a customer's name list including who are having enough money to buy all items they want which is on sale.
         * Items that are not on sale can be counted as 0 money cost.
         * If there is several same items with different prices, customer can choose the cheapest one.
         */

        // wersja nr 1 - grupujemy ale bez agregacji
        // zostają wszystkie elementy - cała lista
        // a nastepnie sami musimy z tej listy wybrać najtańszy
        // (niekoniecznie najtańszy - wystarczy najniższą cenę)
        // nie mogę tutaj użyć shopStream, bo każdy strumień
        // można użyć tylko RAZ - dane się przelewają, strumień wysycha
        // danych nie ma i leci wyjątek :(
        Map<String, List<Item>> listMap = this.mall.getShopList().stream()
                .flatMap(shop -> shop.getItemList().stream())
                .collect(Collectors.groupingBy(
                        item -> item.getName(),
                        Collectors.toList())
                );
        Map<String, Integer> nameToPriceMapFromList = new HashMap<>();
        for (Map.Entry<String, List<Item>> entry : listMap.entrySet()) {
            List<Item> itemList = entry.getValue();

            OptionalInt min = itemList.stream()
                    .mapToInt(item -> item.getPrice()).min();

            nameToPriceMapFromList.put(entry.getKey(),min.orElse(0));
        }

        // wersja 2 - tutaj grupujemy i robimy agregację
        // wybierając najtańszy przedmiot
        Map<String, Optional<Item>> map = shopStream
                .flatMap(shop -> shop.getItemList().stream())
                .collect(Collectors.groupingBy(
                        item -> item.getName(),
                        Collectors.minBy(Comparator
                                .comparingInt(Item::getPrice))
                ));

        Map<String, Integer> nameToPriceMap = new HashMap<>();
        for (Map.Entry<String, Optional<Item>> entry : map.entrySet()) {
            Optional<Item> itemOptional = entry.getValue();
            Optional<Integer> priceOptional = itemOptional
                    .map(item -> item.getPrice());

            nameToPriceMap.put(entry.getKey(),
                    priceOptional.orElse(0));
        }

        Predicate<Customer> havingEnoughMoney = customer
                -> customer.getWantToBuy().stream()
                .mapToInt(item ->
                        nameToPriceMap.getOrDefault(
                                item.getName(), 0))
                .sum() <= customer.getBudget();
        List<String> customerNameList = customerStream
                .filter(havingEnoughMoney)
                .map(customer -> customer.getName())
                .collect(Collectors.toList());

        // przed Java8
        Integer jajkaPrice = nameToPriceMap.get("jajka");
        if(jajkaPrice == null){
            jajkaPrice = 0;
        }

        // java8
        Integer jajkaPrice2 = nameToPriceMap.getOrDefault("jajka",0);

        assertThat(customerNameList, hasSize(7));
        assertThat(customerNameList, hasItems("Joe", "Patrick", "Chris", "Kathy", "Alice", "Andrew", "Amy"));
    }
}
