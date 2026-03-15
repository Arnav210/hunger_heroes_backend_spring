package com.open.spring.mvc.donation;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Abstract base class representing any food item in the Hunger Heroes system.
 * 
 * <p>Demonstrates <strong>abstraction</strong> and <strong>inheritance</strong>:
 * concrete food items (like {@link Donation}) extend this class and inherit
 * common properties such as name, category, quantity, and expiry tracking.</p>
 *
 * <p>Uses JPA {@code SINGLE_TABLE} inheritance strategy so all subtypes share
 * the same database table, keeping queries simple and fast.</p>
 *
 * <p><strong>Data Structures used:</strong></p>
 * <ul>
 *   <li>{@code String} category validated against an allowed {@link java.util.Set} of values</li>
 * </ul>
 *
 * @author Ahaan
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "item_type")
@Table(name = "food_items")
public abstract class FoodItem {

    /** Unique identifier for the food item. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable name of the food (e.g. "Canned Tomato Soup"). */
    @Column(nullable = false)
    private String foodName;

    /**
     * Category this food belongs to (e.g. "canned", "dairy", "bakery").
     * Validated against {@link DonationConstants#ALLOWED_CATEGORIES}.
     */
    @Column(nullable = false)
    private String category;

    /** Numeric quantity of the food item. */
    @Column(nullable = false)
    private int quantity;

    /** Unit of measurement (e.g. "cans", "lbs", "items"). */
    @Column(nullable = false)
    private String unit;

    /** Free-form description or notes about the food. */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Date after which the food should not be consumed. */
    @Column(nullable = false)
    private LocalDate expiryDate;

    /**
     * Storage requirement for the food (e.g. "room-temp", "refrigerated").
     */
    @Column(nullable = false)
    private String storage;

    /**
     * Checks whether this food item has expired relative to today's date.
     *
     * @return {@code true} if the expiry date is before today
     */
    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    /**
     * Returns the number of days remaining until expiry.
     * A negative value means the item has already expired.
     *
     * @return days until expiry (negative if expired)
     */
    public long daysUntilExpiry() {
        if (expiryDate == null) return Long.MAX_VALUE;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }

    /**
     * Template method for producing a human-readable summary.
     * Subclasses may override to add additional detail.
     *
     * @return a formatted summary string
     */
    public String getSummary() {
        return String.format("%s (%d %s) — expires %s", foodName, quantity, unit, expiryDate);
    }
}
