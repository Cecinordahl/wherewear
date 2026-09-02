package com.wherewear.backend.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ItemNameMatcherTest {

    @Test
    void matchesCaseInsensitiveSubstring() {
        assertThat(ItemNameMatcher.matches("Truser x5", "truser")).isTrue();
        assertThat(ItemNameMatcher.matches("Ullsokker", "Blå ullsokker fra Fjellet")).isTrue();
    }

    @Test
    void doesNotMatchUnrelatedNames() {
        assertThat(ItemNameMatcher.matches("Powerbank", "Sjampo")).isFalse();
    }

    @Test
    void blankInventoryNameNeverMatches() {
        assertThat(ItemNameMatcher.matches("Powerbank", "")).isFalse();
        assertThat(ItemNameMatcher.matches("", "Powerbank")).isFalse();
    }
}
