package com.open.spring.mvc.donation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DonationConstants}.
 *
 * <p>Verifies that all constant sets contain expected values and that the
 * {@code Set} data structure provides correct membership checks.</p>
 *
 * @author Ahaan
 */
class DonationConstantsTest {

    @Test
    @DisplayName("ALLOWED_CATEGORIES contains all 12 food categories")
    void testAllowedCategories() {
        assertEquals(12, DonationConstants.ALLOWED_CATEGORIES.size());
        assertTrue(DonationConstants.ALLOWED_CATEGORIES.contains("canned"));
        assertTrue(DonationConstants.ALLOWED_CATEGORIES.contains("fresh-produce"));
        assertTrue(DonationConstants.ALLOWED_CATEGORIES.contains("dairy"));
        assertTrue(DonationConstants.ALLOWED_CATEGORIES.contains("bakery"));
        assertFalse(DonationConstants.ALLOWED_CATEGORIES.contains("invalid-category"));
    }

    @Test
    @DisplayName("ALLOWED_UNITS contains expected units")
    void testAllowedUnits() {
        assertTrue(DonationConstants.ALLOWED_UNITS.contains("cans"));
        assertTrue(DonationConstants.ALLOWED_UNITS.contains("lbs"));
        assertTrue(DonationConstants.ALLOWED_UNITS.contains("items"));
        assertFalse(DonationConstants.ALLOWED_UNITS.contains("megabytes"));
    }

    @Test
    @DisplayName("ALLOWED_STORAGE has exactly 4 storage types")
    void testAllowedStorage() {
        assertEquals(4, DonationConstants.ALLOWED_STORAGE.size());
        assertTrue(DonationConstants.ALLOWED_STORAGE.contains("room-temp"));
        assertTrue(DonationConstants.ALLOWED_STORAGE.contains("refrigerated"));
        assertTrue(DonationConstants.ALLOWED_STORAGE.contains("frozen"));
        assertTrue(DonationConstants.ALLOWED_STORAGE.contains("cool-dry"));
    }

    @Test
    @DisplayName("ALLOWED_ALLERGENS validates correctly")
    void testAllowedAllergens() {
        assertTrue(DonationConstants.ALLOWED_ALLERGENS.contains("gluten"));
        assertTrue(DonationConstants.ALLOWED_ALLERGENS.contains("dairy"));
        assertTrue(DonationConstants.ALLOWED_ALLERGENS.contains("none"));
        assertFalse(DonationConstants.ALLOWED_ALLERGENS.contains("platinum"));
    }

    @Test
    @DisplayName("ALLOWED_STATUSES covers full lifecycle")
    void testAllowedStatuses() {
        assertTrue(DonationConstants.ALLOWED_STATUSES.contains("active"));
        assertTrue(DonationConstants.ALLOWED_STATUSES.contains("accepted"));
        assertTrue(DonationConstants.ALLOWED_STATUSES.contains("delivered"));
        assertTrue(DonationConstants.ALLOWED_STATUSES.contains("expired"));
        assertTrue(DonationConstants.ALLOWED_STATUSES.contains("cancelled"));
    }

    @Test
    @DisplayName("CATEGORY_EMOJI maps categories to emojis")
    void testCategoryEmoji() {
        assertEquals("🥫", DonationConstants.CATEGORY_EMOJI.get("canned"));
        assertEquals("🍞", DonationConstants.CATEGORY_EMOJI.get("bakery"));
        assertEquals("📦", DonationConstants.CATEGORY_EMOJI.get("other"));
        assertNull(DonationConstants.CATEGORY_EMOJI.get("invalid"));
    }

    @Test
    @DisplayName("Constant sets are unmodifiable")
    void testUnmodifiable() {
        assertThrows(UnsupportedOperationException.class,
            () -> DonationConstants.ALLOWED_CATEGORIES.add("hack"));
        assertThrows(UnsupportedOperationException.class,
            () -> DonationConstants.ALLOWED_UNITS.remove("lbs"));
    }
}
