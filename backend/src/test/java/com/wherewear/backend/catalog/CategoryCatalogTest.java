package com.wherewear.backend.catalog;

import com.wherewear.backend.model.LocationType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryCatalogTest {

    @Test
    void everyCategoryHasSeedItems() {
        for (LocationType type : LocationType.values()) {
            for (String category : CategoryCatalog.categoriesFor(type)) {
                assertThat(CategoryCatalog.seedItemsFor(type, category))
                        .as("seed items for %s / %s", type, category)
                        .isNotEmpty();
            }
        }
    }

    @Test
    void flightAndCabinCategoriesMatchSpec() {
        assertThat(CategoryCatalog.categoriesFor(LocationType.FLIGHT)).containsExactly(
                "Håndbagasje", "Toalettsaker", "Undertøy", "Trening",
                "Golf/Tennis", "Hverdagsklær", "Middagsantrekk", "Div/Tech"
        );
        assertThat(CategoryCatalog.categoriesFor(LocationType.CABIN)).containsExactly(
                "Toalettsaker", "Undertøy", "Div", "Tech", "Tur",
                "Hverdagsklær", "Middagsantrekk"
        );
    }
}
