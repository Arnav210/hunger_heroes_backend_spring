package com.open.spring.mvc.donation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Donation} entity.
 *
 * <p>Tests cover entity construction, expiry logic, allergen/dietary tag
 * serialisation, and the polymorphic {@code getSummary()} method.</p>
 *
 * @author Ahaan
 */
class DonationEntityTest {

    private Donation donation;

    @BeforeEach
    void setUp() {
        donation = new Donation();
        donation.setDonationId("HH-TEST01-ABCD");
        donation.setFoodName("Canned Beans");
        donation.setCategory("canned");
        donation.setQuantity(10);
        donation.setUnit("cans");
        donation.setDescription("Black beans, organic");
        donation.setExpiryDate(LocalDate.now().plusDays(30));
        donation.setStorage("room-temp");
        donation.setDonorName("Test Donor");
        donation.setDonorEmail("test@example.com");
        donation.setDonorZip("92101");
        donation.setStatus("active");
    }

    @Test
    @DisplayName("Donation should not be expired when expiry is in the future")
    void testNotExpired() {
        assertFalse(donation.isExpired());
    }

    @Test
    @DisplayName("Donation should be expired when expiry is in the past")
    void testIsExpired() {
        donation.setExpiryDate(LocalDate.now().minusDays(1));
        assertTrue(donation.isExpired());
    }

    @Test
    @DisplayName("daysUntilExpiry returns correct positive value")
    void testDaysUntilExpiry() {
        donation.setExpiryDate(LocalDate.now().plusDays(5));
        assertEquals(5, donation.daysUntilExpiry());
    }

    @Test
    @DisplayName("daysUntilExpiry returns negative for expired items")
    void testDaysUntilExpiryNegative() {
        donation.setExpiryDate(LocalDate.now().minusDays(3));
        assertEquals(-3, donation.daysUntilExpiry());
    }

    @Test
    @DisplayName("Allergen list serialisation round-trip")
    void testAllergenListRoundTrip() {
        donation.setAllergenList(List.of("gluten", "dairy"));
        assertEquals("gluten,dairy", donation.getAllergens());
        assertEquals(List.of("gluten", "dairy"), donation.getAllergenList());
    }

    @Test
    @DisplayName("Empty allergen list returns empty list")
    void testEmptyAllergenList() {
        donation.setAllergens(null);
        assertTrue(donation.getAllergenList().isEmpty());

        donation.setAllergens("");
        assertTrue(donation.getAllergenList().isEmpty());
    }

    @Test
    @DisplayName("Dietary tag list serialisation round-trip")
    void testDietaryTagRoundTrip() {
        donation.setDietaryTagList(List.of("vegetarian", "organic"));
        assertEquals("vegetarian,organic", donation.getDietaryTags());
        assertEquals(List.of("vegetarian", "organic"), donation.getDietaryTagList());
    }

    @Test
    @DisplayName("getSummary includes donation ID and donor name (polymorphism)")
    void testGetSummary() {
        String summary = donation.getSummary();
        assertTrue(summary.contains("HH-TEST01-ABCD"));
        assertTrue(summary.contains("Test Donor"));
        assertTrue(summary.contains("Canned Beans"));
    }

    @Test
    @DisplayName("Status defaults to 'active'")
    void testDefaultStatus() {
        assertEquals("active", donation.getStatus());
    }

    @Test
    @DisplayName("setAllergenList with null sets allergens to null")
    void testSetAllergenListNull() {
        donation.setAllergenList(null);
        assertNull(donation.getAllergens());
    }
}
