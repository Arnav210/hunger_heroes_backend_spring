package com.open.spring.mvc.donation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Centralised constants for the Hunger Heroes donation system.
 *
 * <p>Each allowed-value set is stored as an <strong>unmodifiable {@link Set}</strong>,
 * demonstrating use of the {@code Set} data structure for O(1) membership
 * checks throughout the codebase.</p>
 *
 * @author Ahaan
 * @version 1.0
 */
public final class DonationConstants {

    private DonationConstants() { /* utility class */ }

    // ────────── Categories ──────────
    /** Allowed food-category values (mirrors the Flask backend). */
    public static final Set<String> ALLOWED_CATEGORIES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "canned", "fresh-produce", "dairy", "bakery", "meat-protein",
        "grains", "beverages", "frozen", "snacks", "baby-food",
        "prepared-meals", "other"
    )));

    // ────────── Units ──────────
    /** Allowed unit-of-measure values. */
    public static final Set<String> ALLOWED_UNITS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "items", "lbs", "kg", "cans", "boxes", "bags", "bottles",
        "gallons", "liters", "oz", "servings", "packs"
    )));

    // ────────── Storage ──────────
    /** Allowed storage-type values. */
    public static final Set<String> ALLOWED_STORAGE = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "room-temp", "refrigerated", "frozen", "cool-dry"
    )));

    // ────────── Allergens ──────────
    /** Allowed allergen tag values. */
    public static final Set<String> ALLOWED_ALLERGENS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "gluten", "dairy", "nuts", "soy", "eggs", "shellfish", "fish", "none"
    )));

    // ────────── Dietary Tags ──────────
    /** Allowed dietary-tag values. */
    public static final Set<String> ALLOWED_DIETARY = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "vegetarian", "vegan", "halal", "kosher", "gluten-free",
        "organic", "sugar-free", "low-sodium"
    )));

    // ────────── Statuses ──────────
    /** Valid donation lifecycle statuses. */
    public static final Set<String> ALLOWED_STATUSES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "active", "accepted", "in-transit", "delivered", "expired", "cancelled"
    )));

    /** Category → emoji mapping used by the analytics endpoint. */
    public static final java.util.Map<String, String> CATEGORY_EMOJI;
    static {
        java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
        m.put("canned",         "🥫");
        m.put("fresh-produce",  "🥬");
        m.put("dairy",          "🧀");
        m.put("bakery",         "🍞");
        m.put("meat-protein",   "🥩");
        m.put("grains",         "🌾");
        m.put("beverages",      "🥤");
        m.put("frozen",         "❄️");
        m.put("snacks",         "🍪");
        m.put("baby-food",      "🍼");
        m.put("prepared-meals", "🍱");
        m.put("other",          "📦");
        CATEGORY_EMOJI = Collections.unmodifiableMap(m);
    }
}
