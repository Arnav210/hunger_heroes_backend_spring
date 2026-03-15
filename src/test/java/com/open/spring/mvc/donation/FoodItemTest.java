package com.open.spring.mvc.donation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link FoodItem} abstract base class, tested via
 * the concrete {@link Donation} subclass.
 *
 * <p>Validates abstraction and inheritance: the abstract methods and
 * fields defined in FoodItem are exercised through Donation.</p>
 *
 * @author Ahaan
 */
class FoodItemTest {

    @Test
    @DisplayName("FoodItem.isExpired works via Donation subclass (inheritance)")
    void testIsExpiredViaInheritance() {
        // Demonstrates polymorphism: FoodItem reference, Donation instance
        FoodItem item = new Donation();
        item.setFoodName("Test Item");
        item.setCategory("canned");
        item.setQuantity(5);
        item.setUnit("cans");
        item.setStorage("room-temp");

        item.setExpiryDate(LocalDate.now().plusDays(10));
        assertFalse(item.isExpired(), "Future date should not be expired");

        item.setExpiryDate(LocalDate.now().minusDays(1));
        assertTrue(item.isExpired(), "Past date should be expired");
    }

    @Test
    @DisplayName("FoodItem.daysUntilExpiry with null returns MAX_VALUE")
    void testDaysUntilExpiryNull() {
        FoodItem item = new Donation();
        item.setFoodName("Null Expiry");
        item.setCategory("other");
        item.setQuantity(1);
        item.setUnit("items");
        item.setStorage("room-temp");
        item.setExpiryDate(null);
        // When expiry is null, should return MAX_VALUE
        assertEquals(Long.MAX_VALUE, item.daysUntilExpiry());
    }

    @Test
    @DisplayName("FoodItem.getSummary is overridden by Donation (polymorphism)")
    void testGetSummaryPolymorphism() {
        FoodItem base = new Donation();
        base.setFoodName("Milk");
        base.setQuantity(3);
        base.setUnit("gallons");
        base.setExpiryDate(LocalDate.now().plusDays(7));

        // Base getSummary
        String baseSummary = "Milk (3 gallons)";
        assertTrue(base.getSummary().contains(baseSummary));

        // Donation overridden getSummary includes donor info
        Donation d = (Donation) base;
        d.setDonationId("HH-POLY01-TEST");
        d.setDonorName("Poly Test");
        d.setDonorZip("00000");
        d.setStatus("active");

        String donationSummary = d.getSummary();
        assertTrue(donationSummary.contains("HH-POLY01-TEST"));
        assertTrue(donationSummary.contains("Poly Test"));
    }
}
