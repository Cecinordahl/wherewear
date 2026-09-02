package com.wherewear.backend.catalog;

import com.wherewear.backend.model.LocationType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.List.of;

/**
 * Fixed catalog of categories per location type, plus starter seed items for
 * each category's template. This is deliberately hardcoded rather than
 * stored in Firestore: the category set per location type is defined by the
 * project spec and isn't meant to be user-editable, only the item lists
 * within each category are (see CategoryTemplate).
 */
public final class CategoryCatalog {

    private CategoryCatalog() {
    }

    private static final Map<LocationType, List<String>> CATEGORIES = new LinkedHashMap<>();
    private static final Map<LocationType, Map<String, List<String>>> SEED_ITEMS = new LinkedHashMap<>();

    static {
        CATEGORIES.put(LocationType.FLIGHT, of(
                "Håndbagasje", "Toalettsaker", "Undertøy", "Trening",
                "Golf/Tennis", "Hverdagsklær", "Middagsantrekk", "Div/Tech"
        ));

        CATEGORIES.put(LocationType.CABIN, of(
                "Toalettsaker", "Undertøy", "Div", "Tech", "Tur",
                "Hverdagsklær", "Middagsantrekk"
        ));

        Map<String, List<String>> flightSeed = new LinkedHashMap<>();
        flightSeed.put("Håndbagasje", of("Pass", "Lommebok", "Nøkler", "Hodetelefoner", "Bok/nettbrett"));
        flightSeed.put("Toalettsaker", of(
                "Tannbørste", "Tannpasta", "Hudkrem", "Sjampo/balsam", "Hårbørste",
                "Hårstrikk", "Deodorant", "Parfyme", "Paracet"
        ));
        flightSeed.put("Undertøy", of("Truser x5", "Sokker x5", "Sports-BH", "Vanlig BH", "Tynne ullsokker"));
        flightSeed.put("Trening", of("Treningsbukse", "Treningstopp", "Treningssko", "Sports-BH"));
        flightSeed.put("Golf/Tennis", of("Golfsko", "Golfhansker", "Tennisbukse/skjørt"));
        flightSeed.put("Hverdagsklær", of("Bukser x2", "Topper x3", "Genser", "Jakke"));
        flightSeed.put("Middagsantrekk", of("Kjole/skjorte", "Fine sko", "Smykker"));
        flightSeed.put("Div/Tech", of("Telefonlader", "PC-lader", "Klokkelader", "Powerbank"));
        SEED_ITEMS.put(LocationType.FLIGHT, flightSeed);

        Map<String, List<String>> cabinSeed = new LinkedHashMap<>();
        cabinSeed.put("Toalettsaker", of(
                "Tannbørste", "Tannpasta", "Hudkrem", "Sjampo/balsam", "Hårbørste",
                "Hårstrikk", "Deodorant", "Parfyme", "Paracet"
        ));
        cabinSeed.put("Undertøy", of("Truser", "Sports-BH", "Vanlig BH", "Tynne ullsokker"));
        cabinSeed.put("Div", of(
                "Stillongs x2", "Ullgenser x2", "Ullsokker", "Solbriller", "Hårbånd", "Lue", "Hals"
        ));
        cabinSeed.put("Tech", of("Telefonlader", "PC-lader", "Klokkelader", "Powerbank"));
        cabinSeed.put("Tur", of(
                "Tur bukse", "Tynn boblejakke", "Tur shorts",
                "Boblebukse", "Boblejakke", "Skalljakke", "Slalåm sokker", "Fleece"
        ));
        cabinSeed.put("Hverdagsklær", of("Bukser x2", "Topper x3", "Genser"));
        cabinSeed.put("Middagsantrekk", of("Fin genser", "Fine bukser"));
        SEED_ITEMS.put(LocationType.CABIN, cabinSeed);
    }

    public static List<String> categoriesFor(LocationType type) {
        return CATEGORIES.get(type);
    }

    public static List<String> seedItemsFor(LocationType type, String category) {
        return SEED_ITEMS.getOrDefault(type, Map.of()).getOrDefault(category, List.of());
    }
}
